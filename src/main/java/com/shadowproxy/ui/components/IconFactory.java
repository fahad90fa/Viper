package com.shadowproxy.ui.components;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.HashMap;
import java.util.Map;

public final class IconFactory {
    private static final Map<String, Icon> CACHE = new HashMap<>();

    private IconFactory() {
    }

    public static Icon of(String name, Color color, int size) {
        String key = name + ":" + color.getRGB() + ":" + size;
        return CACHE.computeIfAbsent(key, k -> new VectorIcon(name, color, size));
    }

    private static final class VectorIcon implements Icon {
        private final String name;
        private final Color color;
        private final int size;

        private VectorIcon(String name, Color color, int size) {
            this.name = name;
            this.color = color;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.translate(x, y);
            g2.setColor(color);
            switch (name) {
                case "folder" -> folder(g2);
                case "file" -> file(g2);
                case "save" -> save(g2);
                case "open" -> open(g2);
                case "new" -> plus(g2);
                case "play" -> play(g2);
                case "pause" -> pause(g2);
                case "trash" -> trash(g2);
                case "gear" -> gear(g2);
                case "help" -> help(g2);
                case "shield" -> shield(g2);
                case "proxy" -> proxy(g2);
                case "intercept" -> intercept(g2);
                case "dashboard" -> dashboard(g2);
                case "target" -> target(g2);
                case "scanner" -> scanner(g2);
                case "intruder" -> intruder(g2);
                case "repeater" -> repeater(g2);
                case "sequencer" -> sequencer(g2);
                case "decoder" -> decoder(g2);
                case "comparer" -> comparer(g2);
                case "extender" -> extender(g2);
                case "search" -> search(g2);
                case "clock" -> clock(g2);
                case "success" -> success(g2);
                case "warning" -> warning(g2);
                case "error" -> error(g2);
                default -> generic(g2);
            }
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        private void generic(Graphics2D g2) {
            g2.fill(new RoundRectangle2D.Double(2, 2, size - 4, size - 4, 4, 4));
        }

        private void folder(Graphics2D g2) {
            g2.fill(new Path2D.Double() {{
                moveTo(2, 6);
                lineTo(7, 6);
                lineTo(9, 4);
                lineTo(size - 3, 4);
                lineTo(size - 3, size - 3);
                lineTo(2, size - 3);
                closePath();
            }});
        }

        private void file(Graphics2D g2) {
            g2.fill(new RoundRectangle2D.Double(4, 2, size - 8, size - 4, 4, 4));
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fillRect(size - 9, 4, 4, 6);
        }

        private void save(Graphics2D g2) {
            g2.fillRect(3, 3, size - 6, size - 6);
            g2.setColor(new Color(255, 255, 255, 180));
            g2.fillRect(5, 5, size - 10, 4);
            g2.fillRect(6, size / 2, size - 12, size / 2 - 6);
        }

        private void open(Graphics2D g2) {
            g2.drawRect(3, 6, size - 6, size - 9);
            g2.fillRect(4, 4, size / 3, 4);
            g2.fillRect(3, 5, size / 4, 3);
        }

        private void plus(Graphics2D g2) {
            int mid = size / 2;
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(mid, 4, mid, size - 4);
            g2.drawLine(4, mid, size - 4, mid);
        }

        private void play(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(5, 3);
            path.lineTo(size - 4, size / 2.0);
            path.lineTo(5, size - 3);
            path.closePath();
            g2.fill(path);
        }

        private void pause(Graphics2D g2) {
            g2.fillRect(5, 3, 4, size - 6);
            g2.fillRect(size - 9, 3, 4, size - 6);
        }

        private void trash(Graphics2D g2) {
            g2.fillRect(4, 6, size - 8, size - 9);
            g2.fillRect(3, 4, size - 6, 3);
            g2.fillRect(6, 2, size - 12, 2);
        }

        private void gear(Graphics2D g2) {
            int mid = size / 2;
            g2.fillOval(5, 5, size - 10, size - 10);
            g2.setColor(Color.WHITE);
            g2.fillOval(mid - 3, mid - 3, 6, 6);
        }

        private void help(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(3, 3, size - 6, size - 6);
            g2.drawString("?", size / 2f - 3, size - 5);
        }

        private void shield(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(size / 2.0, 3);
            path.lineTo(size - 4, 6);
            path.lineTo(size - 6, size / 2.5);
            path.lineTo(size / 2.0, size - 3);
            path.lineTo(6, size / 2.5);
            path.lineTo(4, 6);
            path.closePath();
            g2.fill(path);
        }

        private void proxy(Graphics2D g2) {
            g2.fillRoundRect(3, 6, size - 6, size - 12, 6, 6);
            g2.setColor(Color.WHITE);
            g2.fillRect(size / 2 - 1, 4, 2, size - 8);
        }

        private void intercept(Graphics2D g2) {
            g2.fillRoundRect(3, 3, size - 6, size - 6, 6, 6);
            g2.setColor(Color.WHITE);
            g2.fillRect(6, size / 2 - 1, size - 12, 2);
        }

        private void dashboard(Graphics2D g2) {
            g2.fillRoundRect(3, 3, size - 6, size - 6, 6, 6);
            g2.setColor(Color.WHITE);
            g2.fillRect(6, 6, size / 3, size / 3);
            g2.fillRect(size / 2, 6, size / 3, size / 3);
            g2.fillRect(6, size / 2, size / 3, size / 3);
            g2.fillRect(size / 2, size / 2, size / 3, size / 3);
        }

        private void target(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.2f));
            int mid = size / 2;
            g2.drawOval(3, 3, size - 6, size - 6);
            g2.drawOval(6, 6, size - 12, size - 12);
            g2.drawLine(4, mid, size - 4, mid);
            g2.drawLine(mid, 4, mid, size - 4);
        }

