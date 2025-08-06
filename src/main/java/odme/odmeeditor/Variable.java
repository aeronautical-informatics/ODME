package odme.odmeeditor;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import odme.jtreetograph.JtreeToGraphDelete;
import odme.jtreetograph.JtreeToGraphVariables;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * <h1>Variable</h1>
 * <p>
 * Creates an window to show added variables in the SES model. The window
 * contains a table and variables of the selected node are displayed in that
 * table.
 * </p>
 *
 * @author 
 * @version 
 */
public class Variable extends JPanel {

	private static final long serialVersionUID = 1L;
	public static JTable table;
    private static DefaultTableModel model;
    // selectedType is using in below function: addVariableFromGraphPopup
    public static String selectedType;
    
    public static final String variableFieldRegEx = "[a-zA-Z_][a-zA-Z0-9_]*";
    

    public Variable() {
        setLayout(new GridLayout(1, 0)); // rows,cols

        String[] columnNames =
                {"Node Name", "Variables", "Type", "Default Value", "Lower Bound", "Upper Bound"};
        model = new DefaultTableModel(columnNames, 0);
        table = new JTable();
        table.setModel(model);

        table.setPreferredScrollableViewportSize(new Dimension());
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setDefaultEditor(Object.class, null);
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

                    String nodeName = (String) target.getModel().getValueAt(0, 0);
                    String variableName = (String) target.getModel().getValueAt(row, 1);
                    String variableType = (String) target.getModel().getValueAt(row, 2);
                    String variableValue = (String) target.getModel().getValueAt(row, 3);
                    String lowerBound = (String) target.getModel().getValueAt(row, 4);
                    String uperBound = (String) target.getModel().getValueAt(row, 5);
//                    String comment = (String) target.getModel().getValueAt(row, 6);
                    
                    if (variableName != "")
                    	updateTableData(nodeName, variableName, variableType, variableValue, lowerBound,
                    			uperBound,"comment");
                }
            }
        });

        // Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        // Add the scroll pane to this panel.
        add(scrollPane);
        
        setNullRowsToVariableTable();
    }

    public static void setNullRowsToVariableTable() {
        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {"", "", "", "", "", ""});
        }
    }

    public static void setNullToAllRows() {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content

        for (int i = 0; i < 100; i++) {
            model.addRow(new Object[] {""});
        }
    }

    public void showNodeValuesInTable(String selectedNode, String[] nodeVariables) {
        DefaultTableModel dtm = (DefaultTableModel) table.getModel();
        dtm.setRowCount(0); // for deleting previous table content
        String[] properties = null;
        int a = 0; // what is this??

        for (String value : nodeVariables) {
            if (a == 0) {
                if (value == null) {
                    model.addRow(new Object[] {selectedNode, "", "", "", "", ""});
                } 
                else {
                    properties = value.split(",");

                    if (properties[1].equals("string") || properties[1].equals("boolean")) {
                        model.addRow(
                                new Object[] {selectedNode, properties[0], properties[1], properties[2]});
                    } 
                    else {
                        model.addRow(new Object[] {selectedNode, properties[0], properties[1], properties[2],
                                properties[3], properties[4]});
                    }
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

                if (properties[1].equals("string") || properties[1].equals("boolean")) {
                    model.addRow(new Object[] {selectedNode, properties[0], properties[1], properties[2]});
                }
                else {
                    model.addRow(new Object[] {selectedNode, properties[0], properties[1], properties[2],
                            properties[3], properties[4]});
                }
            }
        }
        
        setNullRowsToVariableTable();
    }
    
    // added by amir - for compatibility with other parts of program (whoever called the old version, is gonna get it still)
    public static void updateTableData(String nodeName, String variableName, String variableType,
            String variableValue, String variableLowerBound, String variableUpperBound) {
    	updateTableData(nodeName,variableName,variableType,variableValue,variableLowerBound,variableUpperBound,null);
    }

    public static void updateTableData(String nodeName, String variableName, String variableType,
                                String variableValue, String variableLowerBound, String variableUpperBound,String variableComment) {

        // multiple input for variable---------------------------------
    	JLabel errorLabelField = new JLabel();
        JTextField nodeNameleField = new JTextField();
        JTextField variableField = new JTextField();
        JTextField valueField = new JTextField();
        JTextField lowerBoundField = new JTextField();
        JTextField upperBoundField = new JTextField();
        JTextField commentField = new JTextField(); // added by amir
        
        lowerBoundField.setEnabled(false);
        upperBoundField.setEnabled(false);
        nodeNameleField.setEnabled(false);
        
        if(variableComment!=null)
        	commentField.setEnabled(false); // added by amir
        
        // for validation of input
        errorLabelField.setText("Value is not Valid");
        errorLabelField.setForeground(Color.RED);
        errorLabelField.setVisible(true);

        String[] typeList = {"boolean", "int", "float", "double", "string"};

        JComboBox<String> variableTypeField = new JComboBox<String>(typeList);
        variableTypeField.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
            	if (event.getStateChange() == ItemEvent.SELECTED) {
            	variableTypeFieldChange(variableField, valueField, errorLabelField,
                		lowerBoundField, upperBoundField, variableTypeField);
            	}
            }
        });

        nodeNameleField.setText(nodeName);
        variableField.setText(variableName);
        variableTypeField.setSelectedItem(variableType);
        valueField.setText(variableValue);
        lowerBoundField.setText(variableLowerBound);
        upperBoundField.setText(variableUpperBound);
        commentField.setText(variableComment); // added by amir

        String variableNameOld = null;
        selectedType = variableType;

        if (selectedType.equals("string") || selectedType.equals("boolean")) {
            lowerBoundField.setText(null);
            upperBoundField.setText(null);
            lowerBoundField.setEnabled(false);
            upperBoundField.setEnabled(false);
            commentField.setEnabled(false);

            errorLabelField.setVisible(false);
            // have to check why without this gives error during opening this form even
            // value is correct.during debugging i saw if condition is not working and make
            // the error label visible but values was correct
        } 
        else {
            lowerBoundField.setEnabled(true);
            upperBoundField.setEnabled(true);
        }
        
        if (selectedType.equals("string") || selectedType.equals("boolean")) {
            variableNameOld = variableName + "," + variableType + "," + variableValue;
            //System.out.println(variableNameOld);
        } else {
            variableNameOld =
                    variableName + "," + variableType + "," + variableValue + "," + variableLowerBound + ","
                    + variableUpperBound;
        }
        
        if (ODMEEditor.toolMode == "pes") {
        	nodeNameleField.setEnabled(false);
        	variableField.setEnabled(false);
        	lowerBoundField.setEnabled(false);
        	upperBoundField.setEnabled(false);
        	variableTypeField.setEnabled(false);
        	commentField.setEnabled(false);
        }
        
        
        variableTypeFieldChange(variableField, valueField, errorLabelField,
        		lowerBoundField, upperBoundField, variableTypeField);
        
        variableFieldValidator(
        		variableField, valueField, errorLabelField,
        		lowerBoundField, upperBoundField);
        
        valueFieldvalidator(
        		variableField, valueField, errorLabelField,
        		lowerBoundField, upperBoundField);
        
        lowerBoundFieldValidator(
        		variableField, valueField, errorLabelField,
        	    lowerBoundField, upperBoundField);
        
        upperBoundFieldValidator(
        		variableField, valueField, errorLabelField,
        		lowerBoundField, upperBoundField);
        
        variableCommentValidator(variableField,errorLabelField); // added by amir

        variableField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
            	variableFieldValidator(
                		variableField, valueField, errorLabelField,
                		lowerBoundField, upperBoundField);
            }
            @Override
            public void keyPressed(KeyEvent e) {}
        });

        valueField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
            	valueFieldvalidator(
                		variableField, valueField, errorLabelField,
                		lowerBoundField, upperBoundField);
            }
            @Override
            public void keyPressed(KeyEvent e) {}
        });

        lowerBoundField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
            	lowerBoundFieldValidator(
                		variableField, valueField, errorLabelField,
                		lowerBoundField, upperBoundField);
            }
            @Override
            public void keyPressed(KeyEvent e) {}
        });

        upperBoundField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyReleased(KeyEvent e) {
            	upperBoundFieldValidator(
                		variableField, valueField, errorLabelField,
                		lowerBoundField, upperBoundField);
            }
            @Override
            public void keyPressed(KeyEvent e) {}
        });

        Object[] message = {"Node Name:", nodeNameleField, "Variable Name:", variableField, "Variable Type:",
                variableTypeField, "Value:", valueField, "Lower Bound:", lowerBoundField, "Upper Bound:",
                upperBoundField, " ", errorLabelField,"Comment:",commentField};

        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Update", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION && !errorLabelField.isVisible()) {
            variableName = variableField.getText();
            variableType = (String) variableTypeField.getSelectedItem();
            variableValue = valueField.getText();
            variableLowerBound = lowerBoundField.getText();
            variableUpperBound = upperBoundField.getText();
            variableComment=commentField.getText();

            if (variableType.equals("")) {
                variableType = "none";
            }

            if (variableValue.equals("")) {
                variableValue = "none";
            }

            if (variableLowerBound.equals("")) {
            	variableLowerBound = "none";
            }

            if (variableUpperBound.equals("")) {
                variableUpperBound = "none";
            }


            if (variableTypeField.getSelectedItem().toString().trim().equals("string") ||
            	variableTypeField.getSelectedItem().toString().trim().equals("boolean")) {
                   variableName = variableName + "," + variableType + "," + variableValue;
            }
            else {
                   variableName = variableName + "," + variableType + "," + variableValue + "," + variableLowerBound
                            + "," + variableUpperBound;
            }

            JtreeToGraphDelete.deleteVariableFromScenarioTableForUpdate(
            		JtreeToGraphVariables.selectedNodeCellForVariableUpdate, variableNameOld, variableName);
        }
        
        else if (option == JOptionPane.OK_OPTION && errorLabelField.isVisible()){
        	JOptionPane.showMessageDialog(Main.frame, "Value is not Valid!", "Error",
                    JOptionPane.ERROR_MESSAGE);
        	
        	updateTableData(nodeName, variableName, variableType,
                    variableValue, variableLowerBound, variableUpperBound);
        }
    }
    
    public static void variableFieldValidator(
    		JTextField variableField, JTextField valueField, JLabel errorLabelField,
    		JTextField lowerBoundField, JTextField upperBoundField) {
    	if (selectedType.equals("string")) {
            errorLabelField.setVisible(
                    !variableField.getText().trim().matches(variableFieldRegEx) || !valueField
                            .getText().trim().matches(variableFieldRegEx));
        }
        else if (selectedType.equals("boolean")) {

            errorLabelField.setVisible(
                    (!valueField.getText().trim().equals("false") && !valueField.getText().trim()
                            .equals("true")) || !variableField.getText().trim()
                            .matches(variableFieldRegEx));
        }
        else if (selectedType.equals("double")) {

            errorLabelField.setVisible(
                    !valueField.getText().trim().matches("^\\d*\\.\\d+") || !variableField.getText()
                            .trim().matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                            .matches("^\\d*\\.\\d+") || !upperBoundField.getText().trim()
                            .matches("^\\d*\\.\\d+"));
        } 
        else {
            errorLabelField.setVisible(
                    !variableField.getText().trim().matches(variableFieldRegEx) || !valueField
                            .getText().trim().matches("^[0-9]+") || !lowerBoundField.getText().trim()
                            .matches("^[0-9]+") || !upperBoundField.getText().trim()
                            .matches("^[0-9]+"));
        }
    }
    
    // added by amir
    public static void variableCommentValidator(JTextField commentFiled,JLabel errorLabelField) {
    	try {
    		errorLabelField.setVisible(
				!commentFiled.getText().trim().matches("^[a-zA-Z_][a-Z0-9A-Z ]*")
    		);
    	}
    	catch (Exception e) {
    		errorLabelField.setVisible(true);
		}
    }
    
    public static void valueFieldvalidator(
    		JTextField variableField, JTextField valueField, JLabel errorLabelField,
    		JTextField lowerBoundField, JTextField upperBoundField) {
    	if (selectedType.equals("boolean")) {
            errorLabelField.setVisible(
                    !valueField.getText().trim().equals("false") 
                    && !valueField.getText().trim().equals("true"));
        } 
        else if (selectedType.equals("int")) {
        	try {
        		errorLabelField.setVisible(
        				!valueField.getText().trim().matches("^-{0,1}[0-9]+") ||
        				Integer.parseInt(valueField.getText()) > Integer.parseInt(upperBoundField.getText()) || 
        				Integer.parseInt(valueField.getText()) < Integer.parseInt(lowerBoundField.getText()));
        	}
        	catch (Exception e) {
        		errorLabelField.setVisible(true);
			}
        } 
        else if (selectedType.equals("float")) {
        	try {
        		errorLabelField.setVisible(
        				!valueField.getText().trim().matches("^\\d*\\.\\d+") ||
        				Float.parseFloat(valueField.getText()) > Float.parseFloat(upperBoundField.getText()) || 
        				Float.parseFloat(valueField.getText()) < Float.parseFloat(lowerBoundField.getText()));
        	}
        	catch (Exception e) {
        		errorLabelField.setVisible(true);
        	}
        } 
    	
        else if (selectedType.equals("double")) {
        	try {
        		errorLabelField.setVisible(
        				!valueField.getText().trim().matches("^\\d*\\.\\d+") ||
        				Double.parseDouble(valueField.getText()) > Double.parseDouble(upperBoundField.getText()) || 
        				Double.parseDouble(valueField.getText()) < Double.parseDouble(lowerBoundField.getText()));
        	}
        	catch (Exception e) {
        		errorLabelField.setVisible(true);
        	}
        }
        	
        else if (selectedType.equals("string")) {
            errorLabelField.setVisible(
                    !valueField.getText().trim().matches(variableFieldRegEx));
        }
    }

    public static void lowerBoundFieldValidator(
    		JTextField variableField, JTextField valueField, JLabel errorLabelField,
    		JTextField lowerBoundField, JTextField upperBoundField){
    	
        if (selectedType.equals("int")) {

            errorLabelField.setVisible(
                    !valueField.getText().trim().matches("^[0-9]+") || !variableField.getText().trim()
                            .matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                            .matches("^[0-9]+") || !upperBoundField.getText().trim()
                            .matches("^[0-9]+"));
        } 
        else if (selectedType.equals("float") || selectedType.equals("double")) {

            errorLabelField.setVisible(
                    !valueField.getText().trim().matches("^\\d*\\.\\d+") || !variableField.getText()
                            .trim().matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                            .matches("^\\d*\\.\\d+") || !upperBoundField.getText().trim()
                            .matches("^\\d*\\.\\d+"));
        }
    }
    
    public static void upperBoundFieldValidator(
    		JTextField variableField, JTextField valueField, JLabel errorLabelField,
    		JTextField lowerBoundField, JTextField upperBoundField){
    	if (selectedType.equals("float")) {
        	
            errorLabelField.setVisible(
                    !valueField.getText().trim().matches("^[0-9]+") || !variableField.getText().trim()
                            .matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                            .matches("^[0-9]+") || !upperBoundField.getText().trim()
                            .matches("^[0-9]+"));
        }
        else if (selectedType.equals("int")) {

            errorLabelField.setVisible(
                    !valueField.getText().trim().matches("^[0-9]+") || !variableField.getText().trim()
                            .matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                            .matches("^[0-9]+") || !upperBoundField.getText().trim()
                            .matches("^[0-9]+"));
        } 
        else if (selectedType.equals("double")) {

            errorLabelField.setVisible(
                    !valueField.getText().trim().matches("^\\d*\\.\\d+") || !variableField.getText()
                            .trim().matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                            .matches("^\\d*\\.\\d+") || !upperBoundField.getText().trim()
                            .matches("^\\d*\\.\\d+"));
        }
    }
    
    public static void variableTypeFieldChange(JTextField variableField,
    		JTextField valueField, JLabel errorLabelField,
    		JTextField lowerBoundField, JTextField upperBoundField,
    		JComboBox<String> variableTypeField) {
    	
    	
            selectedType = variableTypeField.getSelectedItem().toString();

            if (selectedType.equals("string") || selectedType.equals("boolean")) {
                lowerBoundField.setText(null);
                upperBoundField.setText(null);
                lowerBoundField.setEnabled(false);
                upperBoundField.setEnabled(false);
            } 
            else if (ODMEEditor.toolMode == "ses"){
                lowerBoundField.setEnabled(true);
                upperBoundField.setEnabled(true);
            }

            // --------------
            if (selectedType.equals("boolean")) {
                errorLabelField.setVisible(
                        !variableField.getText().trim().matches(variableFieldRegEx) || (
                                !valueField.getText().trim().equals("true") && !variableField
                                        .getText().trim().equals("false")));
            }
            else if (selectedType.equals("string")) {
                errorLabelField.setVisible(
                        !variableField.getText().trim().matches(variableFieldRegEx) || !valueField
                                .getText().trim().matches(variableFieldRegEx));
            } 
            else if (selectedType.equals("double")) {
                errorLabelField.setVisible(
                        !valueField.getText().trim().matches("^\\d*\\.\\d+") || !variableField
                                .getText().trim().matches(variableFieldRegEx) || !lowerBoundField
                                .getText().trim().matches("^\\d*\\.\\d+") || !upperBoundField
                                .getText().trim().matches("^\\d*\\.\\d+"));
            } 
            else {
                errorLabelField.setVisible(
                        !variableField.getText().trim().matches(variableFieldRegEx) || !valueField
                                .getText().trim().matches("^[0-9]+") || !lowerBoundField.getText()
                                .trim().matches("^[0-9]+") || !upperBoundField.getText().trim()
                                .matches("^[0-9]+"));
            }
    }
}
