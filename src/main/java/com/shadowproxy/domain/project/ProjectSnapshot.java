package com.shadowproxy.domain.project;

import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.scanner.ScanIssue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProjectSnapshot(
        UUID id,
        String name,
        ProjectType type,
        Instant createdAt,
        List<HttpExchangeRecord> history,
        List<ScanIssue> issues
) {
}
