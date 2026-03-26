package com.shadowproxy.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.shadowproxy.core.scanner.ScanIssueStore;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.project.ProjectSnapshot;
import com.shadowproxy.domain.project.ProjectType;
import com.shadowproxy.domain.scanner.ScanIssue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectManager {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    private final HistoryRepository historyRepository;
    private final ScanIssueStore scanIssueStore;

    public ProjectManager(HistoryRepository historyRepository, ScanIssueStore scanIssueStore) {
        this.historyRepository = historyRepository;
        this.scanIssueStore = scanIssueStore;
    }

    public void save(Path projectFile, String name) {
        try {
            Path parent = projectFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ProjectSnapshot snapshot = new ProjectSnapshot(
                    UUID.randomUUID(),
                    name,
                    ProjectType.DISK,
                    Instant.now(),
                    historyRepository.findAll(),
                    scanIssueStore.findAll()
            );
            MAPPER.writeValue(projectFile.toFile(), snapshot);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save project", e);
        }
    }

    public ProjectSnapshot load(Path projectFile) {
        try {
            ProjectSnapshot snapshot = MAPPER.readValue(projectFile.toFile(), ProjectSnapshot.class);
            historyRepository.clear();
            scanIssueStore.clear();
            for (HttpExchangeRecord exchange : snapshot.history()) {
                historyRepository.save(exchange);
            }
            for (ScanIssue issue : snapshot.issues()) {
                for (int i = 0; i < Math.max(1, issue.occurrences()); i++) {
                    scanIssueStore.save(issue);
                }
            }
            return snapshot;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to open project", e);
        }
    }
}
