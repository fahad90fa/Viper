package com.shadowproxy.ui.dialogs;

import com.shadowproxy.ui.components.IconFactory;
import com.shadowproxy.ui.theme.ThemeManager;
import com.shadowproxy.ui.theme.UiStyler;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionListener;

public final class ShadowProxyDialogs {
    private ShadowProxyDialogs() {
    }

    public static void showWelcome(Frame owner) {
        JDialog dialog = createDialog(owner, "Welcome to ShadowProxy!", 760, 460);
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JTextArea text = new JTextArea("""
                Welcome to ShadowProxy!

                Quick Setup Wizard helps you configure the listener, CA certificate, browser trust, and theme.
                Manual configuration is always available from Settings.
                """);
        text.setEditable(false);
        text.setOpaque(false);
        JButton wizard = new JButton("Quick Setup Wizard");
        JButton manual = new JButton("Manual configuration");
        wizard.addActionListener(e -> JOptionPane.showMessageDialog(dialog, "Setup wizard will be connected to the settings workflow."));
        manual.addActionListener(e -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(manual);
        actions.add(wizard);
        root.add(new JLabel(IconFactory.of("shield", new java.awt.Color(80, 160, 255), 48)), BorderLayout.WEST);
        root.add(text, BorderLayout.CENTER);
        root.add(actions, BorderLayout.SOUTH);
        finishDialog(dialog, root);
        dialog.setVisible(true);
    }

    public static void showAbout(Frame owner) {
        JOptionPane.showMessageDialog(owner,
                "ShadowProxy Professional v1.0\nBurp-inspired desktop security testing workspace.",
                "About ShadowProxy",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showNewScan(Frame owner) {
        JDialog dialog = createDialog(owner, "New Scan", 560, 360);
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel form = new JPanel(new GridLayout(0, 1, 8, 8));
        form.add(new JLabel("Target URL or host:"));
        form.add(new JTextField("http://example.com"));
        JCheckBox crawl = new JCheckBox("Crawl before scanning", true);
        JCheckBox scope = new JCheckBox("Scan only in-scope items", false);
        form.add(crawl);
        form.add(scope);
        form.add(new JLabel("Scan configuration:"));
        form.add(new JComboBox<>(new String[]{"Quick scan (passive only)", "Standard scan (balanced)", "Deep scan (thorough, slow)"}));
        root.add(form, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton start = new JButton("Start Scan");
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());
        buttons.add(cancel);
        buttons.add(start);
        root.add(buttons, BorderLayout.SOUTH);
        finishDialog(dialog, root);
        dialog.setVisible(true);
    }

    public static void showPreferences(Frame owner, ThemeManager.ThemeChoice currentTheme) {
        JDialog dialog = createDialog(owner, "Preferences", 860, 620);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("User Interface", buildThemePanel(currentTheme));
        tabs.addTab("Proxy", buildSimpleSettingsPanel("Proxy listeners, intercept options, SSL settings"));
        tabs.addTab("Scanner", buildSimpleSettingsPanel("Live scanning and issue definitions"));
        tabs.addTab("Network", buildSimpleSettingsPanel("Upstream proxy, SOCKS, and timeout values"));
        root.add(tabs, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton("Restore Defaults"));
        buttons.add(new JButton("Cancel"));
        buttons.add(new JButton("Save & Apply"));
        root.add(buttons, BorderLayout.SOUTH);
        finishDialog(dialog, root);
        dialog.setVisible(true);
    }

    public static void showKeyboardShortcuts(Frame owner) {
        JDialog dialog = createDialog(owner, "Keyboard Shortcuts Reference", 860, 620);
        String[] columns = {"Action", "Shortcut", "Context"};
        Object[][] rows = {
                {"New Project", "Ctrl+N", "Global"},
                {"Save Project", "Ctrl+S", "Global"},
                {"Find", "Ctrl+F", "Message editor"},
                {"Send to Repeater", "Ctrl+R", "History / tables"},
                {"Toggle Proxy", "Ctrl+Shift+P", "Global"}
        };
        javax.swing.JTable table = new javax.swing.JTable(rows, columns);
        finishDialog(dialog, new JScrollPane(table));
        dialog.setVisible(true);
    }

    public static void showFindReplace(Frame owner) {
        JDialog dialog = createDialog(owner, "Find and Replace", 620, 260);
        JPanel root = new JPanel(new GridLayout(0, 1, 8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(new JLabel("Find:"));
        root.add(new JTextField());
        root.add(new JLabel("Replace with:"));
        root.add(new JTextField());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton("Find Next"));
        buttons.add(new JButton("Replace"));
        buttons.add(new JButton("Replace All"));
        buttons.add(new JButton("Close"));
        root.add(buttons);
        finishDialog(dialog, root);
        dialog.setVisible(true);
    }

    public static void showExportReport(Frame owner) {
        JDialog dialog = createDialog(owner, "Export Scanner Report", 640, 420);
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(new JComboBox<>(new String[]{"HTML", "XML", "JSON", "PDF"}), BorderLayout.NORTH);
        root.add(new JScrollPane(new JList<>(new String[]{"Executive summary", "Vulnerability details", "Evidence", "Remediation"})), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton("Generate"));
        buttons.add(new JButton("Cancel"));
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    public static void showInstallCa(Frame owner) {
        JDialog dialog = createDialog(owner, "Install CA Certificate", 820, 540);
        JTabbedPane tabs = new JTabbedPane();
        for (String name : new String[]{"Windows", "macOS", "Linux", "Firefox", "iOS", "Android"}) {
            tabs.addTab(name, buildSimpleSettingsPanel("Step-by-step installation instructions for " + name + "."));
        }
        JButton export = new JButton("Export Certificate...");
        JPanel root = new JPanel(new BorderLayout());
        root.add(tabs, BorderLayout.CENTER);
        root.add(export, BorderLayout.SOUTH);
        finishDialog(dialog, root);
        dialog.setVisible(true);
    }

    public static void showComment(Frame owner) {
        JDialog dialog = createDialog(owner, "Add Comment", 500, 300);
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(new JScrollPane(new JTextArea()), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(new JButton("OK"));
        buttons.add(new JButton("Cancel"));
        root.add(buttons, BorderLayout.SOUTH);
        finishDialog(dialog, root);
        dialog.setVisible(true);
    }

    public static void showError(Frame owner, String message, Throwable throwable) {
        JTextArea area = new JTextArea(message + "\n\n" + (throwable != null ? throwable.getMessage() : ""));
        area.setEditable(false);
        area.setPreferredSize(new Dimension(640, 280));
        JOptionPane.showMessageDialog(owner, new JScrollPane(area), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static JFileChooser createFileChooser() {
        return new JFileChooser();
    }

    private static JDialog createDialog(Frame owner, String title, int width, int height) {
        JDialog dialog = new JDialog(owner, title, true);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    private static void finishDialog(JDialog dialog, java.awt.Container content) {
        dialog.setContentPane(content);
        UiStyler.applyBurpStyle(dialog.getContentPane());
        UiStyler.applyBurpStyle(content);
    }

    private static JPanel buildThemePanel(ThemeManager.ThemeChoice currentTheme) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel radios = new JPanel(new GridLayout(0, 1));
        javax.swing.JRadioButton light = new javax.swing.JRadioButton("Light theme");
        javax.swing.JRadioButton dark = new javax.swing.JRadioButton("Dark theme");
        javax.swing.JRadioButton system = new javax.swing.JRadioButton("System default");
        javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
        group.add(light);
        group.add(dark);
        group.add(system);
        switch (currentTheme) {
            case LIGHT -> light.setSelected(true);
            case SYSTEM -> system.setSelected(true);
            default -> dark.setSelected(true);
        }
        radios.add(light);
        radios.add(dark);
        radios.add(system);
        panel.add(radios, BorderLayout.NORTH);
        panel.add(buildSimpleSettingsPanel("Preview panel with fonts, colors, and sample controls."), BorderLayout.CENTER);
        return panel;
    }

    private static JPanel buildSimpleSettingsPanel(String description) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(new JLabel(description));
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JTextField("example"));
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JCheckBox("Enabled", true));
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JButton("Apply"));
        return panel;
    }
}
