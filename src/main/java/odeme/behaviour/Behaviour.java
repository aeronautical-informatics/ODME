package odeme.behaviour;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import odme.jtreetograph.JtreeToGraphDelete;
import odme.jtreetograph.JtreeToGraphVariables;
import odme.odmeeditor.Main;

public class Behaviour extends JPanel {


    private static final long serialVersionUID = 1L;
    public static JTable table;
    public static DefaultTableModel model;
    
    
    public Behaviour() {

        setLayout(new GridLayout(1, 0));

        final String[] columnNames = {"Node Name","Behaviours"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable();
        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension());
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setEnabled(false);
        table.setSelectionBackground(new Color(217, 237, 146));
        table.setSelectionForeground(new Color(188, 71, 73));
        
        // row listener
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Double click
                if (e.getClickCount() == 2) {
                    JTable target = (JTable) e.getSource();

                    Point point = e.getPoint();
                    int row = table.rowAtPoint(point);
                    
                    String behaviour = (String) target.getModel().getValueAt(row, 1);
                    String nodeName = (String) target.getModel().getValueAt(0,0);
                    System.out.println(nodeName);
                    updateTableData(behaviour);
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

    public void showBehavioursInTable(String nodeName , String[] nodesToSelectedNode) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content
        for (String value : nodesToSelectedNode) {
            if (value == null) {
                model.addRow(new Object[] {""});
            } else {
                model.addRow(new Object[] { nodeName, value});
            }
        }
        setNullRowsToVariableTable();
    }


    /*
      * Autor: Lionce Vadece
      * This is to fill the behavior table
     */
    public void showBehaviourInTable(String selectedNode, String[] nodeVariables) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content
        String[] properties = null;
        int a = 0; // what is this??

        for (String value : nodeVariables) {
            if (a == 0) {
                if (value == null) {
                    model.addRow(new Object[] {""});
                }
                else {
                    properties = value.split(",");

                    model.addRow(new Object[] {selectedNode, properties[0]});
                }
                a = 1;
            }
        }

        for (String value : nodeVariables) {
            if (a == 1) {
                a = 0;
                continue;
            }

            if (value != null) {
                properties = value.split(",");

                model.addRow(new Object[] {selectedNode, properties[0]});
            }
        }

        setNullRowsToVariableTable();
    }
    
    private void updateTableData(String behaviour) {
        // multiple input for variable---------------------------------
        JTextArea behavioursField = new JTextArea(10, 30);
        behavioursField.setLineWrap(true);
        behavioursField.setWrapStyleWord(true);
        behavioursField.setText(behaviour);
        
        String behaviourOld = behaviour;
        Object[] message = {"Behaviours:", behavioursField};
        
        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Update", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.OK_OPTION) {
            behaviour = behavioursField.getText();
            JtreeToGraphDelete.deleteBehaviourFromScenarioTableForUpdate(
                    JtreeToGraphVariables.selectedNodeCellForVariableUpdate, behaviourOld, behaviour);
        }
    }

}
