package com.shadowproxy.core.scanner;

import com.shadowproxy.core.proxy.JavaHttpForwarder;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;
import com.shadowproxy.domain.scanner.IssueConfidence;
import com.shadowproxy.domain.scanner.IssueSeverity;
import com.shadowproxy.domain.scanner.ScanIssue;
import com.shadowproxy.persistence.HistoryRepository;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.nio.charset.StandardCharsets;

public final class ActiveScannerService {
    private final HistoryRepository historyRepository;
    private final ScanIssueStore issueStore;
    private final JavaHttpForwarder forwarder = new JavaHttpForwarder();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ActiveScannerService(HistoryRepository historyRepository, ScanIssueStore issueStore) {
        this.historyRepository = historyRepository;
        this.issueStore = issueStore;
    }

    public CompletableFuture<Void> scanTargets(List<String> targets,
                                                Consumer<ScanUpdate> progressConsumer,
                                                Consumer<String> activityConsumer,
                                                AtomicBoolean paused,
                                                AtomicBoolean cancelled) {
        return CompletableFuture.runAsync(() -> {
            List<ScanProbe> probes = expandTargets(targets);
            int total = Math.max(probes.size(), 1);
            int completed = 0;
            for (ScanProbe probe : probes) {
                if (cancelled.get()) {
                    activityConsumer.accept("Scan cancelled.");
                    break;
                }
                while (paused.get() && !cancelled.get()) {
                    sleep(250);
                }
                long start = System.nanoTime();
                activityConsumer.accept("Scanning " + probe.label());
                try {
                    HttpRequestRecord request = probe.request();
                    HttpResponseRecord response = forwarder.forward(request).join();
                    HttpExchangeRecord exchange = new HttpExchangeRecord(UUID.randomUUID(), request, response, "SCANNER");
                    historyRepository.save(exchange);
                    runActiveChecks(exchange, probe);
                    activityConsumer.accept("Completed " + probe.label() + " (" + response.statusCode() + ")");
                } catch (Exception ex) {
                    activityConsumer.accept("Failed " + probe.label() + ": " + ex.getMessage());
                }
                completed++;
                long elapsedMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
                progressConsumer.accept(new ScanUpdate(probe.label(), completed, total, elapsedMs, cancelled.get()));
            }
        }, executor);
    }

    public List<String> buildTargetsFromHistory() {
        Set<String> urls = new LinkedHashSet<>();
        for (HttpExchangeRecord exchange : historyRepository.findAll()) {
            String url = exchange.request().url();
            if (url != null && !url.isBlank()) {
                urls.add(url);
            }
        }
        return new ArrayList<>(urls);
    }

    private HttpRequestRecord buildRequest(String url) {
        String normalized = normalizeScanUrl(url);
        return new HttpRequestRecord("GET", normalized, java.util.Map.of("User-Agent", "ShadowProxy-Scanner", "Accept", "*/*"), new byte[0], Instant.now());
    }

    private List<ScanProbe> expandTargets(List<String> targets) {
        List<ScanProbe> probes = new ArrayList<>();
        for (String target : targets) {
            HttpRequestRecord baseline = buildRequest(target);
            probes.add(new ScanProbe(target, baseline, null));
            probes.addAll(buildParameterProbes(baseline));
        }
        return probes;
    }

