package com.shadowproxy.domain.http;

import java.time.Instant;
import java.util.Map;

public record HttpResponseRecord(
        int statusCode,
        String reasonPhrase,
        Map<String, String> headers,
        byte[] body,
        Instant capturedAt
) {
}
