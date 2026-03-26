package com.shadowproxy.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class WorkspaceTabs extends JTabbedPane {
    private final Map<Component, DetachedTab> detachedTabs = new HashMap<>();

    public WorkspaceTabs() {
        setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    int index = getUI().tabForCoordinate(WorkspaceTabs.this, e.getX(), e.getY());
                    if (index >= 0 && isCloseable(index)) {
                        removeTabAt(index);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }
        });
    }

    public void addWorkspaceTab(String title, javax.swing.Icon icon, Component component, boolean closeable) {
        addTab(title, icon, component);
        int index = indexOfComponent(component);
        setTabComponentAt(index, new TabHeader(title, icon, component, closeable));
    }

    public void updateTabTitle(Component component, String title) {
        int index = indexOfComponent(component);
        if (index >= 0) {
            setTitleAt(index, title);
            Component header = getTabComponentAt(index);
            if (header instanceof TabHeader tabHeader) {
                tabHeader.setTitle(title);
            }
        }
    }

    public void setTabCloseable(Component component, boolean closeable) {
        int index = indexOfComponent(component);
        if (index >= 0 && getTabComponentAt(index) instanceof TabHeader tabHeader) {
            tabHeader.setCloseable(closeable);
        }
    }

    private void showPopup(MouseEvent e) {
        int index = getUI().tabForCoordinate(this, e.getX(), e.getY());
        if (index < 0) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem detach = new JMenuItem("Detach Tab");
        detach.addActionListener(ev -> detachTab(index));
        JMenuItem close = new JMenuItem("Close Tab");
        close.addActionListener(ev -> {
            if (isCloseable(index)) {
                removeTabAt(index);
            }
        });
        menu.add(detach);
        if (isCloseable(index)) {
            menu.add(close);
        }
        menu.show(this, e.getX(), e.getY());
    }

    private void detachTab(int index) {
        if (index < 0 || index >= getTabCount()) {
            return;
        }
        Component component = getComponentAt(index);
        String title = getTitleAt(index);
        javax.swing.Icon icon = getIconAt(index);
        Component header = getTabComponentAt(index);
        boolean closeable = header instanceof TabHeader tabHeader ? tabHeader.closeable : true;

        removeTabAt(index);
        DetachedTab detached = new DetachedTab(index, component, title, icon, closeable);
        detachedTabs.put(component, detached);

        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 650));
        frame.setLocationByPlatform(true);
        frame.setContentPane(wrapDetachedComponent(detached));
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                reattach(detached);
            }
        });
        frame.setVisible(true);
    }

    private JPanel wrapDetachedComponent(DetachedTab detached) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        wrapper.add(detached.component(), BorderLayout.CENTER);
        return wrapper;
    }

    private void reattach(DetachedTab detached) {
        if (!detachedTabs.containsKey(detached.component())) {
            return;
        }
        detachedTabs.remove(detached.component());
        int index = Math.min(detached.index(), getTabCount());
        insertTab(detached.title(), detached.icon(), detached.component(), null, index);
        setTabComponentAt(index, new TabHeader(detached.title(), detached.icon(), detached.component(), detached.closeable()));
        setSelectedComponent(detached.component());
    }

    private boolean isCloseable(int index) {
        Component header = getTabComponentAt(index);
        return !(header instanceof TabHeader tabHeader) || tabHeader.closeable;
    }

    private final class TabHeader extends JPanel {
        private final JLabel titleLabel = new JLabel();
        private final JButton closeButton = new JButton("×");
        private final Component component;
        private boolean closeable;

        private TabHeader(String title, javax.swing.Icon icon, Component component, boolean closeable) {
            super(new BorderLayout(4, 0));
            this.component = Objects.requireNonNull(component);
            this.closeable = closeable;
            setOpaque(false);
            titleLabel.setText(title);
            titleLabel.setIcon(icon);
            closeButton.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
            closeButton.setFocusPainted(false);
            closeButton.setOpaque(false);
            closeButton.addActionListener(e -> {
                int index = indexOfComponent(this.component);
                if (index >= 0 && this.closeable) {
                    removeTabAt(index);
                }
            });
            add(titleLabel, BorderLayout.CENTER);
            add(closeButton, BorderLayout.EAST);
            setCloseable(closeable);
        }

        private void setTitle(String title) {
            titleLabel.setText(title);
        }

        private void setCloseable(boolean closeable) {
            this.closeable = closeable;
            closeButton.setVisible(closeable);
        }
    }

    private record DetachedTab(int index, Component component, String title, javax.swing.Icon icon, boolean closeable) {
    }
}
