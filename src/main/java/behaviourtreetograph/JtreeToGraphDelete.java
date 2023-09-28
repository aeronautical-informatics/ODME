package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import odeme.behaviour.MainWindow;

import odme.odmeeditor.DynamicTree;

import odme.odmeeditor.ODMEEditor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;


import static behaviourtreetograph.JTreeToGraphBehaviour.benhaviourGraph;
import static odme.jtreetograph.JtreeToGraphVariables.*;

public class JtreeToGraphDelete {

	public static String selectedVariable = "";
	public static ArrayList<mxCell> deletableChildNodes = new ArrayList<mxCell>();

	public static void deleteBehaviourFromScenarioTableForUpdate(mxCell cellForAddingVariable, String variableName,
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
			for (TreePath key : DynamicTree.behavioursList.keySet()) {
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
					for (String value : DynamicTree.behavioursList.get(key)) {
						if (value.equals(variableName)) {
							yv = 1;
							keyDel = key; // to avoid java.util.ConcurrentModificationException
						}
					}
				}
			}
			if (yv == 1) {
				DynamicTree.behavioursList.remove(keyDel, variableName); // for removing only one values
				yv = 0;
			}

			// have to call a function to refresh the table view
			DynamicTree.behavioursList.put(treePathForVariable, variableNameNew);
			// ---------------------------------------------------end

			pathToRoot.clear();
			ODMEEditor.treePanel.showBehavioursInTable(treePathForVariable);
		}
	}


	public static void deleteNodeFromGraphPopup(Object pos) {
		// for deleting from tree at the same time
		mxCell cellForAddingVariable = (mxCell) pos;

		if (cellForAddingVariable.getId().startsWith("uniformity") && !cellForAddingVariable.getId()
				.endsWith("RefNode")) {

			boolean connected = JtreeToGraphCheck.isConnectedToRoot(cellForAddingVariable);
			connectedToRoot = false;
			if (!connected) {

				benhaviourGraph.getModel().beginUpdate();
				try {
					benhaviourGraph.removeCells(new Object[] {cellForAddingVariable});
				} finally {
					benhaviourGraph.getModel().endUpdate();
				}
			} else {
				JOptionPane.showMessageDialog(MainWindow.frame,
						"You can not delete from here. Delete from the reference node.");
			}

		} else {
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
//			ODMEEditor.treePanel.removeCurrentNodeWithGraphDelete(treePathForVariable);

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

				benhaviourGraph.getModel().beginUpdate();
				try {
					if ("rootnode".equals(cellForAddingVariable.getId())) {
						toolkit.beep();

					} else {
						deletableChildNodes.add(cellForAddingVariable);
						deleteAllChildNode(cellForAddingVariable);
						mxCell[] allnodes = deletableChildNodes.toArray(new mxCell[0]);
						for (int i = 0; i < allnodes.length; i++) {
							mxCell a = allnodes[i];
							benhaviourGraph.removeCells(new Object[] {a});
							deletableChildNodes.clear();
						}
					}
				} finally {
					benhaviourGraph.getModel().endUpdate();
				}
			}
		}
	}


	public static void deleteEdgeFromGraphPopup(Object pos) {
		Object cell = benhaviourGraph.getModel().getTerminal(pos, false);
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

//		// calling function for deleting the node
//		ODMEEditor.treePanel.removeCurrentNodeWithGraphDelete(treePathForVariable);
		pathToRoot.clear();
		benhaviourGraph.getModel().beginUpdate();
		try {
			benhaviourGraph.removeCells(new Object[] {pos});
		} finally {
			benhaviourGraph.getModel().endUpdate();
		}
	}

	private static void deleteAllChildNode(mxCell cell) {
		Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

		if (outgoing.length > 0) {
			for (int i = 0; i < outgoing.length; i++) {
				Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);
				mxCell targetCell2 = (mxCell) targetCell;
				deletableChildNodes.add(targetCell2);
				deleteAllChildNode(targetCell2);
			}
		}
	}

	public static void deleteNodeFromGraphPopupReferenceDeleteSync(Object pos) {
		// for deleting from tree at the same time
		mxCell cellForAddingVariable = (mxCell) pos;

		pathToRoot.add((String) cellForAddingVariable.getValue());
		odme.jtreetograph.JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

		String[] stringArray = pathToRoot.toArray(new String[0]);
		ArrayList<String> pathToRootRev = new ArrayList<String>();

		for (int i = stringArray.length - 1; i >= 0; i--) {
			pathToRootRev.add(stringArray[i]);
		}

		String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
		TreePath treePathForVariable = odme.jtreetograph.JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

		// calling function for deleting the node
//		ODMEEditor.treePanel.removeCurrentNodeWithGraphDelete(treePathForVariable);
		pathToRoot.clear();

		// if i put these delete section above tree node delete then it will not work
		// because before detecting it is deleting the node and could not find tree path

		final Toolkit toolkit = Toolkit.getDefaultToolkit();

		if (cellForAddingVariable != null) {

			benhaviourGraph.getModel().beginUpdate();
			try {
				if ("rootnode".equals(cellForAddingVariable.getId())) {
					toolkit.beep();
				} else {
					deletableChildNodes.add(cellForAddingVariable);
					deleteAllChildNode(cellForAddingVariable);
					mxCell[] allnodes = deletableChildNodes.toArray(new mxCell[0]);
					for (int i = 0; i < allnodes.length; i++) {
						mxCell a = allnodes[i];
						benhaviourGraph.removeCells(new Object[] {a});
						deletableChildNodes.clear();
					}
				}
			} finally {
				benhaviourGraph.getModel().endUpdate();
			}
		}
	}


}
