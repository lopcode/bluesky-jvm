package com.lopcode.bluesky.jetstream;

import com.github.luben.zstd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;

public class Jetstream {

    private final Logger logger = LoggerFactory.getLogger(Jetstream.class);

    public void start() throws IOException {
        var decompressDict = loadJetstreamZstdDictionary();

        var counter = new AtomicInteger(0);
        var threshold = 100_000;
        Thread.startVirtualThread(() -> {
            while (true) {
                if (counter.get() >= threshold) {
                    logger.info("exiting after {} messages consumed", threshold);
                    System.exit(0);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        try (var client = HttpClient.newHttpClient()) {
            client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://jetstream2.us-east.bsky.network/subscribe?compress=true"), new WebSocket.Listener() {
                    @Override
                    public void onOpen(WebSocket webSocket) {
                        logger.info("websocket opened");
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
                        try (var stream = new ZstdBufferDecompressingStream(data).setDict(decompressDict)) {
                            logDecompressedMessage(counter.getAndIncrement(), stream);
                            return WebSocket.Listener.super.onBinary(webSocket, data, last);
                        } catch (IOException exception) {
                            throw new RuntimeException(exception);
                        }
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

    void logDecompressedMessage(int count, ZstdBufferDecompressingStream stream) {
        var stringBuilder = new StringBuilder();
        var decompressBuffer = ByteBuffer.allocate(4096);
        while (stream.hasRemaining()) {
            try {
                decompressBuffer.clear();
                var readCount = stream.read(decompressBuffer);
                if (readCount <= 0) {
                    return;
                }
            } catch (ZstdIOException e) {
                logger.error("zstd exception with code {}", e.getErrorCode(), e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            decompressBuffer.flip();
            stringBuilder.append(StandardCharsets.UTF_8.decode(decompressBuffer));
        }
        var text = stringBuilder.toString();
        logger.info("{}: {}", count, text);
    }
}
