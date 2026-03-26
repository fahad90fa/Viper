package com.shadowproxy.persistence;

import com.shadowproxy.domain.http.HttpExchangeRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HistoryRepository {
    void save(HttpExchangeRecord exchange);

    List<HttpExchangeRecord> findAll();

    Optional<HttpExchangeRecord> findById(UUID id);

    void clear();

    void addListener(HistoryListener listener);

    void removeListener(HistoryListener listener);
}
