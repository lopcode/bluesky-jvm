package com.lopcode.bluesky.jetstream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
import com.github.luben.zstd.ZstdDecompressCtx;
import com.github.luben.zstd.ZstdDictDecompress;
import com.github.luben.zstd.ZstdException;
import com.lopcode.bluesky.jetstream.model.JetstreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Jetstream {

    private final Logger logger = LoggerFactory.getLogger(Jetstream.class);
    private final int maxWebsocketFrameBufferSizeBytes = 16 * 1024 * 1000;
    private final ByteBuffer frameBuffer = ByteBuffer.allocateDirect(maxWebsocketFrameBufferSizeBytes);
    private final ByteBuffer decompressBuffer = ByteBuffer.allocateDirect(maxWebsocketFrameBufferSizeBytes);
    private final JsonMapper jsonMapper = JsonMapper.builder()
        .addModule(new BlackbirdModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    private Instant connectedAt;

    public void start() throws IOException {
        var decompressDict = loadJetstreamZstdDictionary();
        var decompressContext = new ZstdDecompressCtx();
        decompressContext.loadDict(decompressDict);

        var counter = new AtomicInteger(0);
        var threshold = 100_000;

        var latch = new CountDownLatch(threshold);
        Thread.startVirtualThread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                return;
            }
            logger.info("exiting after {} messages consumed", threshold);
            var endedAt = Instant.now(Clock.systemUTC());
            var duration = Duration.between(connectedAt, endedAt);
            var throughput = ((double) threshold / duration.toMillis()) * 1000;
            logger.info("throughput: {}/s", throughput);
            System.exit(0);
        });

        try (var client = HttpClient.newBuilder().build()) {
            client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://jetstream2.us-east.bsky.network/subscribe?compress=true&maxMessageSizeBytes=" + maxWebsocketFrameBufferSizeBytes), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        logger.info("websocket opened");
                        connectedAt = Instant.now(Clock.systemUTC());
                        WebSocket.Listener.super.onOpen(webSocket);
                    }

                    @Override
                    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                        logger.info(data.toString());
                        return WebSocket.Listener.super.onText(webSocket, data, last);
                    }

                    @Override
                    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
                        logger.info("ping: {}", message);
                        return WebSocket.Listener.super.onPing(webSocket, message);
                    }

                    @Override
                    public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                        logger.info("pong: {}", message);
                        return WebSocket.Listener.super.onPong(webSocket, message);
                    }

                    @Override
                    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                        logger.info("closed: {} {}", statusCode, reason);
                        return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                    }

                    @Override
                    public void onError(WebSocket webSocket, Throwable error) {
                        logger.info("error", error);
                        WebSocket.Listener.super.onError(webSocket, error);
                    }

                    @Override
                    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                        if (latch.getCount() <= 0) {
                            return WebSocket.Listener.super.onBinary(webSocket, data, last);
                        }
                        frameBuffer.put(data);
                        if (!last) {
                            logger.info("buffered frame");
                            return WebSocket.Listener.super.onBinary(webSocket, data, last);
                        }
                        frameBuffer.flip();
                        try {
                            logDecompressedMessage(counter.getAndIncrement(), decompressContext);
                            latch.countDown();
                        } finally {
                            frameBuffer.clear();
                        }
                        return WebSocket.Listener.super.onBinary(webSocket, data, last);
                    }
                }).join();
        }
    }

    private ZstdDictDecompress loadJetstreamZstdDictionary() {
        var stream = this.getClass().getClassLoader().getResourceAsStream("zstd_dictionary");
        byte[] bytes;
        try (stream) {
            bytes = Objects.requireNonNull(stream).readAllBytes();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        return new ZstdDictDecompress(bytes);
    }

    void logDecompressedMessage(
        int messageId,
        ZstdDecompressCtx context
    ) {
        try {
            var readCount = context.decompress(decompressBuffer, frameBuffer);
            if (readCount <= 0) {
                return;
            }
        } catch (ZstdException e) {
            logger.error("zstd exception with code {}", e.getErrorCode());
            throw new RuntimeException(e);
        }
        decompressBuffer.flip();
        String text;
        try {
            text = StandardCharsets.UTF_8.decode(decompressBuffer).toString();
        } finally {
            decompressBuffer.clear();
        }
        JetstreamEvent event;
        try {
            event = jsonMapper.readValue(text, JetstreamEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        logger.info("{}: {}", messageId, event);
    }
}
