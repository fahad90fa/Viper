package com.shadowproxy.ui.state;

import com.shadowproxy.ui.theme.ThemeManager;

import java.awt.Rectangle;
import java.util.prefs.Preferences;

public final class UiStateStore {
    private static final String KEY_THEME = "theme";
    private static final String KEY_BOUNDS_X = "boundsX";
    private static final String KEY_BOUNDS_Y = "boundsY";
    private static final String KEY_BOUNDS_W = "boundsW";
    private static final String KEY_BOUNDS_H = "boundsH";
    private static final String KEY_MAXIMIZED = "maximized";
    private static final String KEY_SELECTED_TAB = "selectedTab";
    private static final String KEY_SHOW_TOOLBAR_LABELS = "showToolbarLabels";
    private static final String KEY_FIRST_RUN_DONE = "firstRunDone";

    private final Preferences preferences = Preferences.userNodeForPackage(UiStateStore.class);

    public ThemeManager.ThemeChoice loadTheme() {
        return ThemeManager.parse(preferences.get(KEY_THEME, ThemeManager.ThemeChoice.DARK.name()));
    }

    public void saveTheme(ThemeManager.ThemeChoice choice) {
        preferences.put(KEY_THEME, choice.name());
    }

    public Rectangle loadBounds() {
        int x = preferences.getInt(KEY_BOUNDS_X, Integer.MIN_VALUE);
        int y = preferences.getInt(KEY_BOUNDS_Y, Integer.MIN_VALUE);
        int w = preferences.getInt(KEY_BOUNDS_W, Integer.MIN_VALUE);
        int h = preferences.getInt(KEY_BOUNDS_H, Integer.MIN_VALUE);
        if (x == Integer.MIN_VALUE || y == Integer.MIN_VALUE || w <= 0 || h <= 0) {
            return null;
        }
        return new Rectangle(x, y, w, h);
    }

    public void saveBounds(Rectangle bounds) {
        if (bounds == null) {
            return;
        }
        preferences.putInt(KEY_BOUNDS_X, bounds.x);
        preferences.putInt(KEY_BOUNDS_Y, bounds.y);
        preferences.putInt(KEY_BOUNDS_W, bounds.width);
        preferences.putInt(KEY_BOUNDS_H, bounds.height);
    }

    public boolean loadMaximized() {
        return preferences.getBoolean(KEY_MAXIMIZED, true);
    }

    public void saveMaximized(boolean maximized) {
        preferences.putBoolean(KEY_MAXIMIZED, maximized);
    }

    public int loadSelectedTabIndex() {
        return preferences.getInt(KEY_SELECTED_TAB, 0);
    }

    public void saveSelectedTabIndex(int index) {
        preferences.putInt(KEY_SELECTED_TAB, Math.max(index, 0));
    }

    public boolean loadShowToolbarLabels() {
        return preferences.getBoolean(KEY_SHOW_TOOLBAR_LABELS, true);
    }

    public void saveShowToolbarLabels(boolean value) {
        preferences.putBoolean(KEY_SHOW_TOOLBAR_LABELS, value);
    }

    public boolean isFirstRun() {
        return !preferences.getBoolean(KEY_FIRST_RUN_DONE, false);
    }

    public void markFirstRunComplete() {
        preferences.putBoolean(KEY_FIRST_RUN_DONE, true);
    }
}
