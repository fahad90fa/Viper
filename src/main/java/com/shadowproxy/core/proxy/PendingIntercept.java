package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public record PendingIntercept(
        UUID id,
        HttpRequestRecord originalRequest,
        Instant queuedAt,
        CompletableFuture<InterceptDecision> decisionFuture
) {
}
