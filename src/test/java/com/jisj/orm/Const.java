package com.jisj.orm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Const {
    /**
     * Path to resources: "src/test/resources"
     */
    public static final Path RES_PATH = Path.of("src/test/resources");
    /**
     * Path to test results: "target/test-results"
     */
    public static final Path TST_PATH = Path.of("target/test-results");

    static {
        try {
            Files.createDirectories(TST_PATH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
