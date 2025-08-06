package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import javax.swing.tree.DefaultMutableTreeNode;
import com.mxgraph.model.mxCell;

import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.ImportProject;
import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphImport {

	public static void importExistingProjectIntoGraph() {
        Integer num = 0;
        mxCell root = JtreeToGraphGeneral.getRootNode();
        Object obj = root;
        Object[] addedNodes = new Object[100];
        int addedNodeCount = 0;
        String[] nodeNameSplits = null;
        int nodeCount = 0;

        String[] parentNames = new String[100];
        int parentCount = 0;

        String[] nodeNames = new String[100];

        try {
            FileReader reader = new FileReader(
                    (ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + ODMEEditor.projName + ".xml"));
            BufferedReader bufferedReader = new BufferedReader(reader);

            String line;

            while ((line = bufferedReader.readLine()) != null) {

                if (line.startsWith("<?")) {
                    continue;
                } 
                else if (line.startsWith("<start") || line.startsWith("</start")) {
                    continue;
                } 
                else if (line.startsWith("</")) {
                    parentCount--;
                } 
                else if (line.startsWith("<")) {
                    if (line.endsWith("/>")) {
                        String ln = line.replaceAll("[</>]", "");
                        nodeNames[nodeCount] = ln + "->" + parentNames[parentCount - 1] + "->hasparent";
                        nodeCount++;
                    } 
                    else {
                        String ln = line.replaceAll("[</>]", "");

                        if (parentCount > 0) {
                            nodeNames[nodeCount] = ln + "->" + parentNames[parentCount - 1] + "->hasparent";
                            nodeCount++;
                        } 
                        else {
                            nodeNames[nodeCount] = ln;
                            nodeCount++;
                        }
                        parentNames[parentCount] = ln;
                        parentCount++;
                    }
                }
            }
            
            reader.close();
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        String rootName = nodeNames[0];
        
        JtreeToGraphDelete.deleteAllNodesFromGraphWindow(rootName);

        DefaultMutableTreeNode rootNode2 = new DefaultMutableTreeNode(rootName);
        DynamicTree.treeModel.setRoot(rootNode2);
        DynamicTree.treeModel.reload();

        for (int i = 1; i < nodeCount; i++) {
            String nodeName = nodeNames[i];

            if (nodeName.endsWith("hasparent")) {
                nodeNameSplits = nodeName.split("->");
                nodeName = nodeNameSplits[0];

                String parentNode = nodeNameSplits[1];
                for (Object node : addedNodes) {
                    if (!(node == null)) {
                        mxCell cell = (mxCell) node;
                        if (cell.getValue().equals(parentNode)) {
                            obj = cell;
                        }
                    }
                }
            }

            mxCell selectedCell = (mxCell) obj;

            Object[] outgoing = graph.getOutgoingEdges(selectedCell);

            mxCell selectedCellNew = null;
            int len = outgoing.length;

            double x;
            double y;

            if (len > 0) {
                Object targetCell = graph.getModel().getTerminal(outgoing[len - 1], false);
                selectedCellNew = (mxCell) targetCell;
                x = selectedCellNew.getGeometry().getX();
                y = selectedCellNew.getGeometry().getY();
                x = x + 100;

                // if the added node manually changed to other position then overlap may happen.
                // for removing that bug have to compare all the child nodes x positions and
                // have to
                // add after the most right one. didn't implement it yet. will do later after
                // first
                // prototype
            } 
            else {
                x = selectedCell.getGeometry().getX();
                y = selectedCell.getGeometry().getY();
                y = y + 100;
            }

            graph.getModel().beginUpdate();
            try {
                if (nodeName.endsWith("Dec")) {
                    obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Aspect");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;

                    JtreeToGraphAdd.addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                } 
                else if (nodeName.endsWith("MAsp")) {

                    obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Multiaspect");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;

                    JtreeToGraphAdd.addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                } 
                else if (nodeName.endsWith("Spec")) {

                    obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Specialization");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;

                    JtreeToGraphAdd.addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                }
                else if (nodeName.endsWith("BevOr")) { // Author: Vadece Kamdem--> this is to add the behaviours to be linked to the graph

                    ImportProject.importBehaviour(selectedCell,nodeName);

                } else { // Author: Vadece Kamdem--> this is to add the variables to be linked to the graph
                    String[] varNodeNameSplits = nodeNames[i].split(",");

                     if ( varNodeNameSplits.length >= 3 ) {

                         if (varNodeNameSplits.length == 3) {
                             String[] refVariableList = varNodeNameSplits[2].split("->");
                             String refOfRefVariableList = refVariableList[0].substring(0, refVariableList[0].length() - 5);
                             String variableParams = varNodeNameSplits[0]+","+varNodeNameSplits[1]+","+refOfRefVariableList;

//                             System.out.println(num + "-->" + variableParams);
//                             num++;
//                             ImportProject.importVariable(selectedCell, variableParams);
                             JtreeToGraphAdd.addVariableFromImport(variableParams, selectedCell);

                         }

                         if (varNodeNameSplits.length == 5) {
                             String[] refVariableList = varNodeNameSplits[4].split("->");
                             String refOfRefVariableList = refVariableList[0].substring(0, refVariableList[0].length() - 5);
                             String variableParams = varNodeNameSplits[0]+","+varNodeNameSplits[1]+","+varNodeNameSplits[2]+","+varNodeNameSplits[3]+","+refOfRefVariableList;

//                             System.out.println(num + "-->" + variableParams);
//                             num++;
//                             ImportProject.importVariable(selectedCell, variableParams);
                             JtreeToGraphAdd.addVariableFromImport(variableParams, selectedCell);

                         }
                     } else /* if (DynamicTreeDemo.nodeAddDetector.equals("entity")) */ {
                        obj = graph.insertVertex(parent, null, nodeName, x, y, 80, 30,
                                "Entity"); // "Aspect;fillColor=#0759cf;strokeColor=white;");
                        graph.insertEdge(parent, null, "", selectedCell, obj);
                        lastAddedCell = null; // so that it will not cause duplicate addition in tree
                        nodeNumber++;

                        JtreeToGraphAdd.addNodeIntoJtreeWithNewModuleAddition(selectedCell);
                    }
                }

                // added nodes in the array
                addedNodes[addedNodeCount] = obj;
                addedNodeCount++;
            } 
            finally {
                graph.getModel().endUpdate();
            }
        }
    }
}
