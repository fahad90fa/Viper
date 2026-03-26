package com.shadowproxy.ui.modules;

import com.shadowproxy.config.AppConfig;
import com.shadowproxy.core.proxy.InterceptionManager;
import com.shadowproxy.core.proxy.JavaHttpForwarder;
import com.shadowproxy.core.proxy.PendingIntercept;
import com.shadowproxy.core.proxy.ProxyServer;
import com.shadowproxy.core.intruder.IntruderAttackResult;
import com.shadowproxy.core.intruder.IntruderAttackRunner;
import com.shadowproxy.core.intruder.IntruderAttackType;
import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.core.routing.ToolRouterListener;
import com.shadowproxy.core.routing.ToolType;
import com.shadowproxy.core.scanner.ScanIssueStore;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.http.HttpRequestRecord;
import com.shadowproxy.domain.http.HttpResponseRecord;
import com.shadowproxy.domain.scanner.IssueConfidence;
import com.shadowproxy.domain.scanner.IssueSeverity;
import com.shadowproxy.domain.scanner.ScanIssue;
import com.shadowproxy.persistence.HistoryRepository;
import com.shadowproxy.ui.components.IconFactory;
import com.shadowproxy.ui.components.MessageEditorPanel;
import com.shadowproxy.ui.components.SiteMapPanel;
import com.shadowproxy.ui.components.ScannerWorkspacePanel;
import com.shadowproxy.util.HttpMessageCodec;
import com.shadowproxy.ui.components.WorkspaceTabs;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JSpinner;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.charset.StandardCharsets;

public final class ModulePanelFactory {
    private ModulePanelFactory() {
    }

    public static JPanel placeholder(String title, String description) {
        JPanel card = cardPanel(title, description, IconFactory.of("dashboard", new Color(90, 150, 255), 24));
        JTextArea body = new JTextArea("""
                This module shell is ready.

                The layout, spacing, and interaction model now mirror the Burp-style desktop workspace.
                """);
        body.setEditable(false);
        body.setOpaque(false);
        card.add(body, BorderLayout.CENTER);
        return wrapWithPadding(card);
    }

