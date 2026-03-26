package com.shadowproxy.persistence;

import com.shadowproxy.domain.http.HttpExchangeRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryHistoryRepository implements HistoryRepository {
    private final CopyOnWriteArrayList<HttpExchangeRecord> items = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<HistoryListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void save(HttpExchangeRecord exchange) {
        items.add(exchange);
        listeners.forEach(listener -> listener.onExchangeSaved(exchange));
    }

    @Override
    public List<HttpExchangeRecord> findAll() {
        return List.copyOf(items);
    }

    @Override
    public Optional<HttpExchangeRecord> findById(UUID id) {
        return items.stream().filter(item -> item.id().equals(id)).findFirst();
    }

    @Override
    public void clear() {
        items.clear();
    }

    @Override
    public void addListener(HistoryListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(HistoryListener listener) {
        listeners.remove(listener);
    }
}
