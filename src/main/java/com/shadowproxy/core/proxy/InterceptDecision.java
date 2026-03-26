package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;

public record InterceptDecision(
        InterceptAction action,
        HttpRequestRecord requestRecord
) {
    public static InterceptDecision forward(HttpRequestRecord requestRecord) {
        return new InterceptDecision(InterceptAction.FORWARD, requestRecord);
    }

    public static InterceptDecision drop() {
        return new InterceptDecision(InterceptAction.DROP, null);
    }
}
