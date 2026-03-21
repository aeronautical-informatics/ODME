package odme.odmeeditor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Panel showing constraints that apply within a single entity (intra-entity).
 * Double-click a row to view/edit the constraint expression.
 */
public class IntraEntityConstraint extends JPanel {

    private static final long serialVersionUID = 1L;
    public static JTable table;
    public static DefaultTableModel model;

    public IntraEntityConstraint() {
        setLayout(new GridLayout(1, 0));

        final String[] columnNames = {"IntraEntityConstraints"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable();
        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension());
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setEnabled(false);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    Point point = e.getPoint();
                    int row = table.rowAtPoint(point);
                    String constraints = (String) target.getModel().getValueAt(row, 0);
                    updateTableData(constraints);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
        setNullRowsToVariableTable();
    }

    private static void setNullRowsToVariableTable() {
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[]{""});
        }
    }

    public static void setNullToAllRows() {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[]{""});
        }
    }

    public void showConstraintsInTable(String[] nodesToSelectedNode) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);
        for (String value : nodesToSelectedNode) {
            if (value == null) {
                model.addRow(new Object[]{""});
            } else {
                model.addRow(new Object[]{value});
            }
        }
        setNullRowsToVariableTable();
    }

    private void updateTableData(String constraints) {
        JTextArea constraintsField = new JTextArea(10, 30);
        constraintsField.setLineWrap(true);
        constraintsField.setWrapStyleWord(true);
        constraintsField.setText(constraints);

        Object[] message = {"Constraints:", constraintsField};
        JOptionPane.showConfirmDialog(Main.frame, message, "Intra-Entity Constraint",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        // Read-only view for now — future: support in-place editing via tree model
    }
}
