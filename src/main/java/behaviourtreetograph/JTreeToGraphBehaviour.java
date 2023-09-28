package behaviourtreetograph;


import java.awt.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;


import com.mxgraph.model.mxCell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxUndoManager;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import odeme.behaviour.ODMEBehaviourEditor;

import odme.core.UndoableTreeModel;

import odme.odmeeditor.ODMEEditor;

import static odme.jtreetograph.JtreeToGraphVariables.*;

public class JTreeToGraphBehaviour {


	public static mxGraph benhaviourGraph;
	public static Object behaviourParent;
	private DefaultMutableTreeNode rootNode;
	public static UndoableTreeModel treeModel;
	public JTree tree;

	private Toolkit toolkit;

	JTreeToGraphBehaviour(){

		toolkit = Toolkit.getDefaultToolkit();

//		rootNode = new DefaultMutableTreeNode("Thing");
//		treeModel = new UndoableTreeModel(rootNode);
//		treeModel.addTreeModelListener(new MyTreeModelListener());

	}
	public static void createGraph(JInternalFrame frame) {

		benhaviourGraph = new mxGraph();
		undoManager = new mxUndoManager();

		// setting default edge color
		benhaviourGraph.getStylesheet().getDefaultEdgeStyle()
				.put(mxConstants.STYLE_STROKECOLOR, mxUtils.getHexColorString(new Color(17, 138, 178)));
		benhaviourGraph.setCellsEditable(false);

		// creating new style
		mxStylesheet stylesheet = benhaviourGraph.getStylesheet();

		//
		Hashtable<String, Object> entityBehaviour = new Hashtable<String, Object>();
		entityBehaviour.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE);
		entityBehaviour.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
		entityBehaviour.put(mxConstants.STYLE_FILLCOLOR, "#FFFFFF");
		entityBehaviour.put(mxConstants.STYLE_ROUNDED, true);
		entityBehaviour.put(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_BOTTOM);
		entityBehaviour.put(mxConstants.STYLE_STROKECOLOR, mxUtils.getHexColorString(new Color(64, 61, 57)));
		entityBehaviour.put(mxConstants.STYLE_STROKEWIDTH, 2);
		entityBehaviour.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.BLACK));
		stylesheet.putCellStyle("Entity", entityBehaviour);

		Hashtable<String, Object> decorator = new Hashtable<String, Object>();
		decorator.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
		decorator.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
		decorator.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));

		decorator.put(mxConstants.STYLE_IMAGE,
				ODMEEditor.class.getClassLoader().getResource("images/decorator.png"));

		stylesheet.putCellStyle("Decorator", decorator);

		Hashtable<String, Object> selector = new Hashtable<String, Object>();
		selector.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
		selector.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
		selector.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));
		selector.put(mxConstants.STYLE_IMAGE,
				ODMEEditor.class.getClassLoader().getResource("images/selector.png"));
		stylesheet.putCellStyle("Selector", selector);

		Hashtable<String, Object> sequence = new Hashtable<String, Object>();
		sequence.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
		sequence.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
		sequence.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));

		sequence.put(mxConstants.STYLE_IMAGE,
				ODMEEditor.class.getClassLoader().getResource("images/sequence.png"));

		sequence.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.BLACK));

		stylesheet.putCellStyle("Sequence", sequence);


		Hashtable<String, Object> parallel = new Hashtable<String, Object>();
		parallel.put(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_IMAGE);
		parallel.put(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP);
		parallel.put(mxConstants.STYLE_FONTCOLOR, mxUtils.getHexColorString(Color.BLACK));

		parallel.put(mxConstants.STYLE_IMAGE,
				ODMEEditor.class.getClassLoader().getResource("images/parallel.png"));

		parallel.put(mxConstants.STYLE_EDGE, mxUtils.getHexColorString(Color.BLACK));

		stylesheet.putCellStyle("Parallel", parallel);

		behaviourParent = benhaviourGraph.getDefaultParent();

