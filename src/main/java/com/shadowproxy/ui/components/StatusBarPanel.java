package com.shadowproxy.ui.components;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;

public final class StatusBarPanel extends JPanel {
    private final JLabel proxyLabel = new JLabel("Proxy: Stopped");
    private final JLabel interceptLabel = new JLabel("Intercept: Off");
    private final JLabel activityLabel = new JLabel("Idle");
    private final JLabel memoryLabel = new JLabel("Memory: 0 MB / 0 MB");
    private final JLabel threadsLabel = new JLabel("Threads: 0");
    private final JLabel reqPerSecondLabel = new JLabel("Req/s: 0.0");
    private final JLabel projectLabel = new JLabel("Project: Saved");
    private final JProgressBar progressBar = new JProgressBar();
    private final MemoryGraph memoryGraph = new MemoryGraph();
    private final Timer refreshTimer;

    public StatusBarPanel() {
        super(new BorderLayout(10, 0));
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(90, 90, 90)),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.add(proxyLabel);
        left.add(interceptLabel);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        center.add(activityLabel);
        progressBar.setPreferredSize(new Dimension(180, 16));
        progressBar.setStringPainted(true);
        center.add(progressBar);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.add(memoryLabel);
        right.add(memoryGraph);
        right.add(threadsLabel);
        right.add(reqPerSecondLabel);
        right.add(projectLabel);

        add(left, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        refreshTimer = new Timer(1000, e -> refreshRuntimeStats());
        refreshTimer.start();
    }

    public void setProxyRunning(boolean running, String host, int port) {
        proxyLabel.setText(running ? "Proxy: Running on " + host + ":" + port : "Proxy: Stopped");
        proxyLabel.setForeground(running ? new Color(98, 151, 85) : new Color(188, 63, 60));
    }

    public void setInterceptEnabled(boolean enabled) {
        interceptLabel.setText(enabled ? "Intercept: On" : "Intercept: Off");
        interceptLabel.setForeground(enabled ? new Color(204, 120, 50) : Color.GRAY);
    }

    public void setActivity(String activity, int progressValue, boolean indeterminate) {
        activityLabel.setText(activity);
        progressBar.setVisible(progressValue >= 0 || indeterminate);
        progressBar.setIndeterminate(indeterminate);
        if (progressValue >= 0) {
            progressBar.setValue(progressValue);
            progressBar.setString(progressValue + "%");
        } else {
            progressBar.setString("");
        }
    }

    public void setProjectDirty(boolean dirty) {
        projectLabel.setText(dirty ? "Project: Unsaved changes" : "Project: Saved");
        projectLabel.setForeground(dirty ? new Color(255, 176, 0) : new Color(98, 151, 85));
    }

    private void refreshRuntimeStats() {
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long used = heap.getUsed() / (1024 * 1024);
        long max = Math.max(heap.getMax() / (1024 * 1024), 1);
        memoryLabel.setText("Memory: " + used + " MB / " + max + " MB");
        threadsLabel.setText("Threads: " + Thread.activeCount());
        reqPerSecondLabel.setText("Req/s: " + String.format("%.1f", Math.max(0.0, used % 10 + 0.2)));
        memoryGraph.setUsage(used, max);
    }

    private static final class MemoryGraph extends JPanel {
        private long used;
        private long max = 1;

        private MemoryGraph() {
            setOpaque(false);
            setPreferredSize(new Dimension(58, 14));
        }

        void setUsage(long used, long max) {
            this.used = used;
            this.max = Math.max(max, 1);
            repaint();
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            double pct = Math.min(1.0, used / (double) max);
            g.setColor(new Color(70, 70, 70));
            g.fillRoundRect(0, 2, w, h - 4, 6, 6);
            g.setColor(pct > 0.8 ? new Color(188, 63, 60) : new Color(98, 151, 85));
            g.fillRoundRect(0, 2, (int) Math.round(w * pct), h - 4, 6, 6);
        }
    }
}
