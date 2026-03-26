package com.shadowproxy.core.intruder;

import java.time.Duration;

public record IntruderAttackResult(
        int requestNumber,
        String payloadSummary,
        int statusCode,
        int length,
        Duration timeTaken,
        String error
) {
}
