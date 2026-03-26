package com.shadowproxy.domain.http;

import java.time.Instant;
import java.util.Map;

public record HttpRequestRecord(
        String method,
        String url,
        Map<String, String> headers,
        byte[] body,
        Instant capturedAt
) {
}
