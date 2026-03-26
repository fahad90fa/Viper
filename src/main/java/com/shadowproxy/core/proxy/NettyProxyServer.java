package com.shadowproxy.core.proxy;

import com.shadowproxy.config.AppConfig;
import com.shadowproxy.core.cert.CertificateService;
import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.persistence.HistoryRepository;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyProxyServer implements ProxyServer {
    private static final Logger LOG = LoggerFactory.getLogger(NettyProxyServer.class);
    private final AppConfig appConfig;
    private final HistoryRepository historyRepository;
    private final HttpForwarder httpForwarder;
    private final ProxyInterceptionPipeline interceptionPipeline;
    private final CertificateService certificateService;
    private final InterceptionManager interceptionManager;
    private final ToolRouter toolRouter;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture bindFuture;
    private volatile boolean running;

    public NettyProxyServer(AppConfig appConfig,
                            HistoryRepository historyRepository,
                            CertificateService certificateService,
                            InterceptionManager interceptionManager,
                            ToolRouter toolRouter) {
        this.appConfig = appConfig;
        this.historyRepository = historyRepository;
        this.certificateService = certificateService;
        this.interceptionManager = interceptionManager;
        this.toolRouter = toolRouter;
        this.httpForwarder = new JavaHttpForwarder();
        this.interceptionPipeline = new ProxyInterceptionPipeline();
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("httpServerCodec", new HttpServerCodec());
                            pipeline.addLast("httpAggregator", new HttpObjectAggregator(10 * 1024 * 1024));
                            pipeline.addLast("proxyFrontendHandler", new ProxyFrontendHandler(
                                    historyRepository,
                                    httpForwarder,
                                    interceptionPipeline,
                                    certificateService,
                                    appConfig,
                                    interceptionManager,
                                    toolRouter
                            ));
                        }
                    });
            bindFuture = bootstrap.bind(appConfig.listenHost(), appConfig.listenPort()).sync();
            running = true;
            LOG.info("Proxy server started on {}:{}", appConfig.listenHost(), appConfig.listenPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Proxy startup interrupted", e);
        } catch (Exception e) {
            stop();
            throw new IllegalStateException("Unable to start proxy server", e);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        try {
            if (bindFuture != null) {
                bindFuture.channel().close().syncUninterruptibly();
            }
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
