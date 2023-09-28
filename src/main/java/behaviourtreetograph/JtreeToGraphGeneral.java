package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import odme.core.FindByName;
import odme.jtreetograph.JtreeToGraphCheck;
import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphSave;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

import static behaviourtreetograph.JTreeToGraphBehaviour.benhaviourGraph;
import static odme.jtreetograph.JtreeToGraphVariables.*;

public class JtreeToGraphGeneral {

    public static String rootNodeName() {
        Object[] cells = benhaviourGraph.getChildVertices(benhaviourGraph.getDefaultParent());
        mxCell rootcell = null;

        for (Object c : cells) {
            mxCell cell2 = (mxCell) c; // casting
            if (cell2.isVertex()) {
                String val = cell2.getId();
                if (val.equals("rootnode")) {
                    rootcell = cell2;
                }
            }
        } // end of for
        return rootcell.getValue().toString();
    }

    public static void zoomIn() {
        behaviourGraphComponent.zoomIn();
    }
    
    public static void zoomOut() {
        behaviourGraphComponent.zoomOut();
    }

    public static void redo() {
        if (undoManager.canRedo())
        {
            undoManager.redo();
        }else {
            System.out.println("Cannot redo ");
        }
    }
    
    public static void undo() {
        if (undoManager.canUndo())
            undoManager.undo();
    }
    
