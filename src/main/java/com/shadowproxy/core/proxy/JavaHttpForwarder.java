package com.shadowproxy.core.proxy;

import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class JavaHttpForwarder implements HttpForwarder {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private final HttpClient httpClient;

    public JavaHttpForwarder() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Override
    public CompletableFuture<HttpResponseRecord> forward(HttpRequestRecord requestRecord) {
        URI targetUri = resolveTargetUri(requestRecord);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(REQUEST_TIMEOUT);

        requestRecord.headers().forEach((name, value) -> {
            if (!isRestrictedHeader(name)) {
                builder.header(name, value);
            }
        });

        byte[] body = requestRecord.body() == null ? new byte[0] : requestRecord.body();
        HttpRequest.BodyPublisher publisher = body.length == 0
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body);
        builder.method(requestRecord.method(), publisher);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(response -> toResponseRecord(response, Instant.now()));
    }

    private URI resolveTargetUri(HttpRequestRecord requestRecord) {
        String raw = requestRecord.url();
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            return URI.create(raw);
        }
        String host = requestRecord.headers().getOrDefault("Host", requestRecord.headers().get("host"));
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Proxy request missing Host header for relative URI: " + raw);
        }
        String normalizedPath = raw.startsWith("/") ? raw : "/" + raw;
        return URI.create("http://" + host + normalizedPath);
    }

    private HttpResponseRecord toResponseRecord(HttpResponse<byte[]> response, Instant capturedAt) {
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : response.headers().map().entrySet()) {
            headers.put(entry.getKey(), String.join(", ", entry.getValue()));
        }
        return new HttpResponseRecord(
                response.statusCode(),
                "",
                headers,
                response.body(),
                capturedAt
        );
    }

    private boolean isRestrictedHeader(String headerName) {
        return "host".equalsIgnoreCase(headerName)
                || "content-length".equalsIgnoreCase(headerName)
                || "connection".equalsIgnoreCase(headerName)
                || "proxy-connection".equalsIgnoreCase(headerName)
                || "upgrade".equalsIgnoreCase(headerName)
                || "transfer-encoding".equalsIgnoreCase(headerName);
    }
}
