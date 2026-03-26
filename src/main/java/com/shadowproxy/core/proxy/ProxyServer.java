package com.shadowproxy.core.proxy;

public interface ProxyServer {
    void start();

    void stop();

    boolean isRunning();
}
