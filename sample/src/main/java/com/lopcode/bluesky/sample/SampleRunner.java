package com.lopcode.bluesky.sample;

import com.lopcode.bluesky.jetstream.Jetstream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class SampleRunner {

    private static Logger logger = LoggerFactory.getLogger(SampleRunner.class);

    public static void main(String[] args) throws IOException {
        logger.info("hello, bluesky");
        new Jetstream().start();
        logger.info("ended");
    }
}