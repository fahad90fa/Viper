package com.shadowproxy.core.proxy;

import com.shadowproxy.config.AppConfig;
import com.shadowproxy.core.cert.CertificateService;
import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;
import com.shadowproxy.persistence.HistoryRepository;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProxyFrontendHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(ProxyFrontendHandler.class);
    private static final AttributeKey<String> CHANNEL_SCHEME = AttributeKey.valueOf("shadowproxy.scheme");
    private static final AttributeKey<String> CHANNEL_CONNECT_HOST = AttributeKey.valueOf("shadowproxy.connectHost");
    private final HistoryRepository historyRepository;
    private final HttpForwarder httpForwarder;
    private final ProxyInterceptionPipeline interceptionPipeline;
    private final CertificateService certificateService;
    private final AppConfig appConfig;
    private final InterceptionManager interceptionManager;
    private final ToolRouter toolRouter;
    private static final ExecutorService INTERCEPT_EXECUTOR = Executors.newCachedThreadPool();

    public ProxyFrontendHandler(HistoryRepository historyRepository,
                                HttpForwarder httpForwarder,
                                ProxyInterceptionPipeline interceptionPipeline,
                                CertificateService certificateService,
                                AppConfig appConfig,
                                InterceptionManager interceptionManager,
                                ToolRouter toolRouter) {
        this.historyRepository = historyRepository;
        this.httpForwarder = httpForwarder;
        this.interceptionPipeline = interceptionPipeline;
        this.certificateService = certificateService;
        this.appConfig = appConfig;
        this.interceptionManager = interceptionManager;
        this.toolRouter = toolRouter;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {
        if (HttpMethod.CONNECT.equals(msg.method())) {
            handleConnect(ctx, msg);
            return;
        }

        Map<String, String> requestHeaders = new HashMap<>();
        msg.headers().forEach(entry -> requestHeaders.put(entry.getKey(), entry.getValue()));
        byte[] requestBody = new byte[msg.content().readableBytes()];
        msg.content().getBytes(0, requestBody);
        String targetUrl = resolveTargetUrl(ctx, msg.uri(), requestHeaders);
        HttpRequestRecord requestRecord = interceptionPipeline.applyRequestInterceptors(new HttpRequestRecord(
                msg.method().name(),
                targetUrl,
                requestHeaders,
                requestBody,
                Instant.now()
        ));
        CompletableFuture.supplyAsync(() -> interceptionManager.awaitInterceptionDecision(requestRecord), INTERCEPT_EXECUTOR)
                .thenCompose(interceptedRequest -> {
                    if (interceptedRequest == null) {
                        return CompletableFuture.completedFuture(new ForwardResult(
                                requestRecord,
                                new HttpResponseRecord(
                                        204,
                                        "Dropped by interceptor",
                                        Map.of("X-ShadowProxy-Action", "dropped"),
                                        new byte[0],
                                        Instant.now()
                                )
                        ));
                    }
                    return httpForwarder.forward(interceptedRequest)
                            .thenApply(response -> new ForwardResult(interceptedRequest, response));
                })
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        ctx.executor().execute(() -> writeErrorResponse(ctx, requestRecord, throwable));
                        return;
                    }
                    HttpResponseRecord interceptedResponse = interceptionPipeline.applyResponseInterceptors(result.responseRecord());
                    historyRepository.save(new HttpExchangeRecord(UUID.randomUUID(), result.requestRecord(), interceptedResponse, "PROXY"));
                    ctx.executor().execute(() -> ctx.writeAndFlush(toNettyResponse(interceptedResponse)));
                });
    }

    private void handleConnect(ChannelHandlerContext ctx, FullHttpRequest msg) {
        String authority = msg.uri();
        int separator = authority.lastIndexOf(':');
        String host = separator > 0 ? authority.substring(0, separator) : authority;
        int port = separator > 0 ? Integer.parseInt(authority.substring(separator + 1)) : 443;
        ctx.channel().attr(CHANNEL_CONNECT_HOST).set(host);

        if (appConfig.mitmEnabled() && !appConfig.sslPassthroughHosts().contains(host.toLowerCase())) {
            handleMitmConnect(ctx, host);
            return;
        }
        startRawTunnel(ctx, host, port);
    }

    private void startRawTunnel(ChannelHandlerContext ctx, String host, int port) {
        Bootstrap bootstrap = new Bootstrap()
                .group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .handler(new TunnelRelayHandler(ctx.channel()));
        ChannelFuture outboundFuture = bootstrap.connect(host, port);
        outboundFuture.addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                LOG.warn("CONNECT failed for {}:{}", host, port, connectFuture.cause());
                FullHttpResponse errorResponse = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1,
                        HttpResponseStatus.BAD_GATEWAY,
                        Unpooled.copiedBuffer("CONNECT failed", StandardCharsets.UTF_8)
                );
                errorResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH, errorResponse.content().readableBytes());
                ctx.writeAndFlush(errorResponse).addListener(f -> ctx.close());
                return;
            }

            Channel outbound = outboundFuture.channel();
            FullHttpResponse connectOk = new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1,
                    new HttpResponseStatus(200, "Connection Established")
            );
            connectOk.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
            connectOk.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
            ctx.writeAndFlush(connectOk).addListener(future -> switchToTunnelMode(ctx, outbound));
        });
    }

    private void handleMitmConnect(ChannelHandlerContext ctx, String host) {
        FullHttpResponse connectOk = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                new HttpResponseStatus(200, "Connection Established")
        );
        connectOk.headers().set(HttpHeaderNames.CONNECTION, "keep-alive");
        connectOk.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, 0);
        ctx.writeAndFlush(connectOk).addListener(future -> switchToMitmMode(ctx, host));
    }

    private void switchToMitmMode(ChannelHandlerContext ctx, String host) {
        try {
            SslContext sslContext = certificateService.createServerSslContext(host);
            removeHttpHandlers(ctx);
            ctx.channel().attr(CHANNEL_SCHEME).set("https");
            ctx.pipeline().addLast("mitmSsl", sslContext.newHandler(ctx.alloc()));
            ctx.pipeline().addLast("httpServerCodec", new HttpServerCodec());
            ctx.pipeline().addLast("httpAggregator", new HttpObjectAggregator(10 * 1024 * 1024));
            ctx.pipeline().addLast("proxyFrontendHandler", new ProxyFrontendHandler(
                    historyRepository,
                    httpForwarder,
                    interceptionPipeline,
                    certificateService,
                    appConfig,
                    interceptionManager,
                    toolRouter
            ));
        } catch (Exception e) {
            LOG.error("MITM setup failed for {}", host, e);
            ctx.close();
        }
    }

    private void switchToTunnelMode(ChannelHandlerContext ctx, Channel outbound) {
        removeHttpHandlers(ctx);
        ctx.pipeline().addLast("clientToServerRelay", new TunnelRelayHandler(outbound));
    }

    private void removeHttpHandlers(ChannelHandlerContext ctx) {
        if (ctx.pipeline().get("httpServerCodec") != null) {
            ctx.pipeline().remove("httpServerCodec");
        }
        if (ctx.pipeline().get("httpAggregator") != null) {
            ctx.pipeline().remove("httpAggregator");
        }
        if (ctx.pipeline().get("proxyFrontendHandler") != null) {
            ctx.pipeline().remove("proxyFrontendHandler");
        }
    }

    private String resolveTargetUrl(ChannelHandlerContext ctx, String rawUri, Map<String, String> requestHeaders) {
        if (rawUri.startsWith("http://") || rawUri.startsWith("https://")) {
            return rawUri;
        }
        String host = requestHeaders.getOrDefault("Host", requestHeaders.get("host"));
        if (host == null || host.isBlank()) {
            host = ctx.channel().attr(CHANNEL_CONNECT_HOST).get();
        }
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Unable to resolve host for URI: " + rawUri);
        }
        String scheme = ctx.channel().attr(CHANNEL_SCHEME).get();
        if (scheme == null || scheme.isBlank()) {
            scheme = "http";
        }
        String normalizedPath = rawUri.startsWith("/") ? rawUri : "/" + rawUri;
        return scheme + "://" + host + normalizedPath;
    }

    private void writeErrorResponse(ChannelHandlerContext ctx, HttpRequestRecord requestRecord, Throwable throwable) {
        LOG.error("Proxy forward error for {}", requestRecord.url(), throwable);
        String message = "ShadowProxy forward error: " + throwable.getMessage();
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.BAD_GATEWAY,
                Unpooled.copiedBuffer(message, StandardCharsets.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        HttpResponseRecord responseRecord = new HttpResponseRecord(
                HttpResponseStatus.BAD_GATEWAY.code(),
                HttpResponseStatus.BAD_GATEWAY.reasonPhrase(),
                Map.of(
                        HttpHeaderNames.CONTENT_TYPE.toString(), "text/plain; charset=UTF-8",
                        HttpHeaderNames.CONTENT_LENGTH.toString(), String.valueOf(response.content().readableBytes())
                ),
                message.getBytes(StandardCharsets.UTF_8),
                Instant.now()
        );
        historyRepository.save(new HttpExchangeRecord(UUID.randomUUID(), requestRecord, responseRecord, "PROXY"));
        ctx.writeAndFlush(response);
    }

    private FullHttpResponse toNettyResponse(HttpResponseRecord responseRecord) {
        HttpResponseStatus status = HttpResponseStatus.valueOf(responseRecord.statusCode());
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(responseRecord.body())
        );
        responseRecord.headers().forEach(response.headers()::set);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, responseRecord.body().length);
        return response;
    }

    private record ForwardResult(HttpRequestRecord requestRecord, HttpResponseRecord responseRecord) {
    }
}
