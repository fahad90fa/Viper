package com.shadowproxy;

import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;
import com.shadowproxy.persistence.InMemoryHistoryRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryHistoryRepositoryTest {
    @Test
    void storesAndFindsById() {
        InMemoryHistoryRepository repository = new InMemoryHistoryRepository();
        HttpExchangeRecord record = new HttpExchangeRecord(
                UUID.randomUUID(),
                new HttpRequestRecord("GET", "https://example.org", Map.of(), new byte[0], Instant.now()),
                new HttpResponseRecord(200, "OK", Map.of(), new byte[0], Instant.now()),
                "TEST"
        );

        repository.save(record);

        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(record.id()).isPresent());
    }
}