        private void scanner(Graphics2D g2) {
            g2.fillRoundRect(3, 3, size - 6, size - 6, 6, 6);
            g2.setColor(Color.WHITE);
            g2.fillRect(6, 6, size - 12, 2);
            g2.fillRect(6, 10, size - 12, 2);
            g2.fillRect(6, 14, size - 12, 2);
        }

        private void intruder(Graphics2D g2) {
            g2.fillOval(4, 4, size - 8, size - 8);
            g2.setColor(Color.WHITE);
            g2.fillRect(size / 2 - 1, 4, 2, size - 8);
            g2.fillRect(4, size / 2 - 1, size - 8, 2);
        }

        private void repeater(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(3, 5, size - 8, size - 10, 5, 5);
            g2.drawLine(5, 8, size - 5, size - 8);
        }

        private void sequencer(Graphics2D g2) {
            g2.fillRect(4, size - 6, 2, 2);
            g2.fillRect(7, size - 10, 2, 6);
            g2.fillRect(10, size - 14, 2, 10);
            g2.fillRect(13, size - 9, 2, 5);
            g2.fillRect(16, size - 16, 2, 12);
        }

        private void decoder(Graphics2D g2) {
            g2.fillRoundRect(3, 3, size - 6, size - 6, 5, 5);
            g2.setColor(Color.WHITE);
            g2.fillRect(6, 6, size - 12, 2);
            g2.fillRect(6, 10, size - 16, 2);
            g2.fillRect(6, 14, size - 20, 2);
        }

        private void comparer(Graphics2D g2) {
            g2.fillRect(4, 4, (size - 8) / 2 - 1, size - 8);
            g2.fillRect(size / 2 + 1, 4, (size - 8) / 2 - 1, size - 8);
        }

        private void extender(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawRoundRect(3, 3, size - 6, size - 6, 5, 5);
            g2.drawLine(8, size / 2, size - 8, size / 2);
            g2.drawLine(size / 2, 8, size / 2, size - 8);
        }

        private void search(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawOval(4, 4, size - 10, size - 10);
            g2.drawLine(size - 7, size - 7, size - 3, size - 3);
        }

        private void clock(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.0f));
            g2.drawOval(3, 3, size - 6, size - 6);
            g2.drawLine(size / 2, size / 2, size / 2, 7);
            g2.drawLine(size / 2, size / 2, size - 8, size / 2);
        }

        private void success(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(4, size / 2, size / 2 - 1, size - 5);
            g2.drawLine(size / 2 - 1, size - 5, size - 4, 5);
        }

        private void warning(Graphics2D g2) {
            Path2D path = new Path2D.Double();
            path.moveTo(size / 2.0, 3);
            path.lineTo(size - 3, size - 4);
            path.lineTo(3, size - 4);
            path.closePath();
            g2.fill(path);
        }

        private void error(Graphics2D g2) {
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawLine(4, 4, size - 4, size - 4);
            g2.drawLine(size - 4, 4, 4, size - 4);
        }
    }
}