    public static Element childNodes(Document thisDoc, mxCell cell) {
        Element thisElement = null;

        String nodeName = benhaviourGraph.getModel().getValue(cell).toString();
        thisElement = thisDoc.createElement(nodeName);

        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);
                // for next call
                mxCell targetCell2 = (mxCell) targetCell;
                thisElement.appendChild(childNodes(thisDoc, targetCell2));
            }
        }
        return thisElement;
    }

    public static Element behaviourChildNodes(Document thisDoc, mxCell cell) {
        Element thisElement = null;

        String nodeName = benhaviourGraph.getModel().getValue(cell).toString();
        thisElement = thisDoc.createElement(nodeName);

        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);
                // for next call
                mxCell targetCell2 = (mxCell) targetCell;
                thisElement.appendChild(behaviourChildNodes(thisDoc, targetCell2));
            }
        }
        return thisElement;
    }



    public static Element childNodesWithUniformity(Document thisDoc, mxCell cell) {
        Element thisElement = null;

        String nodeName = benhaviourGraph.getModel().getValue(cell).toString();
        thisElement = thisDoc.createElement(nodeName);

        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);
                mxCell targetCell2 = (mxCell) targetCell;

                if (targetCell2.getId().startsWith("uniformity")) {
                    if (targetCell2.getId().endsWith("RefNode")) {

                    }
                    else {
                        continue;
                    }
                } 
                else {
                    thisElement.appendChild(childNodesWithUniformity(thisDoc, targetCell2));
                }
            }
        }
        return thisElement;
    }
    
 // for modifying the generated xml output
    public static void xmlOutputForXSD() {
        PrintWriter f0 = null;
        try {
            String path = new String();
            if (ODMEEditor.toolMode == "ses")
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/xmlforxsd.xml";
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/xmlforxsd.xml";

            f0 = new PrintWriter(
                    new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
            String path = new String();
            if (ODMEEditor.toolMode == "ses")
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml";
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsd.xml";

            in = new Scanner(new File(path));

        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int first = 0;

        while (in.hasNext()) { // Iterates each line in the file
            String mod = null;
            String line = in.nextLine();

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");

            }
            else if (line.startsWith("</")) {
                String result = line.replaceAll("[</>]", "");

                if (result.endsWith("Dec")) {
                    mod = "</aspect>";
                }
                else if (result.endsWith("MAsp")) {
                    mod = "</multiAspect>";
                } 
                else if (result.endsWith("Spec")) {
                    mod = "</specialization>";
                } 
                else {
                    if (result.endsWith("Seq")) {
                        continue;
                    }
                    mod = "</entity>";
                }
                f0.println(mod);
            } 
            else if (line.startsWith("<")) {
                if (line.endsWith("/>")) {
                    String result = line.replaceAll("[</>]", "");

                    if (result.endsWith("Var")) {
                        String novarresult = result.replace("Var", "");
                        
                        String[] properties = novarresult.split(",");
                        if (properties[1].equals("string") || properties[1].equals("boolean")) {

                            f0.println("<var name=\"" + properties[0] + "\" " + "default=\"" + properties[2]
                                       + "\"></var>");
                        }
                        else {

                            f0.println("<var name=\"" + properties[0] + "\" " + "default=\"" + properties[2]
                                       + "\" " + "lower=\"" + properties[3] + "\" " + "upper=\""
                                       + properties[4] + "\" " + "></var>");
                        }

                    } 
                    else if (result.endsWith("RefNode")) {
                        String noRefNoderesult = result.replace("RefNode", "");

                        if (noRefNoderesult.endsWith("Dec")) {
                            f0.println("<aspect name=\"" + noRefNoderesult + "\" ref=\"" + noRefNoderesult
                                       + "\"/>");
                        }
                        else if (noRefNoderesult.endsWith("MAsp")) {
                            f0.println(
                                    "<multiAspect name=\"" + noRefNoderesult + "\" ref=\"" + noRefNoderesult
                                    + "\"/>");
                        } 
                        else if (noRefNoderesult.endsWith("Spec")) {
                            f0.println("<specialization name=\"" + noRefNoderesult + "\" ref=\""
                                       + noRefNoderesult + "\"/>");
                        }
                        else {
                            f0.println("<entity name=\"" + noRefNoderesult + "\" ref=\"" + noRefNoderesult
                                       + "\"/>");
                        }
                    } else {}
                } 
                else {
                    String result = line.replaceAll("[</>]", "");

                    if (result.endsWith("Dec")) {
                        mod = "<aspect name=\"" + result + "\">";
                    } 
                    else if (result.endsWith("MAsp")) {
                        mod = "<multiAspect name=\"" + result + "\">";
                    } 
                    else if (result.endsWith("Spec")) {
                        mod = "<specialization name=\"" + result + "\">";
                    } 
                    else {
                        if (first == 0) {
                            mod = "<entity xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\""
                                  + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                                  //+ " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema\""
                                  + " xsi:noNamespaceSchemaLocation=\"ses.xsd\" name=\"" + result + "\">";
                            first = 1;
                        }
                        else {
                            if (result.endsWith("Seq")) {
                                continue;
                            }
                            mod = "<entity name=\"" + result + "\">";
                        }
                    }
                    f0.println(mod);
                }
            }
        }
        in.close();
        f0.close();
    }
    
    public static void writeSaveModuleToFileAsXML(Object obj) {
        mxCell cell = (mxCell) obj;
        String fileName = cell.getValue().toString();

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlfilter);
        fileChooser.setSelectedFile(new File(fileName));
        fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            JtreeToGraphSave.saveModuleFromCurrentModelAsXML(obj, selectedFile.getAbsolutePath());
        }
    }
    
    public static mxCell getRootNode() {
        Object[] cells = benhaviourGraph.getChildVertices(benhaviourGraph.getDefaultParent()); // getSelectionCells();

        mxCell rootcell = null;

        for (Object c : cells) {
            mxCell cell2 = (mxCell) c;
            if (cell2.isVertex()) {
                String val = cell2.getId();
                if (val.equals("rootnode")) {
                    rootcell = cell2;
                }
            }
        }
        return rootcell;
    }

    /**
     * Receives a array of string and return the TreePath of the specific node.
     */
    public static TreePath getTreeNodePath(String[] nodePath) {
        TreePath parentPath;
        new FindByName(ODMEEditor.treePanel.tree, nodePath);
        parentPath = FindByName.path;
        return parentPath;
    }
    
    /**
     * Rename node name of both graph node and jtree node. If the graph root has
     * child then it will show an message that node having child can't be renamed.
     * It take selected mxCell object as a parameter for changing the name.
     *
     * @param pos
     */
    public static void renameCell(Object pos) {
        mxCell cell = (mxCell) pos;
       
        if (cell.getId().equals("rootnode")) {
            Object[] outgoing = graph.getOutgoingEdges(cell);
            if (outgoing.length == 0) {
                String newName = JOptionPane.showInputDialog(Main.frame, "New Name", "Rename Node",
                        JOptionPane.PLAIN_MESSAGE);
                
                if (newName == null)
                    return;
                
                else if (Character.isDigit(newName.trim().charAt(0))) {
                    JOptionPane.showMessageDialog(Main.frame,
                            "Node's name should not start with a number!", "Name Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                newName = newName.replaceAll("\\s+", "");

                if ((newName != null) && (!newName.trim().isEmpty())) {
                    // jtree
                    if (cell.getId().equals("rootnode")) {
                        // assigning new projectname and root to tree
                        DefaultMutableTreeNode rootNode2 = new DefaultMutableTreeNode(newName);
                        DynamicTree.treeModel.setRoot(rootNode2);
                        DynamicTree.treeModel.reload();
                    }
                    // for graph
                    graph.getModel().beginUpdate();
                    try {
                        graph.getModel().setValue(cell, newName);
                    } 
                    finally {
                        graph.getModel().endUpdate();
                    }
                }
                odme.jtreetograph.JtreeToGraphCheck.checkSubtreeNodeForSyncRename(cell, newName);
            }
            else {
                JOptionPane.showMessageDialog(Main.frame,
                        "You can't rename a root node having child node.");
            }
        } 
        else {

            String newName = JOptionPane
                    .showInputDialog(Main.frame, "New Name", "Rename Node", JOptionPane.PLAIN_MESSAGE);
            
            if (newName == null)
                return;
            
            else if (Character.isDigit(newName.trim().charAt(0))) {
                JOptionPane.showMessageDialog(Main.frame,
                            "Node's name should not start with a number!", "Name Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newName != null) {
                newName = newName.replaceAll("\\s+", "");
            }

            if ((newName != null) && (!newName.trim().isEmpty())) {
                // if the new name is not correct then change it according to cell type.
                String currentName = (String) cell.getValue();

                if (currentName.endsWith("Dec")) {
                    if (!newName.endsWith("Dec")) {
                        newName = newName + "Dec";
                    }
                } 
                else if (currentName.endsWith("Spec")) {
                    if (!newName.endsWith("Spec")) {
                        newName = newName + "Spec";
                    }
                } 
                else if (currentName.endsWith("MAsp")) {
                    if (!newName.endsWith("MAsp")) {
                        newName = newName + "MAsp";
                    }
                }

                // for jtree

                // if a node is not connected to root then it will not try to change
                // corresponding node name from tree
                JtreeToGraphCheck.checkRootConnectivity(cell);

                if (connectedToRoot) {
                    pathToRoot.add((String) cell.getValue());
                    JtreeToGraphConvert.nodeToRootPathVar(cell);

                    String[] stringArray = pathToRoot.toArray(new String[0]);
                    ArrayList<String> pathToRootRev = new ArrayList<String>();

                    for (int i = stringArray.length - 1; i >= 0; i--) {
                        pathToRootRev.add(stringArray[i]);
                    }

                    String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

                    TreePath treePathForRename = getTreeNodePath(stringArrayRev);

                    DynamicTree.treeModel.valueForPathChanged(treePathForRename, newName);

                    pathToRoot.clear();
                }

                connectedToRoot = false;

                // for graph
                graph.getModel().beginUpdate();
                try {
                    graph.getModel().setValue(cell, newName);

                } 
                finally {
                    graph.getModel().endUpdate();
                }
            }
        }
    }
}
