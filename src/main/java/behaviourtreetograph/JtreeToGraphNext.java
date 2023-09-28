package behaviourtreetograph;

import com.mxgraph.model.mxCell;
import odme.core.FileConvertion;


import java.util.ArrayList;

import static behaviourtreetograph.JTreeToGraphBehaviour.benhaviourGraph;
import static odme.jtreetograph.JtreeToGraphVariables.*;

public class JtreeToGraphNext {

	public static void nextChildNodeForVariable(mxCell cell) {
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);

                // for next call
                mxCell targetCell2 = (mxCell) targetCell;

                if (targetCell2.getId().startsWith("uniformity")) {
                    if (targetCell2.getId().endsWith("RefNode")) {
                        // finding and saving root to this node path
                    	odme.jtreetograph.JtreeToGraphConvert.nodeToRootPath(targetCell2);

                        String[] stringArray = path.toArray(new String[0]);
                        ArrayList<String> pathRev = new ArrayList<String>();

                        for (int j = stringArray.length - 1; j >= 0; j--) {
                            pathRev.add(stringArray[j]);
                        }

                        String[] stringArrayRev = pathRev.toArray(new String[0]);

                        String cellName = targetCell2.getValue().toString();
                        FileConvertion fileConvertion = new FileConvertion();
                        fileConvertion.addingUniformityRefNodeToXML(stringArrayRev, cellName);
                        path.clear();
                    } 
                    else {
                        continue;
                    }
                } 
                else {
                	JtreeToGraphConvert.rootToEndVariableAddition(targetCell2); // variable addition
                	JtreeToGraphConvert.rootToEndConstraintAddition(targetCell2); // constraint addition
                    nextChildNodeForVariable(targetCell2);
                }
            }
        }
    }
	
	// if i want to create less xml file i can store data in buffer an write only
    // one xml

    public static void nextChild(mxCell cell) {
        int specDecCount = 0;
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        for (int j = 0; j < outgoing.length; j++) {
            Object targetCellTT = benhaviourGraph.getModel().getTerminal(outgoing[j], false);
            mxCell targetCell2TT = (mxCell) targetCellTT;
            if (targetCell2TT.getValue().toString().endsWith("Spec") || targetCell2TT.getValue().toString()
                    .endsWith("Dec")) {
                specDecCount++;
            }
        }

        if (specDecCount > 1) {
            path.add((String) cell.getValue());
            JtreeToGraphConvert.nodeToRootPath(cell);

            // update jtree
            String[] stringArray = path.toArray(new String[0]);
            ArrayList<String> pathRev = new ArrayList<String>();

            for (int k = stringArray.length - 1; k >= 0; k--) {
                pathRev.add(stringArray[k]);
            }

            String[] stringArrayRev = pathRev.toArray(new String[0]);
            path.clear();
            specDecCount = 0;

            FileConvertion obj = new FileConvertion();
            obj.fixingSequenceProblem(stringArrayRev);
        }

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);
                mxCell targetCell2 = (mxCell) targetCell;
                nextChild(targetCell2);
            }
        }
    }
    
    public static void nextChildNodeInPath(mxCell cell) {
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length && nodeReached != totalNodes; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);

                // for next call
                mxCell targetCell2 = (mxCell) targetCell;
                if (targetCell2.getValue().equals(nodesToSelectedNode[nodeReached])) {
                    // call to find next match child node on the path
                    nodeReached++;
                    lastNodeInPath = targetCell2;

                    if (nodeReached < totalNodes) {
                        nextChildNodeInPath(targetCell2);
                    }
                }
            }
        }
    }
    
    public static void nextChildNodeForcheckSubtreeNode(mxCell cell) {
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);

                // for next call
                mxCell targetCell2 = (mxCell) targetCell;

                if (targetCell2.getValue().toString().equals(subtreeCheckLabel)
                    && targetCell2.getId() != subtreeCheckCell.getId() && !(targetCell2.getId().startsWith(
                        "uniformity"))) {

                    if (benhaviourGraph.getOutgoingEdges(targetCell2).length > 1) {

                        subtreeCheckCell.setId("uniformity" + uniformityNodeNumber + "RefNode");
                        uniformityNodeNumber++;

//                        JtreeToGraphSave.saveModuleFromCurrentModelSecondStep(targetCell2);
//                        JtreeToGraphAdd.addModuleFromSubgraphUniformity(subtreeCheckCell);
                    } 
                    else {

                        subtreeCheckCell.setId("uniformity" + uniformityNodeNumber + "RefNode");
                        uniformityNodeNumber++;

//                        JtreeToGraphSave.saveModuleFromCurrentModel(targetCell2);
//                        JtreeToGraphAdd.addModuleFromSubgraphUniformity(subtreeCheckCell);
                    }
                    break;
                } 
                else {
                    nextChildNodeForcheckSubtreeNode(targetCell2);
                }
            }
        }
    }
    
    public static void nextChildNodeForcheckSubtreeNodeSync(mxCell cell) {
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);

                // for next call
                mxCell targetCell2 = (mxCell) targetCell;

                if (targetCell2.getValue().toString().equals(subtreeSyncCell.getValue())
                    && targetCell2.getId() != subtreeSyncCell.getId() && targetCell2.getId()
                            .startsWith("uniformity")) {

                    currentSelectedCell = targetCell2;
//                    JtreeToGraphAdd.addNodeFromConsole(addedCellNameSync);
                    currentSelectedCell = null;
                    break;
                } 
                else {
                    nextChildNodeForcheckSubtreeNodeSync(targetCell2);
                }
            }
        }
    }
    
    /**
     * Find next node in the path for deleting.
     *
     * @param deleteCell
     */
    public static void nextChildNodeForcheckSubtreeNodeSyncDelete(mxCell deleteCell) {
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(deleteCell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);

                // for next call
                mxCell targetCell2 = (mxCell) targetCell;
                //System.out.println(targetCell2.getValue());

                if (targetCell2.getValue().toString().equals(subtreeSyncCell.getValue())
                    && targetCell2.getId() != subtreeSyncCell.getId() && targetCell2.getId()
                            .startsWith("uniformity")) {

                    // for deleting subtree reference
                	JtreeToGraphDelete.deleteNodeFromGraphPopupReferenceDeleteSync(targetCell2);
                    break;
                }
                else {
                    nextChildNodeForcheckSubtreeNodeSyncDelete(targetCell2);
                }
            }
        }
    }
    
    /**
     * Find next node in the path for renaming.
     *
     * @param renameCell
     * @param newName
     */
    public static void nextChildNodeForcheckSubtreeNodeSyncRename(mxCell renameCell, String newName) {
        Object[] outgoing = benhaviourGraph.getOutgoingEdges(renameCell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = benhaviourGraph.getModel().getTerminal(outgoing[i], false);

                // for next call
                mxCell targetCell2 = (mxCell) targetCell;

                if (targetCell2.getValue().toString().equals(subtreeSyncCell.getValue())
                    && targetCell2.getId() != subtreeSyncCell.getId() && targetCell2.getId()
                            .startsWith("uniformity")) {

                    // for deleting subtree reference
                    benhaviourGraph.getModel().beginUpdate();
                    try {
                        benhaviourGraph.getModel().setValue(targetCell2, newName);
                    } 
                    finally {
                        benhaviourGraph.getModel().endUpdate();
                    }
                    break;
                } 
                else {

                    nextChildNodeForcheckSubtreeNodeSyncDelete(targetCell2);
                }
            }
        }
    }
}
