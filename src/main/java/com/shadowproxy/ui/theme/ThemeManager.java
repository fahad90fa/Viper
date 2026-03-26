package com.shadowproxy.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.UIManager;
import java.awt.Font;

public final class ThemeManager {
    private ThemeManager() {
    }

    public enum ThemeChoice {
        LIGHT,
        DARK,
        SYSTEM
    }

    public static void applyTheme(ThemeChoice choice) {
        try {
            switch (choice) {
                case LIGHT -> UIManager.setLookAndFeel(new FlatLightLaf());
                case DARK -> UIManager.setLookAndFeel(new FlatDarkLaf());
                case SYSTEM -> UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            FlatLaf.updateUI();
        } catch (Exception ignored) {
            // Fall back to the platform defaults if FlatLaf cannot be installed.
        }
        configureDefaults();
    }

    public static ThemeChoice parse(String value) {
        if (value == null) {
            return ThemeChoice.DARK;
        }
        try {
            return ThemeChoice.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return ThemeChoice.DARK;
        }
    }

    public static Font uiFont() {
        Font font = UIManager.getFont("Label.font");
        return font != null ? font : new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    private static void configureDefaults() {
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 6);
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.rowHeight", 24);
        UIManager.put("Tree.rowHeight", 24);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("defaultFont", uiFont().deriveFont(12f));
        UIManager.put("Panel.background", new java.awt.Color(60, 63, 65));
        UIManager.put("MenuBar.background", new java.awt.Color(60, 63, 65));
        UIManager.put("Menu.background", new java.awt.Color(60, 63, 65));
        UIManager.put("MenuItem.background", new java.awt.Color(60, 63, 65));
        UIManager.put("TextArea.background", new java.awt.Color(43, 43, 43));
        UIManager.put("TextArea.foreground", new java.awt.Color(187, 187, 187));
        UIManager.put("TextField.background", new java.awt.Color(43, 43, 43));
        UIManager.put("TextField.foreground", new java.awt.Color(187, 187, 187));
        UIManager.put("Table.background", new java.awt.Color(60, 63, 65));
        UIManager.put("Table.foreground", new java.awt.Color(187, 187, 187));
        UIManager.put("Table.selectionBackground", new java.awt.Color(74, 136, 199));
        UIManager.put("Table.selectionForeground", java.awt.Color.WHITE);
        UIManager.put("Tree.background", new java.awt.Color(60, 63, 65));
        UIManager.put("Tree.foreground", new java.awt.Color(187, 187, 187));
        UIManager.put("TabbedPane.background", new java.awt.Color(60, 63, 65));
        UIManager.put("TabbedPane.foreground", new java.awt.Color(187, 187, 187));
        UIManager.put("TabbedPane.selectedBackground", new java.awt.Color(74, 136, 199));
        UIManager.put("TabbedPane.selectedForeground", java.awt.Color.WHITE);
        UIManager.put("ToolBar.background", new java.awt.Color(60, 63, 65));
        UIManager.put("ProgressBar.foreground", new java.awt.Color(74, 136, 199));
    }
}
