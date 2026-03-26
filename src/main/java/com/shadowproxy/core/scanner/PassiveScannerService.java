package com.shadowproxy.core.scanner;

import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.scanner.IssueConfidence;
import com.shadowproxy.domain.scanner.IssueSeverity;
import com.shadowproxy.domain.scanner.ScanIssue;
import com.shadowproxy.persistence.HistoryListener;
import com.shadowproxy.persistence.HistoryRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PassiveScannerService implements HistoryListener {
    private final HistoryRepository historyRepository;
    private final ScanIssueStore issueStore;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    public PassiveScannerService(HistoryRepository historyRepository, ScanIssueStore issueStore) {
        this.historyRepository = historyRepository;
        this.issueStore = issueStore;
    }

    public void start() {
        historyRepository.addListener(this);
    }

    @Override
    public void onExchangeSaved(HttpExchangeRecord exchangeRecord) {
        executor.submit(() -> analyze(exchangeRecord));
    }

    private void analyze(HttpExchangeRecord exchangeRecord) {
        String body = new String(exchangeRecord.response().body(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        String url = exchangeRecord.request().url();
        if (body.contains("sql syntax") || body.contains("sqlstate") || body.contains("you have an error in your sql syntax")) {
            issueStore.save(new ScanIssue(
                    UUID.randomUUID(),
                    "Potential SQL Error Disclosure",
                    IssueSeverity.MEDIUM,
                    IssueConfidence.FIRM,
                    url,
                    "Response indicates SQL engine error details.",
                    "Handle database exceptions and avoid returning raw SQL error messages.",
                    "Matched SQL error pattern in response body.",
                    Instant.now()
            ));
        }
        if (url.startsWith("http://") && exchangeRecord.request().headers().containsKey("Authorization")) {
            issueStore.save(new ScanIssue(
                    UUID.randomUUID(),
                    "Authorization header over cleartext transport",
                    IssueSeverity.HIGH,
                    IssueConfidence.CERTAIN,
                    url,
                    "Credentials/token appears on non-TLS request.",
                    "Enforce HTTPS and HSTS for authenticated endpoints.",
                    "Authorization header seen on URL: " + url,
                    Instant.now()
            ));
        }
        String setCookie = exchangeRecord.response().headers().getOrDefault("set-cookie",
                exchangeRecord.response().headers().getOrDefault("Set-Cookie", ""));
        if (!setCookie.isBlank() && !setCookie.toLowerCase(Locale.ROOT).contains("httponly")) {
            issueStore.save(new ScanIssue(
                    UUID.randomUUID(),
                    "Cookie missing HttpOnly",
                    IssueSeverity.LOW,
                    IssueConfidence.FIRM,
                    url,
                    "Set-Cookie header does not contain HttpOnly.",
                    "Mark session cookies as HttpOnly.",
                    setCookie,
                    Instant.now()
            ));
        }
        if (exchangeRecord.response().headers().containsKey("Access-Control-Allow-Origin")
                && "*".equals(exchangeRecord.response().headers().get("Access-Control-Allow-Origin"))
                && "true".equalsIgnoreCase(exchangeRecord.response().headers().getOrDefault("Access-Control-Allow-Credentials", ""))) {
            issueStore.save(new ScanIssue(
                    UUID.randomUUID(),
                    "Potential CORS misconfiguration",
                    IssueSeverity.MEDIUM,
                    IssueConfidence.FIRM,
                    url,
                    "Wildcard ACAO is combined with credentials support.",
                    "Use explicit trusted origins and avoid wildcard with credentials.",
                    "ACAO=* and ACAC=true",
                    Instant.now()
            ));
        }
    }
}
