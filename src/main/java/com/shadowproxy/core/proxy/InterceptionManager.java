package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class InterceptionManager {
    private final BlockingQueue<PendingIntercept> pendingQueue = new LinkedBlockingQueue<>();
    private final Map<UUID, PendingIntercept> pendingById = new ConcurrentHashMap<>();
    private volatile boolean interceptEnabled = true;

    public HttpRequestRecord awaitInterceptionDecision(HttpRequestRecord requestRecord) {
        if (!interceptEnabled) {
            return requestRecord;
        }
        PendingIntercept pending = new PendingIntercept(
                UUID.randomUUID(),
                requestRecord,
                java.time.Instant.now(),
                new CompletableFuture<>()
        );
        pendingById.put(pending.id(), pending);
        pendingQueue.offer(pending);
        InterceptDecision decision = pending.decisionFuture().join();
        pendingById.remove(pending.id());
        if (decision.action() == InterceptAction.DROP) {
            return null;
        }
        return decision.requestRecord();
    }

    public PendingIntercept pollPending(long timeout, TimeUnit unit) throws InterruptedException {
        return pendingQueue.poll(timeout, unit);
    }

    public void forward(UUID pendingId, HttpRequestRecord editedRequest) {
        PendingIntercept pending = pendingById.get(pendingId);
        if (pending != null) {
            pending.decisionFuture().complete(InterceptDecision.forward(editedRequest));
        }
    }

    public void drop(UUID pendingId) {
        PendingIntercept pending = pendingById.get(pendingId);
        if (pending != null) {
            pending.decisionFuture().complete(InterceptDecision.drop());
        }
    }

    public boolean isInterceptEnabled() {
        return interceptEnabled;
    }

    public void setInterceptEnabled(boolean interceptEnabled) {
        this.interceptEnabled = interceptEnabled;
    }
}
