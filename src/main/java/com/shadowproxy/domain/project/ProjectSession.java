package com.shadowproxy.domain.project;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

public record ProjectSession(
        UUID id,
        String name,
        ProjectType type,
        Path projectPath,
        Instant createdAt
) {
}
