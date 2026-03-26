package com.shadowproxy.util;

import com.shadowproxy.domain.http.HttpRequestRecord;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpMessageCodec {
    private HttpMessageCodec() {
    }

    public static String toRawRequest(HttpRequestRecord requestRecord) {
        StringBuilder builder = new StringBuilder();
        builder.append(requestRecord.method()).append(" ").append(requestRecord.url()).append(" HTTP/1.1\n");
        requestRecord.headers().forEach((k, v) -> builder.append(k).append(": ").append(v).append("\n"));
        builder.append("\n");
        if (requestRecord.body() != null && requestRecord.body().length > 0) {
            builder.append(new String(requestRecord.body(), StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    public static HttpRequestRecord parseRawRequest(String raw) {
        String[] parts = raw.split("\\n\\n", 2);
        String head = parts.length > 0 ? parts[0] : "";
        String bodyPart = parts.length > 1 ? parts[1] : "";
        String[] lines = head.split("\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new IllegalArgumentException("Invalid raw request");
        }
        String[] requestLine = lines[0].trim().split("\\s+", 3);
        if (requestLine.length < 2) {
            throw new IllegalArgumentException("Invalid request line: " + lines[0]);
        }
        String method = requestLine[0];
        String url = requestLine[1];
        Map<String, String> headers = new LinkedHashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int idx = line.indexOf(':');
            if (idx > 0) {
                headers.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
            }
        }
        return new HttpRequestRecord(
                method,
                url,
                headers,
                bodyPart.getBytes(StandardCharsets.UTF_8),
                Instant.now()
        );
    }
}
