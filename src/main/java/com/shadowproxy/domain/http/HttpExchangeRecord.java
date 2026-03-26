package com.shadowproxy.domain.http;

import java.util.UUID;

public record HttpExchangeRecord(
        UUID id,
        HttpRequestRecord request,
        HttpResponseRecord response,
        String sourceTool
) {
}
