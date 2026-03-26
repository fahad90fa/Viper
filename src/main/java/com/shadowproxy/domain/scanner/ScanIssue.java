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
        int occurrences,
        Instant createdAt
) {
    public ScanIssue(UUID id,
                     String name,
                     IssueSeverity severity,
                     IssueConfidence confidence,
                     String url,
                     String description,
                     String remediation,
                     String evidence,
                     Instant createdAt) {
        this(id, name, severity, confidence, url, description, remediation, evidence, 1, createdAt);
    }

    public ScanIssue withOccurrences(int occurrences) {
        return new ScanIssue(id, name, severity, confidence, url, description, remediation, evidence, occurrences, createdAt);
    }
}
