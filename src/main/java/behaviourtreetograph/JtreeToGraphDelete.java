package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import odme.behaviour.MainWindow;
import odme.behaviour.ODMEBehaviourEditor;
import odme.behaviour.BehaviourToTree;

import odme.odmeeditor.DynamicTree;

import odme.odmeeditor.ODMEEditor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;


import static behaviourtreetograph.JTreeToGraphBehaviour.benhaviourGraph;
import static behaviourtreetograph.JTreeToGraphBehaviour.behaviorsWithAttributes;
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
		mxCell deleteCell = (mxCell) pos;
		if (deleteCell == null) {
			return;
		}

		String cellId = deleteCell.getId();
		if (cellId != null && cellId.startsWith("uniformity") && !cellId.endsWith("RefNode")) {

			boolean connected = JtreeToGraphCheck.isConnectedToRoot(deleteCell);
			connectedToRoot = false;
			if (!connected) {
				removeBehaviourSubtree(deleteCell);
			} else {
				JOptionPane.showMessageDialog(MainWindow.frame,
						"You can not delete from here. Delete from the reference node.");
			}
			return;
		}

		if (cellId != null && !cellId.endsWith("RefNode")) {
			try {
				JtreeToGraphCheck.checkSubtreeNodeForSyncDelete(deleteCell);
			} catch (RuntimeException ignored) {
				// Reference-node sync is legacy behavior; deletion of the clicked node should still succeed.
			}
		}

		removeBehaviourSubtree(deleteCell);
	}


	public static void deleteEdgeFromGraphPopup(Object pos) {
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
		removeBehaviourSubtree((mxCell) pos);
	}


	private static void removeBehaviourSubtree(mxCell cell) {
		if (cell == null) {
			return;
		}

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		if (isProtectedCanvasNode(cell)) {
			toolkit.beep();
			return;
		}

		removeBehaviorAttributesForSubtree(cell);
		deletableChildNodes.clear();
		deletableChildNodes.add(cell);
		deleteAllChildNode(cell);
		mxCell[] subtreeCells = deletableChildNodes.toArray(new mxCell[0]);
		benhaviourGraph.getModel().beginUpdate();
		try {
			benhaviourGraph.removeCells(subtreeCells, true);
		} finally {
			deletableChildNodes.clear();
			benhaviourGraph.getModel().endUpdate();
		}
	}

	private static boolean isProtectedCanvasNode(mxCell cell) {
		String cellId = cell.getId();
		return "rootnode".equals(cellId) || "hideV".equals(cellId) || "hideH".equals(cellId);
	}

	private static void removeBehaviorAttributesForSubtree(mxCell cell) {
		String behaviorName = cell.getStyle() != null && cell.getStyle().equals("Entity")
				? String.valueOf(cell.getValue())
				: null;
		if (behaviorName != null) {
			behaviorsWithAttributes.removeIf(overview -> overview.getBehaviorName().equals(behaviorName));
			if (BehaviourToTree.behaviorAttributeModel != null && ODMEBehaviourEditor.treePanel != null) {
				BehaviourToTree.behaviorAttributeModel.setRowCount(0);
			}
		}

		Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);
		for (Object edge : outgoing) {
			Object targetCell = benhaviourGraph.getModel().getTerminal(edge, false);
			if (targetCell instanceof mxCell mxTargetCell) {
				removeBehaviorAttributesForSubtree(mxTargetCell);
			}
		}
	}

}
