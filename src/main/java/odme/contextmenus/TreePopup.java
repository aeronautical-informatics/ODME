/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package odme.contextmenus;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import odme.jtreetograph.JtreeToGraphAdd;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <h1>TreePopup</h1>
 * <p>
 * This class implements the JTree node pop up action of the SES JTree displayed
 * in the left side of the editor. Only few actions are implemented here because
 * all the actions are implemented in graphical panel right click option. Node
 * addition and deletion and variable deletion options are added in the pop pup
 * action of the SES JTree.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class TreePopup extends JPopupMenu {

	private static final long serialVersionUID = 1L;

	public TreePopup(JTree tree) {
    	String[] items = {"Add Node", "Add Variable", "Delete Variable", "Delete All Variable", "Delete Node"};
    	JMenuItem item;
    	
    	for (int i=0; i<items.length; i++) {
    		item = new JMenuItem(items[i]);
    		add(item);
    		if (i<items.length-1)
    			add(new JSeparator());
    		
    		item.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
            	  switch (ae.getActionCommand()){
            	  	case "Add Node":
            		  popUpActionAdd();
	            	  break;
            	  	case "Add Variable":
	            	  popUpActionAddVariable();
	            	  break;
            	  	case "Delete Node":
	            	  popUpActionDelete();
		              break;
		          } 
              }
          });
    	}
    }
    
    private void popUpActionAdd() {
        ODMEEditor.nodeName = JOptionPane
                .showInputDialog(Main.frame, "Node Name:", "New Node", JOptionPane.INFORMATION_MESSAGE);
        if (ODMEEditor.nodeName != null) {
            ODMEEditor.nodeName = ODMEEditor.nodeName.replaceAll("\\s+", "");
        }

        if ((ODMEEditor.nodeName != null) && (!ODMEEditor.nodeName.trim().isEmpty())) {
            TreePath currentSelection = ODMEEditor.treePanel.tree.getSelectionPath();

            if (currentSelection != null) {
                DefaultMutableTreeNode currentNode =
                        (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());

                TreeNode[] nodes = currentNode.getPath();
                String[] nodesToSelectedNode = new String[nodes.length];

                for (int i = 0; i < nodes.length; i++) {
                    nodesToSelectedNode[i] = (nodes[i].toString());
                }

                if (currentNode.toString().endsWith("Spec")) {
                	ODMEEditor.treePanel.addObject(ODMEEditor.nodeName);
                	JtreeToGraphAdd.addNodeWithJtreeAddition(ODMEEditor.nodeName, nodesToSelectedNode);

                } else if (currentNode.toString().endsWith("Dec")) {
                	ODMEEditor.treePanel.addObject(ODMEEditor.nodeName);
                	JtreeToGraphAdd.addNodeWithJtreeAddition(ODMEEditor.nodeName, nodesToSelectedNode);

                } else if (currentNode.toString().endsWith("MAsp")) {
                	ODMEEditor.treePanel.addObject(ODMEEditor.nodeName);
                	JtreeToGraphAdd.addNodeWithJtreeAddition(ODMEEditor.nodeName, nodesToSelectedNode);

                } else {
                	ODMEEditor.treePanel.addObject(ODMEEditor.nodeName);
                	JtreeToGraphAdd.addNodeWithJtreeAddition(ODMEEditor.nodeName, nodesToSelectedNode);
                }
            }
        }
    }
    
    private void popUpActionAddVariable() { // have to change this function so that it will like add variable of
        String variableName, variableType, variableValue, variableLowerBound, variableUpperBound;

        // multiple input for variable---------------------------------
        JTextField variableField, variableTypeField, valueField, lowerBoundField, upperBoundField;
        
        variableField = new JTextField();
        variableTypeField = new JTextField();
        valueField = new JTextField();
        lowerBoundField = new JTextField();
        upperBoundField = new JTextField();

        Object[] message =
                {"Variable Name:", variableField, "Variable Type:", variableTypeField, "Value:", valueField,
                        "Lower Bound:", lowerBoundField, "Upper Bound:", upperBoundField};

        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Enter", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            variableName = variableField.getText();
            variableType = variableTypeField.getText();
            variableValue = valueField.getText();
            variableLowerBound = lowerBoundField.getText();
            variableUpperBound = upperBoundField.getText();

            // added inside IF block so that if variable window closed without adding then
            // nothing will happen.
            variableName =
                    variableName + "," + variableType + "," + variableValue + "," + variableLowerBound + ","
                    + variableUpperBound;

            TreePath currentSelection = ODMEEditor.treePanel.tree.getSelectionPath();

            boolean validInput =
                    (variableField.getText() != null) && (!variableField.getText().trim().isEmpty()) && (
                            variableTypeField.getText() != null) && (!variableTypeField.getText().trim()
                            .isEmpty()) && (valueField.getText() != null) && (!valueField.getText().trim()
                            .isEmpty()) && (lowerBoundField.getText() != null) && (!lowerBoundField.getText()
                            .trim().isEmpty()) && (upperBoundField.getText() != null) && (!upperBoundField
                            .getText().trim().isEmpty());

            if (!validInput) {
                JOptionPane.showMessageDialog(Main.frame, "Please input all values correctly.");
            }

            // end of multiple input for variable----------------------------
            if (validInput) {
                // have to handle added variable in a list

                if (currentSelection != null) {
                    DynamicTree.varMap.put(currentSelection, variableName);
                    // have to call a function to refresh the table view
                    ODMEEditor.treePanel.refreshVariableTable(currentSelection);
                }
            }
        }
    }

    private void popUpActionDelete() {
    	ODMEEditor.treePanel.removeCurrentNode();
    }
}
