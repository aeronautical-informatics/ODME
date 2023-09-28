package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.mxgraph.model.mxCell;

import odme.core.FileConvertion;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphAdd {
	
	public static String selectedType = "byte";

	public static void addNodeIntoJtreeWithNewModuleAddition(mxCell lastAddedCell) {
        mxCell addedCell = null;
        if (lastAddedCell != null) {
            Object[] outgoing = graph.getOutgoingEdges(lastAddedCell);
            Object targetCell = graph.getModel().getTerminal(outgoing[outgoing.length - 1], false);
            addedCell = (mxCell) targetCell;

            if (addedCell != null) {
            	JtreeToGraphConvert.nodeToRootPath(addedCell);
                lastAddedCell = null;

                // update jtree
                String[] stringArray = path.toArray(new String[0]);
                ArrayList<String> pathRev = new ArrayList<String>();

                for (int i = stringArray.length - 1; i >= 0; i--) {
                    pathRev.add(stringArray[i]);
                }
                
                String[] stringArrayRev = pathRev.toArray(new String[0]);
                addNodeWIthGraphAddition(addedCell.getValue().toString(), stringArrayRev);
                path.clear();
            }
        }
    }
	
	private static void addNodeIntoJtreeForSubTree(mxCell lastAddedCell) {
        path.clear();
        mxCell addedCell = null;
        if (lastAddedCell != null) {
            Object[] outgoing = graph.getOutgoingEdges(lastAddedCell);
            Object targetCell = graph.getModel().getTerminal(outgoing[outgoing.length - 1], false);
            addedCell = (mxCell) targetCell;

            if (addedCell != null) {
            	JtreeToGraphConvert.nodeToRootPath(addedCell);
                lastAddedCell = null;

                // update jtree
                String[] stringArray = path.toArray(new String[0]);
                ArrayList<String> pathRev = new ArrayList<String>();

                for (int i = stringArray.length - 1; i >= 0; i--) {
                    pathRev.add(stringArray[i]);
                }

                String[] stringArrayRev = pathRev.toArray(new String[0]);
                addNodeWIthGraphAddition(addedCell.getValue().toString(), stringArrayRev);
                path.clear();
            }
        }
    }

    public static void addBehaviourFromGraphPopup(Object pos) {
        mxCell varCell = (mxCell) pos;
        selectedNodeCellForVariableUpdate = varCell;
        String variableName = null;

        JTextField variableField = new JTextField();
//	     JTextField valueField = new JTextField();

        String variableFieldRegEx = "[a-zA-Z_][a-zA-Z0-9_]*"; // alphanumeric but not start with number

        // for validation of input
        JLabel errorLabelField = new JLabel();
        errorLabelField.setForeground(Color.RED);
        errorLabelField.setVisible(true);

        variableField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                if (selectedType.equals("string")) {
                    errorLabelField.setVisible(
                            !variableField.getText().trim().matches(variableFieldRegEx));
                }
                else {
                    errorLabelField.setVisible(
                            !variableField.getText().trim().matches(variableFieldRegEx));
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {}
        });

        Object[] message = {"Behaviour:", variableField};
        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Enter", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);


        if (option == JOptionPane.OK_OPTION) {


            variableName = variableField.getText();

            boolean valid = (variableField.getText() != null) && (!variableField.getText().trim().isEmpty());

            if(!valid) {

                JOptionPane.showMessageDialog(Main.frame, "Please input all values correctly.");
            }
            if(valid) {
                mxCell cellForAddingBehaviour = (mxCell) pos;
                pathToRoot.add((String) cellForAddingBehaviour.getValue());
                JtreeToGraphConvert.nodeToRootPathVar(cellForAddingBehaviour);

                String[] stringArray = pathToRoot.toArray(new String[0]);
                ArrayList<String> pathToRootRev = new ArrayList<String>();

                for (int i = stringArray.length - 1; i >= 0; i--) {
                    pathToRootRev.add(stringArray[i]);
                    System.out.println(" " + stringArray[i]);
                }

                String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

                TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

                System.out.println("Tree path " + treePathForVariable );


                DynamicTree.behavioursList.put(treePathForVariable, variableName);

                pathToRoot.clear();

                // have to call a function to refresh the table view
                ODMEEditor.treePanel.showBehavioursInTable(treePathForVariable);
            }
        }

    }

    public static void addVariableFromGraphPopup(Object pos) {
        mxCell varCell = (mxCell) pos;
        selectedNodeCellForVariableUpdate = varCell;

        String variableName = null;
        String variableType = null;
        String variableValue = null;
        String variableLowerBound = null;
        String variableUpperBound = null;
        String variableComment=null;

        // multiple input for variable---------------------------------
        JTextField variableField = new JTextField();
        JTextField valueField = new JTextField();
        JTextField lowerBoundField = new JTextField();
        JTextField upperBoundField = new JTextField();
        JTextField commentField = new JTextField();
        
        // for validation of input
        JLabel errorLabelField = new JLabel();
        errorLabelField.setForeground(Color.RED);
        errorLabelField.setVisible(true);

        lowerBoundField.setEnabled(false);
        upperBoundField.setEnabled(false);
        commentField.setEnabled(true);

        String[] typeList = {"Select Type:", "boolean", "int", "float", "double", "string"};

        String variableFieldRegEx = "[a-zA-Z_][a-zA-Z0-9_]*"; // alphanumeric but not start with number

        JComboBox<String> variableTypeField = new JComboBox<String>(typeList);

        variableTypeField.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    selectedType = variableTypeField.getSelectedItem().toString();

                    if (selectedType.equals("string") || selectedType.equals("boolean")) {
                        lowerBoundField.setEnabled(false);
                        upperBoundField.setEnabled(false);
                    } 
                    else {
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
        });
        
        commentField.addKeyListener(new KeyListener() {
        	@Override
            public void keyTyped(KeyEvent e) {}
        	
        	@Override
            public void keyReleased(KeyEvent e) {
        		errorLabelField.setVisible(
                        !commentField.getText().trim().matches(variableFieldRegEx) || !commentField
                                .getText().trim().matches(variableFieldRegEx));
        	}
        	
        	@Override
            public void keyPressed(KeyEvent e) {}
        });

        variableField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
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

            @Override
            public void keyPressed(KeyEvent e) {}
        });

        valueField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {

                if (selectedType.equals("boolean")) {
                    errorLabelField.setVisible(
                            (!valueField.getText().trim().equals("false") && !valueField.getText().trim()
                                    .equals("true")) || !variableField.getText().trim()
                                    .matches(variableFieldRegEx));

                }
                else if (selectedType.equals("int")) {
                    errorLabelField.setVisible(
                            !valueField.getText().trim().matches("^[0-9]+") || !variableField.getText().trim()
                                    .matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                                    .matches("^[0-9]+") || !upperBoundField.getText().trim()
                                    .matches("^[0-9]+"));

                }
                else if (selectedType.equals("float")) {
                    errorLabelField.setVisible(
                            !valueField.getText().trim().matches("^\\d*\\.\\d+") || !variableField.getText()
                                    .trim().matches(variableFieldRegEx)
                            || !lowerBoundField.getText().trim().matches("^\\d*\\.\\d+") || !upperBoundField
                                    .getText().trim().matches("^\\d*\\.\\d+"));

                } 
                else if (selectedType.equals("double")) {
                    errorLabelField.setVisible(
                            !valueField.getText().trim().matches("^\\d*\\.\\d+") || !variableField.getText()
                                    .trim().matches(variableFieldRegEx) || !lowerBoundField.getText().trim()
                                    .matches("^\\d*\\.\\d+") || !upperBoundField.getText().trim()
                                    .matches("^\\d*\\.\\d+"));

                }
                else if (selectedType.equals("string")) {
                    errorLabelField.setVisible(
                            !valueField.getText().trim().matches(variableFieldRegEx) || !variableField
                                    .getText().trim().matches(variableFieldRegEx));
                }

            }

            @Override
            public void keyPressed(KeyEvent e) {}
        });

        lowerBoundField.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
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

            @Override
            public void keyPressed(KeyEvent e) {}
        });

        upperBoundField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
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

            @Override
            public void keyPressed(KeyEvent e) {}
        });

        Object[] message =
                {"Variable Name:", variableField, "Variable Type:", variableTypeField, "Default Value:",
                        valueField, "Lower Bound:", lowerBoundField, "Upper Bound:", upperBoundField, " ",
                        errorLabelField,"Comment: ",commentField};

        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Enter", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            variableName = variableField.getText();
            variableType = (String) variableTypeField.getSelectedItem();
            variableValue = valueField.getText();
            variableLowerBound = lowerBoundField.getText();
            variableUpperBound = upperBoundField.getText();
            variableComment = commentField.getText();

            if (variableType.equals("")) {
                variableType = "none";
            }

            if (variableValue.equals("")) {
                variableValue = "none";
            }

            if (variableLowerBound.equals("")) {
            	if (selectedType.equals("int")){
            		variableLowerBound = Integer.toString(Integer.MIN_VALUE);
            	}
            	else if (selectedType.equals("float")){
            		variableLowerBound = Float.toString(Float.MIN_VALUE);
            	}
            	else if (selectedType.equals("double")){
            		variableLowerBound = Double.toString(Double.MIN_VALUE);
            	}
            	else
            		variableLowerBound = "none";
            }

            if (variableUpperBound.equals("")) {
            	if (selectedType.equals("int")){
            		variableUpperBound = Integer.toString(Integer.MAX_VALUE);
            	}
            	else if (selectedType.equals("float")){
            		variableUpperBound = Float.toString(Float.MAX_VALUE);
            	}
            	else if (selectedType.equals("double")){
            		variableUpperBound = Double.toString(Double.MAX_VALUE);
            	}
            	else
            		variableUpperBound = "none";
            }
            //...........................

            // added inside IF block so that if variable window closed without adding then
            // nothing will happen.
            if (variableTypeField.getSelectedItem().toString().trim().equals("string") || variableTypeField
                    .getSelectedItem().toString().trim().equals("boolean")) {
                variableName = variableName + "," + variableType + "," + variableValue;
            } 
            else {
                variableName =
                        variableName + "," + variableType + "," + variableValue + "," + variableLowerBound
                        + "," + variableUpperBound;
            }


            boolean validInput =
                    (variableField.getText() != null) && (!variableField.getText().trim().isEmpty()) && (
                            variableTypeField.getSelectedItem() != null) && (!variableTypeField
                            .getSelectedItem().toString().trim().isEmpty());
            
            if (!validInput) {
                JOptionPane.showMessageDialog(Main.frame, "Please input all values correctly.");
            }

            // end of multiple input for variable----------------------------
            if (validInput) {
                mxCell cellForAddingVariable = (mxCell) pos;
                pathToRoot.add((String) cellForAddingVariable.getValue());
                JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

                String[] stringArray = pathToRoot.toArray(new String[0]);
                ArrayList<String> pathToRootRev = new ArrayList<String>();

                for (int i = stringArray.length - 1; i >= 0; i--) {
                    pathToRootRev.add(stringArray[i]);
                }

                String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

                TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

                DynamicTree.varMap.put(treePathForVariable, variableName);

                pathToRoot.clear();

                // have to call a function to refresh the table view
                ODMEEditor.treePanel.refreshVariableTable(treePathForVariable);
            }
        }
    }

	public static void addConstraintFromGraphPopup(Object pos) {
        JTextArea constraintsField = new JTextArea(10, 30);
        constraintsField.setLineWrap(true);
        constraintsField.setWrapStyleWord(true);

        Object[] message = {"Constraint:", constraintsField};

        int option = JOptionPane
                .showConfirmDialog(Main.frame, message, "Please Enter", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String constraints = constraintsField.getText();

            boolean validInput =
                    (constraintsField.getText() != null) && (!constraintsField.getText().trim().isEmpty());

            if (validInput) {
                mxCell cellForAddingVariable = (mxCell) pos;
                pathToRoot.add((String) cellForAddingVariable.getValue());
                JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

                String[] stringArray = pathToRoot.toArray(new String[0]);
                ArrayList<String> pathToRootRev = new ArrayList<String>();

                for (int i = stringArray.length - 1; i >= 0; i--) {
                    pathToRootRev.add(stringArray[i]);
                }

                String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

                TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

                DynamicTree.constraintsList.put(treePathForVariable, constraints);

                pathToRoot.clear();

                // have to call a function to refresh the table view
                ODMEEditor.treePanel.showConstraintsInTable(treePathForVariable);
            }
        }
    }
    
    public static void addNodeWithJtreeAddition(String nodeName, String[] nodesToSelectedNode) {
        mxCell selectedCell = JtreeToGraphConvert.rootToSelectedNode(nodesToSelectedNode);

        Object[] outgoing = graph.getOutgoingEdges(selectedCell);

        mxCell selectedCellNew = null;
        int len = outgoing.length;

        double x;
        double y;

        if (len > 0) {

            Object targetCell = graph.getModel().getTerminal(outgoing[len - 1], false);
            selectedCellNew = (mxCell) targetCell;
            x = selectedCellNew.getGeometry().getX();
            y = selectedCellNew.getGeometry().getY();
            x = x + 100;

            // if the added node manually changed to other position then overlap may happen.
            // for removing that bug have to compare all the child nodes x positions and
            // have to
            // add after the most right one. didn't implement it yet. will do later after
            // first
            // prototype

        } 
        else {
            x = selectedCell.getGeometry().getX();
            y = selectedCell.getGeometry().getY();
            y = y + 100;
        }

        graph.getModel().beginUpdate();
        try {
            if (nodeName.endsWith("Dec")) {
                Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Aspect");
                graph.insertEdge(parent, null, "", selectedCell, obj);
                lastAddedCell = null; // so that it will not cause duplicate addition in tree
                nodeNumber++;
            } 
            else if (nodeName.endsWith("MAsp")) {
                Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Multiaspectp");
                graph.insertEdge(parent, null, "", selectedCell, obj);
                lastAddedCell = null; // so that it will not cause duplicate addition in tree
                nodeNumber++;
            }
            else if (nodeName.endsWith("Spec")) {
                Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Specializationp");
                graph.insertEdge(parent, null, "", selectedCell, obj);
                lastAddedCell = null; // so that it will not cause duplicate addition in tree
                nodeNumber++;
            } 
            else /* if (DynamicTreeDemo.nodeAddDetector.equals("entity")) */ {
                Object obj = graph.insertVertex(parent, null, nodeName, x, y, 80, 30,
                        "Entityp"); // "Aspect;fillColor=#0759cf;strokeColor=white;");
                graph.insertEdge(parent, null, "", selectedCell, obj);
                lastAddedCell = null; // so that it will not cause duplicate addition in tree
                nodeNumber++;
            }
        } 
        finally {
            graph.getModel().endUpdate();
        }
    }

    public static void addNodeFromConsole(String nodeName) {
        if (currentSelectedCell != null) {
            mxCell selectedCell = currentSelectedCell;

            Object[] outgoing = graph.getOutgoingEdges(selectedCell);

            mxCell selectedCellNew = null;
            int len = outgoing.length;

            double x;
            double y;

            if (len > 0) {

                Object targetCell = graph.getModel().getTerminal(outgoing[len - 1], false);
                selectedCellNew = (mxCell) targetCell;
                x = selectedCellNew.getGeometry().getX();
                y = selectedCellNew.getGeometry().getY();
                x = x + 100;

                // if the added node manually changed to other position then overlap may happen.
                // for removing that bug have to compare all the child nodes x positions and
                // have to
                // add after the most right one. didn't implement it yet. will do later after
                // first
                // prototype
            } 
            else {
                x = selectedCell.getGeometry().getX();
                y = selectedCell.getGeometry().getY();
                y = y + 100;
            }

            graph.getModel().beginUpdate();
            try {
                if (nodeName.endsWith("Dec")) {
                    Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Aspect");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                } 
                else if (nodeName.endsWith("MAsp")) {
                    Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Multiaspectp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                }
                else if (nodeName.endsWith("Spec")) {
                    Object obj =
                            graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Specializationp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                }
                else /* if (DynamicTreeDemo.nodeAddDetector.equals("entity")) */ {
                    Object obj = graph.insertVertex(parent, null, nodeName, x, y, 80, 30,
                            "Entityp"); // "Aspect;fillColor=#0759cf;strokeColor=white;");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                }
            } 
            finally {
                graph.getModel().endUpdate();
            }
        }
    }
    

    public static void addModuleFromOtherModelAsXML(Object obj) {
        // a small tree will be added dynamically to the selected node.

        String fileName = "";
        // choosing file from a directory
        //Path currentDirectory = Paths.get("").toAbsolutePath();
        //String repFslas = currentDirectory.toString().replace("\\", "/");

        JFileChooser fileChooser = new JFileChooser();

        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlfilter);

        fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName));
        int result = fileChooser.showOpenDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            fileName = selectedFile.getName();

            Object[] addedNodes = new Object[100];
            int addedNodeCount = 0;
            String[] nodeNameSplits = null;
            int nodeCount = 0;

            String[] parentNames = new String[100];
            int parentCount = 0;

            String[] nodeNames = new String[100];

            try {
                FileReader reader = new FileReader(selectedFile.getAbsolutePath());
                BufferedReader bufferedReader = new BufferedReader(reader);

                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    if (line.startsWith("<?")) {
                        continue;
                    } 
                    else if (line.startsWith("<start") || line.startsWith("</start")) {
                        continue;
                    } 
                    else if (line.startsWith("</")) {
                        parentCount--;
                    } 
                    else if (line.startsWith("<")) {
                        if (line.endsWith("/>")) {
                            String ln = line.replaceAll("[</>]", "");
                            nodeNames[nodeCount] = ln + "-" + parentNames[parentCount - 1] + "-hasparent";
                            nodeCount++;
                        }
                        else {
                            String ln = line.replaceAll("[</>]", "");

                            if (parentCount > 0) {
                                nodeNames[nodeCount] = ln + "-" + parentNames[parentCount - 1] + "-hasparent";
                                nodeCount++;
                            } 
                            else {
                                nodeNames[nodeCount] = ln;
                                nodeCount++;
                            }
                            parentNames[parentCount] = ln;
                            parentCount++;
                        }
                    }
                }

                reader.close();

            } 
            catch (IOException e) {
                e.printStackTrace();
            }
            for (int i = 0; i < nodeCount; i++) {
                String nodeName = nodeNames[i];

                if (nodeName.endsWith("hasparent")) {
                    nodeNameSplits = nodeName.split("-");
                    nodeName = nodeNameSplits[0];

                    String parentNode = nodeNameSplits[1];
                    for (Object node : addedNodes) {
                        if (!(node == null)) {
                            mxCell cell = (mxCell) node;
                            if (cell.getValue().equals(parentNode)) {
                                obj = cell;
                            }
                        }
                    }
                }

                mxCell selectedCell = (mxCell) obj;

                Object[] outgoing = graph.getOutgoingEdges(selectedCell);

                mxCell selectedCellNew = null;
                int len = outgoing.length;

                double x;
                double y;

                if (len > 0) {

                    Object targetCell = graph.getModel().getTerminal(outgoing[len - 1], false);
                    selectedCellNew = (mxCell) targetCell;
                    x = selectedCellNew.getGeometry().getX();
                    y = selectedCellNew.getGeometry().getY();
                    x = x + 100;

                    // if the added node manually changed to other position then overlap may happen.
                    // for removing that bug have to compare all the child nodes x positions and
                    // have to
                    // add after the most right one. didn't implement it yet. will do later after
                    // first
                    // prototype

                }
                else {
                    x = selectedCell.getGeometry().getX();
                    y = selectedCell.getGeometry().getY();
                    y = y + 100;
                }

                graph.getModel().beginUpdate();
                try {
                    if (nodeName.endsWith("Dec")) {
                        obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Aspect");
                        graph.insertEdge(parent, null, "", selectedCell, obj);
                        lastAddedCell = null; // so that it will not cause duplicate addition in tree
                        nodeNumber++;

                        addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                    } 
                    else if (nodeName.endsWith("MAsp")) {

                        obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Multiaspectp");
                        graph.insertEdge(parent, null, "", selectedCell, obj);
                        lastAddedCell = null; // so that it will not cause duplicate addition in tree
                        nodeNumber++;

                        addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                    } 
                    else if (nodeName.endsWith("Spec")) {

                        obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Specializationp");
                        graph.insertEdge(parent, null, "", selectedCell, obj);
                        lastAddedCell = null; // so that it will not cause duplicate addition in tree
                        nodeNumber++;

                        addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                    } 
                    else {
                        obj = graph.insertVertex(parent, null, nodeName, x, y, 80, 30,
                                "Entityp"); // "Aspect;fillColor=#0759cf;strokeColor=white;");
                        graph.insertEdge(parent, null, "", selectedCell, obj);
                        lastAddedCell = null; // so that it will not cause duplicate addition in tree
                        nodeNumber++;

                        addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                    }

                    // added nodes in the array
                    addedNodes[addedNodeCount] = obj;
                    addedNodeCount++;
                }
                finally {
                    graph.getModel().endUpdate();
                }
            }
            // have to add xml file to project here
            ODMEEditor.projectPanel.addModueFile(fileName);
        }
    }
    
    public static void addModuleFromSubgraph(Object obj) {
        // a small tree will be added dynamically to the jtree which is drawn in graph
        // editor and connected later to other node

        Object[] addedNodes = new Object[100];
        int addedNodeCount = 0;
        String[] nodeNameSplits = null;

        String[] nodeNames = new String[100];
        int nodeCount = 0;
        nodeCount = treeSyncNodeCount;
        nodeNames = nodeNamesForGraphSync;

        for (int i = 0; i < nodeCount; i++) {
            String nodeName = nodeNames[i];

            if (nodeName.endsWith("hasparent")) {
                nodeNameSplits = nodeName.split("-");
                nodeName = nodeNameSplits[0];

                String parentNode = nodeNameSplits[1];
                for (Object node : addedNodes) {
                    if (!(node == null)) {
                        mxCell cell = (mxCell) node;
                        if (cell.getValue().equals(parentNode)) {
                            obj = cell;
                        }
                    }
                }
            }

            mxCell selectedCell = (mxCell) obj;

            Object[] outgoing = graph.getOutgoingEdges(selectedCell);

            mxCell selectedCellNew = null;
            int len = outgoing.length;

            double x;
            double y;

            if (len > 0) {

                Object targetCell = graph.getModel().getTerminal(outgoing[len - 1], false);
                selectedCellNew = (mxCell) targetCell;

                x = selectedCellNew.getGeometry().getX();
                y = selectedCellNew.getGeometry().getY();
                x = x + 100;

                // if the added node manually changed to other position then overlap may happen.
                // for removing that bug have to compare all the child nodes x positions and
                // have to
                // add after the most right one. didn't implement it yet. will do later after
                // first
                // prototype
            }
            else {
                x = selectedCell.getGeometry().getX();
                y = selectedCell.getGeometry().getY();
                y = y + 100;
            }

            graph.getModel().beginUpdate();
            try {
                if (nodeName.endsWith("Dec")) {
                    obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Aspect");

                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree

                    // mxCell cell = (mxCell) obj; cell.setVisible(false);

                    // have to send tree path and node name
                    // i have node name- nodeName
                    // have to find tree path for that nodeName

                    addNodeIntoJtreeForSubTree(selectedCell);

                    if (firstAddedCellForSubTree == 0) {
                        firstAddedCellForSubTreeDeletion = (mxCell) obj;
                        firstAddedCellForSubTree = 1;
                    }
                } 
                else if (nodeName.endsWith("MAsp")) {
                    obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Multiaspectp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null;

                    addNodeIntoJtreeForSubTree(selectedCell);

                    if (firstAddedCellForSubTree == 0) {
                        firstAddedCellForSubTreeDeletion = (mxCell) obj;
                        firstAddedCellForSubTree = 1;
                    }
                } 
                else if (nodeName.endsWith("Spec")) {
                    obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Specializationp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null;

                    addNodeIntoJtreeForSubTree(selectedCell);

                    if (firstAddedCellForSubTree == 0) {
                        firstAddedCellForSubTreeDeletion = (mxCell) obj;
                        firstAddedCellForSubTree = 1;
                    }
                } 
                else {
                    obj = graph.insertVertex(parent, null, nodeName, x, y, 80, 30, "Entityp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);

                    lastAddedCell = null;

                    addNodeIntoJtreeForSubTree(selectedCell);

                    if (firstAddedCellForSubTree == 0) {
                        firstAddedCellForSubTreeDeletion = (mxCell) obj;
                        firstAddedCellForSubTree = 1;
                    }
                }

                // added nodes in the array
                addedNodes[addedNodeCount] = obj;
                addedNodeCount++;
            } 
            finally {
                graph.getModel().endUpdate();
            }
        }

        // have to make used array null and count 0 here
        nodeNamesForGraphSync = new String[100]; // or have to delete the added nodes in other way
        //undoForSubTreeCount = treeSyncNodeCount; // not using have to check carefully then delete
        treeSyncNodeCount = 0;
    }

    public static void addModuleFromSubgraphUniformity(Object obj) {
        // a small tree will be added dynamically to the jtree which is drawn in graph
        // editor and connected later to other node

        Object[] addedNodes = new Object[100];
        int addedNodeCount = 0;
        String[] nodeNameSplits = null;

        String[] nodeNames = new String[100];
        int nodeCount = 0;
        nodeCount = treeSyncNodeCount;
        nodeNames = nodeNamesForGraphSync;

        // deleting null nodes;
        nodeNames =
                Arrays.stream(nodeNames).filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);

        // adding uniformity node to the array
        addedNodes[addedNodeCount] = obj;
        addedNodeCount++;

        for (int i = 0; i < nodeCount; i++) {
            String nodeName = nodeNames[i];

            if (nodeName.endsWith("hasparent")) {
                nodeNameSplits = nodeName.split("-");
                nodeName = nodeNameSplits[0];

                String parentNode = nodeNameSplits[1];
                for (Object node : addedNodes) {
                    if (!(node == null)) {
                        mxCell cell = (mxCell) node;
                        if (cell.getValue().equals(parentNode)) {
                            obj = cell;
                        }
                    }
                }
            }

            mxCell selectedCell = (mxCell) obj;

            Object[] outgoing = graph.getOutgoingEdges(selectedCell);

            mxCell selectedCellNew = null;
            int len = outgoing.length;

            double x;
            double y;

            if (len > 0) {

                Object targetCell = graph.getModel().getTerminal(outgoing[len - 1], false);
                selectedCellNew = (mxCell) targetCell;

                x = selectedCellNew.getGeometry().getX();
                y = selectedCellNew.getGeometry().getY();
                x = x + 100;

                // if the added node manually changed to other position then overlap may happen.
                // for removing that bug have to compare all the child nodes x positions and
                // have to
                // add after the most right one. didn't implement it yet. will do later after
                // first
                // prototype
            } 
            else {
                x = selectedCell.getGeometry().getX();
                y = selectedCell.getGeometry().getY();
                y = y + 100;
            }

            String nodeId = "uniformity" + uniformityNodeNumber;

            graph.getModel().beginUpdate();
            try {
                if (nodeName.endsWith("Dec")) {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x + 25, y, 30, 30, "Aspect");

                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                } 
                else if (nodeName.endsWith("MAsp")) {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x + 25, y, 30, 30, "Multiaspectp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null;

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                } 
                else if (nodeName.endsWith("Spec")) {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x + 25, y, 30, 30, "Specializationp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null;

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                } 
                else {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x, y, 80, 30, "Entityp");
                    graph.insertEdge(parent, null, "", selectedCell, obj);

                    lastAddedCell = null;

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                }

                // added nodes in the array
                addedNodes[addedNodeCount] = obj;
                addedNodeCount++;
            } 
            finally {
                graph.getModel().endUpdate();
            }
        }

        // have to make used array null and count 0 here
        nodeNamesForGraphSync = new String[100]; // or have to delete the added nodes in other way
        //undoForSubTreeCount = treeSyncNodeCount; // not using have to check carefully then delete
        treeSyncNodeCount = 0;
    }
    
    public static void addNodeFromGraphPopup(String node, int x, int y) {
        String variableName = JOptionPane
                .showInputDialog(Main.frame, "Node Name:", "New Node", JOptionPane.INFORMATION_MESSAGE);

        if (variableName != null) {
            variableName = variableName.replaceAll("\\s+", "");
        }

        if ((variableName != null) && (!variableName.trim().isEmpty())) {
            graph.getModel().beginUpdate();
            try {
                if (node.endsWith("Dec")) {
                    if (!variableName.endsWith("Dec")) {
                        variableName = variableName + "Dec";
                    }
                    graph.insertVertex(parent, null, variableName, x - 15, y, 30, 30, "Aspect");
                    nodeNumber++;
                } 
                else if (node.endsWith("MAsp")) {
                    if (!variableName.endsWith("MAsp")) {
                        variableName = variableName + "MAsp";
                    }
                    graph.insertVertex(parent, null, variableName, x - 15, y, 30, 30, "Multiaspectp");
                    nodeNumber++;
                } 
                else if (node.endsWith("Spec")) {
                    if (!variableName.endsWith("Spec")) {
                        variableName = variableName + "Spec";
                    }
                    graph.insertVertex(parent, null, variableName, x - 15, y, 30, 30, "Specializationp");
                    nodeNumber++;
                } 
                else /* if (DynamicTreeDemo.nodeAddDetector.equals("entity")) */ {
                    graph.insertVertex(parent, null, variableName, x - 40, y, 80, 30,
                            "Entityp"); // "Aspect;fillColor=#0759cf;strokeColor=white;");
                    nodeNumber++;
                }
            } 
            finally {
                graph.getModel().endUpdate();
            }
        }
    }
    
    /**
     * This function add two nodes at Top-Right and Bottom-Left corner to make the
     * page big enough during new project creation.
     */
    public static void addPageLengthNodes() {
        graph.getModel().beginUpdate();
        try {
        	graph.insertVertex(parent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");

        	graph.insertVertex(parent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
        } 
        finally {
            graph.getModel().endUpdate();
        }
    }
    
    /**
     * Add constraint to the aspect node in the SES XML structure.
     */
    public static void addconstraintToSESStructure() {
        TreePath keyPath = null;
        String constraint = "";

        for (TreePath keyPath2 : DynamicTree.constraintsList.keySet()) {
            keyPath = keyPath2;
        }

        if (keyPath != null) {
            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) (keyPath.getLastPathComponent());
            TreeNode[] nodes = currentNode.getPath();
            int len = nodes.length;
            String[] sesNodesInPath = new String[len];

            String[] nodesToSelectedNode = new String[100];

            int b = 0;

            for (TreePath key : DynamicTree.constraintsList.keySet()) {
                int a = 0;

                for (String value : DynamicTree.constraintsList.get(key)) {
                    DefaultMutableTreeNode currentNode2 =
                            (DefaultMutableTreeNode) (key.getLastPathComponent());

                    TreeNode[] nodes2 = currentNode2.getPath();

                    if (nodes.length == nodes2.length) {
                        for (int i = 0; i < nodes.length; i++) {

                            if (nodes[i].toString().equals(nodes2[i].toString())) {
                                a = 1;
                            } 
                            else {
                                a = 0;
                            }
                        }
                    }

                    if (a == 1) {
                        nodesToSelectedNode[b] = value;
                        b++;
                    }
                }
            }

            nodesToSelectedNode =
                    Arrays.stream(nodesToSelectedNode).filter(s -> (s != null && s.length() > 0))
                            .toArray(String[]::new);

            for (String con : nodesToSelectedNode) {
                if (constraint.equals("")) {
                    constraint = con;
                }
                else {
                    constraint = constraint + ", " + con;
                }
            }

            for (int i = 0; i < len; i++) {
                if (nodes[i].toString().endsWith("Dec")) {
                    sesNodesInPath[i] = "aspect";
                }
                else if (nodes[i].toString().endsWith("MAsp")) {
                    sesNodesInPath[i] = "multiAspect";
                } 
                else if (nodes[i].toString().endsWith("Spec")) {
                    sesNodesInPath[i] = "specialization";
                } 
                else {
                    sesNodesInPath[i] = "entity";
                }
            }
            FileConvertion fileConvertion = new FileConvertion();
            fileConvertion.addConstraintToSESStructure(sesNodesInPath, constraint);
        }
    }
    
    public static void addNodeWIthGraphAddition(String childNode, String[] nodePath) {
    	ODMEEditor.treePanel.addObjectWIthGraphAddition(childNode, nodePath);
    }
    
    public static void addVariableFromScenarioTableForUpdate(mxCell cellForAddingVariable, String variableName) {
        pathToRoot.add((String) cellForAddingVariable.getValue());
        JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

        String[] stringArray = pathToRoot.toArray(new String[0]);
        ArrayList<String> pathToRootRev = new ArrayList<String>();

        for (int i = stringArray.length - 1; i >= 0; i--) {
            pathToRootRev.add(stringArray[i]);
        }

        String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
        TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

        DynamicTree.varMap.put(treePathForVariable, variableName);
        pathToRoot.clear();

        // //just testing setting as attributes
        // //not working have to delete later
        // cellForAddingVariable.setAttribute("variable", variableName);
        // String st = cellForAddingVariable.getAttribute("variable");
        // System.out.println(st);

        // have to call a function to refresh the table view
        //System.out.println("#####"+treePathForVariable);
        
        ODMEEditor.treePanel.refreshVariableTable(treePathForVariable);
    }
}
