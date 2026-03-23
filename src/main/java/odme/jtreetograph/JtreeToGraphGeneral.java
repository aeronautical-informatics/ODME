package odme.jtreetograph;

import odme.core.EditorContext;
import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;

import odme.core.FindByName;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;

import static behaviourtreetograph.JTreeToGraphBehaviour.benhaviourGraph;
import static odme.jtreetograph.JtreeToGraphVariables.*;

import odme.domain.transform.XmlTransformRules;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter; 
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JtreeToGraphGeneral {

    public static String rootNodeName() {
        Object[] cells = graph.getChildVertices(graph.getDefaultParent());
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
        graphComponent.zoomIn();
    }
    
    public static void zoomOut() {
        graphComponent.zoomOut();
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

        String nodeName = graph.getModel().getValue(cell).toString();
        thisElement = thisDoc.createElement(nodeName);

        Object[] outgoing = graph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
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

        String nodeName = graph.getModel().getValue(cell).toString();
        thisElement = thisDoc.createElement(nodeName);

        Object[] outgoing = graph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
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
        XmlTransformRules transformRules = new XmlTransformRules();

        // Read input file
        List<String> inputLines = new ArrayList<>();
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/outputgraphxmlforxsd.xml";

            Scanner in = new Scanner(new File(path));
            while (in.hasNext()) {
                inputLines.add(in.nextLine());
            }
            in.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Apply XSD transformation rules
        List<String> outputLines = transformRules.applyXsdTransformRules(inputLines);

        // Write output file
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/xmlforxsd.xml";

            PrintWriter f0 = new PrintWriter(new FileWriter(path));
            for (String line : outputLines) {
                f0.println(line);
            }
            f0.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static void writeSaveModuleToFileAsXML(Object obj) {
        mxCell cell = (mxCell) obj;
        String fileName = cell.getValue().toString();

        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
        fileChooser.setFileFilter(xmlfilter);
        fileChooser.setSelectedFile(new File(fileName));
        fileChooser.setCurrentDirectory(new File(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName()));
        int result = fileChooser.showSaveDialog(Main.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            JtreeToGraphSave.saveModuleFromCurrentModelAsXML(obj, selectedFile.getAbsolutePath());
        }
    }
    
    public static mxCell getRootNode() {
        Object[] cells = graph.getChildVertices(graph.getDefaultParent()); // getSelectionCells();

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
                JtreeToGraphCheck.checkSubtreeNodeForSyncRename(cell, newName);
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
    
    /**
     * Open existing project from disk. Read the XML file from the specified
     * location with the provided file name then according to XML file changes the
     * parent and update the graph after accordingly.
     *
     * @param filename
     * @param oldProjectTreeProjectName
     */
    public static void openExistingProject(String filename, String oldProjectTreeProjectName) {
        parent = graph.getDefaultParent();

        String path = EditorContext.getInstance().getWorkingDir() + "/" + filename + "Graph.xml";
        
        /* EditorContext.getInstance().getSsdFileGraph() set via context */

        if (EditorContext.getInstance().getSsdFileGraph().exists()) {
            graph.getModel().beginUpdate();
            try {
                Document xml = mxXmlUtils.parseXml(mxUtils.readFile(path));
                mxCodec codec = new mxCodec(xml);
                codec.decode(xml.getDocumentElement(), graph.getModel());
                parent = graph.getDefaultParent();

            } 
            catch (Exception ex) {
                ex.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
            } 
            finally {
                graph.getModel().endUpdate();
            }
                     
            ODMEEditor.treePanel.openExistingProject(filename, oldProjectTreeProjectName);
        }
    }   
}
