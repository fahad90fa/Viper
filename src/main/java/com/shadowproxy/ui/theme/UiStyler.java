package com.shadowproxy.ui.theme;

import org.jfree.chart.ChartPanel;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

public final class UiStyler {
    private UiStyler() {
    }

    public static void applyBurpStyle(Component component) {
        if (component == null) {
            return;
        }
        Color panelBg = color("Panel.background", new Color(60, 63, 65));
        Color text = color("Label.foreground", new Color(187, 187, 187));
        Color editorBg = color("TextArea.background", new Color(43, 43, 43));
        Color editorFg = color("TextArea.foreground", new Color(187, 187, 187));
        Color tableBg = color("Table.background", panelBg);
        Color tableFg = color("Table.foreground", text);
        Color selectionBg = color("Table.selectionBackground", new Color(74, 136, 199));
        Color selectionFg = color("Table.selectionForeground", Color.WHITE);
        Color border = color("Component.borderColor", new Color(85, 85, 85));

        style(component, panelBg, text, editorBg, editorFg, tableBg, tableFg, selectionBg, selectionFg, border);
    }

    private static void style(Component component,
                              Color panelBg,
                              Color text,
                              Color editorBg,
                              Color editorFg,
                              Color tableBg,
                              Color tableFg,
                              Color selectionBg,
                              Color selectionFg,
                              Color border) {
        if (component instanceof JTabbedPane tabs) {
            tabs.setOpaque(true);
            tabs.setBackground(panelBg);
            tabs.setForeground(text);
            tabs.setFont(tabs.getFont().deriveFont(Font.PLAIN, 12f));
        } else if (component instanceof JToolBar toolBar) {
            toolBar.setOpaque(true);
            toolBar.setBackground(panelBg);
            toolBar.setFloatable(false);
            toolBar.setBorderPainted(false);
        } else if (component instanceof JMenuBar menuBar) {
            menuBar.setOpaque(true);
            menuBar.setBackground(panelBg);
            menuBar.setForeground(text);
        } else if (component instanceof JMenu menu) {
            menu.setOpaque(true);
            menu.setBackground(panelBg);
            menu.setForeground(text);
        } else if (component instanceof JMenuItem menuItem) {
            menuItem.setOpaque(true);
            menuItem.setBackground(panelBg);
            menuItem.setForeground(text);
        } else if (component instanceof JTable table) {
            table.setBackground(tableBg);
            table.setForeground(tableFg);
            table.setSelectionBackground(selectionBg);
            table.setSelectionForeground(selectionFg);
            table.setGridColor(border);
            table.setFillsViewportHeight(true);
            table.setRowHeight(Math.max(table.getRowHeight(), 24));
            table.setFont(table.getFont().deriveFont(12f));
        } else if (component instanceof JTree tree) {
            tree.setBackground(panelBg);
            tree.setForeground(text);
            tree.setRowHeight(Math.max(tree.getRowHeight(), 24));
            tree.setFont(tree.getFont().deriveFont(12f));
        } else if (component instanceof JProgressBar progressBar) {
            progressBar.setBackground(panelBg);
            progressBar.setForeground(selectionBg);
        } else if (component instanceof JTextComponent textComponent) {
            textComponent.setBackground(editorBg);
            textComponent.setForeground(editorFg);
            textComponent.setCaretColor(editorFg);
            textComponent.setSelectionColor(selectionBg);
            textComponent.setSelectedTextColor(selectionFg);
            textComponent.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        } else if (component instanceof JLabel label) {
            label.setForeground(text);
        } else if (component instanceof JButton button) {
            button.setBackground(panelBg);
            button.setForeground(text);
            button.setFocusPainted(false);
        } else if (component instanceof JPanel panel) {
            panel.setOpaque(true);
            panel.setBackground(panelBg);
        } else if (component instanceof JScrollPane scrollPane) {
            scrollPane.getViewport().setBackground(panelBg);
            scrollPane.setBackground(panelBg);
        } else if (component instanceof JSplitPane splitPane) {
            splitPane.setDividerSize(4);
        } else if (component instanceof ChartPanel chartPanel) {
            chartPanel.setBackground(panelBg);
        }

        if (component instanceof JComponent jc) {
            jc.setFont(jc.getFont() != null ? jc.getFont().deriveFont(12f) : new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                style(child, panelBg, text, editorBg, editorFg, tableBg, tableFg, selectionBg, selectionFg, border);
            }
        }
    }

    private static Color color(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }
}
