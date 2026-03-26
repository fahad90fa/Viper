package com.shadowproxy.ui;

import com.shadowproxy.config.AppConfig;
import com.shadowproxy.core.proxy.InterceptionManager;
import com.shadowproxy.core.proxy.ProxyServer;
import com.shadowproxy.core.routing.ToolRouter;
import com.shadowproxy.core.routing.ToolType;
import com.shadowproxy.core.scanner.ScanIssueStore;
import com.shadowproxy.persistence.HistoryRepository;
import com.shadowproxy.persistence.ProjectManager;
import com.shadowproxy.ui.components.IconFactory;
import com.shadowproxy.ui.components.StatusBarPanel;
import com.shadowproxy.ui.components.WorkspaceTabs;
import com.shadowproxy.ui.dialogs.ShadowProxyDialogs;
import com.shadowproxy.ui.modules.ModulePanelFactory;
import com.shadowproxy.ui.state.UiStateStore;
import com.shadowproxy.ui.theme.ThemeManager;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.KeyboardFocusManager;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.Toolkit;
import java.awt.TrayIcon.MessageType;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class MainWindow extends JFrame {
    private final AppConfig appConfig;
    private final HistoryRepository historyRepository;
    private final ToolRouter toolRouter;
    private final ProxyServer proxyServer;
    private final InterceptionManager interceptionManager;
    private final ScanIssueStore scanIssueStore;
    private final UiStateStore uiStateStore;
    private final ProjectManager projectManager;
    private final WorkspaceTabs workspaceTabs = new WorkspaceTabs();
    private final StatusBarPanel statusBar = new StatusBarPanel();
    private final Map<String, JComponent> tabContent = new LinkedHashMap<>();
    private final Timer statusTimer;
    private boolean projectDirty;
    private Path currentProjectFile;
    private TrayIcon trayIcon;
    private boolean welcomeShown;

    public MainWindow(AppConfig appConfig,
                      HistoryRepository historyRepository,
                      ToolRouter toolRouter,
                      ProxyServer proxyServer,
                      InterceptionManager interceptionManager,
                      ScanIssueStore scanIssueStore,
                      UiStateStore uiStateStore) {
        super("ShadowProxy Professional v1.0");
        this.appConfig = appConfig;
        this.historyRepository = historyRepository;
        this.toolRouter = toolRouter;
        this.proxyServer = proxyServer;
        this.interceptionManager = interceptionManager;
        this.scanIssueStore = scanIssueStore;
        this.uiStateStore = uiStateStore;
        this.projectManager = new ProjectManager(historyRepository, scanIssueStore);

        historyRepository.addListener(exchangeRecord -> {
            projectDirty = true;
            statusBar.setProjectDirty(true);
        });

        initializeFrame();
        installWorkspace();
        installActions();
        installMenus();
        installToolbar();
        installStatusBar();
        installTray();
        installCloseHandling();
        statusTimer = new Timer(1000, e -> refreshStatus());
        statusTimer.start();
        refreshStatus();
        applyPersistedGeometry();
        updateWindowTitle();
    }

    public void setProjectDirty(boolean dirty) {
        this.projectDirty = dirty;
        statusBar.setProjectDirty(dirty);
        updateWindowTitle();
    }

    public void showWindow() {
        setVisible(true);
        toFront();
        if (welcomeShown || !uiStateStore.isFirstRun()) {
            return;
        }
        welcomeShown = true;
        uiStateStore.markFirstRunComplete();
        SwingUtilities.invokeLater(() -> ShadowProxyDialogs.showWelcome(this));
    }

    private void initializeFrame() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1280, 720));
        setPreferredSize(new Dimension(1600, 900));
        setLayout(new BorderLayout());
        setIconImage(createFrameIcon());
        getRootPane().setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    private void installWorkspace() {
        workspaceTabs.addWorkspaceTab("Dashboard", icon("dashboard"), ModulePanelFactory.dashboardPanel(appConfig, historyRepository, toolRouter, proxyServer, scanIssueStore), false);
        workspaceTabs.addWorkspaceTab("Target", icon("target"), ModulePanelFactory.targetPanel(historyRepository, scanIssueStore, toolRouter), false);
        workspaceTabs.addWorkspaceTab("Proxy", icon("proxy"), ModulePanelFactory.proxyPanel(appConfig, historyRepository, proxyServer, toolRouter, interceptionManager), false);
        workspaceTabs.addWorkspaceTab("Intruder", icon("intruder"), ModulePanelFactory.intruderPanel(toolRouter, historyRepository), true);
        workspaceTabs.addWorkspaceTab("Repeater", icon("repeater"), ModulePanelFactory.repeaterPanel(toolRouter, historyRepository), true);
        workspaceTabs.addWorkspaceTab("Scanner", icon("scanner"), ModulePanelFactory.scannerPanel(historyRepository, scanIssueStore, toolRouter), true);
        workspaceTabs.addWorkspaceTab("Sequencer", icon("sequencer"), ModulePanelFactory.sequencerPanel(), true);
        workspaceTabs.addWorkspaceTab("Decoder", icon("decoder"), ModulePanelFactory.decoderPanel(), true);
        workspaceTabs.addWorkspaceTab("Comparer", icon("comparer"), ModulePanelFactory.comparerPanel(toolRouter), true);
        workspaceTabs.addWorkspaceTab("Extender", icon("extender"), ModulePanelFactory.extenderPanel(), true);
        workspaceTabs.addWorkspaceTab("Settings", icon("gear"), ModulePanelFactory.settingsPanel(appConfig, toolRouter), false);
        workspaceTabs.setSelectedIndex(Math.min(uiStateStore.loadSelectedTabIndex(), workspaceTabs.getTabCount() - 1));
        workspaceTabs.addChangeListener(e -> uiStateStore.saveSelectedTabIndex(workspaceTabs.getSelectedIndex()));
        add(workspaceTabs, BorderLayout.CENTER);
    }

    private void installMenus() {
        setJMenuBar(buildMenuBar());
    }

    private void installToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.add(newToolbarButton("new", "New Project", e -> actions().get("newProject").actionPerformed(e)));
        toolbar.add(newToolbarButton("open", "Open Project", e -> actions().get("openProject").actionPerformed(e)));
        toolbar.add(newToolbarButton("save", "Save Project", e -> actions().get("saveProject").actionPerformed(e)));
        toolbar.addSeparator();
        toolbar.add(newToolbarButton("play", "Start/Stop Proxy", e -> actions().get("toggleProxy").actionPerformed(e)));
        toolbar.add(newToolbarButton("intercept", "Intercept On/Off", e -> actions().get("toggleIntercept").actionPerformed(e)));
        toolbar.addSeparator();
        toolbar.add(newToolbarButton("trash", "Clear History", e -> actions().get("clearHistory").actionPerformed(e)));
        toolbar.addSeparator();
        toolbar.add(newToolbarButton("gear", "Settings", e -> actions().get("preferences").actionPerformed(e)));
        toolbar.add(newToolbarButton("help", "Help", e -> actions().get("about").actionPerformed(e)));
        JCheckBox showLabels = new JCheckBox("Show text labels", uiStateStore.loadShowToolbarLabels());
        showLabels.addActionListener(e -> {
            uiStateStore.saveShowToolbarLabels(showLabels.isSelected());
            rebuildToolbar(toolbar, showLabels.isSelected());
        });
        toolbar.addSeparator();
        toolbar.add(showLabels);
        add(toolbar, BorderLayout.NORTH);
        rebuildToolbar(toolbar, showLabels.isSelected());
    }

    private void rebuildToolbar(JToolBar toolbar, boolean showLabels) {
        for (int i = 0; i < toolbar.getComponentCount(); i++) {
            if (toolbar.getComponent(i) instanceof JButton button) {
                button.setText(showLabels ? button.getToolTipText() : "");
            }
        }
        toolbar.revalidate();
        toolbar.repaint();
    }

    private void installStatusBar() {
        add(statusBar, BorderLayout.SOUTH);
    }

    private void installTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        trayIcon = new TrayIcon(toTrayImage(createFrameIcon()), "ShadowProxy");
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> setVisible(true));
        java.awt.PopupMenu popup = new java.awt.PopupMenu();
        java.awt.MenuItem showHide = new java.awt.MenuItem("Show/Hide main window");
        showHide.addActionListener(e -> setVisible(!isVisible()));
        java.awt.MenuItem toggleProxy = new java.awt.MenuItem("Start/Stop proxy");
        toggleProxy.addActionListener(e -> actions().get("toggleProxy").actionPerformed(e));
        java.awt.MenuItem exit = new java.awt.MenuItem("Exit");
        exit.addActionListener(e -> actions().get("exit").actionPerformed(e));
        popup.add(showHide);
        popup.add(toggleProxy);
        popup.add(exit);
        trayIcon.setPopupMenu(popup);
        try {
            SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception ignored) {
            trayIcon = null;
        }
    }

    private void installCloseHandling() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (confirmClose()) {
                    persistUiState();
                    shutdown();
                }
            }
        });
    }

    private void applyPersistedGeometry() {
        Rectangle bounds = uiStateStore.loadBounds();
        if (bounds != null) {
            setBounds(bounds);
        } else {
            setSize(1600, 900);
            setLocationRelativeTo(null);
        }
        boolean maximized = uiStateStore.loadMaximized();
        if (maximized) {
            setExtendedState(getExtendedState() | JFrame.MAXIMIZED_BOTH);
        }
    }

    private void refreshStatus() {
        statusBar.setProxyRunning(proxyServer.isRunning(), appConfig.listenHost(), appConfig.listenPort());
        statusBar.setInterceptEnabled(interceptionManager.isInterceptEnabled());
        statusBar.setProjectDirty(projectDirty);
        statusBar.setActivity("Idle", -1, false);
        if (trayIcon != null) {
            trayIcon.setImage(toTrayImage(createFrameIcon()));
        }
    }

    private boolean confirmClose() {
        if (!projectDirty) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Save project before closing?", "ShadowProxy", JOptionPane.YES_NO_CANCEL_OPTION);
        if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }
        if (choice == JOptionPane.YES_OPTION) {
            actions().get("saveProject").actionPerformed(null);
        }
        return true;
    }

    private void persistUiState() {
        uiStateStore.saveBounds(getBounds());
        uiStateStore.saveMaximized((getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH);
        uiStateStore.saveSelectedTabIndex(workspaceTabs.getSelectedIndex());
    }

    private void updateWindowTitle() {
        String projectName = currentProjectFile == null ? "Untitled" : currentProjectFile.getFileName().toString();
        setTitle("ShadowProxy Professional v1.0 - " + projectName + (projectDirty ? " *" : ""));
    }

    private void shutdown() {
        statusTimer.stop();
        if (proxyServer.isRunning()) {
            proxyServer.stop();
        }
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        dispose();
        System.exit(0);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.add(buildFileMenu());
        bar.add(buildEditMenu());
        bar.add(buildViewMenu());
        bar.add(buildToolsMenu());
        bar.add(buildHelpMenu());
        return bar;
    }

    private JMenu buildFileMenu() {
        JMenu menu = new JMenu("File");
        addMenuItem(menu, "New Project", "newProject", KeyStroke.getKeyStroke('N', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Open Project...", "openProject", KeyStroke.getKeyStroke('O', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Save Project", "saveProject", KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Save Project As...", "saveProjectAs", KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        addMenuItem(menu, "Close Project", "closeProject", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Import Burp Project", "importBurp", null);
        addMenuItem(menu, "Import HAR File", "importHar", null);
        addMenuItem(menu, "Import Request/Response from File", "importRequest", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Recent Projects", "recentProjects", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Exit", "exit", KeyStroke.getKeyStroke('Q', InputEvent.CTRL_DOWN_MASK));
        return menu;
    }

    private JMenu buildEditMenu() {
        JMenu menu = new JMenu("Edit");
        addMenuItem(menu, "Cut", "cut", KeyStroke.getKeyStroke('X', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Copy", "copy", KeyStroke.getKeyStroke('C', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Paste", "paste", KeyStroke.getKeyStroke('V', InputEvent.CTRL_DOWN_MASK));
        menu.add(new JSeparator());
        addMenuItem(menu, "Find...", "find", KeyStroke.getKeyStroke('F', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Find Next", "findNext", KeyStroke.getKeyStroke("F3"));
        addMenuItem(menu, "Replace...", "replace", KeyStroke.getKeyStroke('H', InputEvent.CTRL_DOWN_MASK));
        menu.add(new JSeparator());
        addMenuItem(menu, "Preferences...", "preferences", KeyStroke.getKeyStroke(',', InputEvent.CTRL_DOWN_MASK));
        return menu;
    }

    private JMenu buildViewMenu() {
        JMenu menu = new JMenu("View");
        addMenuItem(menu, "Reset Window Layout", "resetLayout", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Show/Hide Proxy History", "toggleHistory", null);
        addMenuItem(menu, "Show/Hide Site Map", "toggleSiteMap", null);
        addMenuItem(menu, "Show/Hide Logger", "toggleLogger", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Increase Font Size", "fontIncrease", KeyStroke.getKeyStroke('=', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Decrease Font Size", "fontDecrease", KeyStroke.getKeyStroke('-', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Reset Font Size", "fontReset", KeyStroke.getKeyStroke('0', InputEvent.CTRL_DOWN_MASK));
        menu.add(new JSeparator());
        JMenu theme = new JMenu("Theme");
        addMenuItem(theme, "Light Theme", "themeLight", null);
        addMenuItem(theme, "Dark Theme", "themeDark", null);
        addMenuItem(theme, "System Default", "themeSystem", null);
        menu.add(theme);
        return menu;
    }

    private JMenu buildToolsMenu() {
        JMenu menu = new JMenu("Tools");
        addMenuItem(menu, "Send to Repeater", "sendRepeater", KeyStroke.getKeyStroke('R', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Send to Intruder", "sendIntruder", KeyStroke.getKeyStroke('I', InputEvent.CTRL_DOWN_MASK));
        addMenuItem(menu, "Send to Scanner", "sendScanner", KeyStroke.getKeyStroke('A', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        addMenuItem(menu, "Send to Comparer", "sendComparer", KeyStroke.getKeyStroke('C', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        menu.add(new JSeparator());
        addMenuItem(menu, "Encode/Decode Selection", "encodeDecode", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Start/Stop Proxy", "toggleProxy", KeyStroke.getKeyStroke('P', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        addMenuItem(menu, "Toggle Intercept", "toggleIntercept", KeyStroke.getKeyStroke('I', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        addMenuItem(menu, "Clear Proxy History", "clearHistory", null);
        addMenuItem(menu, "Clear All Scanner Issues", "clearIssues", null);
        return menu;
    }

    private JMenu buildHelpMenu() {
        JMenu menu = new JMenu("Help");
        addMenuItem(menu, "Documentation", "docs", KeyStroke.getKeyStroke("F1"));
        addMenuItem(menu, "Video Tutorials", "videos", null);
        addMenuItem(menu, "Keyboard Shortcuts Reference", "shortcuts", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "Check for Updates...", "updates", null);
        addMenuItem(menu, "Report a Bug...", "bug", null);
        menu.add(new JSeparator());
        addMenuItem(menu, "About ShadowProxy", "about", null);
        return menu;
    }

    private void addMenuItem(JMenu menu, String title, String actionKey, KeyStroke accelerator) {
        JMenuItem item = new JMenuItem(actions().get(actionKey));
        item.setText(title);
        if (accelerator != null) {
            item.setAccelerator(accelerator);
        }
        menu.add(item);
    }

    private void installActions() {
        for (Map.Entry<String, AbstractAction> entry : actions().entrySet()) {
            getRootPane().getActionMap().put(entry.getKey(), entry.getValue());
        }
        bind("newProject", KeyStroke.getKeyStroke('N', InputEvent.CTRL_DOWN_MASK));
        bind("openProject", KeyStroke.getKeyStroke('O', InputEvent.CTRL_DOWN_MASK));
        bind("saveProject", KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK));
        bind("saveProjectAs", KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        bind("exit", KeyStroke.getKeyStroke('Q', InputEvent.CTRL_DOWN_MASK));
        bind("find", KeyStroke.getKeyStroke('F', InputEvent.CTRL_DOWN_MASK));
        bind("replace", KeyStroke.getKeyStroke('H', InputEvent.CTRL_DOWN_MASK));
        bind("toggleProxy", KeyStroke.getKeyStroke('P', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        bind("toggleIntercept", KeyStroke.getKeyStroke('I', InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        bind("nextTab", KeyStroke.getKeyStroke("ctrl TAB"));
        bind("prevTab", KeyStroke.getKeyStroke("ctrl shift TAB"));
        bind("closeTab", KeyStroke.getKeyStroke('W', InputEvent.CTRL_DOWN_MASK));
        bind("help", KeyStroke.getKeyStroke("F1"));
    }

    private void bind(String actionKey, KeyStroke keyStroke) {
        if (keyStroke != null) {
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, actionKey);
            getRootPane().getActionMap().put(actionKey, actions().get(actionKey));
        }
    }

    private Map<String, AbstractAction> actions() {
        if (cachedActions == null) {
            cachedActions = buildActions();
        }
        return cachedActions;
    }

    private Map<String, AbstractAction> cachedActions;

    private Map<String, AbstractAction> buildActions() {
        Map<String, AbstractAction> map = new LinkedHashMap<>();
        map.put("newProject", new AbstractAction("New Project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!confirmDiscardCurrentProject()) {
                    return;
                }
                historyRepository.clear();
                scanIssueStore.clear();
                currentProjectFile = null;
                setProjectDirty(false);
                updateWindowTitle();
            }
        });
        map.put("openProject", new AbstractAction("Open Project...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = ShadowProxyDialogs.createFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("ShadowProxy Project (*.shadowproject)", "shadowproject", "json"));
                if (chooser.showOpenDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION) {
                    openProject(chooser.getSelectedFile().toPath());
                }
            }
        });
        map.put("saveProject", new AbstractAction("Save Project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentProjectFile == null) {
                    actions().get("saveProjectAs").actionPerformed(e);
                    return;
                }
                saveProject(currentProjectFile);
            }
        });
        map.put("saveProjectAs", new AbstractAction("Save Project As...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = ShadowProxyDialogs.createFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("ShadowProxy Project (*.shadowproject)", "shadowproject", "json"));
                if (chooser.showSaveDialog(MainWindow.this) == JFileChooser.APPROVE_OPTION) {
                    Path selected = chooser.getSelectedFile().toPath();
                    currentProjectFile = ensureProjectExtension(selected);
                    saveProject(currentProjectFile);
                }
            }
        });
        map.put("closeProject", new AbstractAction("Close Project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!confirmDiscardCurrentProject()) {
                    return;
                }
                historyRepository.clear();
                scanIssueStore.clear();
                currentProjectFile = null;
                setProjectDirty(false);
                updateWindowTitle();
            }
        });
        map.put("importBurp", new AbstractAction("Import Burp Project") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShadowProxyDialogs.showNewScan(MainWindow.this);
            }
        });
        map.put("importHar", new AbstractAction("Import HAR File") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShadowProxyDialogs.showNewScan(MainWindow.this);
            }
        });
        map.put("importRequest", new AbstractAction("Import Request/Response from File") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShadowProxyDialogs.showNewScan(MainWindow.this);
            }
        });
        map.put("recentProjects", new AbstractAction("Recent Projects") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(MainWindow.this, "Recent projects list will appear here.");
            }
        });
        map.put("exit", new AbstractAction("Exit") {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispatchEvent(new WindowEvent(MainWindow.this, WindowEvent.WINDOW_CLOSING));
            }
        });
        map.put("cut", new AbstractAction("Cut") {
            @Override public void actionPerformed(ActionEvent e) { focusOwnerAction("cut"); }
        });
        map.put("copy", new AbstractAction("Copy") {
            @Override public void actionPerformed(ActionEvent e) { focusOwnerAction("copy"); }
        });
        map.put("paste", new AbstractAction("Paste") {
            @Override public void actionPerformed(ActionEvent e) { focusOwnerAction("paste"); }
        });
        map.put("find", new AbstractAction("Find...") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showFindReplace(MainWindow.this); }
        });
        map.put("findNext", new AbstractAction("Find Next") {
            @Override public void actionPerformed(ActionEvent e) { focusCurrentEditor(); }
        });
        map.put("replace", new AbstractAction("Replace...") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showFindReplace(MainWindow.this); }
        });
        map.put("preferences", new AbstractAction("Preferences...") {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShadowProxyDialogs.showPreferences(MainWindow.this, uiStateStore.loadTheme());
            }
        });
        map.put("resetLayout", new AbstractAction("Reset Window Layout") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex(0); }
        });
        map.put("toggleHistory", new AbstractAction("Show/Hide Proxy History") {
            @Override public void actionPerformed(ActionEvent e) { toggleTab("Proxy"); }
        });
        map.put("toggleSiteMap", new AbstractAction("Show/Hide Site Map") {
            @Override public void actionPerformed(ActionEvent e) { toggleTab("Target"); }
        });
        map.put("toggleLogger", new AbstractAction("Show/Hide Logger") {
            @Override public void actionPerformed(ActionEvent e) { toggleTab("Scanner"); }
        });
        map.put("fontIncrease", new AbstractAction("Increase Font Size") {
            @Override public void actionPerformed(ActionEvent e) { adjustFontSize(1); }
        });
        map.put("fontDecrease", new AbstractAction("Decrease Font Size") {
            @Override public void actionPerformed(ActionEvent e) { adjustFontSize(-1); }
        });
        map.put("fontReset", new AbstractAction("Reset Font Size") {
            @Override public void actionPerformed(ActionEvent e) { adjustFontSize(0); }
        });
        map.put("themeLight", new AbstractAction("Light Theme") {
            @Override public void actionPerformed(ActionEvent e) {
                ThemeManager.applyTheme(ThemeManager.ThemeChoice.LIGHT);
                uiStateStore.saveTheme(ThemeManager.ThemeChoice.LIGHT);
                SwingUtilities.updateComponentTreeUI(MainWindow.this);
            }
        });
        map.put("themeDark", new AbstractAction("Dark Theme") {
            @Override public void actionPerformed(ActionEvent e) {
                ThemeManager.applyTheme(ThemeManager.ThemeChoice.DARK);
                uiStateStore.saveTheme(ThemeManager.ThemeChoice.DARK);
                SwingUtilities.updateComponentTreeUI(MainWindow.this);
            }
        });
        map.put("themeSystem", new AbstractAction("System Default") {
            @Override public void actionPerformed(ActionEvent e) {
                ThemeManager.applyTheme(ThemeManager.ThemeChoice.SYSTEM);
                uiStateStore.saveTheme(ThemeManager.ThemeChoice.SYSTEM);
                SwingUtilities.updateComponentTreeUI(MainWindow.this);
            }
        });
        map.put("sendRepeater", new AbstractAction("Send to Repeater") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex(4); }
        });
        map.put("sendIntruder", new AbstractAction("Send to Intruder") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex(3); }
        });
        map.put("sendScanner", new AbstractAction("Send to Scanner") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex(5); }
        });
        map.put("sendComparer", new AbstractAction("Send to Comparer") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex(8); }
        });
        map.put("encodeDecode", new AbstractAction("Encode/Decode Selection") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex(7); }
        });
        map.put("toggleProxy", new AbstractAction("Start/Stop Proxy") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (proxyServer.isRunning()) {
                    proxyServer.stop();
                } else {
                    proxyServer.start();
                }
                refreshStatus();
                projectDirty = true;
            }
        });
        map.put("toggleIntercept", new AbstractAction("Toggle Intercept") {
            @Override
            public void actionPerformed(ActionEvent e) {
                interceptionManager.setInterceptEnabled(!interceptionManager.isInterceptEnabled());
                refreshStatus();
                projectDirty = true;
            }
        });
        map.put("clearHistory", new AbstractAction("Clear Proxy History") {
            @Override public void actionPerformed(ActionEvent e) { historyRepository.clear(); projectDirty = true; }
        });
        map.put("clearIssues", new AbstractAction("Clear All Scanner Issues") {
            @Override public void actionPerformed(ActionEvent e) {
                scanIssueStore.clear();
                setProjectDirty(true);
            }
        });
        map.put("docs", new AbstractAction("Documentation") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showAbout(MainWindow.this); }
        });
        map.put("videos", new AbstractAction("Video Tutorials") {
            @Override public void actionPerformed(ActionEvent e) { JOptionPane.showMessageDialog(MainWindow.this, "Tutorial links will be opened here."); }
        });
        map.put("shortcuts", new AbstractAction("Keyboard Shortcuts Reference") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showKeyboardShortcuts(MainWindow.this); }
        });
        map.put("updates", new AbstractAction("Check for Updates") {
            @Override public void actionPerformed(ActionEvent e) { JOptionPane.showMessageDialog(MainWindow.this, "No updates available."); }
        });
        map.put("bug", new AbstractAction("Report a Bug") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showError(MainWindow.this, "Bug reporting is not connected yet.", null); }
        });
        map.put("about", new AbstractAction("About ShadowProxy") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showAbout(MainWindow.this); }
        });
        map.put("nextTab", new AbstractAction("Next Tab") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex((workspaceTabs.getSelectedIndex() + 1) % workspaceTabs.getTabCount()); }
        });
        map.put("prevTab", new AbstractAction("Previous Tab") {
            @Override public void actionPerformed(ActionEvent e) { workspaceTabs.setSelectedIndex((workspaceTabs.getSelectedIndex() - 1 + workspaceTabs.getTabCount()) % workspaceTabs.getTabCount()); }
        });
        map.put("closeTab", new AbstractAction("Close Current Tab") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = workspaceTabs.getSelectedIndex();
                if (idx < 0) {
                    return;
                }
                String title = workspaceTabs.getTitleAt(idx);
                if (Objects.equals(title, "Dashboard") || Objects.equals(title, "Target") || Objects.equals(title, "Proxy") || Objects.equals(title, "Settings")) {
                    return;
                }
                workspaceTabs.removeTabAt(idx);
            }
        });
        map.put("help", new AbstractAction("Help") {
            @Override public void actionPerformed(ActionEvent e) { ShadowProxyDialogs.showAbout(MainWindow.this); }
        });
        return map;
    }

    private boolean confirmDiscardCurrentProject() {
        if (!projectDirty) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Discard current project changes?", "ShadowProxy", JOptionPane.YES_NO_OPTION);
        return choice == JOptionPane.YES_OPTION;
    }

    private void saveProject(Path projectFile) {
        projectManager.save(projectFile, projectFile.getFileName().toString());
        currentProjectFile = projectFile;
        setProjectDirty(false);
        updateWindowTitle();
        JOptionPane.showMessageDialog(this, "Project saved to " + projectFile);
    }

    private void openProject(Path projectFile) {
        if (!confirmDiscardCurrentProject()) {
            return;
        }
        projectManager.load(projectFile);
        currentProjectFile = projectFile;
        setProjectDirty(false);
        updateWindowTitle();
        workspaceTabs.setSelectedIndex(0);
        JOptionPane.showMessageDialog(this, "Project opened from " + projectFile);
    }

    private Path ensureProjectExtension(Path selected) {
        String name = selected.getFileName().toString().toLowerCase();
        if (name.endsWith(".shadowproject") || name.endsWith(".json")) {
            return selected;
        }
        return selected.resolveSibling(selected.getFileName().toString() + ".shadowproject");
    }

    private void focusOwnerAction(String action) {
        java.awt.Component current = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        JComponent focus = current instanceof JComponent component ? component : null;
        if (focus != null && focus.getActionMap().get(action) != null) {
            focus.getActionMap().get(action).actionPerformed(new ActionEvent(focus, ActionEvent.ACTION_PERFORMED, action));
        }
    }

    private void focusCurrentEditor() {
        // Placeholder for wiring a current editor search target.
    }

    private void toggleTab(String title) {
        for (int i = 0; i < workspaceTabs.getTabCount(); i++) {
            if (Objects.equals(workspaceTabs.getTitleAt(i), title)) {
                workspaceTabs.setSelectedIndex(i);
                return;
            }
        }
    }

    private void adjustFontSize(int delta) {
        java.awt.Font font = getFont();
        if (font == null) {
            return;
        }
        if (delta == 0) {
            font = font.deriveFont(12f);
        } else {
            font = font.deriveFont(Math.max(10f, font.getSize2D() + delta));
        }
        javax.swing.UIManager.put("defaultFont", font);
        SwingUtilities.updateComponentTreeUI(this);
    }

    private JButton newToolbarButton(String iconName, String tooltip, java.awt.event.ActionListener listener) {
        JButton button = new JButton(icon(iconName));
        button.setToolTipText(tooltip);
        button.addActionListener(listener);
        button.setFocusPainted(false);
        button.setPreferredSize(new Dimension(34, 34));
        return button;
    }

    private Image createFrameIcon() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        icon("shield").paintIcon(this, g2, 8, 8);
        g2.dispose();
        return image;
    }

    private Image toTrayImage(Image source) {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = image.createGraphics();
        g2.drawImage(source, 0, 0, 32, 32, null);
        g2.dispose();
        return image;
    }

    private Icon icon(String name) {
        return IconFactory.of(name, new java.awt.Color(104, 151, 187), 24);
    }
}
