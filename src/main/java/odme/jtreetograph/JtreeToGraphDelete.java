package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.mxgraph.model.mxCell;

import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphDelete {
	
	public static String selectedVariable = "";
	public static ArrayList<mxCell> deletableChildNodes = new ArrayList<mxCell>();

	public static void deleteConstraintFromScenarioTableForUpdate(mxCell cellForAddingVariable, String variableName,
            String variableNameNew) {
		if ((variableName != null) && (!variableName.trim().isEmpty())) {
			pathToRoot.add((String) cellForAddingVariable.getValue());
			JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

			String[] stringArray = pathToRoot.toArray(new String[0]);
			ArrayList<String> pathToRootRev = new ArrayList<String>();

			for (int i = stringArray.length - 1; i >= 0; i--) {
				pathToRootRev.add(stringArray[i]);
			}

			String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
			TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

			DefaultMutableTreeNode currentNode =
					(DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());
			TreeNode[] nodes = currentNode.getPath();

			// have to delete that variable here
			// ---------------------------------------------------start
			int yv = 0;
			TreePath keyDel = null;
			for (TreePath key : DynamicTree.constraintsList.keySet()) {
				int a = 0;
				DefaultMutableTreeNode currentNode2 = (DefaultMutableTreeNode) (key.getLastPathComponent());
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
					for (String value : DynamicTree.constraintsList.get(key)) {
						if (value.equals(variableName)) {
							yv = 1;
							keyDel = key; // to avoid java.util.ConcurrentModificationException
						}
					}
				}
			}
			if (yv == 1) {
				DynamicTree.constraintsList.remove(keyDel, variableName); // for removing only one values
				yv = 0;
			}

			// have to call a function to refresh the table view
			DynamicTree.constraintsList.put(treePathForVariable, variableNameNew);
			// ---------------------------------------------------end

			pathToRoot.clear();
			ODMEEditor.treePanel.showConstraintsInTable(treePathForVariable);
		}
	}

	public static void deleteVariableFromGraphPopup(Object pos) {
		String variableName = null;
		mxCell cell = (mxCell) pos;

		// all variables
		pathToRoot.add((String) cell.getValue());
		JtreeToGraphConvert.nodeToRootPathVar(cell);

		String[] stringArray = pathToRoot.toArray(new String[0]);
		ArrayList<String> pathToRootRev = new ArrayList<String>();

		for (int i = stringArray.length - 1; i >= 0; i--) {
			pathToRootRev.add(stringArray[i]);
		}

		String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

		TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

		int b = 0;
		String[] nodesToSelectedNode = new String[100];
		nodesToSelectedNode[b] = "Select Variable:";
		b++;

		if (!(treePathForVariable == null)) {
			DefaultMutableTreeNode currentNode =
					(DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

			TreeNode[] nodes = currentNode.getPath();

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
		}

		// java 8 way null removal
		nodesToSelectedNode = Arrays.stream(nodesToSelectedNode).filter(s -> (s != null && s.length() > 0))
				.toArray(String[]::new);
		
		JComboBox<String> variableList = new JComboBox<String>(nodesToSelectedNode);
		variableList.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent e) {
				selectedVariable = variableList.getSelectedItem().toString().trim();
			}
		});

		Object[] message = {"Variable:", variableList};

		int option = JOptionPane
				.showConfirmDialog(Main.frame, message, "Please Select", JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.PLAIN_MESSAGE);

		if (option == JOptionPane.OK_OPTION) {
			variableName = selectedVariable;

			// end of all variables

			if ((variableName != null) && (!variableName.trim().isEmpty())) {
				DefaultMutableTreeNode currentNode =
						(DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());
				TreeNode[] nodes = currentNode.getPath();

				int yv = 0;
				TreePath keyDel = null;
				for (TreePath key : DynamicTree.varMap.keySet()) {
					int a = 0;
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
						for (String value : DynamicTree.varMap.get(key)) {
							if (value.equals(variableName)) {
								yv = 1;
								keyDel = key; // to avoid java.util.ConcurrentModificationException
							}
						}
					}
				}
				if (yv == 1) {
					DynamicTree.varMap.remove(keyDel, variableName); // for removing only one values
					yv = 0;
				}

				// have to call a function to refresh the table view
				ODMEEditor.treePanel.refreshVariableTable(treePathForVariable);
				// ---------------------------------------------------end
				pathToRoot.clear();
			}
		}
		selectedVariable = "";
	}


	public static void deleteAllVariablesFromGraphPopup(Object pos) {
		// deleting all the variables of a node

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

		DefaultMutableTreeNode currentNode =
				(DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());
		TreeNode[] nodes = currentNode.getPath();

		// have to delete that variable here
		// ---------------------------------------------------start

		List<TreePath> delKeys = new ArrayList<TreePath>();

		int yv = 0;
		for (TreePath key : DynamicTree.varMap.keySet()) {

			int a = 0;
			DefaultMutableTreeNode currentNode2 = (DefaultMutableTreeNode) (key.getLastPathComponent());
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
				yv = 1;
				delKeys.add(key);
			}
		}
		if (yv == 1) {
			for (TreePath k : delKeys) {
				DynamicTree.varMap.asMap().remove(k);
				yv = 0;
			}
		}

		// have to call a function to refresh the table view
		ODMEEditor.treePanel.refreshVariableTable(treePathForVariable);
		// ---------------------------------------------------end
		pathToRoot.clear();
	}


	public static void deleteAllConstraintFromGraphPopup(Object pos) {
		// deleting all the variables of a node

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

		DefaultMutableTreeNode currentNode =
				(DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());
		TreeNode[] nodes = currentNode.getPath();

		// DynamicTree.varMap.put(treePathForVariable, variableName);
		// have to delete that variable here
		// ---------------------------------------------------start

		List<TreePath> delKeys = new ArrayList<TreePath>();

		int yv = 0;
		for (TreePath key : DynamicTree.constraintsList.keySet()) {

			int a = 0;
			DefaultMutableTreeNode currentNode2 = (DefaultMutableTreeNode) (key.getLastPathComponent());
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
				yv = 1;
				delKeys.add(key);
			}
		}
		if (yv == 1) {
			for (TreePath k : delKeys) {
				DynamicTree.constraintsList.asMap().remove(k);
				yv = 0;
			}
		}

		// have to call a function to refresh the table view
		ODMEEditor.treePanel.showConstraintsInTable(treePathForVariable);
		// ---------------------------------------------------end
		pathToRoot.clear();
	}

	public static void deleteNodeFromGraphPopup(Object pos) {
		// for deleting from tree at the same time
		mxCell cellForAddingVariable = (mxCell) pos;

		if (cellForAddingVariable.getId().startsWith("uniformity") && !cellForAddingVariable.getId()
				.endsWith("RefNode")) {

			boolean connected = JtreeToGraphCheck.isConnectedToRoot(cellForAddingVariable);
			connectedToRoot = false;
			if (!connected) {

				graph.getModel().beginUpdate();
				try {
					graph.removeCells(new Object[] {cellForAddingVariable});
				} finally {
					graph.getModel().endUpdate();
				}
			} 
			else {
				JOptionPane.showMessageDialog(Main.frame,
						"You can not delete from here. Delete from the reference node.");
			}

		} 
		else {
			pathToRoot.add((String) cellForAddingVariable.getValue());
			JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

			String[] stringArray = pathToRoot.toArray(new String[0]);
			ArrayList<String> pathToRootRev = new ArrayList<String>();

			for (int i = stringArray.length - 1; i >= 0; i--) {
				pathToRootRev.add(stringArray[i]);
			}

			String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

			TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

			// calling function for deleting the node
			ODMEEditor.treePanel.removeCurrentNodeWithGraphDelete(treePathForVariable);

			pathToRoot.clear();

			// reference delete syn
			if (!cellForAddingVariable.getId().endsWith("RefNode")) {
				JtreeToGraphCheck.checkSubtreeNodeForSyncDelete(cellForAddingVariable);
			}

			// if i put these delete section above tree node delete then it will not work
			// because before detecting it is deleting the node and could not find tree path

			// Object delcell = pos;
			final Toolkit toolkit = Toolkit.getDefaultToolkit();

			if (cellForAddingVariable != null) {

				graph.getModel().beginUpdate();
				try {
					if ("rootnode".equals(cellForAddingVariable.getId())) {
						toolkit.beep();

					} 
					else {
						deletableChildNodes.add(cellForAddingVariable);
						deleteAllChildNode(cellForAddingVariable);
						mxCell[] allnodes = deletableChildNodes.toArray(new mxCell[0]);
						for (int i = 0; i < allnodes.length; i++) {
							mxCell a = allnodes[i];
							graph.removeCells(new Object[] {a});
							deletableChildNodes.clear();
						}
					}
				} finally {
					graph.getModel().endUpdate();
				}
			}
		}
	}

	public static void deleteNodeFromGraphPopupReferenceDeleteSync(Object pos) {
		// for deleting from tree at the same time
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

		// calling function for deleting the node
		ODMEEditor.treePanel.removeCurrentNodeWithGraphDelete(treePathForVariable);
		pathToRoot.clear();

		// if i put these delete section above tree node delete then it will not work
		// because before detecting it is deleting the node and could not find tree path

		final Toolkit toolkit = Toolkit.getDefaultToolkit();

		if (cellForAddingVariable != null) {

			graph.getModel().beginUpdate();
			try {
				if ("rootnode".equals(cellForAddingVariable.getId())) {
					toolkit.beep();
				} 
				else {
					deletableChildNodes.add(cellForAddingVariable);
					deleteAllChildNode(cellForAddingVariable);
					mxCell[] allnodes = deletableChildNodes.toArray(new mxCell[0]);
					for (int i = 0; i < allnodes.length; i++) {
						mxCell a = allnodes[i];
						graph.removeCells(new Object[] {a});
						deletableChildNodes.clear();
					}
				}
			} 
			finally {
				graph.getModel().endUpdate();
			}
		}
	}

	public static void deleteEdgeFromGraphPopup(Object pos) {
		Object cell = graph.getModel().getTerminal(pos, false);
		mxCell targetCell = (mxCell) cell;

		// for deleting from tree at the same time
		pathToRoot.add((String) targetCell.getValue());
		JtreeToGraphConvert.nodeToRootPathVar(targetCell);

		String[] stringArray = pathToRoot.toArray(new String[0]);
		ArrayList<String> pathToRootRev = new ArrayList<String>();

		for (int i = stringArray.length - 1; i >= 0; i--) {
			pathToRootRev.add(stringArray[i]);
		}

		String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
		TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

		// calling function for deleting the node
		ODMEEditor.treePanel.removeCurrentNodeWithGraphDelete(treePathForVariable);
		pathToRoot.clear();
		graph.getModel().beginUpdate();
		try {
			graph.removeCells(new Object[] {pos});
		} 
		finally {
			graph.getModel().endUpdate();
		}
	}

	private static void deleteAllChildNode(mxCell cell) {
		Object[] outgoing = graph.getOutgoingEdges(cell);

		if (outgoing.length > 0) {
			for (int i = 0; i < outgoing.length; i++) {
				Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
				mxCell targetCell2 = (mxCell) targetCell;
				deletableChildNodes.add(targetCell2);
				deleteAllChildNode(targetCell2);
			}
		}
	}

	public static void deleteNodeFromGraphPopupForSubTree(Object pos) {
		// for deleting from tree at the same time
		mxCell cellForAddingVariable = (mxCell) pos;
		final Toolkit toolkit = Toolkit.getDefaultToolkit();

		if (cellForAddingVariable != null) {

			graph.getModel().beginUpdate();
			try {
				if ("rootnode".equals(cellForAddingVariable.getId())) {
					toolkit.beep();
				} 
				else {
					deletableChildNodes.add(cellForAddingVariable);
					deleteAllChildNode(cellForAddingVariable);
					mxCell[] allnodes = deletableChildNodes.toArray(new mxCell[0]);
					for (int i = 0; i < allnodes.length; i++) {
						mxCell a = allnodes[i];
						graph.removeCells(new Object[] {a});
						deletableChildNodes.clear();
					}
				}
			} 
			finally {
				graph.getModel().endUpdate();
			}
		}
	}

	public static void deleteNodeWithTree(String[] nodesToSelectedNode) {
		JtreeToGraphConvert.rootToSelectedNode(nodesToSelectedNode);

		// below codes also used in another function. i can make a separate function
		// using this to make the code more understandable and organized.
		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		
		if (lastNodeInPath != null) {

			graph.getModel().beginUpdate();
			try {
				if ("rootnode".equals(lastNodeInPath.getId())) {
					toolkit.beep();
				} 
				else {
					deletableChildNodes.add(lastNodeInPath);
					deleteAllChildNode(lastNodeInPath);
					mxCell[] allnodes = deletableChildNodes.toArray(new mxCell[0]);
					for (int i = 0; i < allnodes.length; i++) {
						mxCell a = allnodes[i];
						System.err.println(a.getValue());
						graph.removeCells(new Object[] {a});
						deletableChildNodes.clear();
					}
				}
			} 
			finally {
				graph.getModel().endUpdate();
			}
		}
		// this variable is using by many functions. so have to make it null here so
		// that it will not affect others
		lastNodeInPath = null;
	}

	/**
	 * This function deletes all the nodes except root node during new project
	 * creation. Also changes the root name with new root name which is coming as an
	 * argument.
	 *
	 * @param newRootName
	 */
	public static void deleteAllNodesFromGraphWindow(String newRootName) {
		graph.getModel().beginUpdate();
		try {
			Object[] cells = graph.getChildVertices(graph.getDefaultParent());
			for (Object x : cells) {
				mxCell cell = (mxCell) x;
				
				if (cell.getId().equals("rootnode")) {
					graph.getModel().setValue(cell, newRootName);
				} 
				else {
					graph.removeCells(new Object[] {x});
				}
			}
		} 
		finally {
			graph.getModel().endUpdate();
		}
	}
	
	public static void deleteVariableFromScenarioTableForUpdate(mxCell cellForAddingVariable, String variableName,
            String variableNameNew) {
		if ((variableName != null) && (!variableName.trim().isEmpty())) {
			pathToRoot.add((String) cellForAddingVariable.getValue());
			JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

			String[] stringArray = pathToRoot.toArray(new String[0]);
			ArrayList<String> pathToRootRev = new ArrayList<String>();

			for (int i = stringArray.length - 1; i >= 0; i--) {
				pathToRootRev.add(stringArray[i]);
			}

			String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
			TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

			DefaultMutableTreeNode currentNode =
					(DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());
			TreeNode[] nodes = currentNode.getPath();

			// DynamicTree.varMap.put(treePathForVariable, variableName);
			// have to delete that variable here
			// ---------------------------------------------------start
			int yv = 0;
			TreePath keyDel = null;
			for (TreePath key : DynamicTree.varMap.keySet()) {
				int a = 0;
				DefaultMutableTreeNode currentNode2 = (DefaultMutableTreeNode) (key.getLastPathComponent());

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
					for (String value : DynamicTree.varMap.get(key)) {
						if (value.equals(variableName)) {
							yv = 1;
							keyDel = key; // to avoid java.util.ConcurrentModificationException
						}
					}
				}
			}
			if (yv == 1) {
				// DynamicTree.varMap.asMap().remove(keyDel);
				DynamicTree.varMap.remove(keyDel, variableName); // for removing only one values
				yv = 0;
			}

			// have to call a function to refresh the table view
			//ODMEEditor.treePanel.refreshVariableTable(treePathForVariable);
			// ---------------------------------------------------end

			pathToRoot.clear();
			JtreeToGraphAdd.addVariableFromScenarioTableForUpdate(cellForAddingVariable, variableNameNew);
		}
	}
}
