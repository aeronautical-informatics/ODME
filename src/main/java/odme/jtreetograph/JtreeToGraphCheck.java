package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.addedCellNameSync;
import static odme.jtreetograph.JtreeToGraphVariables.connectedToRoot;
import static odme.jtreetograph.JtreeToGraphVariables.graph;
import static odme.jtreetograph.JtreeToGraphVariables.subtreeCheckCell;
import static odme.jtreetograph.JtreeToGraphVariables.subtreeCheckLabel;
import static odme.jtreetograph.JtreeToGraphVariables.subtreeSyncCell;

import com.mxgraph.model.mxCell;

public class JtreeToGraphCheck {

	public static void checkRootConnectivity(mxCell cell) {
        Object[] incoming = graph.getIncomingEdges(cell);
        if (incoming.length != 0) {
            Object source = graph.getModel().getTerminal(incoming[incoming.length - 1], true);
            mxCell sourceCell = (mxCell) source;
            if (sourceCell.getId()
                    .equals("rootnode")) {
                connectedToRoot = true;
            }
            checkRootConnectivity(sourceCell);
        }
    }
	
	public static boolean isConnectedToRoot(mxCell cell) {
        checkRootConnectivity(cell);
        return connectedToRoot;
    }
	
	public static void checkSubtreeNode(mxCell cell) {
        subtreeCheckLabel = cell.getValue().toString();
        subtreeCheckCell = cell;

        JtreeToGraphNext.nextChildNodeForcheckSubtreeNode(JtreeToGraphGeneral.getRootNode());

        subtreeCheckLabel = null;
        subtreeCheckCell = null;
    }
    
    public static void checkSubtreeNodeForSync(mxCell addedCellParent, mxCell addedCell) {
        subtreeSyncCell = addedCellParent;
        addedCellNameSync = addedCell.getValue().toString();
        JtreeToGraphNext.nextChildNodeForcheckSubtreeNodeSync(JtreeToGraphGeneral.getRootNode());
    }
    
    /**
     * Check whether the renaming node contains any reference node or not. If it has
     * any reference node then it will also rename that at the same time.
     *
     * @param renameCell
     * @param newName
     */
    public static void checkSubtreeNodeForSyncRename(mxCell renameCell, String newName) {
        subtreeSyncCell = renameCell;
        JtreeToGraphNext.nextChildNodeForcheckSubtreeNodeSyncRename(JtreeToGraphGeneral.getRootNode(), newName);
    }
    
    /**
     * Check whether the deleting node contains any reference subtree or not. If it
     * has some reference nodes then it will also delete those at the same time.
     *
     * @param deleteCell
     */
    public static void checkSubtreeNodeForSyncDelete(mxCell deleteCell) {
        subtreeSyncCell = deleteCell;
        JtreeToGraphNext.nextChildNodeForcheckSubtreeNodeSyncDelete(JtreeToGraphGeneral.getRootNode());
    }
}
