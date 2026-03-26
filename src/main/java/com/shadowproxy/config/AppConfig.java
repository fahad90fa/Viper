package com.shadowproxy.config;

import java.nio.file.Path;
import java.util.Set;

public record AppConfig(
        String listenHost,
        int listenPort,
        Path dataDirectory,
        Path certificateDirectory,
        boolean mitmEnabled,
        Set<String> sslPassthroughHosts
) {
    public static AppConfig defaultConfig() {
        Path dataDir = Path.of(System.getProperty("user.home"), ".shadowproxy");
        return new AppConfig("127.0.0.1", 8080, dataDir, dataDir.resolve("certs"), true, Set.of());
    }
}