    private List<ScanProbe> buildParameterProbes(HttpRequestRecord baseline) {
        List<ScanProbe> probes = new ArrayList<>();
        try {
            URI uri = URI.create(baseline.url());
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                URI mutated = appendProbeParameter(uri, "shadowproxy_probe");
                probes.add(new ScanProbe(mutated.toString(), copyRequest(baseline, mutated.toString()), "shadowproxy_probe"));
                return probes;
            }

            String[] parts = query.split("&");
            for (int i = 0; i < parts.length; i++) {
                String[] nameValue = parts[i].split("=", 2);
                String name = decode(nameValue[0]);
                String probeValue = "shadowproxy-probe-" + (i + 1);
                URI mutated = replaceQueryParameter(uri, name, probeValue);
                probes.add(new ScanProbe(mutated.toString(), copyRequest(baseline, mutated.toString()), name));
            }
        } catch (Exception ex) {
            probes.add(new ScanProbe(baseline.url(), baseline, null));
        }
        return probes;
    }

    private URI appendProbeParameter(URI uri, String parameterName) {
        String query = uri.getRawQuery();
        String separator = (query == null || query.isBlank()) ? "" : "&";
        String probeQuery = (query == null || query.isBlank() ? "" : query + separator)
                + URLEncoder.encode(parameterName, StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode("shadowproxy-probe", StandardCharsets.UTF_8);
        return rebuildUri(uri, probeQuery);
    }

    private URI replaceQueryParameter(URI uri, String parameterName, String probeValue) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return appendProbeParameter(uri, parameterName);
        }
        String encodedName = URLEncoder.encode(parameterName, StandardCharsets.UTF_8);
        StringBuilder rebuilt = new StringBuilder();
        for (String part : query.split("&")) {
            if (rebuilt.length() > 0) {
                rebuilt.append('&');
            }
            String[] nameValue = part.split("=", 2);
            String currentName = nameValue.length > 0 ? decode(nameValue[0]) : "";
            if (parameterName.equals(currentName)) {
                rebuilt.append(encodedName).append('=').append(URLEncoder.encode(probeValue, StandardCharsets.UTF_8));
            } else {
                rebuilt.append(part);
            }
        }
        return rebuildUri(uri, rebuilt.toString());
    }

    private URI rebuildUri(URI uri, String query) {
        return URI.create((uri.getScheme() == null ? "https" : uri.getScheme())
                + "://"
                + uri.getAuthority()
                + uri.getPath()
                + (query == null || query.isBlank() ? "" : "?" + query)
                + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment()));
    }

    private HttpRequestRecord copyRequest(HttpRequestRecord original, String url) {
        return new HttpRequestRecord(
                original.method(),
                url,
                original.headers(),
                original.body(),
                Instant.now()
        );
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String normalizeScanUrl(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null) {
                return "https://" + url;
            }
            return url;
        } catch (Exception ex) {
            return "https://" + url;
        }
    }

    private void runActiveChecks(HttpExchangeRecord exchange, ScanProbe probe) {
        String body = new String(exchange.response() == null ? new byte[0] : exchange.response().body(), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        if (body.contains("reflected") || body.contains("shadowproxy-probe")) {
            String probeLabel = probe.parameterName() == null ? probe.label() : probe.parameterName();
            saveIssue("Reflected input detected", IssueSeverity.MEDIUM, IssueConfidence.FIRM, probe.label(),
                    "Response appears to reflect the probe payload for " + probeLabel + ".",
                    "Validate and encode reflected input.",
                    "Probe marker found in body for " + probeLabel + ".");
        }
        if (exchange.response() != null) {
            String server = exchange.response().headers().getOrDefault("Server", exchange.response().headers().getOrDefault("server", ""));
            if (server.isBlank()) {
                saveIssue("Missing Server header", IssueSeverity.INFO, IssueConfidence.TENTATIVE, probe.label(),
                        "Server header is not present.", "Not always required, but make an explicit choice.",
                        "No Server header observed.");
            }
            String csp = exchange.response().headers().getOrDefault("Content-Security-Policy", exchange.response().headers().getOrDefault("content-security-policy", ""));
            if (csp.isBlank()) {
                saveIssue("Missing Content-Security-Policy", IssueSeverity.LOW, IssueConfidence.FIRM, probe.label(),
                        "No CSP header was returned.", "Add a restrictive CSP to reduce XSS impact.",
                        "CSP header absent.");
            }
            String xfo = exchange.response().headers().getOrDefault("X-Frame-Options", exchange.response().headers().getOrDefault("x-frame-options", ""));
            if (xfo.isBlank()) {
                saveIssue("Missing X-Frame-Options", IssueSeverity.LOW, IssueConfidence.FIRM, probe.label(),
                        "No clickjacking protection header was returned.", "Set X-Frame-Options or frame-ancestors.", "XFO header absent.");
            }
            String hsts = exchange.response().headers().getOrDefault("Strict-Transport-Security", exchange.response().headers().getOrDefault("strict-transport-security", ""));
            if (probe.label().startsWith("https://") && hsts.isBlank()) {
                saveIssue("Missing Strict-Transport-Security", IssueSeverity.LOW, IssueConfidence.FIRM, probe.label(),
                        "HTTPS endpoint does not advertise HSTS.",
                        "Enable HSTS for HTTPS hosts after verifying all subdomains support TLS.",
                        "HSTS header absent.");
            }
            String xContentType = exchange.response().headers().getOrDefault("X-Content-Type-Options", exchange.response().headers().getOrDefault("x-content-type-options", ""));
            if (xContentType.isBlank()) {
                saveIssue("Missing X-Content-Type-Options", IssueSeverity.INFO, IssueConfidence.FIRM, probe.label(),
                        "Browser MIME sniffing protection is absent.",
                        "Set X-Content-Type-Options: nosniff.",
                        "X-Content-Type-Options header absent.");
            }
            String referrerPolicy = exchange.response().headers().getOrDefault("Referrer-Policy", exchange.response().headers().getOrDefault("referrer-policy", ""));
            if (referrerPolicy.isBlank()) {
                saveIssue("Missing Referrer-Policy", IssueSeverity.INFO, IssueConfidence.TENTATIVE, probe.label(),
                        "Referrer policy header is not present.",
                        "Set an explicit Referrer-Policy to limit URL leakage.",
                        "Referrer-Policy header absent.");
            }
        }
    }

    private void saveIssue(String name,
                           IssueSeverity severity,
                           IssueConfidence confidence,
                           String url,
                           String description,
                           String remediation,
                           String evidence) {
        issueStore.save(new ScanIssue(UUID.randomUUID(), name, severity, confidence, url, description, remediation, evidence, Instant.now()));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record ScanUpdate(String target, int completed, int total, long elapsedMs, boolean cancelled) {
        public int progressPercent() {
            return Math.min(100, (int) Math.round(completed * 100.0 / Math.max(total, 1)));
        }
    }

    private record ScanProbe(String label, HttpRequestRecord request, String parameterName) {
    }
}
