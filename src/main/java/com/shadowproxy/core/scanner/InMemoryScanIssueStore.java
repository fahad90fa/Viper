package com.shadowproxy.core.scanner;

import com.shadowproxy.domain.scanner.ScanIssue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryScanIssueStore implements ScanIssueStore {
    private final CopyOnWriteArrayList<ScanIssue> items = new CopyOnWriteArrayList<>();

    @Override
    public void save(ScanIssue scanIssue) {
        items.add(scanIssue);
    }

    @Override
    public List<ScanIssue> findAll() {
        return List.copyOf(items);
    }

    @Override
    public boolean exists(UUID id) {
        return items.stream().anyMatch(item -> item.id().equals(id));
    }
}
