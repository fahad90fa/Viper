package com.shadowproxy.ui.components;

import com.shadowproxy.core.scanner.ActiveScannerService;
import com.shadowproxy.core.scanner.ScanIssueStore;
import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.core.routing.ToolType;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.scanner.ScanIssue;
import com.shadowproxy.persistence.HistoryRepository;
import com.shadowproxy.util.HttpMessageCodec;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ScannerWorkspacePanel extends JPanel {
    private final HistoryRepository historyRepository;
    private final ScanIssueStore issueStore;
    private final ToolRouter toolRouter;
    private final ActiveScannerService scannerService;
    private final DefaultTableModel queueModel = new DefaultTableModel(new Object[]{"Status", "Target", "Progress", "Remaining", "Completed", "Requests", "Elapsed"}, 0);
    private final JTable queueTable = new JTable(queueModel);
    private final DefaultTableModel issuesModel = new DefaultTableModel(new Object[]{"Severity", "Confidence", "Issue Type", "URL", "Count"}, 0);
    private final JTable issuesTable = new JTable(issuesModel);
    private final JTextArea activityLog = new JTextArea();
    private final JTextArea issueDetails = new JTextArea();
    private final MessageEditorPanel issueRequestViewer = new MessageEditorPanel(false);
    private final MessageEditorPanel issueResponseViewer = new MessageEditorPanel(false);
    private final JLabel statusLabel = new JLabel("Idle");
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final com.shadowproxy.core.routing.ToolRouterListener scannerListener;
    private SwingWorker<Void, String> activeWorker;

    public ScannerWorkspacePanel(HistoryRepository historyRepository, ScanIssueStore issueStore, ToolRouter toolRouter) {
        super(new BorderLayout(8, 8));
        this.historyRepository = historyRepository;
        this.issueStore = issueStore;
        this.toolRouter = toolRouter;
        this.scannerService = new ActiveScannerService(historyRepository, issueStore);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        queueTable.setFillsViewportHeight(true);
        issuesTable.setFillsViewportHeight(true);
        issuesTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        activityLog.setEditable(false);
        issueDetails.setEditable(false);

        issuesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedIssue();
            }
        });
        scannerListener = (toolType, exchangeRecord) -> SwingUtilities.invokeLater(() -> {
            if (exchangeRecord != null) {
                statusLabel.setText("Queued scan target from " + exchangeRecord.sourceTool() + ".");
                if (activeWorker == null || activeWorker.isDone()) {
                    startScan();
                }
            }
        });
        toolRouter.addListener(ToolType.SCANNER, scannerListener);
        installIssueContextMenu();
        refreshIssues();
        refreshQueue();
        startAutoRefresh();
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton newScan = new JButton("New scan...");
        JButton pauseAll = new JButton("Pause all");
        JButton resumeAll = new JButton("Resume all");
        JButton clearCompleted = new JButton("Clear completed");
        JTextField targetField = new JTextField(24);
        JComboBox<String> mode = new JComboBox<>(new String[]{"Standard scan", "Quick scan", "Deep scan"});
        toolbar.add(newScan);
        toolbar.add(pauseAll);
        toolbar.add(resumeAll);
        toolbar.add(clearCompleted);
        toolbar.add(new JLabel("Target:"));
        toolbar.add(targetField);
        toolbar.add(mode);

        newScan.addActionListener(e -> startScan());
        pauseAll.addActionListener(e -> paused.set(true));
        resumeAll.addActionListener(e -> paused.set(false));
        clearCompleted.addActionListener(e -> {
            queueModel.setRowCount(0);
            statusLabel.setText("Cleared completed jobs.");
        });
        return toolbar;
    }

    private JComponent buildContent() {
        JSplitPane bottomRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrap(new JScrollPane(issuesTable), "Issues"),
                buildIssueDetails());
        bottomRight.setResizeWeight(0.4);

        JSplitPane vertical = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrap(new JScrollPane(queueTable), "Scan Queue"),
                new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                        wrap(new JScrollPane(activityLog), "Issue Activity"),
                        bottomRight));
        ((JSplitPane) vertical.getRightComponent()).setResizeWeight(0.3);
        vertical.setResizeWeight(0.35);
        return vertical;
    }

    private JComponent buildIssueDetails() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Description", wrap(issueDetails));
        tabs.addTab("Request/Response", buildIssueMessageView());
        tabs.addTab("Advisory", wrap(new JTextArea("Technical explanation and recommendations appear here.")));
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel wrap(JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel wrap(JComponent component, String title) {
        JPanel panel = wrap(component);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private JComponent buildIssueMessageView() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, issueRequestViewer, issueResponseViewer);
        split.setResizeWeight(0.5);
        return split;
    }

    private void installIssueContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem sendRepeater = new JMenuItem("Send request to Repeater");
        JMenuItem sendComparer = new JMenuItem("Send request to Comparer");
        sendRepeater.addActionListener(e -> sendSelectedToTool("Repeater"));
        sendComparer.addActionListener(e -> sendSelectedToTool("Comparer"));
        menu.add(sendRepeater);
        menu.add(sendComparer);
        issuesTable.setComponentPopupMenu(menu);
    }

    private void sendSelectedToTool(String tool) {
        int row = issuesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        ScanIssue issue = issueStore.findAll().get(issuesTable.convertRowIndexToModel(row));
        HttpExchangeRecord exchange = findExchangeForUrl(issue.url());
        if (exchange == null) {
            statusLabel.setText("No matching exchange found for this issue.");
            return;
        }
        ToolType toolType = "Comparer".equalsIgnoreCase(tool) ? ToolType.COMPARER : ToolType.REPEATER;
        toolRouter.sendToTool(toolType, exchange);
        statusLabel.setText("Issue sent to " + tool + ".");
    }

    private void startScan() {
        if (activeWorker != null && !activeWorker.isDone()) {
            statusLabel.setText("A scan is already running.");
            return;
        }
        cancelled.set(false);
        paused.set(false);
        List<String> targets = collectTargetsForScan();
        queueModel.setRowCount(0);
        if (targets.isEmpty()) {
            targets = List.of("https://example.com/");
        }
        for (String target : targets) {
            queueModel.addRow(new Object[]{"Queued", target, "0%", "-", "-", "-", "00:00"});
        }
        statusLabel.setText("Starting scan of " + targets.size() + " targets.");
        List<String> finalTargets = targets;
        activeWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                scannerService.scanTargets(finalTargets, update -> SwingUtilities.invokeLater(() -> applyUpdate(update)),
                        message -> publish(message), paused, cancelled).join();
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    activityLog.append(message + "\n");
                }
            }

            @Override
            protected void done() {
                refreshIssues();
                if (!cancelled.get() && !toolRouter.getToolQueue(ToolType.SCANNER).isEmpty()) {
                    statusLabel.setText("Queued scan targets remain, continuing...");
                    startScan();
                    return;
                }
                statusLabel.setText(cancelled.get() ? "Scan cancelled." : "Scan completed.");
            }
        };
        activeWorker.execute();
    }

    private List<String> collectTargetsForScan() {
        Set<String> targets = new LinkedHashSet<>();
        List<HttpExchangeRecord> queued = toolRouter.drainToolQueue(ToolType.SCANNER);
        for (HttpExchangeRecord exchange : queued) {
            String requestUrl = exchange.request().url();
            if (requestUrl != null && !requestUrl.isBlank()) {
                targets.add(requestUrl);
            }
        }
        if (targets.isEmpty()) {
            targets.addAll(scannerService.buildTargetsFromHistory());
        }
        return new ArrayList<>(targets);
    }

    private void applyUpdate(ActiveScannerService.ScanUpdate update) {
        int row = Math.max(0, update.completed() - 1);
        if (row < queueModel.getRowCount()) {
            queueModel.setValueAt("Completed", row, 0);
            queueModel.setValueAt(update.progressPercent() + "%", row, 2);
            queueModel.setValueAt(0, row, 3);
            queueModel.setValueAt(update.completed(), row, 4);
            queueModel.setValueAt(update.completed(), row, 5);
            queueModel.setValueAt(String.format("00:%02d", Math.max(0, update.elapsedMs() / 1000)), row, 6);
        }
        refreshIssues();
    }

    private void refreshQueue() {
        queueTable.setModel(queueModel);
    }

    private void refreshIssues() {
        issuesModel.setRowCount(0);
        for (ScanIssue issue : issueStore.findAll()) {
            issuesModel.addRow(new Object[]{issue.severity(), issue.confidence(), issue.name(), issue.url(), issue.occurrences()});
        }
    }

    private void showSelectedIssue() {
        int row = issuesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        int modelRow = issuesTable.convertRowIndexToModel(row);
        ScanIssue issue = issueStore.findAll().get(modelRow);
        HttpExchangeRecord exchange = findExchangeForUrl(issue.url());
        issueDetails.setText("""
                Title: %s

                Severity: %s
                Confidence: %s
                URL: %s

                %s

                Remediation:
                %s
                """.formatted(
                issue.name(),
                issue.severity(),
                issue.confidence(),
                issue.url(),
                issue.description(),
                issue.remediation()
        ));
        if (exchange != null) {
            issueRequestViewer.setRawText(HttpMessageCodec.toRawRequest(exchange.request()));
            issueResponseViewer.setRawText(exchange.response() == null ? "" : HttpMessageCodec.toRawResponse(exchange.response()));
        } else {
            issueRequestViewer.setRawText("");
            issueResponseViewer.setRawText("");
        }
    }

    private HttpExchangeRecord findExchangeForUrl(String url) {
        List<HttpExchangeRecord> exchanges = historyRepository.findAll();
        for (HttpExchangeRecord exchange : exchanges) {
            String requestUrl = exchange.request().url();
            if (requestUrl != null && (requestUrl.equals(url) || requestUrl.contains(url) || url.contains(requestUrl))) {
                return exchange;
            }
        }
        return exchanges.isEmpty() ? null : exchanges.get(0);
    }

    private void startAutoRefresh() {
        new Timer(2000, e -> refreshIssues()).start();
    }
}
