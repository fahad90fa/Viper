package com.shadowproxy.app;

import com.shadowproxy.config.AppConfig;
import com.shadowproxy.core.cert.CertificateService;
import com.shadowproxy.core.cert.DefaultCertificateService;
import com.shadowproxy.core.proxy.InterceptionManager;
import com.shadowproxy.core.proxy.NettyProxyServer;
import com.shadowproxy.core.proxy.ProxyServer;
import com.shadowproxy.core.routing.SimpleToolRouter;
import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.core.scanner.InMemoryScanIssueStore;
import com.shadowproxy.core.scanner.PassiveScannerService;
import com.shadowproxy.core.scanner.ScanIssueStore;
import com.shadowproxy.persistence.H2HistoryRepository;
import com.shadowproxy.persistence.HistoryRepository;
import com.shadowproxy.persistence.InMemoryHistoryRepository;
import com.shadowproxy.ui.MainWindow;
import com.shadowproxy.ui.state.UiStateStore;
import com.shadowproxy.ui.theme.ThemeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(AppBootstrap.class);
    private final AppConfig appConfig;
    private final HistoryRepository historyRepository;
    private final ToolRouter toolRouter;
    private final InterceptionManager interceptionManager;
    private final ScanIssueStore scanIssueStore;
    private final PassiveScannerService passiveScannerService;
    private final CertificateService certificateService;
    private final ProxyServer proxyServer;
    private final UiStateStore uiStateStore;
    private MainWindow mainWindow;

    public AppBootstrap() {
        this.appConfig = AppConfig.defaultConfig();
        this.historyRepository = createHistoryRepository(appConfig);
        this.toolRouter = new SimpleToolRouter();
        this.interceptionManager = new InterceptionManager();
        this.scanIssueStore = new InMemoryScanIssueStore();
        this.passiveScannerService = new PassiveScannerService(historyRepository, scanIssueStore);
        this.certificateService = new DefaultCertificateService(appConfig);
        this.proxyServer = new NettyProxyServer(appConfig, historyRepository, certificateService, interceptionManager, toolRouter);
        this.uiStateStore = new UiStateStore();
    }

    public void start() {
        ThemeManager.applyTheme(uiStateStore.loadTheme());
        certificateService.initialize();
        passiveScannerService.start();
        proxyServer.start();
        this.mainWindow = new MainWindow(
                appConfig,
                historyRepository,
                toolRouter,
                proxyServer,
                interceptionManager,
                scanIssueStore,
                uiStateStore
        );
    }

    public void showMainWindow() {
        if (mainWindow != null) {
            mainWindow.showWindow();
        }
    }

    private HistoryRepository createHistoryRepository(AppConfig config) {
        try {
            return new H2HistoryRepository(config);
        } catch (Exception ex) {
            LOG.warn("Falling back to in-memory history repository", ex);
            return new InMemoryHistoryRepository();
        }
    }
}