    public static JPanel dashboardPanel(AppConfig appConfig,
                                        HistoryRepository historyRepository,
                                        ToolRouter toolRouter,
                                        ProxyServer proxyServer,
                                        ScanIssueStore scanIssueStore) {
        JPanel root = new JPanel(new GridLayout(2, 2, 10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(buildQuickActionsPanel(appConfig, historyRepository, proxyServer));
        root.add(buildStatisticsPanel(historyRepository, scanIssueStore));
        root.add(buildProxyConfigPanel(appConfig, proxyServer));
        root.add(buildScannerStatusPanel(scanIssueStore, toolRouter));
        return root;
    }

    public static JPanel targetPanel(HistoryRepository historyRepository, ScanIssueStore scanIssueStore, ToolRouter toolRouter) {
        return new SiteMapPanel(historyRepository, scanIssueStore, toolRouter);
    }

    public static JPanel proxyPanel(AppConfig appConfig,
                                    HistoryRepository historyRepository,
                                    ProxyServer proxyServer,
                                    ToolRouter toolRouter,
                                    InterceptionManager interceptionManager) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout(8, 8));
        top.add(buildProxyToolbar(interceptionManager), BorderLayout.NORTH);
        root.add(top, BorderLayout.NORTH);

        JTabbedPane topTabs = new JTabbedPane();
        topTabs.addTab("Intercept", buildInterceptTab(interceptionManager, toolRouter));
        topTabs.addTab("HTTP History", buildHistoryTab(historyRepository, toolRouter));
        topTabs.addTab("WebSockets History", buildWebSocketTab());

        MessageEditorPanel requestViewer = new MessageEditorPanel(false);
        requestViewer.setRawText(sampleRequest());
        MessageEditorPanel responseViewer = new MessageEditorPanel(false);
        responseViewer.setRawText(sampleResponse());

        JSplitPane viewerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle(requestViewer, "Request"),
                wrapWithTitle(responseViewer, "Response"));
        viewerSplit.setResizeWeight(0.5);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topTabs, viewerSplit);
        splitPane.setResizeWeight(0.6);
        root.add(splitPane, BorderLayout.CENTER);

        ProxyStatusUpdater.attach(root, proxyServer, interceptionManager, appConfig);
        return root;
    }

    public static JComponent repeaterPanel(ToolRouter toolRouter, HistoryRepository historyRepository) {
        return new RepeaterWorkspacePanel(toolRouter, historyRepository);
    }

    public static JComponent intruderPanel(ToolRouter toolRouter, HistoryRepository historyRepository) {
        return new IntruderWorkspacePanel(toolRouter, historyRepository);
    }

    public static JPanel scannerPanel(HistoryRepository historyRepository, ScanIssueStore scanIssueStore, ToolRouter toolRouter) {
        return new ScannerWorkspacePanel(historyRepository, scanIssueStore, toolRouter);
    }

    public static JComponent sequencerPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(buildSequencerToolbar(), BorderLayout.NORTH);

        JPanel capture = new JPanel(new GridLayout(0, 1, 8, 8));
        capture.add(new JLabel("Capture panel and token source configuration"));
        capture.add(new JComboBox<>(new String[]{"Live capture from Proxy", "Manual load"}));
        capture.add(new JTextField("https://example.com/login?token=..."));
        capture.add(new JComboBox<>(new String[]{"token", "session", "nonce"}));
        capture.add(new JButton("Start live capture"));
        capture.add(new JSpinner(new SpinnerNumberModel(100, 1, 100000, 1)));
        capture.add(new JLabel("Currently captured: 0 tokens"));

        JTextArea tokenArea = new JTextArea("tok_123\n tok_456\n tok_789");
        tokenArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        tokenArea.setEditable(true);

        JPanel options = new JPanel(new GridLayout(0, 2, 8, 8));
        options.add(new JCheckBox("Character-level analysis", true));
        options.add(new JCheckBox("Bit-level analysis", true));
        options.add(new JCheckBox("FIPS tests", true));
        options.add(new JCheckBox("Spectral tests", true));
        options.add(new JCheckBox("Correlation analysis", true));
        options.add(new JLabel("Sensitivity"));
        options.add(new JSlider(0, 100, 70));

        JTabbedPane results = new JTabbedPane();
        results.addTab("Summary", buildSequencerSummary());
        results.addTab("Character-level analysis", buildSequencerCharAnalysis());
        results.addTab("Bit-level analysis", buildSequencerBitAnalysis());
        results.addTab("Spectral analysis", buildSequencerSpectralAnalysis());
        results.addTab("Correlation", buildSequencerCorrelation());

        JSplitPane top = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle(new JScrollPane(tokenArea), "Tokens"),
                wrapWithTitle(capture, "Token Collection"));
        top.setResizeWeight(0.4);

        JSplitPane bottom = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrapWithTitle(options, "Analysis Options"),
                results);
        bottom.setResizeWeight(0.25);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setResizeWeight(0.3);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    public static JComponent decoderPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JButton("Add panel below"));
        toolbar.add(new JButton("Clear all"));
        toolbar.add(new JButton("Paste from clipboard"));
        toolbar.add(new JButton("Copy result to clipboard"));
        toolbar.add(new JCheckBox("Smart decode mode"));
        root.add(toolbar, BorderLayout.NORTH);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        DecoderLane first = new DecoderLane("Base64");
        DecoderLane second = new DecoderLane("URL");
        first.setNext(second);
        stack.add(first);
        stack.add(Box.createVerticalStrut(8));
        stack.add(second);
        root.add(new JScrollPane(stack), BorderLayout.CENTER);
        return root;
    }

    public static JComponent comparerPanel(ToolRouter toolRouter) {
        return new ComparerWorkspacePanel(toolRouter);
    }

    public static JComponent extenderPanel() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTable table = new JTable(new DefaultTableModel(new Object[]{"Enabled", "Extension Name", "Type", "Version", "Author", "Status"}, 0));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{Boolean.TRUE, "Built-in Logger", "Java", "1.0", "ShadowProxy", "Loaded"});
        model.addRow(new Object[]{Boolean.FALSE, "Sample Scanner Check", "Java", "0.1", "ShadowProxy", "Loaded"});
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(new JCheckBox()));
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JButton("Add"));
        toolbar.add(new JButton("Remove"));
        toolbar.add(new JButton("Up"));
        toolbar.add(new JButton("Down"));
        toolbar.add(new JButton("BApp Store..."));
        root.add(toolbar, BorderLayout.NORTH);

        JTabbedPane details = new JTabbedPane();
        details.addTab("Details", buildExtenderDetails());
        details.addTab("Output", wrapWithPadding(new JTextArea("Console output from extensions appears here.")));
        details.addTab("Errors", wrapWithPadding(new JTextArea("Stack traces and runtime errors appear here.")));
        details.addTab("Options", buildSimpleForm("Extension-specific options render here."));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrapWithTitle(new JScrollPane(table), "Installed Extensions"),
                details);
        split.setResizeWeight(0.3);
        root.add(split, BorderLayout.CENTER);
        return root;
    }

    public static JComponent settingsPanel(AppConfig appConfig, ToolRouter toolRouter) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTree navigation = buildSettingsTree();
        JPanel cards = new JPanel(new CardLayout());
        cards.add(buildProxyListenersCard(appConfig), "Proxy / Listeners");
        cards.add(buildProxySslCard(), "Proxy / SSL");
        cards.add(buildUiThemeCard(), "User Interface / Theme");
        cards.add(buildNetworkCard(), "Network / Connections");
        cards.add(buildGenericSettingsCard("Target / Scope"), "Target / Scope");
        cards.add(buildGenericSettingsCard("Scanner / Live Scanning"), "Scanner / Live Scanning");

        navigation.addTreeSelectionListener(treeSelectionListener(cards));
        root.add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrapWithTitle(new JScrollPane(navigation), "Settings"),
                cards), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton("Restore Defaults"));
        buttons.add(new JButton("Cancel"));
        buttons.add(new JButton("Save & Apply"));
        root.add(buttons, BorderLayout.SOUTH);
        return root;
    }

    private static JPanel buildQuickActionsPanel(AppConfig appConfig, HistoryRepository historyRepository, ProxyServer proxyServer) {
        JPanel panel = cardPanel("Quick Start", "Fast entry points for common actions", IconFactory.of("new", new Color(98, 151, 85), 24));
        JPanel grid = new JPanel(new GridLayout(0, 1, 8, 8));
        grid.add(new JButton("New Scan"));
        grid.add(new JButton("Open Saved Project"));
        grid.add(new JButton("Configure Proxy"));
        grid.add(new JButton("Install CA Certificate"));
        panel.add(grid, BorderLayout.NORTH);
        DefaultListModel<String> activity = new DefaultListModel<>();
        activity.addElement("Opened project: demo.shadow");
        activity.addElement("Proxy listener started on " + appConfig.listenHost() + ":" + appConfig.listenPort());
        activity.addElement("Queued 3 history entries");
        JList<String> recent = new JList<>(activity);
        panel.add(wrapWithTitle(new JScrollPane(recent), "Recent Activity"), BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buildStatisticsPanel(HistoryRepository historyRepository, ScanIssueStore scanIssueStore) {
        JPanel panel = cardPanel("Project Statistics", "Live summary of history and issues", IconFactory.of("dashboard", new Color(104, 151, 187), 24));
        panel.add(statsGrid(historyRepository, scanIssueStore), BorderLayout.NORTH);
        DefaultPieDataset<String> data = new DefaultPieDataset<>();
        data.setValue("High", 7);
        data.setValue("Medium", 10);
        data.setValue("Low", 6);
        JFreeChart chart = ChartFactory.createPieChart("Issue Severity", data, true, true, false);
        panel.add(new ChartPanel(chart), BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buildProxyConfigPanel(AppConfig appConfig, ProxyServer proxyServer) {
        JPanel panel = cardPanel("Proxy Configuration", "Listener and CA certificate status", IconFactory.of("proxy", new Color(98, 151, 85), 24));
        panel.add(new JLabel("Current listen address: " + appConfig.listenHost() + ":" + appConfig.listenPort()), BorderLayout.NORTH);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(new JButton("Configure"));
        buttons.add(new JButton("View CA Certificate"));
        buttons.add(new JButton("Clear History"));
        panel.add(buttons, BorderLayout.CENTER);
        JLabel status = new JLabel(proxyServer.isRunning() ? "Running" : "Stopped");
        status.setForeground(proxyServer.isRunning() ? new Color(98, 151, 85) : new Color(188, 63, 60));
        panel.add(status, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel buildScannerStatusPanel(ScanIssueStore scanIssueStore, ToolRouter toolRouter) {
        JPanel panel = cardPanel("Active Scans", "Current scan queue and status", IconFactory.of("scanner", new Color(104, 151, 187), 24));
        JTextArea area = new JTextArea(scanIssueStore.findAll().isEmpty()
                ? "No active scans. Click 'New Scan' to start."
                : "Target: example.com, Progress: 45%, ETA: 2m 15s");
        area.setEditable(false);
        panel.add(area, BorderLayout.CENTER);
        JPanel queue = new JPanel(new FlowLayout(FlowLayout.LEFT));
        queue.add(new JButton("Pause"));
        queue.add(new JButton("Cancel"));
        panel.add(queue, BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel buildTargetToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JComboBox<>(new String[]{"Show all items", "Show only in-scope items", "Show only items with issues"}));
        toolbar.add(new JTextField("Filter URLs...", 20));
        toolbar.add(new JButton("Add to Scope"));
        toolbar.add(new JButton("Remove from Scope"));
        toolbar.add(new JButton("Delete"));
        toolbar.add(new JButton("Expand All"));
        toolbar.add(new JButton("Collapse All"));
        return toolbar;
    }

    private static JPanel buildIssuesTab(JTable issuesTable, JPanel details) {
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(issuesTable), details);
        split.setResizeWeight(0.55);
        return panelWithBorder(split);
    }

    private static JPanel buildDetailsPanel(String description, String remediation) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.add(buildTextBlock("Description", description));
        panel.add(buildTextBlock("Remediation", remediation));
        panel.add(buildTextBlock("Request/Response", "Select a row to inspect the vulnerable request and response."));
        return panelWithBorder(panel);
    }

    private static JPanel buildTextBlock(String title, String text) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        panel.add(new JScrollPane(area), BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buildProxyToolbar(InterceptionManager interceptionManager) {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox intercept = new JCheckBox("Intercept is on", interceptionManager.isInterceptEnabled());
        intercept.addActionListener(e -> interceptionManager.setInterceptEnabled(intercept.isSelected()));
        toolbar.add(intercept);
        toolbar.add(new JButton("Forward"));
        toolbar.add(new JButton("Drop"));
        toolbar.add(new JComboBox<>(new String[]{"Do intercept", "Don't intercept", "Add to scope", "Send to Repeater", "Send to Intruder"}));
        return toolbar;
    }

    private static JComponent buildInterceptTab(InterceptionManager interceptionManager, ToolRouter toolRouter) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        MessageEditorPanel editor = new MessageEditorPanel(true);
        editor.setRawText(sampleRequest());
        JLabel message = new JLabel("Intercept is off. Click the button below or in the toolbar to turn it on.");
        JLabel status = new JLabel("No pending request");
        JButton forward = new JButton("Forward");
        JButton drop = new JButton("Drop");
        JButton toggle = new JButton("Intercept On/Off");
        JComboBox<String> action = new JComboBox<>(new String[]{"Do intercept", "Don't intercept", "Add to scope", "Send to Repeater", "Send to Intruder", "Send to Scanner"});
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(forward);
        toolbar.add(drop);
        toolbar.add(toggle);
        toolbar.add(action);
        panel.add(toolbar, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.add(message, BorderLayout.NORTH);
        center.add(wrapWithPadding(editor), BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        panel.add(status, BorderLayout.SOUTH);

        final PendingIntercept[] currentPending = new PendingIntercept[1];
        forward.addActionListener(e -> {
            PendingIntercept pending = currentPending[0];
            if (pending == null) {
                return;
            }
            try {
                HttpRequestRecord edited = HttpMessageCodec.parseRawRequest(editor.getRawText());
                interceptionManager.forward(pending.id(), edited);
                currentPending[0] = null;
                status.setText("Forwarded");
                String destination = Objects.toString(action.getSelectedItem(), "");
                if (destination.startsWith("Send to ")) {
                    HttpExchangeRecord exchange = new HttpExchangeRecord(
                            UUID.randomUUID(),
                            edited,
                            new HttpResponseRecord(200, "OK", Map.of("Content-Type", "text/plain"), sampleResponse().getBytes(), Instant.now()),
                            "Proxy");
                    switch (destination) {
                        case "Send to Repeater" -> toolRouter.sendToTool(ToolType.REPEATER, exchange);
                        case "Send to Intruder" -> toolRouter.sendToTool(ToolType.INTRUDER, exchange);
                        case "Send to Scanner" -> toolRouter.sendToTool(ToolType.SCANNER, exchange);
                        case "Send to Comparer" -> toolRouter.sendToTool(ToolType.COMPARER, exchange);
                        default -> {
                        }
                    }
                }
            } catch (Exception ex) {
                status.setText("Invalid edit: " + ex.getMessage());
            }
        });
        drop.addActionListener(e -> {
            PendingIntercept pending = currentPending[0];
            if (pending == null) {
                return;
            }
            interceptionManager.drop(pending.id());
            currentPending[0] = null;
            status.setText("Dropped");
        });
        toggle.addActionListener(e -> {
            boolean enabled = !interceptionManager.isInterceptEnabled();
            interceptionManager.setInterceptEnabled(enabled);
            message.setText(enabled ? "Waiting for an intercepted request..." : "Intercept is off. Click the button below or in the toolbar to turn it on.");
        });
        Timer timer = new Timer(250, e -> {
            if (currentPending[0] != null || !interceptionManager.isInterceptEnabled()) {
                return;
            }
            try {
                PendingIntercept pending = interceptionManager.pollPending(1, TimeUnit.MILLISECONDS);
                if (pending != null) {
                    currentPending[0] = pending;
                    editor.setRawText(HttpMessageCodec.toRawRequest(pending.originalRequest()));
                    status.setText("Intercepted at " + pending.originalRequest().capturedAt());
                } else {
                    message.setText(interceptionManager.isInterceptEnabled()
                            ? "Waiting for an intercepted request..."
                            : "Intercept is off. Click the button below or in the toolbar to turn it on.");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        });
        timer.start();
        return panel;
    }

    private static JComponent buildHistoryTab(HistoryRepository historyRepository, ToolRouter toolRouter) {
        JTable table = new JTable(new HistoryTableModel(historyRepository));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoCreateRowSorter(true);
        installHistoryContextMenu(table, (HistoryTableModel) table.getModel(), toolRouter);
        JTextArea filter = new JTextArea("Filter: regex supported");
        filter.setRows(1);
        filter.setEditable(false);
        JPanel top = new JPanel(new BorderLayout());
        top.add(filter, BorderLayout.CENTER);
        top.add(new JComboBox<>(new String[]{"Show all items", "Show only in-scope items", "Show only parameterized requests", "Show only errors"}), BorderLayout.EAST);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(top, BorderLayout.NORTH);
        wrapper.add(new JScrollPane(table), BorderLayout.CENTER);
        return wrapper;
    }

    private static JComponent buildWebSocketTab() {
        JTable table = new JTable(new DefaultTableModel(new Object[]{"#", "Direction", "WebSocket URL", "Message", "Length", "Time"}, 0));
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{1, "→ Outgoing", "wss://example.com/ws", "hello", 5, "12:34:56"});
        model.addRow(new Object[]{2, "← Incoming", "wss://example.com/ws", "world", 5, "12:34:57"});
        return new JScrollPane(table);
    }

    private static final class IntruderWorkspacePanel extends JPanel {
        private final WorkspaceTabs tabs = new WorkspaceTabs();
        private final ToolRouter toolRouter;
        private final HistoryRepository historyRepository;
        private final AtomicInteger sequence = new AtomicInteger(1);
        private final ToolRouterListener listener;

        private IntruderWorkspacePanel(ToolRouter toolRouter, HistoryRepository historyRepository) {
            super(new BorderLayout(8, 8));
            this.toolRouter = toolRouter;
            this.historyRepository = historyRepository;

            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addBlank = new JButton("Start attack");
            JButton openQueue = new JButton("Open queued item");
            toolbar.add(addBlank);
            toolbar.add(openQueue);
            toolbar.add(new JComboBox<>(new String[]{"Save config", "Load config", "Copy entire attack to new tab"}));
            add(toolbar, BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);

            addBlank.addActionListener(e -> addAttackTab(null));
            openQueue.addActionListener(e -> {
                List<HttpExchangeRecord> queue = toolRouter.getToolQueue(ToolType.INTRUDER);
                if (!queue.isEmpty()) {
                    addAttackTab(queue.get(queue.size() - 1));
                }
            });

            listener = (toolType, exchangeRecord) -> SwingUtilities.invokeLater(() -> addAttackTab(exchangeRecord));
            toolRouter.addListener(ToolType.INTRUDER, listener);

            List<HttpExchangeRecord> initial = toolRouter.getToolQueue(ToolType.INTRUDER);
            if (initial.isEmpty()) {
                addAttackTab(null);
            } else {
                initial.forEach(this::addAttackTab);
            }
        }

        private void addAttackTab(HttpExchangeRecord exchangeRecord) {
            IntruderAttackPanel panel = new IntruderAttackPanel(exchangeRecord, historyRepository);
            String title = "Attack " + sequence.getAndIncrement();
            tabs.addWorkspaceTab(title, IconFactory.of("intruder", new Color(255, 140, 0), 18), panel, true);
        }
    }

    private static final class IntruderAttackPanel extends JPanel {
        private final HistoryRepository historyRepository;
        private final MessageEditorPanel requestEditor = new MessageEditorPanel(true);
        private final JTextArea payloadsArea = new JTextArea(10, 40);
        private final DefaultTableModel resultsModel = new DefaultTableModel(new Object[]{"Request #", "Payload", "Status", "Length", "Time (ms)", "Error"}, 0);
        private final JTable resultsTable = new JTable(resultsModel);
        private final JComboBox<IntruderAttackType> attackType = new JComboBox<>(IntruderAttackType.values());
        private final JLabel status = new JLabel("Ready");
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final IntruderAttackRunner runner = new IntruderAttackRunner();

        private IntruderAttackPanel(HttpExchangeRecord exchangeRecord, HistoryRepository historyRepository) {
            super(new BorderLayout(8, 8));
            this.historyRepository = historyRepository;

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton start = new JButton("Start attack");
            JButton cancel = new JButton("Cancel");
            JButton addMarker = new JButton("Add §");
            JButton clearMarkers = new JButton("Clear §");
            JButton autoMarkers = new JButton("Auto §");
            top.add(start);
            top.add(cancel);
            top.add(addMarker);
            top.add(clearMarkers);
            top.add(autoMarkers);
            top.add(new JLabel("Attack type:"));
            top.add(attackType);
            add(top, BorderLayout.NORTH);

            payloadsArea.setText("admin\nroot\nsuperuser");
            requestEditor.setRawText(sampleIntruderRequest());

            JSplitPane upperSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    buildIntruderPositionsPanel(addMarker, clearMarkers, autoMarkers),
                    buildIntruderPayloadsPanel());
            upperSplit.setResizeWeight(0.6);

            JPanel options = new JPanel(new BorderLayout());
            JTabbedPane optionTabs = new JTabbedPane();
            optionTabs.addTab("Request Engine", buildSimpleForm("Threads, delays, throttling, timeout, retries"));
            optionTabs.addTab("Grep - Match", buildSimpleForm("Patterns to search in responses"));
            optionTabs.addTab("Grep - Extract", buildSimpleForm("Extraction rules and captured values"));
            optionTabs.addTab("Redirections", buildSimpleForm("Follow redirections: Always / Never / On-site only / In-scope only"));
            options.add(optionTabs, BorderLayout.CENTER);

            JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperSplit, options);
            centerSplit.setResizeWeight(0.55);

            JPanel resultsPanel = new JPanel(new BorderLayout(8, 8));
            resultsPanel.add(new JScrollPane(resultsTable), BorderLayout.CENTER);
            resultsTable.setFillsViewportHeight(true);

            JSplitPane bottomSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, centerSplit, resultsPanel);
            bottomSplit.setResizeWeight(0.65);
            add(bottomSplit, BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);

            start.addActionListener(e -> startAttack());
            cancel.addActionListener(e -> cancelled.set(true));

            loadExchange(exchangeRecord);
        }

        private JComponent buildIntruderPositionsPanel(JButton addMarker, JButton clearMarkers, JButton autoMarkers) {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            toolbar.add(new JLabel("Attack positions"));
            panel.add(toolbar, BorderLayout.NORTH);
            panel.add(requestEditor, BorderLayout.CENTER);

            addMarker.addActionListener(e -> wrapSelectionWithMarkers());
            clearMarkers.addActionListener(e -> requestEditor.setRawText(requestEditor.getRawText().replace("§", "")));
            autoMarkers.addActionListener(e -> autoMarkParameters());
            return panel;
        }

        private JComponent buildIntruderPayloadsPanel() {
            JPanel panel = new JPanel(new BorderLayout(8, 8));
            JPanel split = new JPanel(new BorderLayout(8, 8));
            split.add(buildSimpleForm("Payload set: 1\nPayload type: Simple list"), BorderLayout.NORTH);
            payloadsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            split.add(new JScrollPane(payloadsArea), BorderLayout.CENTER);
            panel.add(split, BorderLayout.CENTER);
            panel.add(buildSimpleForm("Payload processing and encoding rules"), BorderLayout.SOUTH);
            return panel;
        }

        private void loadExchange(HttpExchangeRecord exchangeRecord) {
            if (exchangeRecord == null) {
                requestEditor.setRawText(sampleIntruderRequest());
                return;
            }
            requestEditor.setRawText(HttpMessageCodec.toRawRequest(exchangeRecord.request()));
            status.setText("Loaded exchange from " + exchangeRecord.sourceTool());
        }

        private void startAttack() {
            cancelled.set(false);
            resultsModel.setRowCount(0);
            List<String> payloads = payloadsArea.getText().lines()
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
            String raw = requestEditor.getRawText();
            IntruderAttackType type = (IntruderAttackType) attackType.getSelectedItem();
            if (type == null) {
                status.setText("Select an attack type.");
                return;
            }
            status.setText("Running...");
            runner.run(raw, type, payloads, this::appendResult, () -> SwingUtilities.invokeLater(() -> status.setText("Completed")), cancelled);
        }

        private void appendResult(IntruderAttackResult result) {
            SwingUtilities.invokeLater(() -> resultsModel.addRow(new Object[]{
                    result.requestNumber(),
                    result.payloadSummary(),
                    result.statusCode() < 0 ? "Error" : result.statusCode(),
                    result.length(),
                    result.timeTaken().toMillis(),
                    result.error()
            }));
        }

        private void wrapSelectionWithMarkers() {
            try {
                int start = requestEditor.getSelectionStart();
                int end = requestEditor.getSelectionEnd();
                if (start == end) {
                    status.setText("Select text first.");
                    return;
                }
                String raw = requestEditor.getRawText();
                requestEditor.setRawText(raw.substring(0, start) + "§" + raw.substring(start, end) + "§" + raw.substring(end));
                status.setText("Marked payload position.");
            } catch (Exception ex) {
                status.setText("Unable to mark position: " + ex.getMessage());
            }
        }

        private void autoMarkParameters() {
            String raw = requestEditor.getRawText();
            if (raw.contains("=") && raw.contains("&")) {
                String replaced = raw.replaceAll("=([^&\\r\\n]+)", "=§$1§");
                requestEditor.setRawText(replaced);
                status.setText("Auto-marked form parameters.");
                return;
            }
            requestEditor.setRawText(raw.replaceFirst("(?m)^(.*)$", "§$1§"));
            status.setText("Auto-marked first line.");
        }
    }

    private static String sampleIntruderRequest() {
        return """
                POST /login HTTP/1.1
                Host: example.com
                Content-Type: application/x-www-form-urlencoded

                username=§admin§&password=§password§
                """;
    }

    private static JPanel buildAttackSection(String title, JComponent content) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    private static JComponent buildIssueMessageView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                new MessageEditorPanel(false),
                new MessageEditorPanel(false));
        ((MessageEditorPanel) split.getLeftComponent()).setRawText(sampleRequest());
        ((MessageEditorPanel) split.getRightComponent()).setRawText(sampleResponse());
        split.setResizeWeight(0.5);
        return split;
    }

    private static JPanel buildSequencerSummary() {
        JPanel panel = buildSimpleForm("Overall quality: Excellent\nEffective entropy: 87.3 bits\nThe tokens appear to be randomly generated.");
        DefaultPieDataset<String> data = new DefaultPieDataset<>();
        data.setValue("Good", 70);
        data.setValue("Poor", 10);
        data.setValue("Excellent", 20);
        panel.add(new ChartPanel(ChartFactory.createPieChart("Token Quality", data, true, true, false)), BorderLayout.EAST);
        return panel;
    }

    private static JPanel buildSequencerCharAnalysis() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        data.addValue(0.72, "Entropy", "0");
        data.addValue(0.88, "Entropy", "1");
        data.addValue(0.93, "Entropy", "2");
        JFreeChart chart = ChartFactory.createLineChart("Entropy Across Positions", "Position", "Entropy", data, PlotOrientation.VERTICAL, false, true, false);
        return new JPanel(new BorderLayout()) {{
            add(new ChartPanel(chart), BorderLayout.CENTER);
            add(buildSimpleForm("Position, Character count, Entropy"), BorderLayout.SOUTH);
        }};
    }

    private static JPanel buildSequencerBitAnalysis() {
        return buildSimpleForm("Bit-level analysis matrix and heatmap visualization");
    }

    private static JPanel buildSequencerSpectralAnalysis() {
        DefaultCategoryDataset data = new DefaultCategoryDataset();
        data.addValue(12, "Occurrences", "0");
        data.addValue(23, "Occurrences", "1");
        data.addValue(18, "Occurrences", "2");
        return new JPanel(new BorderLayout()) {{
            add(new ChartPanel(ChartFactory.createLineChart("Frequency Distribution", "Value", "Occurrences", data, PlotOrientation.VERTICAL, false, true, false)), BorderLayout.CENTER);
        }};
    }

    private static JPanel buildSequencerCorrelation() {
        return buildSimpleForm("Correlation matrix and heatmap visualization");
    }

    private static JPanel buildComparerSource(String title) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        JComboBox<String> source = new JComboBox<>(new String[]{"Proxy history", "Repeater", "Intruder results", "File", "Paste"});
        JTextArea preview = new JTextArea(sampleRequest());
        panel.add(source, BorderLayout.NORTH);
        panel.add(new JScrollPane(preview), BorderLayout.CENTER);
        panel.add(new JButton("Clear"), BorderLayout.SOUTH);
        return panel;
    }

    private static JPanel buildComparerControls() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.add(new JButton("Words"));
        panel.add(new JButton("Bytes"));
        return panelWithBorder(panel);
    }

    private static JPanel buildExtenderDetails() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.add(new JLabel("Extension name: Built-in Logger"));
        panel.add(new JLabel("Description: Logs extension events."));
        panel.add(new JLabel("Author: ShadowProxy"));
        panel.add(new JLabel("Homepage: https://shadowproxy.local"));
        panel.add(new JLabel("License: Apache 2.0"));
        return panelWithBorder(panel);
    }

    private static JTree buildSettingsTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Settings");
        root.add(treeBranch("Proxy", "Listeners", "Intercept", "HTTP", "SSL"));
        root.add(treeBranch("Target", "Scope", "Site Map"));
        root.add(treeBranch("Scanner", "Live Scanning", "Scan Optimization", "Issue Definitions"));
        root.add(treeBranch("Intruder", "Attack Settings"));
        root.add(treeBranch("Repeater"));
        root.add(treeBranch("Spider", "Crawler Settings", "Form Submission"));
        root.add(treeBranch("Sequencer"));
        root.add(treeBranch("Decoder"));
        root.add(treeBranch("Comparer"));
        root.add(treeBranch("Extensions"));
        root.add(treeBranch("User Interface", "Theme", "Font", "Shortcuts", "Layout"));
        root.add(treeBranch("Network", "Connections", "HTTP", "SSL/TLS"));
        return new JTree(root);
    }

    private static TreeSelectionListener treeSelectionListener(JPanel cards) {
        return e -> {
            Object last = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            if (last == null) {
                return;
            }
            String key = e.getPath().getPathCount() >= 3
                    ? e.getPath().getPathComponent(1) + " / " + last
                    : last.toString();
            CardLayout layout = (CardLayout) cards.getLayout();
            layout.show(cards, key);
        };
    }

    private static JPanel buildProxyListenersCard(AppConfig appConfig) {
        JPanel panel = buildSimpleForm("Active listeners\n" + appConfig.listenHost() + ":" + appConfig.listenPort() + " (running)");
        panel.add(new JScrollPane(new JTable(new DefaultTableModel(new Object[]{"Running", "Bind Address", "Port", "Certificate"}, 0)) ), BorderLayout.EAST);
        return panel;
    }

    private static JPanel buildProxySslCard() {
        return buildSimpleForm("CA certificate management, browser installation instructions, and client certificates.");
    }

    private static JPanel buildUiThemeCard() {
        JPanel panel = buildSimpleForm("Theme selection, font settings, and preview.");
        panel.add(new JComboBox<>(new String[]{"Light", "Dark", "System default"}), BorderLayout.NORTH);
        return panel;
    }

    private static JPanel buildNetworkCard() {
        return buildSimpleForm("Upstream proxy, SOCKS, and timeout values.");
    }

    private static JPanel buildGenericSettingsCard(String title) {
        return buildSimpleForm(title + " settings panel");
    }

    private static JPanel buildSimpleForm(String text) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("<html>" + text.replace("\n", "<br>") + "</html>"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JTextField("example"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JCheckBox("Enabled", true));
        panel.add(Box.createVerticalStrut(8));
        panel.add(new JButton("Apply"));
        return panelWithBorder(panel);
    }

    private static JPanel statsGrid(HistoryRepository historyRepository, ScanIssueStore scanIssueStore) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        panel.add(new JLabel("Unique Hosts: " + Math.max(1, historyRepository.findAll().size())));
        panel.add(new JLabel("Total Requests: " + historyRepository.findAll().size()));
        panel.add(new JLabel("Total Responses: " + historyRepository.findAll().size()));
        panel.add(new JLabel("Scanner Issues: " + scanIssueStore.findAll().size() + " (7 High, 10 Medium, 6 Low)"));
        panel.add(new JLabel("Spider URLs Discovered: 342"));
        return panel;
    }

    private static void installTreeContextMenu(JTree tree) {
        JPopupMenu menu = new JPopupMenu();
        for (String text : new String[]{"Spider from here", "Actively scan this branch", "Add to scope", "Remove from scope", "Delete branch", "Expand all", "Collapse all", "Copy URL", "Copy as curl"}) {
            menu.add(new JMenuItem(text));
        }
        tree.setComponentPopupMenu(menu);
    }

    private static JPanel wrapWithTitle(JComponent child, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel wrapWithPadding(JComponent child) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(child, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel cardPanel(String title, String description, javax.swing.Icon icon) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(80, 80, 80)), title),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        JLabel heading = new JLabel(description);
        heading.setIcon(icon);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(heading, BorderLayout.NORTH);
        return panel;
    }

    private static JPanel panelWithBorder(JComponent component) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(component, BorderLayout.CENTER);
        return wrapper;
    }

    private static List<ScanIssue> sampleOrStoredIssues(ScanIssueStore store) {
        List<ScanIssue> issues = new ArrayList<>(store.findAll());
        if (!issues.isEmpty()) {
            return issues;
        }
        return List.of(
                new ScanIssue(UUID.randomUUID(), "SQL Injection", IssueSeverity.HIGH, IssueConfidence.CERTAIN, "https://example.com/api/users?id=1", "SQL injection evidence", "Use prepared statements", "id=1' OR '1'='1", Instant.now()),
                new ScanIssue(UUID.randomUUID(), "Missing Security Headers", IssueSeverity.MEDIUM, IssueConfidence.FIRM, "https://example.com/admin", "Headers are missing", "Add CSP, HSTS, and X-Frame-Options", "No security headers found", Instant.now())
        );
    }

    private static String sampleRequest() {
        return """
                GET /api/users?id=1 HTTP/1.1
                Host: example.com
                User-Agent: ShadowProxy
                Accept: */*

                """;
    }

    private static String sampleResponse() {
        return """
                HTTP/1.1 200 OK
                Content-Type: application/json

                {"status":"ok","users":[{"id":1,"name":"admin"}]}
                """;
    }

    private static String safeHost(String url) {
        try {
            String value = url == null ? "" : url.trim();
            if (value.isEmpty()) {
                return "-";
            }
            String host = java.net.URI.create(value).getHost();
            return host == null ? value : host;
        } catch (Exception ex) {
            return url == null || url.isBlank() ? "-" : url;
        }
    }

    private static JComponent buildProxyStatusUpdaterPlaceholder() {
        return new JLabel("Proxy running");
    }

    private static final class ProxyStatusUpdater {
        private ProxyStatusUpdater() {
        }

        static void attach(JPanel panel, ProxyServer proxyServer, InterceptionManager interceptionManager, AppConfig appConfig) {
            Timer timer = new Timer(1000, e -> {
                if (panel.getParent() != null) {
                    panel.repaint();
                }
            });
            timer.start();
        }
    }

    private static final class WorkspaceTabContainer extends JTabbedPane {
        private final String prefix;

        private WorkspaceTabContainer(String prefix) {
            this.prefix = prefix;
            setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        }

        void addRepeaterTab(ToolRouter toolRouter, int index) {
            addTab(prefix + " " + index, createRepeaterTab(toolRouter, index));
        }

        int tabCount() {
            return getTabCount();
        }
    }

    private static final class RepeaterWorkspacePanel extends JPanel {
        private final WorkspaceTabs tabs = new WorkspaceTabs();
        private final ToolRouter toolRouter;
        private final HistoryRepository historyRepository;
        private final AtomicInteger sequence = new AtomicInteger(1);
        private final ToolRouterListener listener;

        private RepeaterWorkspacePanel(ToolRouter toolRouter, HistoryRepository historyRepository) {
            super(new BorderLayout(8, 8));
            this.toolRouter = toolRouter;
            this.historyRepository = historyRepository;

            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton addBlank = new JButton("Add repeater");
            JButton addFromQueue = new JButton("Open queued item");
            toolbar.add(addBlank);
            toolbar.add(addFromQueue);
            toolbar.add(new JLabel("Ctrl+Space sends the request"));
            add(toolbar, BorderLayout.NORTH);
            add(tabs, BorderLayout.CENTER);

            addBlank.addActionListener(e -> addRepeaterTab(null));
            addFromQueue.addActionListener(e -> {
                List<HttpExchangeRecord> queue = toolRouter.getToolQueue(ToolType.REPEATER);
                if (!queue.isEmpty()) {
                    addRepeaterTab(queue.get(queue.size() - 1));
                }
            });

            listener = (toolType, exchangeRecord) -> SwingUtilities.invokeLater(() -> addRepeaterTab(exchangeRecord));
            toolRouter.addListener(ToolType.REPEATER, listener);

            List<HttpExchangeRecord> initial = toolRouter.getToolQueue(ToolType.REPEATER);
            if (initial.isEmpty()) {
                addRepeaterTab(null);
            } else {
                initial.forEach(this::addRepeaterTab);
            }
        }

        private void addRepeaterTab(HttpExchangeRecord exchangeRecord) {
            RepeaterTabPanel panel = new RepeaterTabPanel(exchangeRecord, historyRepository);
            String title = panel.titleHint(sequence.getAndIncrement());
            tabs.addWorkspaceTab(title, IconFactory.of("repeater", new Color(103, 141, 255), 18), panel, true);
        }
    }

    private static final class RepeaterTabPanel extends JPanel {
        private final HistoryRepository historyRepository;
        private final MessageEditorPanel requestEditor = new MessageEditorPanel(true);
        private final MessageEditorPanel responseEditor = new MessageEditorPanel(false);
        private final JTextField urlField = new JTextField(34);
        private final JComboBox<String> protocol = new JComboBox<>(new String[]{"http", "https"});
        private final JLabel status = new JLabel("Ready");
        private final JLabel metadata = new JLabel("Target: -");
        private HttpExchangeRecord loadedExchange;

        private RepeaterTabPanel(HttpExchangeRecord exchangeRecord, HistoryRepository historyRepository) {
            super(new BorderLayout(8, 8));
            this.historyRepository = historyRepository;

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            JButton send = new JButton("Send");
            JButton cancel = new JButton("Cancel");
            JButton applyUrl = new JButton("Go");
            top.add(protocol);
            top.add(urlField);
            top.add(applyUrl);
            top.add(send);
            top.add(cancel);
            top.add(metadata);
            add(top, BorderLayout.NORTH);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    wrapWithTitle(requestEditor, "Request"),
                    wrapWithTitle(responseEditor, "Response"));
            split.setResizeWeight(0.5);
            add(split, BorderLayout.CENTER);
            add(status, BorderLayout.SOUTH);

            send.addActionListener(e -> sendRequest());
            applyUrl.addActionListener(e -> syncUrlIntoRequest());

            loadExchange(exchangeRecord);
        }

        private String titleHint(int index) {
            if (loadedExchange == null) {
                return "Repeater " + index;
            }
            return "Repeater " + index + " - " + safeHost(loadedExchange.request().url());
        }

        private void loadExchange(HttpExchangeRecord exchangeRecord) {
            loadedExchange = exchangeRecord;
            if (exchangeRecord == null) {
                urlField.setText("https://example.com/");
                requestEditor.setRawText(sampleRequest());
                responseEditor.setRawText(sampleResponse());
                metadata.setText("Target: example.com");
                return;
            }
            requestEditor.setRawText(HttpMessageCodec.toRawRequest(exchangeRecord.request()));
            if (exchangeRecord.response() != null) {
                responseEditor.setRawText(HttpMessageCodec.toRawResponse(exchangeRecord.response()));
            }
            urlField.setText(exchangeRecord.request().url());
            protocol.setSelectedItem(exchangeRecord.request().url().startsWith("https") ? "https" : "http");
            metadata.setText("Target: " + safeHost(exchangeRecord.request().url()));
        }

        private void syncUrlIntoRequest() {
            try {
                HttpRequestRecord request = HttpMessageCodec.parseRawRequest(requestEditor.getRawText());
                HttpRequestRecord updated = new HttpRequestRecord(
                        request.method(),
                        urlField.getText().isBlank() ? request.url() : urlField.getText(),
                        request.headers(),
                        request.body(),
                        Instant.now()
                );
                requestEditor.setRawText(HttpMessageCodec.toRawRequest(updated));
                metadata.setText("Target: " + safeHost(updated.url()));
                status.setText("URL updated");
            } catch (Exception ex) {
                status.setText("Unable to update URL: " + ex.getMessage());
            }
        }

        private void sendRequest() {
            status.setText("Sending...");
            try {
                HttpRequestRecord request = HttpMessageCodec.parseRawRequest(requestEditor.getRawText());
                new SwingWorker<HttpResponseRecord, Void>() {
                    @Override
                    protected HttpResponseRecord doInBackground() throws Exception {
                        return new JavaHttpForwarder().forward(request).get();
                    }

                    @Override
                    protected void done() {
                        try {
                            HttpResponseRecord response = get();
                            responseEditor.setRawText(HttpMessageCodec.toRawResponse(response));
                            status.setText("Completed: " + response.statusCode());
                            if (historyRepository != null) {
                                historyRepository.save(new HttpExchangeRecord(UUID.randomUUID(), request, response, "REPEATER"));
                            }
                        } catch (Exception ex) {
                            status.setText("Send failed: " + ex.getMessage());
                        }
                    }
                }.execute();
            } catch (Exception ex) {
                status.setText("Invalid request: " + ex.getMessage());
            }
        }
    }

    private static final class ComparerWorkspacePanel extends JPanel {
        private final ToolRouter toolRouter;
        private final DefaultComboBoxModel<ExchangeOption> leftModel = new DefaultComboBoxModel<>();
        private final DefaultComboBoxModel<ExchangeOption> rightModel = new DefaultComboBoxModel<>();
        private final JComboBox<ExchangeOption> leftSelector = new JComboBox<>(leftModel);
        private final JComboBox<ExchangeOption> rightSelector = new JComboBox<>(rightModel);
        private final MessageEditorPanel leftViewer = new MessageEditorPanel(false);
        private final MessageEditorPanel rightViewer = new MessageEditorPanel(false);
        private final JLabel stats = new JLabel("Total differences: 0");
        private final ToolRouterListener listener;

        private ComparerWorkspacePanel(ToolRouter toolRouter) {
            super(new BorderLayout(8, 8));
            this.toolRouter = toolRouter;

            JPanel selectors = new JPanel(new GridLayout(1, 3, 8, 8));
            selectors.add(buildComparerSelection("Item 1", leftSelector));
            selectors.add(buildComparerControls());
            selectors.add(buildComparerSelection("Item 2", rightSelector));
            add(selectors, BorderLayout.NORTH);

            JSplitPane diff = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    wrapWithTitle(leftViewer, "Left"),
                    wrapWithTitle(rightViewer, "Right"));
            diff.setResizeWeight(0.5);
            add(diff, BorderLayout.CENTER);

            JPanel bottom = new JPanel(new BorderLayout());
            bottom.add(stats, BorderLayout.CENTER);
            bottom.add(new JButton("Export comparison to HTML"), BorderLayout.EAST);
            add(bottom, BorderLayout.SOUTH);

            leftSelector.addActionListener(e -> refreshComparison());
            rightSelector.addActionListener(e -> refreshComparison());

            listener = (toolType, exchangeRecord) -> SwingUtilities.invokeLater(() -> {
                addOption(exchangeRecord);
                refreshComparison();
            });
            toolRouter.addListener(ToolType.COMPARER, listener);

            List<HttpExchangeRecord> initial = toolRouter.getToolQueue(ToolType.COMPARER);
            if (initial.isEmpty()) {
                addOption(sampleExchange("Item 1"));
                addOption(sampleExchange("Item 2"));
            } else {
                initial.forEach(this::addOption);
            }
            if (leftModel.getSize() > 0) {
                leftSelector.setSelectedIndex(0);
            }
            if (rightModel.getSize() > 1) {
                rightSelector.setSelectedIndex(1);
            }
            refreshComparison();
        }

        private void addOption(HttpExchangeRecord exchangeRecord) {
            ExchangeOption option = new ExchangeOption(exchangeRecord);
            leftModel.addElement(option);
            if (!Objects.equals(option, rightSelector.getSelectedItem())) {
                rightModel.addElement(option);
            }
        }

        private void refreshComparison() {
            ExchangeOption left = (ExchangeOption) leftSelector.getSelectedItem();
            ExchangeOption right = (ExchangeOption) rightSelector.getSelectedItem();
            if (left == null || right == null) {
                return;
            }
            String leftText = HttpMessageCodec.toRawRequest(left.exchange().request());
            String rightText = HttpMessageCodec.toRawRequest(right.exchange().request());
            leftViewer.setRawText(leftText);
            rightViewer.setRawText(rightText);
            stats.setText(buildDiffStats(leftText, rightText));
        }
    }

    private record ExchangeOption(HttpExchangeRecord exchange) {
        @Override
        public String toString() {
            return exchange.request().method() + " " + safeHost(exchange.request().url());
        }
    }

    private static JPanel buildComparerSelection(String title, JComboBox<ExchangeOption> selector) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(new JLabel("Load from:"), BorderLayout.NORTH);
        panel.add(selector, BorderLayout.CENTER);
        panel.add(new JButton("Clear"), BorderLayout.SOUTH);
        return panel;
    }

    private static String buildDiffStats(String left, String right) {
        String[] leftLines = left.split("\\R");
        String[] rightLines = right.split("\\R");
        int max = Math.max(leftLines.length, rightLines.length);
        int differences = 0;
        for (int i = 0; i < max; i++) {
            String l = i < leftLines.length ? leftLines[i] : "";
            String r = i < rightLines.length ? rightLines[i] : "";
            if (!Objects.equals(l, r)) {
                differences++;
            }
        }
        return "Total differences: " + differences;
    }

    private static HttpExchangeRecord sampleExchange(String label) {
        return new HttpExchangeRecord(UUID.randomUUID(),
                new HttpRequestRecord("GET", "https://example.com/" + label.toLowerCase(Locale.ROOT), Map.of("Host", "example.com"), new byte[0], Instant.now()),
                new HttpResponseRecord(200, "OK", Map.of("Content-Type", "text/plain"), "ok".getBytes(), Instant.now()),
                "SAMPLE");
    }

    private static JComponent createRepeaterTab(ToolRouter toolRouter, int index) {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JComboBox<String> protocol = new JComboBox<>(new String[]{"http", "https"});
        JTextField url = new JTextField("https://example.com/api/users", 28);
        JButton go = new JButton("Go");
        JButton cancel = new JButton("Cancel");
        top.add(protocol);
        top.add(url);
        top.add(go);
        top.add(cancel);
        root.add(top, BorderLayout.NORTH);

        MessageEditorPanel request = new MessageEditorPanel(true);
        request.setRawText(sampleRequest());
        MessageEditorPanel response = new MessageEditorPanel(false);
        response.setRawText(sampleResponse());

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, request, response);
        split.setResizeWeight(0.5);
        root.add(split, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(new JButton("Send"));
        bottom.add(new JButton("Cancel"));
        bottom.add(new JCheckBox("Follow redirections"));
        bottom.add(new JCheckBox("Update Content-Length"));
        bottom.add(new JComboBox<>(new String[]{"Send to Intruder", "Send to Comparer", "Request in browser", "Show response in browser", "Copy URL", "Copy as curl command"}));
        root.add(bottom, BorderLayout.SOUTH);

        go.addActionListener(e -> toolRouter.sendToTool(ToolType.REPEATER,
                new HttpExchangeRecord(UUID.randomUUID(),
                        new HttpRequestRecord("GET", url.getText(), Map.of("Host", "example.com"), request.getRawText().getBytes(), Instant.now()),
                        new HttpResponseRecord(200, "OK", Map.of("Content-Type", "text/plain"), response.getRawText().getBytes(), Instant.now()),
                        "Repeater")));
        return root;
    }

    private static void installHistoryContextMenu(JTable table, HistoryTableModel model, ToolRouter toolRouter) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem toRepeater = new JMenuItem("Send to Repeater");
        JMenuItem toIntruder = new JMenuItem("Send to Intruder");
        JMenuItem toScanner = new JMenuItem("Send to Scanner");
        JMenuItem toComparer = new JMenuItem("Send to Comparer");
        toRepeater.addActionListener(e -> sendSelected(table, model, toolRouter, ToolType.REPEATER));
        toIntruder.addActionListener(e -> sendSelected(table, model, toolRouter, ToolType.INTRUDER));
        toScanner.addActionListener(e -> sendSelected(table, model, toolRouter, ToolType.SCANNER));
        toComparer.addActionListener(e -> sendSelected(table, model, toolRouter, ToolType.COMPARER));
        menu.add(toRepeater);
        menu.add(toIntruder);
        menu.add(toScanner);
        menu.add(toComparer);
        table.setComponentPopupMenu(menu);
    }

    private static void sendSelected(JTable table, HistoryTableModel model, ToolRouter toolRouter, ToolType toolType) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        HttpExchangeRecord exchangeRecord = model.rowAt(modelRow);
        toolRouter.sendToTool(toolType, exchangeRecord);
    }

    private static JPanel buildScannerToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JButton("New scan..."));
        toolbar.add(new JButton("Pause all"));
        toolbar.add(new JButton("Resume all"));
        toolbar.add(new JButton("Clear completed"));
        return toolbar;
    }

    private static JComponent buildSequencerToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(new JButton("Analyze now"));
        toolbar.add(new JButton("Clear tokens"));
        toolbar.add(new JButton("Save tokens to file"));
        return toolbar;
    }

    private static JPanel buildSimpleForm(String description, Component... extra) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextArea area = new JTextArea(description);
        area.setEditable(false);
        panel.add(area, BorderLayout.CENTER);
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(new JTextField("example"));
        form.add(Box.createVerticalStrut(6));
        form.add(new JCheckBox("Enabled", true));
        for (Component component : extra) {
            form.add(component);
        }
        panel.add(form, BorderLayout.SOUTH);
        return panelWithBorder(panel);
    }

    private static DefaultMutableTreeNode treeBranch(String title, String... children) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(title);
        for (String child : children) {
            node.add(new DefaultMutableTreeNode(child));
        }
        return node;
    }

    private static JPanel buildProxyStatusPanelPlaceholder() {
        return new JPanel();
    }

    private static final class DecoderLane extends JPanel {
        private final JComboBox<String> action;
        private final JTextArea input = new JTextArea(8, 60);
        private final JTextArea output = new JTextArea(4, 60);
        private DecoderLane next;

        private DecoderLane(String defaultAction) {
            setLayout(new BorderLayout(8, 8));
            JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            action = new JComboBox<>(new String[]{"Base64", "URL", "Hex", "HTML", "MD5", "SHA-256"});
            action.setSelectedItem(defaultAction);
            toolbar.add(new JLabel("Decode as:"));
            toolbar.add(action);
            toolbar.add(new JButton("Auto decode"));
            toolbar.add(new JButton("Delete this panel"));
            add(toolbar, BorderLayout.NORTH);
            input.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            output.setEditable(false);
            JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(input), new JScrollPane(output));
            split.setResizeWeight(0.7);
            add(split, BorderLayout.CENTER);
            add(new JLabel("Length: 0 bytes | Encoding detected: UTF-8"), BorderLayout.SOUTH);
            input.getDocument().addDocumentListener(MessageEditorPanel.SimpleDocumentListener.onChange(e -> refresh()));
            action.addActionListener(e -> refresh());
        }

        void setNext(DecoderLane next) {
            this.next = next;
        }

        private void refresh() {
            String value = transform(input.getText(), Objects.requireNonNullElse((String) action.getSelectedItem(), "Base64"));
            output.setText(value);
            if (next != null) {
                next.input.setText(value);
            }
        }

        private String transform(String text, String mode) {
            try {
                return switch (mode) {
                    case "URL" -> java.net.URLDecoder.decode(text, java.nio.charset.StandardCharsets.UTF_8);
                    case "Hex" -> bytesToHex(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    case "HTML" -> text.replace("<", "&lt;").replace(">", "&gt;");
                    case "MD5" -> hash("MD5", text);
                    case "SHA-256" -> hash("SHA-256", text);
                    default -> new String(java.util.Base64.getDecoder().decode(text.strip()), java.nio.charset.StandardCharsets.UTF_8);
                };
            } catch (Exception ex) {
                return "Decode error: " + ex.getMessage();
            }
        }

        private String hash(String algorithm, String text) throws Exception {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(algorithm);
            byte[] hashed = digest.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hashed);
        }

        private String bytesToHex(byte[] data) {
            StringBuilder builder = new StringBuilder();
            for (byte datum : data) {
                builder.append(String.format(Locale.ROOT, "%02x", datum));
            }
            return builder.toString();
        }
    }
}
