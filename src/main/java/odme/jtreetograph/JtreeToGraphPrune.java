package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;

import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.Main;
import odme.odmeeditor.ODMEEditor;
import odme.odmeeditor.Variable;

public class JtreeToGraphPrune {

    public static String selectedNodeToPrune = null;
    public static Multimap<TreePath, String> varMapTransfer = ArrayListMultimap.create();

    public static Multimap<TreePath, String> behMapTransfer = ArrayListMultimap.create();
    public static String selecteVarName;

    private static int cellCount = 0;
    private static TreePath[] cellListForIconChange = new TreePath[100];

    public static void pruneNodeFromGraphPopup(Object pos) {

        mxCell selectedCell = (mxCell) pos;
        Object[] outgoing = graph.getOutgoingEdges(selectedCell);

        Object[] incoming = graph.getIncomingEdges(selectedCell);
        Object targetCellIncoming = graph.getModel().getTerminal(incoming[0], true);
        mxCell targetCellIncoming2 = (mxCell) targetCellIncoming;
        int a1 = 0;
        String[] nodeList = new String[outgoing.length+1];
        nodeList[a1] = "Select Node Here:";
        a1++;

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
                mxCell targetCell2 = (mxCell) targetCell;
                nodeList[a1] = targetCell2.getValue().toString();
                a1++;

            }
        }

        // java 8 way null removal
        nodeList = Arrays.stream(nodeList).filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);

        JComboBox<String> nodeListCombo = new JComboBox<String>(nodeList);

        nodeListCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    selectedNodeToPrune = nodeListCombo.getSelectedItem().toString();
                }
            }
        });

        Object[] pruneOptions = {"Available Options:", nodeListCombo};

        int option = JOptionPane
                .showConfirmDialog(Main.frame, pruneOptions, "Please Choose from Options",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            if (outgoing.length > 0) {
                for (int j = 0; j < outgoing.length; j++) {
                    Object targetCell = graph.getModel().getTerminal(outgoing[j], false);
                    mxCell targetCell2 = (mxCell) targetCell;
                    if (targetCell2.getValue().equals(selectedNodeToPrune)) {
                        Object[] outgoingSelected = graph.getOutgoingEdges(targetCell2);
                        if (outgoingSelected.length > 0) {
                            JtreeToGraphSave.saveModuleFromCurrentModel(targetCell2);
                            JtreeToGraphDelete.deleteNodeFromGraphPopup(pos);
                            String newEntityName =
                                    selectedNodeToPrune + "_" + targetCellIncoming2.getValue().toString();
                            System.out.println("inside if block");
                            renameCellPES(targetCellIncoming2, newEntityName);
                            JtreeToGraphAdd.addModuleFromSubgraphUniformity(targetCellIncoming2);
                        } 
                        else {
                            /*
                             * Start variable collection. Variables of targetCell2 will be added in
                             * targateCellIncoming2
                             */

                            String[] nodesToSelectedNode = new String[100];
                            String[] nodesBehaviour = new String[100];

                            nodesToSelectedNode = returnAllVariablesFromPrunedNode(targetCell2);

                            nodesBehaviour = returnAllBehaviorVariablesFromPrunedNode(targetCell2);


                            nodesBehaviour = Arrays.stream(nodesBehaviour)
                                    .filter(s -> (s !=null && s.length() >0)).toArray(String[]::new);

                            nodesToSelectedNode = Arrays.stream(nodesToSelectedNode)
                                    .filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);

                            String[] nodesToSelectedNodeHost = new String[100];
                            String[] nodesToSelectedBehaviorHost = new String[100];
                            nodesToSelectedNodeHost = returnAllVariablesFromPrunedNode(targetCellIncoming2);
                            nodesToSelectedBehaviorHost = returnAllBehaviorVariablesFromPrunedNode(targetCellIncoming2);

                            nodesToSelectedNodeHost = Arrays.stream(nodesToSelectedNodeHost)
                                    .filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);
                            nodesToSelectedBehaviorHost = Arrays.stream(nodesToSelectedBehaviorHost)
                                    .filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);

                            // end of variable collection-------------------

                            JtreeToGraphDelete.deleteNodeFromGraphPopup(pos);
                            String newEntityName =
                                    selectedNodeToPrune + "_" + targetCellIncoming2.getValue().toString();
                            renameCellPES(targetCellIncoming2, newEntityName);


                            //if the node donot have variable but have behavior then for loop will not run.
                            //therefore i am writing different code to transfer behaviors from child node to parent node

                            for(String behavior: nodesBehaviour){
                                addBehaviorFromOneNodeToAnother(targetCellIncoming2 , behavior);
                            }
                            for(String behavior: nodesToSelectedBehaviorHost){
                                addBehaviorFromOneNodeToAnother(targetCellIncoming2 , behavior);
                            }
                            for (String value : nodesToSelectedNode) {
                                 addVariablesFromOneNodeToAnother(targetCellIncoming2, value);
                            }
                        }
                    }
                }
            }
        }
        selectedNodeToPrune = null;
        
        ODMEEditor.changePruneColor();
    }

    public static void pruneMAspNodeFromGraphPopup(Object pos , int limit) {
        // Also read the  behaviour List  and update that according to the selection of node.
//        read behaviour file
        mxCell maspcell = (mxCell) pos;
        String maspcellName = maspcell.getValue().toString().replaceAll("MAsp", "");
        
        JTextField nodeListTf = new JTextField("1");
        nodeListTf.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                int n = 1;
                try {
                    n = Integer.parseInt(nodeListTf.getText());
                } catch(NumberFormatException ex){
                    nodeListTf.setText("");
                }
                if(n> limit){
                    JOptionPane.showMessageDialog(null,
                            "Number of Entities should be between 1 and " + limit);
                    nodeListTf.setText("");
                }
                if (n>1000 || n<1) {
                    JOptionPane.showMessageDialog(null, "Number of Entities should be between 1 and 1000");
                    nodeListTf.setText("");
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {}
        });

        Object[] pruneOptions = {"Number of Entities:", nodeListTf};

        int option = JOptionPane
                .showConfirmDialog(Main.frame, pruneOptions, "Please Choose from Options",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            if (nodeListTf.getText().equals("")) {
                return;
            }
            int number = Integer.parseInt(nodeListTf.getText());

            JtreeToGraphSave.saveModuleFromCurrentModel(maspcell);

            Object[] incoming = graph.getIncomingEdges(maspcell);
            Object aspectParent = graph.getModel().getTerminal(incoming[0], true);
            mxCell aspectParentNode = (mxCell) aspectParent;
            currentSelectedCell = aspectParentNode;

            JtreeToGraphDelete.deleteNodeFromGraphPopup(maspcell);

            addNodeFromConsolePES(maspcellName + "Dec");
            
            JtreeToGraphConvert.nodeToRootPath(currentSelectedCell);
            String[] stringArray = path.toArray(new String[0]);
            ArrayList<String> pathRev = new ArrayList<String>();

            for (int i = stringArray.length - 1; i >= 0; i--) {
                pathRev.add(stringArray[i]);
            }

            String[] stringArrayRev = pathRev.toArray(new String[0]);

            JtreeToGraphAdd.addNodeWIthGraphAddition(maspcellName + "Dec", stringArrayRev);
            path.clear();

            nodeNamesForGraphSync =
                    Arrays.stream(nodeNamesForGraphSync).filter(s -> (s != null && s.length() > 0))
                            .toArray(String[]::new);

            int nodenum = 1;
            for (int i = 0; i < number; i++) {
                addModuleFromSubgraphUniformityPES(currentSelectedCell);

                // for numbering multiple aspect node sequentially after pruning
                Object[] outgoingasp = graph.getOutgoingEdges(currentSelectedCell);
                Object targetCellasp = graph.getModel().getTerminal(outgoingasp[i], false);
                mxCell targetCellasp2 = (mxCell) targetCellasp;
                renameCellPES(targetCellasp2, targetCellasp2.getValue().toString() + "_" + nodenum);

                // if a entity node has aspect node and that entity node is multiaspect node of
                // another then during pruning have to change the aspect node name otherwise in
                // same level there will be two node with same name. solution below.
                Object[] outgoingasp2 = graph.getOutgoingEdges(targetCellasp2);
                if (outgoingasp2.length > 0) {
                    targetCellasp = graph.getModel().getTerminal(outgoingasp2[0], false);
                    mxCell targetCellasp3 = (mxCell) targetCellasp;
                    renameCellPES(targetCellasp3, targetCellasp2.getValue().toString() + "Dec");
                }
                nodenum++;
            }

            /*
             * Copying variables from old node and transfer to new nodes.
             */
            try {

                for (TreePath key : DynamicTree.varMap.keySet()) {
                    for (String value : DynamicTree.varMap.get(key)) {
                        String lastNodeName = key.getLastPathComponent().toString();
                        variableTransferAfterMultiAspectPruning(lastNodeName, value);
                    }
                }

                //copy behaviour of the node to transfer other node
                for (TreePath key : DynamicTree.behavioursList.keySet()) {

                    for (String value : DynamicTree.behavioursList.get(key)) {

//                        System.out.println(" TreePath value = "+value);
                        String lastNodeName = key.getLastPathComponent().toString();
                        behaviourTransferAfterMultiAspectPruning(lastNodeName, value);
//                        DynamicTree.behavioursList.remove(key,value);
                    }
                }
            }
        
            catch (ConcurrentModificationException e) {
                System.out.println(e.getMessage());
            }

            // have to make used array null and count 0 here
            nodeNamesForGraphSync = new String[100]; // or have to delete the added nodes in other way
            treeSyncNodeCount = 0;
            currentSelectedCell = null;

            DynamicTree.behavioursList = behMapTransfer;
            DynamicTree.varMap = varMapTransfer;
        }
        ODMEEditor.changePruneColor();
    }

    /**
     * This function is used to transfer variables which are added during modeling
     * to the final pruned structure after multi-aspect pruned is done.
     *
     * @param lastNodeName
     * @param value
     */


    public static void behaviourTransferAfterMultiAspectPruning(String lastNodeName, String value) {

        Object[] cells = graph.getChildVertices(graph.getDefaultParent());

        for (Object c : cells) {
            mxCell cell2 = (mxCell) c;
            if (cell2.isVertex()) {
                String val = (String) cell2.getValue();
                if (val.equals(lastNodeName) || val.startsWith(lastNodeName + "_")) {

                    pathToRoot.add(val);
                    JtreeToGraphConvert.nodeToRootPathVar(cell2);
                    String[] stringArray = pathToRoot.toArray(new String[0]);
                    ArrayList<String> pathRev = new ArrayList<String>();

                    for (int i = stringArray.length - 1; i >= 0; i--) {
                        pathRev.add(stringArray[i]);
                    }

                    String[] stringArrayRev = pathRev.toArray(new String[0]);
                    TreePath treePathForVariableTransfer = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

                    int a = 1;
                    for (String v : behMapTransfer.get(treePathForVariableTransfer)) {
                        if (v.equals(value)) {
                            a = 0;
                            break;
                        }
                    }

                    if (a == 1)
                        behMapTransfer.put(treePathForVariableTransfer, value);

                    pathToRoot.clear();
                }
            }
        }
    }


    public static void variableTransferAfterMultiAspectPruning(String lastNodeName, String value) {

        Object[] cells = graph.getChildVertices(graph.getDefaultParent());

        for (Object c : cells) {
            mxCell cell2 = (mxCell) c;
            if (cell2.isVertex()) {
                String val = (String) cell2.getValue();
                if (val.equals(lastNodeName) || val.startsWith(lastNodeName + "_")) {

                    pathToRoot.add(val);
                    JtreeToGraphConvert.nodeToRootPathVar(cell2);
                    String[] stringArray = pathToRoot.toArray(new String[0]);
                    ArrayList<String> pathRev = new ArrayList<String>();

                    for (int i = stringArray.length - 1; i >= 0; i--) {
                        pathRev.add(stringArray[i]);
                    }

                    String[] stringArrayRev = pathRev.toArray(new String[0]);
                    TreePath treePathForVariableTransfer = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
                    
                    int a = 1;
                    for (String v : varMapTransfer.get(treePathForVariableTransfer)) {
                        if (v.equals(value)) {
                            a = 0;
                            break;
                        }

                    }

                    if (a == 1)
                        varMapTransfer.put(treePathForVariableTransfer, value);
                    pathToRoot.clear();
                }
            }
        }
    }

    public static void pruneSiblingsFromGraphPopup(Object pos) {
        mxCell selectedCell = (mxCell) pos;

        if (variableList.length == 0) {
            JOptionPane.showMessageDialog(Main.frame, "No variables to prune!", "Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        else {
            selecteVarName = variableList[0].split(",")[0];

            JComboBox<String> nodeListCombo = new JComboBox<String>();
            for (int i=0; i<variableList.length; i++) {
                if (variableList[i] != null) {
                    nodeListCombo.addItem(variableList[i].split(",")[0]);
                }
            }

            nodeListCombo.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent event) {
                    if (event.getStateChange() == ItemEvent.SELECTED) {
                        selecteVarName = nodeListCombo.getSelectedItem().toString();
                    }
                }
            });

            Object[] pruneOptions = {"Available Options:", nodeListCombo};

            int option = JOptionPane
                    .showConfirmDialog(Main.frame, pruneOptions, "Please Choose from Options",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {

                String selecteVar = nodeListCombo.getSelectedItem().toString();
                for (String var : variableList) {

                    if (var.split(",")[0].equals(selecteVarName)) {
                        selecteVar = var;
                        break;
                    }
                }

                if (selecteVar.split(",").length >3)
                    Variable.updateTableData(selectedCell.getValue().toString(), selecteVar.split(",")[0], selecteVar.split(",")[1],
                            selecteVar.split(",")[2], selecteVar.split(",")[3], selecteVar.split(",")[4]);
                else
                    Variable.updateTableData(selectedCell.getValue().toString(), selecteVar.split(",")[0], selecteVar.split(",")[1],
                            selecteVar.split(",")[2], null, null);
            }
        }
    }


    /**
     * Rename node name of both graph node and jtree node after pruning of
     * specialization node.
     *
     * @param cell
     * @param newName
     */
    public static void renameCellPES(mxCell cell, String newName) {
        if (cell.getId().equals("rootnode")) {
            Object[] outgoing = graph.getOutgoingEdges(cell);
            
            if (outgoing.length == 0) {
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
            }
            else {
                JOptionPane.showMessageDialog(Main.frame,
                        "You can't rename a root node having child node.");
            }
        } 
        else {
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

                    TreePath treePathForRename = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

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
    public static String[] returnAllBehaviorVariablesFromPrunedNode(mxCell targetCell2){

        String[] nodesToSelectedNode = new String[100];

        pathToRoot.add((String) targetCell2.getValue());
        JtreeToGraphConvert.nodeToRootPathVar(targetCell2);
        String[] stringArray = pathToRoot.toArray(new String[0]);
//        System.out.println("behavior pathToRoot = "+pathToRoot.toString());
        ArrayList<String> pathToRootRev = new ArrayList<String>();
        for (int i = stringArray.length - 1; i >= 0; i--) {
//            System.out.println("stringArray = "+stringArray[i]);
            pathToRootRev.add(stringArray[i]);
        }

        String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

        TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

        if (!(treePathForVariable == null)) {
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

            TreeNode[] nodes = currentNode.getPath();

            int b = 0;

            for (TreePath key : DynamicTree.behavioursList.keySet()) {
                int a = 0;

                for (String value : DynamicTree.behavioursList.get(key)) {

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
        pathToRoot.clear();
        for (String s : nodesToSelectedNode) {
//            System.out.println("Prune behaviours ="+ s);
        }

        return nodesToSelectedNode;
    }

    private static void addBehaviorFromOneNodeToAnother(mxCell cellForAddingVariable, String behaviorName) {

        pathToRoot.add((String) cellForAddingVariable.getValue());
        JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

        String[] stringArray = pathToRoot.toArray(new String[0]);
        ArrayList<String> pathToRootRev = new ArrayList<String>();

        for (int i = stringArray.length - 1; i >= 0; i--) {
            pathToRootRev.add(stringArray[i]);
        }

        String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

        TreePath treePathForBehavior = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

        DynamicTree.behavioursList.put(treePathForBehavior , behaviorName);

        System.out.println("contains this key = "+DynamicTree.behavioursList.containsKey(treePathForBehavior));

        pathToRoot.clear();

        // have to call a function to refresh the table view
        ODMEEditor.treePanel.showBehavioursInTable(treePathForBehavior);

    }

    public static String[] returnAllVariablesFromPrunedNode(mxCell targetCell2) {
        String[] nodesToSelectedNode = new String[100];

        pathToRoot.add((String) targetCell2.getValue());
        JtreeToGraphConvert.nodeToRootPathVar(targetCell2);

        String[] stringArray = pathToRoot.toArray(new String[0]);

        ArrayList<String> pathToRootRev = new ArrayList<String>();

        for (int i = stringArray.length - 1; i >= 0; i--) {
            pathToRootRev.add(stringArray[i]);
        }

        String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

        TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);


        if (!(treePathForVariable == null)) {
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

            TreeNode[] nodes = currentNode.getPath();

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
//                        System.out.println("variable to prune = " + value);
                        a = aa;
                    }
                    if (a == 1) {
                        nodesToSelectedNode[b] = value;
//                        System.out.println("variable to prune = " + value);
                        b++;
                    }
                }
            }
        }
        pathToRoot.clear();
        return nodesToSelectedNode;
    }
    private static void addVariablesFromOneNodeToAnother(mxCell cellForAddingVariable, String variableName) {

        pathToRoot.add((String) cellForAddingVariable.getValue());
        JtreeToGraphConvert.nodeToRootPathVar(cellForAddingVariable);

        String[] stringArray = pathToRoot.toArray(new String[0]);
        ArrayList<String> pathToRootRev = new ArrayList<String>();

        for (int i = stringArray.length - 1; i >= 0; i--) {
            pathToRootRev.add(stringArray[i]);
        }

        String[] stringArrayRev = pathToRootRev.toArray(new String[0]);

        TreePath treePathForVariable = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);
//        System.out.println("treePathForVariable = "+treePathForVariable);
        DynamicTree.varMap.put(treePathForVariable, variableName);

        pathToRoot.clear();

        // have to call a function to refresh the table view
        ODMEEditor.treePanel.refreshVariableTable(treePathForVariable);
    }

    public static void addNodeFromConsolePES(String nodeName) {
        if (currentSelectedCell != null) {
            mxCell selectedCell = currentSelectedCell;

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
                    Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Aspect");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    // lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                    currentSelectedCell = (mxCell) obj;
                }
                else if (nodeName.endsWith("MAsp")) {
                    Object obj = graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Multiaspect");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    // lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                    currentSelectedCell = (mxCell) obj;
                }
                else if (nodeName.endsWith("Spec")) {
                    Object obj =
                            graph.insertVertex(parent, null, nodeName, x + 25, y, 30, 30, "Specialization");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    // lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                    currentSelectedCell = (mxCell) obj;
                } 
                else /* if (DynamicTreeDemo.nodeAddDetector.equals("entity")) */ {
                    Object obj = graph.insertVertex(parent, null, nodeName, x, y, 80, 30,
                            "Entity"); // "Aspect;fillColor=#0759cf;strokeColor=white;");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    // lastAddedCell = null; // so that it will not cause duplicate addition in tree
                    nodeNumber++;
                    currentSelectedCell = (mxCell) obj;
                }
            } 
            finally {
                graph.getModel().endUpdate();

            }
        }
    }
    
    public static void addModuleFromSubgraphUniformityPES(Object obj) {
        // a small tree will be added dynamically to the jtree which is drawn in graph
        // editor and connected later to other node

        Object[] addedNodes = new Object[100];
        int addedNodeCount = 0;
        String[] nodeNameSplits = null;

        String[] nodeNames = new String[100];
        int nodeCount = 0;
        nodeCount = treeSyncNodeCount;
        nodeNames = nodeNamesForGraphSync;

        // deleting null nodes;
        nodeNames =
                Arrays.stream(nodeNames).filter(s -> (s != null && s.length() > 0)).toArray(String[]::new);

        // adding uniformity node to the array
        addedNodes[addedNodeCount] = obj;
        addedNodeCount++;

        for (int i = 0; i < nodeCount; i++) {
            String nodeName = nodeNames[i];

            if (nodeName.endsWith("hasparent")) {
                nodeNameSplits = nodeName.split("-");
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

            String nodeId = "pesuniformity" + uniformityNodeNumber;

            graph.getModel().beginUpdate();
            try {
                if (nodeName.endsWith("Dec")) {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x + 25, y, 30, 30, "Aspect");

                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null; // so that it will not cause duplicate addition in tree

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                }
                else if (nodeName.endsWith("MAsp")) {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x + 25, y, 30, 30, "Multiaspect");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null;

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                }
                else if (nodeName.endsWith("Spec")) {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x + 25, y, 30, 30, "Specialization");
                    graph.insertEdge(parent, null, "", selectedCell, obj);
                    lastAddedCell = null;

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
                }
                else {
                    obj = graph.insertVertex(parent, nodeId, nodeName, x, y, 80, 30, "Entity");
                    graph.insertEdge(parent, null, "", selectedCell, obj);

                    lastAddedCell = null;

                    uniformityNodeNumber++;

                    addNodeIntoJtreeForSubTree(selectedCell);
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
    
    public static void addNodeIntoJtreeForSubTree(mxCell lastAddedCell) {
        path.clear();
        mxCell addedCell = null;
        if (lastAddedCell != null) {
            Object[] outgoing = graph.getOutgoingEdges(lastAddedCell);
            Object targetCell = graph.getModel().getTerminal(outgoing[outgoing.length - 1], false);
            addedCell = (mxCell) targetCell;

            if (addedCell != null) {
                JtreeToGraphConvert.nodeToRootPath(addedCell);
                lastAddedCell = null;

                // update jtree
                String[] stringArray = path.toArray(new String[0]);
                ArrayList<String> pathRev = new ArrayList<String>();

                for (int i = stringArray.length - 1; i >= 0; i--) {
                    pathRev.add(stringArray[i]);
                }

                String[] stringArrayRev = pathRev.toArray(new String[0]);

                JtreeToGraphAdd.addNodeWIthGraphAddition(addedCell.getValue().toString(), stringArrayRev);

                path.clear();
            }
        }
    }
    
    /**
     * Find next node of the argument cell. If there is more than one aspect node as
     * a child node of the argument node then it change the color of that node.
     *
     * @param nextnode
     */
    public static void nextChildNode(mxCell nextnode) {
        Object[] outgoing = graph.getOutgoingEdges(nextnode);

        if (outgoing.length == 1) {
            Object targetCell = graph.getModel().getTerminal(outgoing[0], false);
            mxCell targetCell2 = (mxCell) targetCell;
            nextChildNode(targetCell2);
        } 
        else if (outgoing.length > 1) {
            int decnum = 0;
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
                mxCell targetCell2 = (mxCell) targetCell;

                if (targetCell2.getValue().toString().endsWith("Dec") || targetCell2.getValue().toString()
                        .endsWith("MAsp")) {
                    decnum++;
                }

                if (decnum >= 2) {
                    graph.setCellStyles(mxConstants.STYLE_STROKECOLOR,
                            mxUtils.getHexColorString(Color.getHSBColor(1.41f, .96f, 0.50f)),
                            new Object[] {nextnode});
                    graphComponent.refresh();

                    // have to change icon of left tree here
                    pathToRoot.add((String) nextnode.getValue());
                    JtreeToGraphConvert.nodeToRootPathVar(nextnode);

                    String[] stringArray = pathToRoot.toArray(new String[0]);
                    ArrayList<String> pathToRootRev = new ArrayList<String>();

                    for (int j = stringArray.length - 1; j >= 0; j--) {
                        pathToRootRev.add(stringArray[j]);
                    }

                    String[] stringArrayRev = pathToRootRev.toArray(new String[0]);
                    TreePath treePathForIconChange = JtreeToGraphGeneral.getTreeNodePath(stringArrayRev);

                    cellListForIconChange[cellCount] = treePathForIconChange;
                    cellCount++;
 
                    pathToRoot.clear();

                    decnum = 0;
                }
            }

            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
                mxCell targetCell2 = (mxCell) targetCell;
                nextChildNode(targetCell2);
            }
        }
    }
}
