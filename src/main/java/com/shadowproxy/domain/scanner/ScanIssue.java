package com.shadowproxy.domain.scanner;

import java.time.Instant;
import java.util.UUID;

public record ScanIssue(
        UUID id,
        String name,
        IssueSeverity severity,
        IssueConfidence confidence,
        String url,
        String description,
        String remediation,
        String evidence,
        Instant createdAt
) {
}
