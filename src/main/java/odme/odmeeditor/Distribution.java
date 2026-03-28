package odme.odmeeditor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Distribution panel — shows the probability distribution assigned to each
 * variable/node in the SES model (Node Name, Variable Name, Distribution Name, Details).
 */
public class Distribution extends JPanel {

    private static final long serialVersionUID = 1L;
    public static JTable table;
    private static DefaultTableModel model;

    public Distribution() {
        setLayout(new GridLayout(1, 0));

        String[] columnNames = {"Node Name", "Variable Name", "Distribution Name", "Details"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable();
        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension());
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionBackground(new Color(217, 237, 146));
        table.setSelectionForeground(new Color(188, 71, 73));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
        setNullRowsToDistributionTable();
    }

    public static void setNullToAllRows() {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[]{"", "", "", ""});
        }
    }

    public static void setNullRowsToDistributionTable() {
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[]{"", "", "", ""});
        }
    }

    public void showNodeValuesInDistributionTable(String nodeName, String[] distributionDetails) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0);
        int a = 0;
        for (String value : distributionDetails) {
            if (value == null) {
                model.addRow(new Object[]{"", "", "", ""});
            } else if (a == 0) {
                String[] properties = value.split(",");
                if (properties.length >= 3) {
                    model.addRow(new Object[]{nodeName, properties[0], properties[1], properties[2]});
                }
                a = 1;
            }
        }
        for (String value : distributionDetails) {
            if (a == 1) { a = 0; continue; }
            if (value != null) {
                String[] properties = value.split(",");
                if (properties.length >= 3) {
                    model.addRow(new Object[]{nodeName, properties[0], properties[1], properties[2]});
                }
            }
        }
        setNullRowsToDistributionTable();
    }
}
