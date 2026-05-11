package langton.ui;

import langton.metrics.PerformanceMetrics.BenchmarkResult;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

// Bottom panel with status labels and the benchmark results table.
@SuppressWarnings({"this-escape", "serial"})
public class PerformancePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final BenchmarkResultTableModel tableModel;
    private final JTable resultsTable;

    private final JLabel fpsLabel;
    private final JLabel stepCountLabel;
    private final JLabel modeLabel;
    private final JLabel antCountLabel;

    // Builds the status strip and results table.
    public PerformancePanel() {
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xc8c8c8)));
        setBackground(new Color(0xececec));

        fpsLabel = statusLabel("FPS: --");
        stepCountLabel = statusLabel("Step: 0");
        modeLabel = statusLabel("Mode: Sequential");
        antCountLabel = statusLabel("Ants: 0");

        JPanel status = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        status.setOpaque(false);
        status.add(stepCountLabel);
        status.add(fpsLabel);
        status.add(antCountLabel);
        status.add(modeLabel);

        tableModel = new BenchmarkResultTableModel();
        resultsTable = new JTable(tableModel);
        resultsTable.setAutoCreateRowSorter(false);
        resultsTable.setFillsViewportHeight(true);
        resultsTable.setRowHeight(22);
        sizeColumns();

        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setPreferredSize(new Dimension(800, 180));
        scroll.setBorder(new EmptyBorder(4, 4, 4, 4));

        add(status, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    // Sets the preferred width of each column.
    private void sizeColumns() {
        TableColumnModel cm = resultsTable.getColumnModel();
        int[] widths = {60, 70, 80, 110, 110, 80, 90};
        for (int i = 0; i < widths.length && i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            col.setPreferredWidth(widths[i]);
        }
    }

    // Builds a plain status label.
    private static JLabel statusLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 12f));
        return l;
    }

    // Adds a result row (safely from any thread).
    public void addResult(BenchmarkResult result) {
        SwingUtilities.invokeLater(() -> {
            int row = tableModel.addRow(result);
            resultsTable.scrollRectToVisible(resultsTable.getCellRect(row, 0, true));
        });
    }

    // Clears the results table.
    public void clearResults() {
        SwingUtilities.invokeLater(tableModel::clear);
    }

    // Updates the FPS label.
    public void setFps(double fps) {
        fpsLabel.setText(String.format("FPS: %.1f", fps));
    }

    // Updates the step label.
    public void setStepCount(long step) {
        stepCountLabel.setText("Step: " + step);
    }

    // Updates the mode label.
    public void setModeLabel(String modeText) {
        modeLabel.setText("Mode: " + modeText);
    }

    // Updates the ant count label.
    public void setAntCount(int ants) {
        antCountLabel.setText("Ants: " + ants);
    }

    // Table model that backs the benchmark results JTable.
    private static final class BenchmarkResultTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        // Column headings.
        private static final String[] COLUMN_NAMES = {
                "Ants", "Regions", "Steps",
                "Seq Time (ms)", "Par Time (ms)", "Speedup", "Efficiency"
        };

        private final List<BenchmarkResult> rows = new ArrayList<>();

        // Appends a row and tells the table to refresh.
        int addRow(BenchmarkResult r) {
            rows.add(r);
            int row = rows.size() - 1;
            fireTableRowsInserted(row, row);
            return row;
        }

        // Removes every row.
        void clear() {
            int last = rows.size() - 1;
            rows.clear();
            if (last >= 0) {
                fireTableRowsDeleted(0, last);
            }
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        // All columns use the default renderer.
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return Object.class;
        }

        // Returns the formatted value for a cell.
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            BenchmarkResult r = rows.get(rowIndex);
            return switch (columnIndex) {
                case 0 -> r.getNumAnts();
                case 1 -> r.getNumRegions();
                case 2 -> r.getNumSteps();
                case 3 -> String.format("%.2f", r.getSeqTimeMs());
                case 4 -> String.format("%.2f", r.getParTimeMs());
                case 5 -> String.format("%.2fx", r.getSpeedup());
                case 6 -> String.format("%.2f", r.getEfficiency());
                default -> "";
            };
        }

        // Cells are read-only.
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }
    }
}
