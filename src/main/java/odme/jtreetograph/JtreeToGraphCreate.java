package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.w3c.dom.Document;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import odme.contextmenus.GraphCellPopUp;
import odme.contextmenus.GraphPopup;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;
import odme.odmeeditor.PanelSplitor;



public class JtreeToGraphCreate {
	
	public static mxStylesheet stylesheet;

	/**
     * Under the MVC model, the JGraph class is a controller, GraphModel is a model,
     * and GraphUI is a view.
     *
     * @param frame
     */
    public static void createGraph(JInternalFrame frame) {
    	
        graph = new mxGraph();
        undoManager = new mxUndoManager();

        // setting default edge color
        graph.getStylesheet().getDefaultEdgeStyle()
                .put(mxConstants.STYLE_STROKECOLOR, mxUtils.getHexColorString(new Color(17, 138, 178)));
        graph.setCellsEditable(false);

        // creating new style
        mxStylesheet stylesheet = graph.getStylesheet();
        
        Hashtable<String, Object> entity = new Hashtable<String, Object>();
        entity.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        entity.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        entity.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        entity.put(mxConstants.STYLE_ROUNDED, true);
        entity.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_BOTTOM);
        entity.put(mxConstants.STYLE_STROKECOLOR, mxUtils.getHexColorString(new Color(64, 61, 57)));
        entity.put(mxConstants.STYLE_STROKEWIDTH, 2);
        entity.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.BLACK));
        stylesheet.putCellStyle("Entity", entity);

        Hashtable<String, Object> multiaspect = new Hashtable<String, Object>();
        multiaspect.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
        multiaspect.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
        multiaspect.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        
        multiaspect.put(mxConstants.STYLE_IMAGE,
                ODMEEditor.class.getClassLoader().getResource("images/multi.png"));
        
        stylesheet.putCellStyle("Multiaspect", multiaspect);

        Hashtable<String, Object> aspect = new Hashtable<String, Object>();
        aspect.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
        aspect.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
        aspect.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        aspect.put(mxConstants.STYLE_IMAGE,
                ODMEEditor.class.getClassLoader().getResource("images/aspect.png"));
        stylesheet.putCellStyle("Aspect", aspect);

        Hashtable<String, Object> specialization = new Hashtable<String, Object>();
        specialization.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
        specialization.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
        specialization.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        
        specialization.put(mxConstants.STYLE_IMAGE,
                ODMEEditor.class.getClassLoader().getResource("images/spec.png"));
        
        specialization.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.BLACK));
        stylesheet.putCellStyle("Specialization", specialization);
        
        //###############
        
        Hashtable<String, Object> entityp = new Hashtable<String, Object>();
        entityp.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
        entityp.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        entityp.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
        entityp.put(mxConstants.STYLE_ROUNDED, true);
        entityp.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_BOTTOM);
        entityp.put(mxConstants.STYLE_STROKECOLOR, mxUtils.getHexColorString(new Color(0, 128, 0)));
        entityp.put(mxConstants.STYLE_STROKEWIDTH, 2);
        entityp.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.green));
        stylesheet.putCellStyle("Entityp", entityp);


        Hashtable<String, Object> multiaspectp = new Hashtable<String, Object>();
        multiaspectp.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
        multiaspectp.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
        multiaspectp.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        multiaspectp.put(mxConstants.STYLE_IMAGE,
                ODMEEditor.class.getClassLoader().getResource("images/maspectToBePruned.png"));
        stylesheet.putCellStyle("Multiaspectp", multiaspectp);

        Hashtable<String, Object> specializationp = new Hashtable<String, Object>();
        specializationp.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
        specializationp.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
        specializationp.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
        specializationp.put(mxConstants.STYLE_IMAGE,
                ODMEEditor.class.getClassLoader().getResource("images/specializationToBePruned.png"));
        specializationp.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.BLACK));
        stylesheet.putCellStyle("Specializationp", specializationp);
        
        //###############

        parent = graph.getDefaultParent();

        if (ssdFileGraph.exists()) {
            graph.getModel().beginUpdate();
            try {
                // use "org.w3c.dom.Document" not swing Document
                Document xml = mxXmlUtils.parseXml(mxUtils.readFile(
                        ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + projectFileNameGraph
                        + "Graph.xml"));

                mxCodec codec = new mxCodec(xml);
                codec.decode(xml.getDocumentElement(), graph.getModel());
                parent = graph.getDefaultParent();
            } 
            catch (Exception ex) {
                ex.printStackTrace();
            } 
            finally {
                graph.getModel().endUpdate();
            }
        } 
        else {
            graph.getModel().beginUpdate();
            try {
                graph.insertVertex(parent, "rootnode", "Thing", 515, 20, 80, 30, "Entity");
                graph.insertVertex(parent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");
                graph.insertVertex(parent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
            } 
            finally {
                graph.getModel().endUpdate();
            }
        }
        graphComponent = new mxGraphComponent(graph);
        graphComponent.getViewport().setBackground(Color.WHITE);
        
        // undo redo settings
        mxEventSource.mxIEventListener listener = new mxEventSource.mxIEventListener() {

            @Override
            public void invoke(Object sender, mxEventObject evt) {
                // this condition is added to control subtree undo addition
                if (!ODMEEditor.undoControlForSubTree == true) {
                    undoManager.undoableEditHappened((mxUndoableEdit) evt.getProperty("edit"));
                }
            }
        };

        // for undo redo
        graph.getModel().addListener(mxEvent.UNDO, listener);
        graph.getView().addListener(mxEvent.UNDO, listener);

        frame.getContentPane().add(graphComponent);

        // for edge connection event
        graph.addListener(mxEvent.CELL_CONNECTED, new mxIEventListener() {
            @Override
            public void invoke(Object sender, mxEventObject evt) {
                mxCell connectionCell = (mxCell) evt.getProperty("edge");

                lastAddedCell = (mxCell) connectionCell
                        .getSource(); // if there is no terminal cell then have to handle
            }
        });

        graphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
            @SuppressWarnings("finally")
			public void mouseReleased(MouseEvent e) {
                Object cell = graphComponent.getCellAt(e.getX(), e.getY());
                currentSelectedCell = (mxCell) cell; // this for console use

                if (e.getButton() == MouseEvent.BUTTON1) {
                	
                    if (cell == null) {
                        String ob = Integer.toString(nodeNumber);
                        String nodeName = "node" + ob;

                        graph.getModel().beginUpdate();

                        try {
                            if (ODMEEditor.nodeAddDetector.equals("entity")) {
                                graph.insertVertex(parent, null, nodeName, e.getX() - 40, e.getY(),
                                                80, 30, "Entity");
                                nodeNumber++;
                            } 
                            else if (ODMEEditor.nodeAddDetector.equals("aspect")) {
                                graph.insertVertex(parent, null, nodeName + "Dec", e.getX() - 15,
                                        e.getY(), 30, 30, "Aspect");
                                nodeNumber++;
                            } 
                            else if (ODMEEditor.nodeAddDetector.equals("multiaspect")) {
                                graph.insertVertex(parent, null, nodeName + "MAsp", e.getX() - 15,
                                                e.getY(), 30, 30, "Multiaspect");
                                nodeNumber++;
                            } 
                            else if (ODMEEditor.nodeAddDetector.equals("specialization")) {
                                graph.insertVertex(parent, null, nodeName + "Spec", e.getX() - 15,
                                                e.getY(), 30, 30, "Specialization");
                                nodeNumber++;
                            }
                        } 
                        finally {
                            graph.getModel().endUpdate();
                            // De-Selecting mouse selection from menu items
                            ODMEEditor.nodeAddDetector = "";
                            return;
                        }
                    } 
                    else {
                        // this section is for showing variables of the selected node to the variable
                        // table
                        if (!ODMEEditor.nodeAddDetector.equals("delete")) {
                        	
                            Object showvar = graphComponent.getCellAt(e.getX(), e.getY());
                            mxCell varCell = (mxCell) showvar;
                            selectedNodeCellForVariableUpdate = varCell;
                            
                            if (varCell.isVertex()) {
                            	
                            	// double click handling
                                if (e.getClickCount() == 2) {
                                    mxCell clikedCell = (mxCell) cell;
                                    if (clikedCell.isVertex()) {
                                    	if (ODMEEditor.toolMode == "ses") {
                                    		Object position = graphComponent.getCellAt(e.getX(), e.getY());
                                    		JtreeToGraphGeneral.renameCell(position);
                                    	}
                                    }
                                }
                                
                                pathToRoot.add((String) varCell.getValue());
                                JtreeToGraphConvert.nodeToRootPathVar(varCell);

                                String[] stringArray = pathToRoot.toArray(new String[0]);
                                ArrayList<String> pathToRootRev = new ArrayList<String>();

                                for (int i = stringArray.length - 1; i >= 0; i--) {
                                    pathToRootRev.add(stringArray[i]);
                                }

                                String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
                            
                                TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
                                if (!(treePathForVariable == null)) {
                                    DefaultMutableTreeNode currentNode =
                                            (DefaultMutableTreeNode) (treePathForVariable
                                                                              .getLastPathComponent()); // if mouse not released then some condition

                                    // -------------------------------------------------------
                                    TreeNode[] nodes = currentNode.getPath();

                                    String[] nodesToSelectedNode = new String[100];
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

                                    nodesToSelectedNode = Arrays.stream(nodesToSelectedNode)
                                            .filter(s -> (s != null && s.length() > 0))
                                            .toArray(String[]::new);

                                    Arrays.parallelSort(nodesToSelectedNode);

                                    ODMEEditor.scenarioVariable
                                            .showNodeValuesInTable(currentNode.toString(),
                                                    nodesToSelectedNode);
                                    
                                    variableList = nodesToSelectedNode;
                                    
                                    // -------------------------------------------------------

                                    pathToRoot.clear();

                                    // for showing constraints in the table when selecting any node from graph;
                                    ODMEEditor.treePanel.showConstraintsInTable(treePathForVariable);
                                } 
                                else {
                                    pathToRoot.clear();
                                    
                                }
                            }
                            
                            
//                            if (ODMEEditor.toolMode == "ses") {
//                            	new MyKeyboardHandler(graphComponent); 
//                            	graph.addListener(mxEvent.CELLS_REMOVED, new mxIEventListener() {
//                            		@Override public void invoke(Object sender, mxEventObject evt) {
//                            			try {
//                            				Object delcell = graphComponent.getCellAt(e.getX(), e.getY());
//
//                                            if (delcell != null) {
//                                                mxCell deleteCell = (mxCell) delcell;
//
//                                                if (deleteCell.isEdge()) {
//                                                	JtreeToGraphDelete.deleteEdgeFromGraphPopup(delcell);
//                                                } 
//                                                else {
//                                                	JtreeToGraphDelete.deleteNodeFromGraphPopup(delcell);
//                                                }
//                                            }
//                            				Thread.sleep(10);
//                            			} catch (InterruptedException e1) {
//                            				e1.printStackTrace();
//                            			}
//                            		} 
//                            	});
//                            }   
                        }
                        
                        //if (SESEditor.nodeAddDetector.equals("delete")) {
                        else {
                            // this delete will not delete all the child node. becuase sometimes we need to
                            // delete
                            // only one node that time we can use this and for that we have to add another
                            // button for not synchronizing with jtree

                            Object delcell = graphComponent.getCellAt(e.getX(), e.getY());
                            if (delcell != null) {
                                mxCell deleteCell = (mxCell) delcell;

                                if (deleteCell.isEdge()) {
                                	JtreeToGraphDelete.deleteEdgeFromGraphPopup(delcell);
                                } 
                                else {
                                	JtreeToGraphDelete.deleteNodeFromGraphPopup(delcell);
                                }
                            }
                            // De-Selecting mouse selection from menu items
                            ODMEEditor.nodeAddDetector = "";
                        }

                        
                    } // end of else from if cell==null

                    
                      
                    
                 // for the last added node
                    callAfterEdgeConnectionComplete();
                } // button 1 end

                // right click events using pop up menu
                if (e.getButton() == MouseEvent.BUTTON3) {
                	
                	Object showvar = graphComponent.getCellAt(e.getX(), e.getY());
                    mxCell varCell = (mxCell) showvar;
                    selectedNodeCellForVariableUpdate = varCell;
                    
                    //------------------------------
                    if (cell != null) {
                    if (varCell.isVertex()) {
                    	
                    	// double click handling
                        if (e.getClickCount() == 2) {
                            mxCell clikedCell = (mxCell) cell;
                            if (clikedCell.isVertex()) {
                            	if (ODMEEditor.toolMode == "ses") {
                            		Object position = graphComponent.getCellAt(e.getX(), e.getY());
                            		JtreeToGraphGeneral.renameCell(position);
                            	}
                            }
                        }
                        
                        pathToRoot.add((String) varCell.getValue());
                        JtreeToGraphConvert.nodeToRootPathVar(varCell);

                        String[] stringArray = pathToRoot.toArray(new String[0]);
                        ArrayList<String> pathToRootRev = new ArrayList<String>();

                        for (int i = stringArray.length - 1; i >= 0; i--) {
                            pathToRootRev.add(stringArray[i]);
                        }

                        String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

                        TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
                        if (!(treePathForVariable == null)) {
                            DefaultMutableTreeNode currentNode =
                                    (DefaultMutableTreeNode) (treePathForVariable
                                                                      .getLastPathComponent()); // if mouse not released then some condition

                            // -------------------------------------------------------
                            TreeNode[] nodes = currentNode.getPath();
                            String[] nodesToSelectedNode = new String[100];
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

                            nodesToSelectedNode = Arrays.stream(nodesToSelectedNode)
                                    .filter(s -> (s != null && s.length() > 0))
                                    .toArray(String[]::new);

                            Arrays.parallelSort(nodesToSelectedNode);

                            ODMEEditor.scenarioVariable
                                    .showNodeValuesInTable(currentNode.toString(),
                                            nodesToSelectedNode);
                            
                            variableList = nodesToSelectedNode;
                            // -------------------------------------------------------

                            pathToRoot.clear();

                            // for showing constraints in the table when selecting any node from graph;
                            ODMEEditor.treePanel.showConstraintsInTable(treePathForVariable);
                        } 
                        else {
                            pathToRoot.clear();
                        }
                    }
                    }
                  //------------------------------
       	
                    // for fixing popup window while page is more thant monitor height
                    Point p = MouseInfo.getPointerInfo().getLocation();
                    int y = p.y;
                    int x = p.x;
                    int screenWidth = java.awt.Toolkit.getDefaultToolkit().getScreenSize().width;

                    if (x > screenWidth) {
                        x = x - PanelSplitor.dividerLocation - 10 - screenWidth;
                    } 
                    else {
                        x = x - PanelSplitor.dividerLocation - 10;
                    }
                    // end of that

                    // here position will be edge or vertex on that place
                    Object position = graphComponent.getCellAt(e.getX(), e.getY());

                    // checking if there is any vertex or edge
                    if (position != null) {
                    	if (!((mxCell) position).isVertex() && ODMEEditor.toolMode == "pes") {}
                    	else {
                    		GraphCellPopUp graphCellPopup = new GraphCellPopUp(position);
                    		if (e.isPopupTrigger()) {
                    			graphCellPopup.show(graphComponent, x, y - 104);
                    		}
                    	}
                        
                    }
                    else {
                    	if (ODMEEditor.toolMode == "ses") {	
                    		GraphPopup graphPopup = new GraphPopup(e.getX(), e.getY());

                    		if (e.isPopupTrigger()) {
                    			graphPopup.show(graphComponent, x, y - 104);
                    		}
                    	}
                    }
                }
                
            }
            // mouse event 2
        });
    }
    
    public static void callAfterEdgeConnectionComplete() {
        mxCell addedCell = null;

        if (lastAddedCell != null && graph.getOutgoingEdges(lastAddedCell).length > 0) {
            Object[] outgoing = graph.getOutgoingEdges(lastAddedCell);
            Object targetCell = graph.getModel().getTerminal(outgoing[outgoing.length - 1], false);
            addedCell = (mxCell) targetCell;
            Object[] incoming = graph.getIncomingEdges(addedCell);

            Object lastAddedEdge = outgoing[outgoing.length - 1];
            mxCell lastEdge = (mxCell) lastAddedEdge;
            if (lastEdge.isEdge()) {
                try {
                    lastEdge.getTarget().getValue();
                } 
                catch (Exception e) {
                    graph.removeCells(new Object[] {outgoing[outgoing.length - 1]});
                    addedCell = null;
                    lastAddedCell = null;
                    return;
                }
            }

            if (lastAddedCell.getId().startsWith("uniformity")) {
                graph.removeCells(new Object[] {outgoing[outgoing.length - 1]});

                JOptionPane.showMessageDialog(Main.frame,
                        "You can not add here. Add to the reference node.");

                addedCell = null;
                lastAddedCell = null;
            }

            if (incoming.length > 1) {
                graph.removeCells(new Object[] {outgoing[outgoing.length - 1]});
                lastAddedCell = null;
            } 
            else {
                if (addedCell != null) {

                	JtreeToGraphConvert.nodeToRootPath(addedCell);

                    mxCell cellParentCheck = lastAddedCell;
                    lastAddedCell = null;

                    // update jtree
                    String[] stringArray = path.toArray(new String[0]);
                    ArrayList<String> pathRev = new ArrayList<String>();

                    for (int i = stringArray.length - 1; i >= 0; i--) {
                        pathRev.add(stringArray[i]);
                    }

                    String[] stringArrayRev = pathRev.toArray(new String[0]);

                    // have to check if the source node is connected with any parent or not.
                    /*
                     * for (int i = stringArrayRev.length - 1; i >= 0; i--) {
                     * System.out.println(stringArrayRev[i]); }
                     */

                    if (cellParentCheck != null) {
                        if (cellParentCheck.getId()
                                .equals("rootnode")) {// by using checkRootConnectivity() i can omit
                            // this section and make it only on if else.
                            // have to check later
                            if (graph.getOutgoingEdges(addedCell).length > 0) {
                                // for cell who has child elements and added to some other parent as- a child
                                // node. subtree addition
                            	JtreeToGraphSave.saveModuleFromCurrentModel(cellParentCheck);
                                ODMEEditor.undoControlForSubTree = true;
                                JtreeToGraphAdd.addModuleFromSubgraph(cellParentCheck);
                                JtreeToGraphDelete.deleteNodeFromGraphPopupForSubTree(firstAddedCellForSubTreeDeletion);
                                ODMEEditor.undoControlForSubTree = false;
                                firstAddedCellForSubTreeDeletion = null;
                                firstAddedCellForSubTree = 0;

                            } else {
                            	JtreeToGraphAdd.addNodeWIthGraphAddition(addedCell.getValue().toString(),
                                        stringArrayRev);
                                // have to check subtree here
                            }
                        }
                        else {// if not root node
                        	JtreeToGraphCheck.checkRootConnectivity(cellParentCheck);

                            if (connectedToRoot) {
                                Object[] forParentCheck = graph.getIncomingEdges(cellParentCheck);
                                if (forParentCheck.length == 1) {
                                    if (graph.getOutgoingEdges(addedCell).length > 0) {
                                        // for cell who has child elements and added to some other parent as a child
                                        // node. subtree creation
                                    	JtreeToGraphSave.saveModuleFromCurrentModel(cellParentCheck);
                                        ODMEEditor.undoControlForSubTree = true;
                                        JtreeToGraphAdd.addModuleFromSubgraph(cellParentCheck);
                                        JtreeToGraphDelete.deleteNodeFromGraphPopupForSubTree(firstAddedCellForSubTreeDeletion);
                                        ODMEEditor.undoControlForSubTree = false;
                                        firstAddedCellForSubTreeDeletion = null;
                                        firstAddedCellForSubTree = 0;
                                    }
                                    else {
                                    	JtreeToGraphAdd.addNodeWIthGraphAddition(addedCell.getValue().toString(),
                                                stringArrayRev);

                                        System.out.println("Tested syn now");
                                        System.out.println("cellParentCheck:" + cellParentCheck.getValue());
                                        System.out.println("addedCell:" + addedCell.getValue());
                                        // Synchronization with its child node while adding into main reference node
                                        JtreeToGraphCheck.checkSubtreeNodeForSync(cellParentCheck, addedCell);
                                        // have to check subtree here for adding node with same node
                                        JtreeToGraphCheck.checkSubtreeNode(addedCell);
                                    }
                                }
                            }
                            connectedToRoot = false;
                        }
                        cellParentCheck = null;
                    }
                    path.clear();
                }
            }
        }
    }
}
