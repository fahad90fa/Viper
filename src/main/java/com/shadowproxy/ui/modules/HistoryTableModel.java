package com.shadowproxy.ui.modules;

import com.shadowproxy.domain.http.HttpExchangeRecord;
import com.shadowproxy.persistence.HistoryListener;
import com.shadowproxy.persistence.HistoryRepository;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class HistoryTableModel extends AbstractTableModel implements HistoryListener {
    private static final String[] COLUMNS = {"#", "Method", "URL", "Status", "Tool"};
    private final HistoryRepository historyRepository;
    private final List<HttpExchangeRecord> rows = new ArrayList<>();

    public HistoryTableModel(HistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
        this.rows.addAll(historyRepository.findAll());
        historyRepository.addListener(this);
    }

    public HttpExchangeRecord rowAt(int rowIndex) {
        return rows.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        HttpExchangeRecord row = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> rowIndex + 1;
            case 1 -> row.request().method();
            case 2 -> row.request().url();
            case 3 -> row.response() != null ? row.response().statusCode() : "-";
            case 4 -> row.sourceTool();
            default -> "";
        };
    }

    @Override
    public void onExchangeSaved(HttpExchangeRecord exchangeRecord) {
        SwingUtilities.invokeLater(() -> {
            int row = rows.size();
            rows.add(exchangeRecord);
            fireTableRowsInserted(row, row);
        });
    }
}
