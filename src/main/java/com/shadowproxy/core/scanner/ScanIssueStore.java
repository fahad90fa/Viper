package com.shadowproxy.core.scanner;

import com.shadowproxy.domain.scanner.ScanIssue;

import java.util.List;
import java.util.UUID;

public interface ScanIssueStore {
    void save(ScanIssue scanIssue);

    List<ScanIssue> findAll();

    boolean exists(UUID id);
}
