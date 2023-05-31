package odme.jtreetograph;

import com.mxgraph.model.mxCell;

import odme.core.FileConvertion;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.ODMEEditor;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class JtreeToGraphConvert {

	public static void nodeToRootPathVar(mxCell cell) {
        Object[] incoming = graph.getIncomingEdges(cell);
        if (incoming.length != 0) {
            Object source = graph.getModel().getTerminal(incoming[incoming.length - 1], true);
            mxCell sourceCell = (mxCell) source;
            pathToRoot.add((String) sourceCell.getValue());
            nodeToRootPathVar(sourceCell);
        }
    }
	
	public static void rootToEndNodeVariable() {
        Object[] cells = graph.getChildVertices(graph.getDefaultParent());
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

        if (rootcell != null) {
        	JtreeToGraphNext.nextChildNodeForVariable(rootcell);
            rootToEndVariableAddition(rootcell);
        }
    }
	
	public static void rootToEndVariableAddition(mxCell varCell) {
        if (varCell.isVertex()) {
            pathToRoot.add((String) varCell.getValue());
            nodeToRootPathVar(varCell);

            String[] stringArray = pathToRoot.toArray(new String[0]);
            ArrayList<String> pathToRootRev = new ArrayList<String>();

            for (int i = stringArray.length - 1; i >= 0; i--) {
                pathToRootRev.add(stringArray[i]);
            }

            String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
            TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
            
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

            // -------------------------------------------------------
            TreeNode[] nodes = currentNode.getPath();

            String[] nodesToSelectedNode = new String[100];
            String variables = "";
            int b = 0;

            for (TreePath key : DynamicTree.varMap.keySet()) {
                int a = 0;

                for (String value : DynamicTree.varMap.get(key)) {

                    DefaultMutableTreeNode currentNode2 =
                            (DefaultMutableTreeNode) (key.getLastPathComponent());

                    TreeNode[] nodes2 = currentNode2.getPath();

                    if (nodes.length == nodes2.length) {
                    	int aa = 1;
                        for (int i = 0; i < nodes.length; i++) {
                            if (!nodes[i].toString().equals(nodes2[i].toString())) {
                                aa = 0;
                                break;
                            } 
                        }
                        a = aa;
                    }

                    if (a == 1) {
                        nodesToSelectedNode[b] = value;
                        b++;
                    }
                }
            }

            // java 8 way null removal
            nodesToSelectedNode =
                    Arrays.stream(nodesToSelectedNode).filter(s -> (s != null && s.length() > 0))
                            .toArray(String[]::new);

            for (String value : nodesToSelectedNode) {
                variables = variables + "<" + value + "Var/>" + "\n";
            }

            // treePathForVariable
            FileConvertion fileConversion = new FileConvertion();
            fileConversion.variableAdditionToNode(treePathForVariable, variables);

            pathToRoot.clear();
        }
    }
	
	// start of constraint addition
    public static void rootToEndConstraintAddition(mxCell varCell) {
        if (varCell.isVertex()) {
            String selectedNode = (String) varCell.getValue();

            pathToRoot.add((String) varCell.getValue());
            nodeToRootPathVar(varCell);

            String[] stringArray = pathToRoot.toArray(new String[0]);
            ArrayList<String> pathToRootRev = new ArrayList<String>();

            for (int i = stringArray.length - 1; i >= 0; i--) {
                pathToRootRev.add(stringArray[i]);
            }

            String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

            TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

            // -------------------------------------------------------
            TreeNode[] nodes = currentNode.getPath();

            String[] nodesToSelectedNode = new String[100];

            String variables = "";
            int b = 0;

            for (TreePath key : DynamicTree.constraintsList.keySet()) {
                int a = 0;

                for (String value : DynamicTree.constraintsList.get(key)) {
                    DefaultMutableTreeNode currentNode2 =
                            (DefaultMutableTreeNode) (key.getLastPathComponent());

                    TreeNode[] nodes2 = currentNode2.getPath();

                    if (nodes.length == nodes2.length) {
                    	int aa = 1;
                        for (int i = 0; i < nodes.length; i++) {
                            if (!nodes[i].toString().equals(nodes2[i].toString())) {
                                aa = 0;
                                break;
                            } 
                        }
                        a = aa;
                    }

                    if (a == 1) {
                        nodesToSelectedNode[b] = value;
                        b++;
                    }
                }
            }

            // java 8 way null removal
            nodesToSelectedNode =
                    Arrays.stream(nodesToSelectedNode).filter(s -> (s != null && s.length() > 0))
                            .toArray(String[]::new);

            for (String value : nodesToSelectedNode) {
                variables = variables + "<" + value + "Con/>" + "\n";
            }

            // treePathForVariable
            if (variables.length() > 8) {
                FileConvertion fileConversion = new FileConvertion();
                fileConversion.constraintAdditionToNode(selectedNode,
                        variables); // sending treePath is correct way, the
                // above one. have to check.
            }
            pathToRoot.clear();
        }
    }
    // end of solving sequence problem--------------------------------------
    
    public static void nodeToRootPath(mxCell cell) {
        Object[] incoming = graph.getIncomingEdges(cell);
        if (incoming.length != 0) {
            Object source = graph.getModel().getTerminal(incoming[incoming.length - 1], true);
            mxCell sourceCell = (mxCell) source;
            path.add((String) sourceCell.getValue());
            nodeToRootPath(sourceCell);
        }
    }
    
 // for solving sequence problem--------------------------------------
    public static void rootToEndNodeSequenceSolve() {
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

        if (rootcell != null) {
        	JtreeToGraphNext.nextChild(rootcell);
        }
    }
    
 // this function is used by other caller for inserting node in specific position
    public static mxCell rootToSelectedNode(String[] nodesToSelectedNodeOrg) {
        nodesToSelectedNode = nodesToSelectedNodeOrg;
        totalNodes = nodesToSelectedNode.length;
        nodeReached = 1; // 1 because rootnode is reached by default

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

        if (rootcell != null) {
        	JtreeToGraphNext.nextChildNodeInPath(rootcell); // important func call
        }

        if (totalNodes == 1) {
            return rootcell;
        } 
        else {
            return lastNodeInPath;
        }
    }
    
    public static void graphToXML() {
        // for sub tree generation i can use that
        // graph.getChildVertices(graph.getDefaultParent()) later

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
        }

        Document calendarDOMDoc = null;
        try {
            DOMImplementation domImpl =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();

            calendarDOMDoc = domImpl.createDocument(null, "start", null);

        } 
        catch (ParserConfigurationException e1) {
            e1.printStackTrace(System.err);
        } 
        catch (DOMException e2) {
            e2.printStackTrace(System.err);
        }

        calendarDOMDoc.getDocumentElement().appendChild(JtreeToGraphGeneral.childNodes(calendarDOMDoc, rootcell));

        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/graphxml.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/graphxml.xml";
        	
        	JtreeToGraphSave.saveToXMLFile(calendarDOMDoc, path);
        } 
        catch (TransformerException ex) {
            Logger.getLogger(ODMEEditor.class.getName()).log(Level.SEVERE, null, ex);
        }

        JtreeToGraphModify.modifyXmlOutput();
    }
    
    public static void graphToXMLWithUniformity() {
        // for sub tree generation i can use that
        // graph.getChildVertices(graph.getDefaultParent()) later
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
        }

        Document calendarDOMDoc = null;
        try {
            DOMImplementation domImpl =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();

            calendarDOMDoc = domImpl.createDocument(null, "start", null);

        } catch (ParserConfigurationException e1) {
            e1.printStackTrace(System.err);
        } catch (DOMException e2) {
            e2.printStackTrace(System.err);
        }

        calendarDOMDoc.getDocumentElement().appendChild(JtreeToGraphGeneral.childNodesWithUniformity(calendarDOMDoc, rootcell));

        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/graphxmluniformity.xml";
        	else 
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/graphxmluniformity.xml"; 
        	JtreeToGraphSave.saveToXMLFile(calendarDOMDoc, path);
        } 
        catch (TransformerException ex) {
            Logger.getLogger(ODMEEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        JtreeToGraphModify.modifyXmlOutput();
    }  
    
 // For saving and retrieving graph and tree now i am not using this, but now i
    // will use this for.
    // For Retrieving project tree this below function is using
    public static void convertTreeToXML() {
        TreeNode thisTreeNode = (TreeNode) ODMEEditor.projectPanel.projectTree.getModel().getRoot();
        Document calendarDOMDoc = null;
        try {
            DOMImplementation domImpl =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            calendarDOMDoc = domImpl.createDocument(null, "start", null);
        } 
        catch (ParserConfigurationException e1) {
            e1.printStackTrace(System.err);
        } 
        catch (DOMException e2) {
            e2.printStackTrace(System.err);
        }
        calendarDOMDoc.getDocumentElement().appendChild(JtreeToGraphSave.saveAllTreeNodes(calendarDOMDoc, thisTreeNode));
        try {
        	if (ODMEEditor.toolMode == "ses")
        		JtreeToGraphSave.saveToXMLFile(calendarDOMDoc, ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/projectTree.xml");
        	else
        		JtreeToGraphSave.saveToXMLFile(calendarDOMDoc, ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/projectTree.xml");

        } 
        catch (TransformerException ex) {
            Logger.getLogger(ODMEEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        JtreeToGraphModify.modifyXmlOutputSES();
    }
}
