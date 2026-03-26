package com.shadowproxy.ui.components;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.InputMap;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MessageEditorPanel extends JPanel {
    private final boolean editable;
    private final RSyntaxTextArea rawArea = new RSyntaxTextArea(18, 80);
    private final RSyntaxTextArea prettyArea = new RSyntaxTextArea(18, 80);
    private final JTextArea hexArea = new JTextArea();
    private final DefaultTableModel paramsModel = new DefaultTableModel(new Object[]{"Type", "Name", "Value"}, 0);
    private final DefaultTableModel headersModel = new DefaultTableModel(new Object[]{"Header", "Value"}, 0);
    private final JTextArea renderArea = new JTextArea();
    private final JPanel searchBar = new JPanel(new BorderLayout(6, 0));
    private final JTextField searchField = new JTextField();
    private final JLabel searchStatus = new JLabel("0 matches");
    private final JCheckBox regexBox = new JCheckBox("Regex");
    private final JCheckBox matchCaseBox = new JCheckBox("Match case");
    private int currentMatchIndex = -1;
    private final List<int[]> matches = new ArrayList<>();

    public MessageEditorPanel(boolean editable) {
        this.editable = editable;
        setLayout(new BorderLayout());
        buildUi();
    }

    public String getRawText() {
        return rawArea.getText();
    }

    public void setRawText(String text) {
        rawArea.setText(text == null ? "" : text);
        rawArea.setCaretPosition(0);
        refreshDerivedViews();
    }

    public void setEditable(boolean editable) {
        rawArea.setEditable(editable);
    }

    public void appendText(String text) {
        rawArea.append(text);
        refreshDerivedViews();
    }

    public void focusSearch() {
        searchBar.setVisible(true);
        searchField.requestFocusInWindow();
    }

    private void buildUi() {
        rawArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        rawArea.setCodeFoldingEnabled(true);
        rawArea.setBracketMatchingEnabled(true);
        rawArea.setEditable(editable);
        rawArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rawArea.getDocument().addDocumentListener(SimpleDocumentListener.onChange(e -> refreshDerivedViews()));

        prettyArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
        prettyArea.setCodeFoldingEnabled(true);
        prettyArea.setEditable(false);
        prettyArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        hexArea.setEditable(false);
        hexArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        renderArea.setEditable(false);
        renderArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        searchBar.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        searchBar.add(buildSearchRow(), BorderLayout.CENTER);
        searchBar.setVisible(false);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Raw", wrap(new RTextScrollPane(rawArea)));
        tabs.addTab("Pretty", wrap(new RTextScrollPane(prettyArea)));
        tabs.addTab("Hex", wrap(new javax.swing.JScrollPane(hexArea)));
        tabs.addTab("Params", buildParamsPanel());
        tabs.addTab("Headers", buildHeadersPanel());
        tabs.addTab("Render", wrap(new javax.swing.JScrollPane(renderArea)));

        add(searchBar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        installPopup(rawArea);
        installPopup(prettyArea);
        installPopup(hexArea);
        installPopup(renderArea);
        installSearchShortcuts();
        refreshDerivedViews();
    }

    private void installSearchShortcuts() {
        bindSearchShortcut(rawArea);
        bindSearchShortcut(prettyArea);
        bindSearchShortcut(hexArea);
        bindSearchShortcut(renderArea);
    }

    private void bindSearchShortcut(JTextArea area) {
        InputMap inputMap = area.getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_DOWN_MASK), "shadowproxy-find");
        area.getActionMap().put("shadowproxy-find", new javax.swing.AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                focusSearch();
            }
        });
    }

    private JPanel buildSearchRow() {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(new JLabel("Find:"));
        left.add(searchField);
        JButton previous = new JButton("Previous");
        JButton next = new JButton("Next");
        left.add(previous);
        left.add(next);
        left.add(matchCaseBox);
        left.add(regexBox);
        row.add(left, BorderLayout.WEST);
        row.add(searchStatus, BorderLayout.EAST);
        searchField.setColumns(24);
        next.addActionListener(e -> findNext(1));
        previous.addActionListener(e -> findNext(-1));
        searchField.addActionListener(e -> findNext(1));
        return row;
    }

    private JComponent buildParamsPanel() {
        JTable table = new JTable(paramsModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new javax.swing.JScrollPane(table), BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add");
        JButton remove = new JButton("Remove");
        toolbar.add(add);
        toolbar.add(remove);
        add.addActionListener(e -> paramsModel.addRow(new Object[]{"Query", "name", "value"}));
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                paramsModel.removeRow(row);
            }
        });
        panel.add(toolbar, BorderLayout.NORTH);
        return panel;
    }

    private JComponent buildHeadersPanel() {
        JTable table = new JTable(headersModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new javax.swing.JScrollPane(table), BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("Add");
        JButton remove = new JButton("Remove");
        toolbar.add(add);
        toolbar.add(remove);
        add.addActionListener(e -> headersModel.addRow(new Object[]{"Header", "Value"}));
        remove.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                headersModel.removeRow(row);
            }
        });
        panel.add(toolbar, BorderLayout.NORTH);
        return panel;
    }

    private JComponent wrap(JComponent child) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(child, BorderLayout.CENTER);
        return wrapper;
    }

    private void installPopup(JTextArea area) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem copyAsCurl = new JMenuItem("Copy as curl");
        copyAsCurl.addActionListener(e -> area.copy());
        menu.add(copyAsCurl);
        menu.add(new JMenuItem(new javax.swing.AbstractAction("Send to Decoder") {
            @Override
            public void actionPerformed(ActionEvent e) {
                javax.swing.JOptionPane.showMessageDialog(MessageEditorPanel.this, "Send to Decoder action is wired at module level.");
            }
        }));
        area.setComponentPopupMenu(menu);
    }

    private void refreshDerivedViews() {
        String raw = rawArea.getText();
        prettyArea.setText(pretty(raw));
        hexArea.setText(hex(raw.getBytes(StandardCharsets.UTF_8)));
        renderArea.setText(renderPreview(raw));
        refreshParams(raw);
        refreshHeaders(raw);
        updateSearchHighlights();
    }

    private void refreshParams(String raw) {
        paramsModel.setRowCount(0);
        int question = raw.indexOf('?');
        if (question < 0) {
            return;
        }
        String query = raw.substring(question + 1).split("\\s", 2)[0];
        for (String pair : query.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] kv = pair.split("=", 2);
            paramsModel.addRow(new Object[]{"Query", decodeComponent(kv[0]), kv.length > 1 ? decodeComponent(kv[1]) : ""});
        }
    }

    private void refreshHeaders(String raw) {
        headersModel.setRowCount(0);
        String[] lines = raw.split("\\R");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int idx = line.indexOf(':');
            if (idx > 0) {
                headersModel.addRow(new Object[]{line.substring(0, idx).trim(), line.substring(idx + 1).trim()});
            }
        }
    }

    private String pretty(String raw) {
        String trimmed = raw.strip();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return prettyJson(trimmed);
        }
        if (trimmed.startsWith("<")) {
            return trimmed.replace("><", ">\n<");
        }
        return raw;
    }

    private String prettyJson(String raw) {
        try {
            Object value = new com.fasterxml.jackson.databind.ObjectMapper().readValue(raw, Object.class);
            return new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception ex) {
            return "Unable to render, showing raw.\n\n" + raw;
        }
    }

    private String hex(byte[] data) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < data.length; i += 16) {
            builder.append(String.format(Locale.ROOT, "%08x  ", i));
            StringBuilder ascii = new StringBuilder();
            for (int j = 0; j < 16; j++) {
                int index = i + j;
                if (index < data.length) {
                    int b = data[index] & 0xff;
                    builder.append(String.format(Locale.ROOT, "%02x ", b));
                    ascii.append(b >= 32 && b < 127 ? (char) b : '.');
                } else {
                    builder.append("   ");
                    ascii.append(' ');
                }
                if (j == 7) {
                    builder.append(' ');
                }
            }
            builder.append(" ").append(ascii).append('\n');
        }
        return builder.toString();
    }

    private String renderPreview(String raw) {
        if (raw.isBlank()) {
            return "No content to render.";
        }
        if (raw.contains("<html") || raw.contains("<HTML")) {
            return "Rendering is sandboxed and may not be fully accurate.\n\n[HTML preview available]";
        }
        return "Rendering is sandboxed and may not be fully accurate.\n\n" + raw;
    }

    private String decodeComponent(String value) {
        try {
            return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }

    private void findNext(int direction) {
        String term = searchField.getText();
        matches.clear();
        currentMatchIndex = -1;
        if (term.isBlank()) {
            updateSearchHighlights();
            return;
        }
        String haystack = matchCaseBox.isSelected() ? rawArea.getText() : rawArea.getText().toLowerCase(Locale.ROOT);
        String needle = matchCaseBox.isSelected() ? term : term.toLowerCase(Locale.ROOT);
        int index = 0;
        while (index >= 0 && index < haystack.length()) {
            index = haystack.indexOf(needle, index);
            if (index >= 0) {
                matches.add(new int[]{index, index + needle.length()});
                index += Math.max(needle.length(), 1);
            }
        }
        if (matches.isEmpty()) {
            searchStatus.setText("0 matches");
            updateSearchHighlights();
            return;
        }
        currentMatchIndex = direction >= 0 ? 0 : matches.size() - 1;
        searchStatus.setText(matches.size() + " matches");
        updateSearchHighlights();
        try {
            int[] current = matches.get(currentMatchIndex);
            rawArea.setCaretPosition(current[0]);
            rawArea.moveCaretPosition(current[1]);
        } catch (Exception ignored) {
        }
    }

    private void updateSearchHighlights() {
        Highlighter highlighter = rawArea.getHighlighter();
        highlighter.removeAllHighlights();
        if (matches.isEmpty()) {
            return;
        }
        for (int i = 0; i < matches.size(); i++) {
            int[] span = matches.get(i);
            try {
                Color color = i == currentMatchIndex ? new Color(255, 165, 0, 120) : new Color(255, 255, 0, 100);
                highlighter.addHighlight(span[0], span[1], new DefaultHighlighter.DefaultHighlightPainter(color));
            } catch (BadLocationException ignored) {
            }
        }
    }

    public interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        static SimpleDocumentListener onChange(java.util.function.Consumer<javax.swing.event.DocumentEvent> consumer) {
            return new SimpleDocumentListener() {
                @Override
                public void insertUpdate(javax.swing.event.DocumentEvent e) {
                    consumer.accept(e);
                }

                @Override
                public void removeUpdate(javax.swing.event.DocumentEvent e) {
                    consumer.accept(e);
                }

                @Override
                public void changedUpdate(javax.swing.event.DocumentEvent e) {
                    consumer.accept(e);
                }
            };
        }
    }
}