//		behaviourParent.toString();
		benhaviourGraph.getModel().beginUpdate();
		try {
			benhaviourGraph.insertVertex(behaviourParent, "rootnode", "Events", 515, 20, 80, 30, "Entity");
			benhaviourGraph.insertVertex(behaviourParent, "hideV", "End of Canvas", 0, 50000, 80, 30, "Entity");
			benhaviourGraph.insertVertex(behaviourParent, "hideH", "End of Canvas", 50000, 0, 80, 30, "Entity");
		} finally {
			benhaviourGraph.getModel().endUpdate();
		}

		behaviourGraphComponent = new mxGraphComponent(benhaviourGraph);
		behaviourGraphComponent.getViewport().setBackground(Color.WHITE);

		// undo redo settings
		mxEventSource.mxIEventListener listener = new mxEventSource.mxIEventListener() {

			@Override
			public void invoke(Object sender, mxEventObject evt) {
				// this condition is added to control subtree undo addition
				if (!ODMEBehaviourEditor.undoControlForSubTree == true) {
					undoManager.undoableEditHappened((mxUndoableEdit) evt.getProperty("edit"));
				}
			}
		};

		// for undo redo
		benhaviourGraph.getModel().addListener(mxEvent.UNDO, listener);
		benhaviourGraph.getView().addListener(mxEvent.UNDO, listener);

		frame.getContentPane().add(behaviourGraphComponent);


		// for edge connection event
		benhaviourGraph.addListener(mxEvent.CELL_CONNECTED, new mxIEventListener() {
			@Override
			public void invoke(Object sender, mxEventObject evt) {
				mxCell connectionCell = (mxCell) evt.getProperty("edge");
//				System.out.println("connectionCell edge count  = "+connectionCell.getEdgeCount());
				System.out.println("connectionCell ID  = "+connectionCell.getId());
				lastAddedCell = (mxCell) connectionCell
						.getSource(); // if there is no terminal cell then have to handle
			}
		});

		behaviourGraphComponent.getGraphControl().addMouseListener(new MouseAdapter() {
			@SuppressWarnings("finally")
			public void mouseReleased(MouseEvent e) {
				Object cell = behaviourGraphComponent.getCellAt(e.getX(), e.getY());
				currentSelectedCell = (mxCell) cell; // this for console use

				if (e.getButton() == MouseEvent.BUTTON1) {
					if (cell == null) {

						String ob = Integer.toString(behaviourNodeNumber);
						String nodeName = "node" + ob;

						benhaviourGraph.getModel().beginUpdate();

						try {

							if (ODMEBehaviourEditor.nodeAddDecorator.equals("entity")) {
								benhaviourGraph.insertVertex(behaviourParent, null, nodeName, e.getX() - 40, e.getY(),
										80, 30, "Entity");
								behaviourNodeNumber++;
							} else if (ODMEBehaviourEditor.nodeAddDecorator.equals("Decorator")) {
								benhaviourGraph.insertVertex(behaviourParent, "Loop repeat ",  "Loop", e.getX() - 40,
										e.getY(), 80, 30, "Decorator");
								behaviourNodeNumber++;
							} else if (ODMEBehaviourEditor.nodeAddDecorator.equals("Selector")) {
								benhaviourGraph.insertVertex(behaviourParent, "Selector",  "Selector", e.getX() - 15,
										e.getY(), 30, 30, "Selector");
								behaviourNodeNumber++;
							} else if (ODMEBehaviourEditor.nodeAddDecorator.equals("Sequence")) {
								benhaviourGraph.insertVertex(behaviourParent, "Sequence",  "Sequence", e.getX() - 15,
										e.getY(), 30, 30, "Sequence");
								behaviourNodeNumber++;
							} else if (ODMEBehaviourEditor.nodeAddDecorator.equals("Parallel")) {
								benhaviourGraph.insertVertex(behaviourParent, "Parallel",  "Parallel", e.getX() - 15,
										e.getY(), 30, 30, "Parallel");
								behaviourNodeNumber++;
							} else if(ODMEBehaviourEditor.nodeBehaviour.length()>0) {

								if(ODMEBehaviourEditor.nodeBehaviour.contains("Dec")) {
								}else if (ODMEBehaviourEditor.nodeBehaviour.contains("MAsp")) {}
								else
									if (ODMEBehaviourEditor.nodeBehaviour.contains("Spec")) {}
									else {
									benhaviourGraph.insertVertex(behaviourParent, ODMEBehaviourEditor.nodeBehaviour, ODMEBehaviourEditor.nodeBehaviour, e.getX() - 40, e.getY(),
											80, 30, "Entity");
									behaviourNodeNumber++;
									ODMEBehaviourEditor.nodeBehaviour = "";
								}
							}

						} finally {
							benhaviourGraph.getModel().endUpdate();
							// De-Selecting mouse selection from menu items
							ODMEBehaviourEditor.nodeAddDecorator = "";
							return;
						}
					}

					else {
//						System.out.println("else block  cell is  null ");

						// this section is for showing variables of the selected node to the variable
						// table
						if (!ODMEBehaviourEditor.nodeAddDecorator.equals("delete")) {

						} else {

							Object delcell = behaviourGraphComponent.getCellAt(e.getX(), e.getY());
							if (delcell != null) {
								mxCell deleteCell = (mxCell) delcell;

								if (deleteCell.isEdge()) {
									JtreeToGraphDelete.deleteEdgeFromGraphPopup(delcell);
								} else {
									JtreeToGraphDelete.deleteNodeFromGraphPopup(delcell);
								}
							}
							// De-Selecting mouse selection from menu items
							ODMEBehaviourEditor.nodeAddDecorator = "";
						}
					} // end of else from if cell==null
					callAfterEdgeConnectionComplete();
				} // button 1 end

				// right click events using pop up menu
//				if (e.getButton() == MouseEvent.BUTTON3) {
////                        	// double click handling
//					if (e.getClickCount() == 2) {
//						mxCell clikedCell = (mxCell) cell;
//						if (clikedCell.isVertex()) {
//
//							Object position = behaviourGraphComponent.getCellAt(e.getX(), e.getY());
//							JtreeToGraphGeneral.renameCell(position);
//
//						}
//					}
//				}

			}
			// mouse event 2
		});
	}


	public static void callAfterEdgeConnectionComplete() {
		mxCell addedCell = null;

		if (lastAddedCell != null && benhaviourGraph.getOutgoingEdges(lastAddedCell).length > 0) {

			Object[] outgoing = benhaviourGraph.getOutgoingEdges(lastAddedCell);
			Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[outgoing.length - 1], false);
			addedCell = (mxCell) targetCell;
			Object[] incoming = benhaviourGraph.getIncomingEdges(addedCell);

			Object lastAddedEdge = outgoing[outgoing.length - 1];
			mxCell lastEdge = (mxCell) lastAddedEdge;
			if (lastEdge.isEdge()) {
				try {
					lastEdge.getTarget().getValue();
				}
				catch (Exception e) {
					benhaviourGraph.removeCells(new Object[] {outgoing[outgoing.length - 1]});
					addedCell = null;
					lastAddedCell = null;
					return;
				}
			}

//			if (lastAddedCell.getId().startsWith("uniformity")) {
//				benhaviourGraph.removeCells(new Object[] {outgoing[outgoing.length - 1]});
//
//				JOptionPane.showMessageDialog(MainWindow.frame,
//						"You can not add here. Add to the reference node.");
//
//				addedCell = null;
//				lastAddedCell = null;
//			}

			if (incoming.length > 1) {
				benhaviourGraph.removeCells(new Object[] {outgoing[outgoing.length - 1]});
				lastAddedCell = null;
			}
		}
	}

	class MyTreeModelListener implements TreeModelListener {

		public void treeNodesChanged(TreeModelEvent e) {
			DefaultMutableTreeNode node;
			node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());

			/*
			 * If the event lists children, then the changed node is the child of the node
			 * we've already gotten. Otherwise, the changed node and the specified node are
			 * the same.
			 */
			int index = e.getChildIndices()[0];
			node = (DefaultMutableTreeNode) (node.getChildAt(index));
		}

		public void treeNodesInserted(TreeModelEvent e) {}

		public void treeNodesRemoved(TreeModelEvent e) {}

		public void treeStructureChanged(TreeModelEvent e) {}
	}
}

