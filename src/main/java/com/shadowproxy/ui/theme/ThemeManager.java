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
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.rowHeight", 24);
        UIManager.put("Tree.rowHeight", 24);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("defaultFont", uiFont().deriveFont(12f));
    }
}
