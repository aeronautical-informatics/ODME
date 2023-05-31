package odme.odmeeditor;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import odme.jtreetograph.JtreeToGraphDelete;
import odme.jtreetograph.JtreeToGraphVariables;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <h1>Constraint</h1>
 * <p>
 * Creates an window to show added constraint in the SES model. The window
 * contains a table and constraint of the selected aspect node is displayed in
 * that table.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class Constraint extends JPanel {

	private static final long serialVersionUID = 1L;
	public static JTable table;
    public static DefaultTableModel model;

    public Constraint() {
    	
        setLayout(new GridLayout(1, 0));

        final String[] columnNames = {"Constraints"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable();
        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension());
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setEnabled(false);
        // row listener
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
            	// Double click
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();
                    Point point = e.getPoint();
                    int row = table.rowAtPoint(point);
                    String constraints = (String) target.getModel().getValueAt(row, 0);
                    updateTableData(constraints);
                }
            }
        });
        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        // Add the scroll pane to this panel.
        add(scrollPane);
        setNullRowsToVariableTable();
    }

    private static void setNullRowsToVariableTable() {
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {""});
        }
    }

    public static void setNullToAllRows() {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {""});
        }
    }

    public void showConstraintsInTable(String[] nodesToSelectedNode) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content
        for (String value : nodesToSelectedNode) {
            if (value == null) {
                model.addRow(new Object[] {""});
            } else {
                model.addRow(new Object[] {value});
            }
        }
        setNullRowsToVariableTable();
    }

    private void updateTableData(String constraints) {
        // multiple input for variable---------------------------------
        JTextArea constraintsField = new JTextArea(10, 30);
        constraintsField.setLineWrap(true);
        constraintsField.setWrapStyleWord(true);
        constraintsField.setText(constraints);
        
        String constraintsOld = constraints;
        Object[] message = {"Constraints:", constraintsField};
        
        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Update", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            constraints = constraintsField.getText();
            JtreeToGraphDelete.deleteConstraintFromScenarioTableForUpdate(
            		JtreeToGraphVariables.selectedNodeCellForVariableUpdate, constraintsOld, constraints);
        }
    }
}
