package com.shadowproxy.core.scanner;

import com.shadowproxy.domain.scanner.ScanIssue;

import java.util.List;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryScanIssueStore implements ScanIssueStore {
    private final Map<String, ScanIssue> items = new LinkedHashMap<>();

    @Override
    public synchronized void save(ScanIssue scanIssue) {
        String key = issueKey(scanIssue);
        ScanIssue existing = items.get(key);
        if (existing == null) {
            items.put(key, scanIssue);
            return;
        }
        items.put(key, existing.withOccurrences(existing.occurrences() + 1));
    }

    @Override
    public synchronized List<ScanIssue> findAll() {
        return List.copyOf(items.values());
    }

    @Override
    public synchronized boolean exists(UUID id) {
        return items.values().stream().anyMatch(item -> item.id().equals(id));
    }

    @Override
    public synchronized void clear() {
        items.clear();
    }

    private String issueKey(ScanIssue issue) {
        return String.join("|",
                safe(issue.name()),
                safe(issue.severity().name()),
                safe(issue.confidence().name()),
                safe(issue.url()),
                safe(issue.description()),
                safe(issue.remediation()),
                safe(issue.evidence()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
