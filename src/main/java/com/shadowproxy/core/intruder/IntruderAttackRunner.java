package com.shadowproxy.core.intruder;

import com.shadowproxy.core.proxy.JavaHttpForwarder;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;
import com.shadowproxy.util.HttpMessageCodec;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class IntruderAttackRunner {
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final JavaHttpForwarder forwarder = new JavaHttpForwarder();

    public CompletableFuture<Void> run(String rawRequest,
                                       IntruderAttackType attackType,
                                       List<String> payloads,
                                       Consumer<IntruderAttackResult> resultConsumer,
                                       Runnable onComplete,
                                       AtomicBoolean cancelled) {
        return CompletableFuture.runAsync(() -> {
            List<IntruderGeneratedRequest> attacks = IntruderPayloadGenerator.buildPayloadRequests(rawRequest, attackType, payloads);
            int index = 1;
            for (IntruderGeneratedRequest attack : attacks) {
                if (cancelled.get()) {
                    break;
                }
                Instant start = Instant.now();
                try {
                    HttpRequestRecord request = HttpMessageCodec.parseRawRequest(attack.rawRequest());
                    HttpResponseRecord response = forwarder.forward(request).join();
                    Duration elapsed = Duration.between(start, Instant.now());
                    resultConsumer.accept(new IntruderAttackResult(
                            index,
                            attack.payloadSummary(),
                            response.statusCode(),
                            response.body() == null ? 0 : response.body().length,
                            elapsed,
                            ""
                    ));
                } catch (Exception ex) {
                    Duration elapsed = Duration.between(start, Instant.now());
                    resultConsumer.accept(new IntruderAttackResult(
                            index,
                            attack.payloadSummary(),
                            -1,
                            0,
                            elapsed,
                            ex.getMessage()
                    ));
                }
                index++;
            }
        }, executor).whenComplete((ignored, throwable) -> onComplete.run());
    }

}
