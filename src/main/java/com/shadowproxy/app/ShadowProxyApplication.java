package com.shadowproxy.app;

import javax.swing.SwingUtilities;

public final class ShadowProxyApplication {
    private ShadowProxyApplication() {
    }

    public static void main(String[] args) {
        AppBootstrap bootstrap = new AppBootstrap();
        bootstrap.start();
        SwingUtilities.invokeLater(bootstrap::showMainWindow);
    }
}
