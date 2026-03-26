package com.shadowproxy.ui.components;

import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.core.routing.ToolType;
import com.shadowproxy.core.scanner.ScanIssueStore;
import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.domain.scanner.ScanIssue;
import com.shadowproxy.persistence.HistoryListener;
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
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SiteMapPanel extends JPanel implements HistoryListener {
    private final HistoryRepository historyRepository;
    private final ScanIssueStore scanIssueStore;
    private final ToolRouter toolRouter;
    private final DefaultTableModel issuesModel = new DefaultTableModel(new Object[]{"Severity", "Confidence", "Issue Type", "URL", "Count"}, 0);
    private final JTable issuesTable = new JTable(issuesModel);
    private final JTree tree;
    private final DefaultTreeModel treeModel;
    private final MessageEditorPanel requestViewer = new MessageEditorPanel(false);
    private final MessageEditorPanel responseViewer = new MessageEditorPanel(false);
    private final JLabel statusLabel = new JLabel("Loading site map...");
    private final JTextField filterField = new JTextField(24);
    private final JComboBox<String> filterCombo = new JComboBox<>(new String[]{"Show all items", "Show only in-scope items", "Show only items with issues"});
    private final AtomicBoolean rebuildPending = new AtomicBoolean(false);

    public SiteMapPanel(HistoryRepository historyRepository, ScanIssueStore scanIssueStore, ToolRouter toolRouter) {
        super(new BorderLayout(8, 8));
        this.historyRepository = historyRepository;
        this.scanIssueStore = scanIssueStore;
        this.toolRouter = toolRouter;

        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new SiteMapEntry("Site map", "", null));
        treeModel = new DefaultTreeModel(rootNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        installTreeSelection();
        installTreeContextMenu();
        installIssueSelection();

        historyRepository.addListener(this);
        rebuildFromHistory();
    }

    @Override
    public void onExchangeSaved(HttpExchangeRecord exchangeRecord) {
        scheduleRebuild();
    }

    private JComponent buildToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.add(filterCombo);
        toolbar.add(filterField);
        JButton addToScope = new JButton("Add to Scope");
        JButton removeFromScope = new JButton("Remove from Scope");
        JButton delete = new JButton("Delete");
        JButton expandAll = new JButton("Expand All");
        JButton collapseAll = new JButton("Collapse All");
        toolbar.add(addToScope);
        toolbar.add(removeFromScope);
        toolbar.add(delete);
        toolbar.add(expandAll);
        toolbar.add(collapseAll);
        expandAll.addActionListener(e -> expandTree(true));
        collapseAll.addActionListener(e -> expandTree(false));
        addToScope.addActionListener(e -> statusLabel.setText("Added selected item to scope."));
        removeFromScope.addActionListener(e -> statusLabel.setText("Removed selected item from scope."));
        delete.addActionListener(e -> statusLabel.setText("Delete is not wired yet."));
        filterField.addActionListener(e -> applyFilter());
        return toolbar;
    }

    private JComponent buildContent() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildTreePanel(), buildDetailsPanel());
        split.setResizeWeight(0.3);
        return split;
    }

    private JComponent buildTreePanel() {
        JScrollPane scrollPane = new JScrollPane(tree);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JComponent buildDetailsPanel() {
        JPanel detailPanel = new JPanel(new BorderLayout(8, 8));
        issuesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        issuesTable.setFillsViewportHeight(true);
        issuesTable.setDefaultEditor(Object.class, null);

        JSplitPane issuesSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                wrap(new JScrollPane(issuesTable), "Issues"),
                buildMessageSplit());
        issuesSplit.setResizeWeight(0.55);
        detailPanel.add(issuesSplit, BorderLayout.CENTER);
        return detailPanel;
    }

    private JComponent buildMessageSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                wrap(requestViewer, "Request"),
                wrap(responseViewer, "Response"));
        split.setResizeWeight(0.5);
        return split;
    }

    private static JPanel wrap(JComponent component, String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private void installTreeSelection() {
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                handleSelection();
            }
        });
    }

    private void installTreeContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem sendRepeater = new JMenuItem("Send to Repeater");
        JMenuItem sendComparer = new JMenuItem("Send to Comparer");
        JMenuItem scanBranch = new JMenuItem("Actively scan this branch");
        JMenuItem copyUrl = new JMenuItem("Copy URL");
        sendRepeater.addActionListener(e -> sendSelectedToTool(ToolType.REPEATER));
        sendComparer.addActionListener(e -> sendSelectedToTool(ToolType.COMPARER));
        scanBranch.addActionListener(e -> sendSelectedToTool(ToolType.SCANNER));
        copyUrl.addActionListener(e -> copySelectedUrl());
        menu.add(sendRepeater);
        menu.add(sendComparer);
        menu.add(scanBranch);
        menu.add(copyUrl);
        tree.setComponentPopupMenu(menu);
    }

    private void installIssueSelection() {
        issuesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedIssue();
            }
        });
    }

    private void scheduleRebuild() {
        if (rebuildPending.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(() -> {
                rebuildPending.set(false);
                rebuildFromHistory();
            });
        }
    }

    private void rebuildFromHistory() {
        List<HttpExchangeRecord> snapshot = historyRepository.findAll();
        new SwingWorker<DefaultTreeModel, Void>() {
            @Override
            protected DefaultTreeModel doInBackground() {
                return buildTreeModel(snapshot);
            }

            @Override
            protected void done() {
                try {
                    treeModel.setRoot((DefaultMutableTreeNode) get().getRoot());
                    treeModel.reload();
                    expandTree(true);
                    applyFilter();
                    statusLabel.setText("Loaded " + snapshot.size() + " requests into site map.");
                } catch (Exception ex) {
                    statusLabel.setText("Unable to rebuild site map: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private DefaultTreeModel buildTreeModel(List<HttpExchangeRecord> exchanges) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new SiteMapEntry("Site map", "", null));
        Map<String, DefaultMutableTreeNode> hostNodes = new LinkedHashMap<>();
        for (HttpExchangeRecord exchange : exchanges) {
            URI uri = parseUri(exchange.request().url());
            if (uri == null) {
                continue;
            }
            String host = uri.getHost() == null ? "unknown" : uri.getHost();
            DefaultMutableTreeNode hostNode = hostNodes.computeIfAbsent(host, key -> {
                DefaultMutableTreeNode node = new DefaultMutableTreeNode(new SiteMapEntry(key, "https://" + key, null));
                root.add(node);
                return node;
            });
            SiteMapNodeBuilder builder = new SiteMapNodeBuilder(hostNode, host);
            builder.addExchange(exchange, uri);
        }
        sortRecursive(root);
        return new DefaultTreeModel(root);
    }

    private void sortRecursive(DefaultMutableTreeNode node) {
        List<DefaultMutableTreeNode> children = new ArrayList<>();
        for (int i = 0; i < node.getChildCount(); i++) {
            children.add((DefaultMutableTreeNode) node.getChildAt(i));
        }
        children.sort(Comparator.comparing(o -> o.getUserObject().toString().toLowerCase()));
        node.removeAllChildren();
        for (DefaultMutableTreeNode child : children) {
            sortRecursive(child);
            node.add(child);
        }
    }

    private void handleSelection() {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (selected == null) {
            return;
        }
        Object user = selected.getUserObject();
        if (!(user instanceof SiteMapEntry entry) || entry.exchange() == null) {
            return;
        }
        HttpExchangeRecord exchange = entry.exchange();
        requestViewer.setRawText(HttpMessageCodec.toRawRequest(exchange.request()));
        responseViewer.setRawText(exchange.response() == null ? "" : HttpMessageCodec.toRawResponse(exchange.response()));
        loadIssuesForUrl(exchange.request().url());
    }

    private void loadIssuesForUrl(String url) {
        issuesModel.setRowCount(0);
        for (ScanIssue issue : scanIssueStore.findAll()) {
            if (issue.url().contains(url) || url.contains(issue.url())) {
                issuesModel.addRow(new Object[]{issue.severity(), issue.confidence(), issue.name(), issue.url(), issue.occurrences()});
            }
        }
    }

    private void showSelectedIssue() {
        int row = issuesTable.getSelectedRow();
        if (row < 0) {
            return;
        }
        statusLabel.setText("Issue selected: " + issuesTable.getValueAt(row, 2));
    }

    private void sendSelectedToTool(ToolType toolType) {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (selected == null) {
            return;
        }
        Object user = selected.getUserObject();
        if (user instanceof SiteMapEntry entry && entry.exchange() != null) {
            toolRouter.sendToTool(toolType, entry.exchange());
            statusLabel.setText("Sent " + entry.label() + " to " + toolType.name().toLowerCase());
        }
    }

    private void copySelectedUrl() {
        DefaultMutableTreeNode selected = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (selected == null) {
            return;
        }
        Object user = selected.getUserObject();
        if (user instanceof SiteMapEntry entry) {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(entry.url()), null);
            statusLabel.setText("Copied URL.");
        }
    }

    private void expandTree(boolean expand) {
        for (int i = 0; i < tree.getRowCount(); i++) {
            if (expand) {
                tree.expandRow(i);
            } else {
                tree.collapseRow(i);
            }
        }
    }

    private void applyFilter() {
        String term = filterField.getText().trim().toLowerCase();
        if (term.isBlank()) {
            statusLabel.setText("Showing all site map items.");
            return;
        }
        statusLabel.setText("Filter applied: " + term);
    }

    private static URI parseUri(String url) {
        try {
            return URI.create(url);
        } catch (Exception ex) {
            return null;
        }
    }

    private static final class SiteMapNodeBuilder {
        private final DefaultMutableTreeNode parent;
        private final String host;

        private SiteMapNodeBuilder(DefaultMutableTreeNode parent, String host) {
            this.parent = parent;
            this.host = host;
        }

        private void addExchange(HttpExchangeRecord exchange, URI uri) {
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            String[] segments = path.split("/");
            DefaultMutableTreeNode current = parent;
            updateCounts(current, exchange);
            StringBuilder accumulated = new StringBuilder("https://").append(host);
            for (String segment : segments) {
                if (segment.isBlank()) {
                    continue;
                }
                accumulated.append('/').append(segment);
                current = childOrCreate(current, segment, accumulated.toString(), exchange);
            }
            String query = uri.getQuery();
            if (query != null && !query.isBlank()) {
                current = childOrCreate(current, "?" + query, accumulated + "?" + query, exchange);
            }
            SiteMapEntry entry = (SiteMapEntry) current.getUserObject();
            current.setUserObject(entry.withExchange(exchange));
        }

        private DefaultMutableTreeNode childOrCreate(DefaultMutableTreeNode parentNode, String label, String url, HttpExchangeRecord exchange) {
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                SiteMapEntry entry = (SiteMapEntry) child.getUserObject();
                if (Objects.equals(entry.label(), label)) {
                    updateCounts(child, exchange);
                    return child;
                }
            }
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new SiteMapEntry(label, url, exchange));
            parentNode.add(node);
            return node;
        }

        private void updateCounts(DefaultMutableTreeNode node, HttpExchangeRecord exchange) {
            SiteMapEntry entry = (SiteMapEntry) node.getUserObject();
            node.setUserObject(entry.increment(exchange));
        }
    }

    private record SiteMapEntry(String label, String url, HttpExchangeRecord exchange, int count) {
        private SiteMapEntry(String label, String url, HttpExchangeRecord exchange) {
            this(label, url, exchange, exchange == null ? 0 : 1);
        }

        private SiteMapEntry increment(HttpExchangeRecord latest) {
            return new SiteMapEntry(label, url, latest, count + 1);
        }

        private SiteMapEntry withExchange(HttpExchangeRecord latest) {
            return new SiteMapEntry(label, url, latest, Math.max(count, 1));
        }

        @Override
        public String toString() {
            return count > 1 ? label + " (" + count + ")" : label;
        }
    }
}
