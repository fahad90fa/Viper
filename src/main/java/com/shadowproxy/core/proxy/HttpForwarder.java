package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;

import java.util.concurrent.CompletableFuture;

public interface HttpForwarder {
    CompletableFuture<HttpResponseRecord> forward(HttpRequestRecord requestRecord);
}
