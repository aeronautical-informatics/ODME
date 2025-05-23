package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.contextmenus.TreePopup;
import odme.core.CustomIconRenderer;
import odme.core.EditorUndoableEditListener;
import odme.core.FindByName;
import odme.core.FlagVariables;
import odme.core.UndoableTreeModel;
import odme.core.XmlJTree;
import odme.jtreetograph.JtreeToGraphDelete;
import odme.jtreetograph.JtreeToGraphSave;
import odme.jtreetograph.JtreeToGraphVariables;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.UndoableEditListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * <h1>DynamicTree</h1>
 * <p>
 * This class handles all the activities related to JTree. It also helps to
 * synchronized the Jtree with the tree from the graphical tree builder. The
 * created tree in the graphical drawing paenl is displayed as a JTree format
 * using this class.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class DynamicTree extends JPanel implements MouseListener {

    private static final long serialVersionUID = 1L;

    public static Multimap<TreePath, String> varMap = ArrayListMultimap.create();
    public static Multimap<TreePath, String> constraintsList = ArrayListMultimap.create();
    public static Multimap<TreePath, String> behavioursList = ArrayListMultimap.create();

    // Store limits for each TreePath associated with an MAsp node
//    public static Multimap<TreePath, String> limitsMAspec = ArrayListMultimap.create();

    public static Map<TreePath, String> limitsMAspec = new HashMap<>();

    public static Variable scenarioVariable = new Variable();
    public static String projectFileName;
    
    private DefaultMutableTreeNode rootNode;
    public static UndoableTreeModel treeModel;
    public JTree tree;
    
    private Toolkit toolkit;
    public File ssdFile;
    public File ssdFileVar;
    public File ssdFileCon;
    public File ssdFileBeh;

    public File ssdFileLimit;
    public File ssdFileFlag;
    @SuppressWarnings("unchecked")
    public DynamicTree() {
        super(new GridLayout(1, 0));
        
        toolkit = Toolkit.getDefaultToolkit();
        
        ssdFile = new File(String.format("%s/%s/%s.xml", ODMEEditor.fileLocation,ODMEEditor.projName, projectFileName));
        ssdFileVar = new File(String.format("%s/%s/%s.ssdvar", ODMEEditor.fileLocation,ODMEEditor.projName, projectFileName));
        ssdFileCon = new File(String.format("%s/%s/%s.ssdcon", ODMEEditor.fileLocation,ODMEEditor.projName, projectFileName));
        ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag", ODMEEditor.fileLocation,ODMEEditor.projName, projectFileName));
        ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh", ODMEEditor.fileLocation,ODMEEditor.projName, projectFileName));

        System.out.println("projectFileName: " + projectFileName);

        if (ssdFile.exists()) {

            try {
                if (ODMEEditor.openClicked == 1) {
                    File ssdFile = new File(ODMEEditor.openFileName + ".ssd");
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ssdFile));
                    ois.close();
                }
                else {
                    // for variable
                    if (ssdFileVar.exists() && ssdFileCon.exists() && ssdFileFlag.exists() && ssdFileBeh.exists()) {
                        ObjectInputStream oisvar = new ObjectInputStream(new FileInputStream(ssdFileVar));
                        varMap = (Multimap<TreePath, String>) oisvar.readObject();
                        oisvar.close();

                        ObjectInputStream oiscon = new ObjectInputStream(new FileInputStream(ssdFileCon));
                        constraintsList = (Multimap<TreePath, String>) oiscon.readObject();
                        oiscon.close();

                        //it  read behaviours from the file
                        ObjectInputStream oisbeh = new ObjectInputStream(new FileInputStream(ssdFileBeh));
                        behavioursList = (Multimap<TreePath , String>) oisbeh.readObject();
                        oisbeh.close();
                    }

                    if (ssdFileFlag.exists()) {

                        ObjectInputStream oisflag = new ObjectInputStream(new FileInputStream(ssdFileFlag));
                        FlagVariables flags = (FlagVariables) oisflag.readObject();
                        JtreeToGraphVariables.nodeNumber = flags.nodeNumber;
                        JtreeToGraphVariables.uniformityNodeNumber = flags.uniformityNodeNumber;
                        oisflag.close();
                    }

                    // restoring jtree from xml
                    XmlJTree myTree = new XmlJTree(
                            ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + projectFileName
                            + ".xml");
                    treeModel = myTree.dtModel;
                    treeModel.addTreeModelListener(new MyTreeModelListener());
                }
            } 
            catch (IOException err) {
                err.printStackTrace();
            } 
            catch (ClassNotFoundException err) {
                err.printStackTrace();
            }
        } 
        else {

            rootNode = new DefaultMutableTreeNode("Thing");
            treeModel = new UndoableTreeModel(rootNode);
            treeModel.addTreeModelListener(new MyTreeModelListener());

        }
        // --------------------
        tree = new JTree(treeModel);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);
        tree.addMouseListener(this);

        // for expanding the tree on starting
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        Path path = Paths.get("").toAbsolutePath();
        String repFslas = path.toString().replace("\\", "/");

        Icon entityIcon = new ImageIcon(repFslas + "/resources/images/en.png");

        CustomIconRenderer customIconRenderer = new CustomIconRenderer();
        tree.setCellRenderer(customIconRenderer);
        customIconRenderer.setLeafIcon(entityIcon);
        tree.setCellRenderer(new CustomIconRenderer());
        tree.setCellRenderer(customIconRenderer);

        // for cursor change
        tree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int x = (int) e.getPoint().getX();
                int y = (int) e.getPoint().getY();
                TreePath path = tree.getPathForLocation(x, y);
                if (path == null) {
                    tree.setCursor(Cursor.getDefaultCursor());
                } else {
                    tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane);
    }

    @SuppressWarnings("unchecked")
    public void openExistingProject(String filename, String oldProjectTreeProjectName) {
        // restoring jtree from xml

        String path = new String();
        if (ODMEEditor.toolMode == "ses")
            path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + filename + ".xml";
        else
            path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + filename + ".xml";

        XmlJTree myTree =
                new XmlJTree(path);
        treeModel = myTree.dtModel;
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        treeModel.addTreeModelListener(new MyTreeModelListener());
        treeModel.reload(root);

        // opening only child node is allowing insertion
        tree.setModel(treeModel);

        // for expanding the tree on starting
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
        String newProjectName = filename;
        // this is similar to new-------------------------------------------------
        ODMEEditor.projName = newProjectName;
        JtreeToGraphVariables.newFileName = newProjectName;
        JtreeToGraphVariables.projectFileNameGraph = newProjectName;
        
        if (ODMEEditor.toolMode == "ses")
            path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + newProjectName + "Graph.xml";
        else
            path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + newProjectName + "Graph.xml";

        JtreeToGraphVariables.ssdFileGraph = new File(path);
        ODMEEditor.treePanel.ssdFile =
                new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + newProjectName + ".xml");
        try {

            if (ODMEEditor.toolMode == "ses")
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + newProjectName + ".ssdvar";
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + newProjectName + ".ssdvar";

            ObjectInputStream oisvar;
            oisvar = new ObjectInputStream(new FileInputStream(path));
            varMap = (Multimap<TreePath, String>) oisvar.readObject();
            oisvar.close();  
            
            
            if (ODMEEditor.toolMode == "ses")
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + newProjectName + ".ssdcon";
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + newProjectName + ".ssdcon";

            ObjectInputStream oiscon = new ObjectInputStream(new FileInputStream(path));
            constraintsList = (Multimap<TreePath, String>) oiscon.readObject();
            oiscon.close();

            if (ssdFileFlag.exists()) {

                if (ODMEEditor.toolMode == "ses")
                    path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName +  "/" + newProjectName + ".ssdflag";
                else
                    path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario +  "/" + newProjectName + ".ssdflag";

                ObjectInputStream oisflag = new ObjectInputStream(new FileInputStream(path));
                FlagVariables flags = new FlagVariables();
                flags = (FlagVariables) oisflag.readObject();
                System.out.println("flags.nodeNumber:" + flags.nodeNumber);
                JtreeToGraphVariables.uniformityNodeNumber = flags.uniformityNodeNumber;
                oisflag.close();
            }

        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        ProjectTree.projectName = newProjectName;

        ODMEEditor.projectPanel.changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);
        ODMEEditor.treePanel.addUndoableEditListener(new EditorUndoableEditListener());

        // this is similar to new end------------------------------------------------

        Variable.setNullToAllRows();
        Constraint.setNullToAllRows();
    }

    // for expanding the tree after changes
    public void expandTree() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    // -----------------------------------------------------------------------------
    // for undo redo
    public void addUndoableEditListener(UndoableEditListener l) {
        treeModel.addUndoableEditListener(l);
    }

    public void removeUndoableEditListener(UndoableEditListener l) {
        treeModel.removeUndoableEditListener(l);
    }

    // -----------------------------------------------------------------------------

    public void removeCurrentNode() {
        TreePath currentSelection = tree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                // ---------------------------------------------------------------------
                // have to call function to delete node from graph here
                TreeNode[] nodes = currentNode.getPath();
                String[] nodesToSelectedNode = new String[nodes.length];

                for (int i = 0; i < nodes.length; i++) {
                    nodesToSelectedNode[i] = (nodes[i].toString());
                }

                treeModel.removeNodeFromParent(
                        currentNode); // if this line is above the currentNode.getPath() then it
                // will not work because before cullect path nodes the
                // variable is deleted then
                JtreeToGraphDelete.deleteNodeWithTree(nodesToSelectedNode);

                return;
            }
        }

        // Either there was no selection, or the root was selected.
        toolkit.beep();
    }

    public TreePath getTreeNodePath(String[] nodePath) {
        TreePath parentPath;
         new FindByName(tree, nodePath);
        parentPath = FindByName.path;
        return parentPath;
    }

    public void removeCurrentNodeWithGraphDelete(TreePath currentSelection) {
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {
                treeModel.removeNodeFromParent(currentNode);
                return;
            }
        }
        // Either there was no selection, or the root was selected.
        toolkit.beep();
    }

    public DefaultMutableTreeNode addObjectWIthGraphAddition(Object child, String[] nodePath) {
        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath;

        new FindByName(tree, nodePath);
        parentPath = FindByName.path;
        if (parentPath == null) {
            parentNode = rootNode;
        } else {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }
        return addObject(parentNode, child, true);
    }

    /**
     * Add child to the currently selected node.
     */
    public DefaultMutableTreeNode addObject(Object child) {
        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();
        if (parentPath == null) {
            parentNode = rootNode;
        } 
        else {
            parentNode = (DefaultMutableTreeNode) (parentPath.getLastPathComponent());
        }

        return addObject(parentNode, child, true);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child) {
        return addObject(parent, child, false);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent, Object child,
                                            boolean shouldBeVisible) {
        DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);

        if (parent == null) {
            parent = rootNode;
        }

        // It is key to invoke this on the TreeModel, and NOT
        // DefaultMutableTreeNode
        treeModel.insertNodeInto(childNode, parent, parent.getChildCount());

        // Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
            // tree.expandPath(new TreePath(parent.getPath()));
        }

        return childNode;
    }

    public void saveTreeModel() {

        try {

            //for limits of MultiAspect in file
            if(ODMEEditor.toolMode == "ses"){

                // Define the file location
                File ssdFileLimit = new File(String.format("%s.ssdLimit",
                        ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + ODMEEditor.projName));

                // Map to hold the data (existing + new)
                Map<TreePath, String> limits;

                // Check if the file exists
                if (ssdFileLimit.exists()) {
                    // File exists, read existing data
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ssdFileLimit))) {
                        limits = (Map<TreePath, String>) ois.readObject();
                        System.out.println("Existing data loaded from file.");
                    } catch (Exception e) {
                        System.err.println("Failed to read existing file " + e.getMessage());
                        limits = new HashMap<>();  // Initialize an empty map in case of failure
                    }
                } else {
                    // File does not exist, initialize a new map
                    limits = new HashMap<>();
                    System.out.println("File does not exist starting with an empty map.");
                }

                if (limits.isEmpty()  && !DynamicTree.limitsMAspec.isEmpty()){

                    // Write the updated map back to the file
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ssdFileLimit))) {
                        oos.writeObject(DynamicTree.limitsMAspec);
                        System.out.println("Updated data written to file.");
                    } catch (Exception e) {
                        System.err.println("Failed to write updated data to file: " + e.getMessage());
                    }
                }
                else if (!limits.isEmpty() && !DynamicTree.limitsMAspec.isEmpty()
                ){
                    //now compare the values in the Map
                    // Step 2: Compare and update `limits` with `DynamicTree.limitsMAspec`
                    for (Map.Entry<TreePath, String> entry : DynamicTree.limitsMAspec.entrySet()) {
                        TreePath key = entry.getKey();
                        String value = entry.getValue();
//                        System.out.println("Key = " + key);
//                        System.out.println("Value = " + value);

                        boolean keyMatched = false;
                        for (Map.Entry<TreePath, String> limitEntry : limits.entrySet()) {
                            if (limitEntry.getKey().toString().equals(key.toString())
                            ) {
                                keyMatched = true;
//                                System.out.println("Key matched: " + key);
                                limits.put(limitEntry.getKey(), value);
                                break;
                            }
                        }
                        if (!keyMatched) {
//                            System.out.println("Key not matched: " + key);
                            limits.put(key, value);
                        }
                    }
                    // Write the updated map back to the file
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ssdFileLimit))) {
                        oos.writeObject(limits);
//                        System.out.println("Updated data written to file.");
                    } catch (Exception e) {
                        System.err.println("Failed to write updated data to file: " + e.getMessage());
                    }
                }
                else {
                    System.out.println("Both Maps are empty ");
                }


                for (Map.Entry<TreePath, String> entry : limits.entrySet()) {
                    System.out.println("After update Key: " + entry.getKey() + ", Value: " + entry.getValue());
                }

            }

            // for variable

            String path;
            if (ODMEEditor.toolMode == "ses"){
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + ODMEEditor.projName;
                System.out.println("path ses = " + path);
            }
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + ODMEEditor.projName;

            ssdFileVar = new File(String.format("%s.ssdvar", path));
            ObjectOutputStream oosvar = new ObjectOutputStream(new FileOutputStream(ssdFileVar));
            oosvar.writeObject(varMap);
            oosvar.close();

            //for behaviour
            ssdFileBeh = new File(String.format("%s.ssdbeh",path ));
            ObjectOutputStream oosbeh = new ObjectOutputStream(new FileOutputStream(ssdFileBeh));
            oosbeh.writeObject(behavioursList);
            oosbeh.close();

            // for constraint
            ssdFileCon = new File(String.format("%s.ssdcon", path));
            ObjectOutputStream ooscons = new ObjectOutputStream(new FileOutputStream(ssdFileCon));
            ooscons.writeObject(constraintsList);
            ooscons.close();

            // for constraint
            ssdFileFlag = new File(String.format("%s.ssdflag", path));
            ObjectOutputStream oosflag = new ObjectOutputStream(new FileOutputStream(ssdFileFlag));
            FlagVariables flags = new FlagVariables();
            flags.nodeNumber = JtreeToGraphVariables.nodeNumber;
            flags.uniformityNodeNumber = JtreeToGraphVariables.uniformityNodeNumber;
            oosflag.writeObject(flags);
            oosflag.close();

            JtreeToGraphSave.saveGraph();
            
        } 
        catch (IOException err) {
            err.printStackTrace();
        }
    }

    public void saveTreeModelAs(String fileName) {
        fileName = fileName + ".ssd";
        try {
            // for variable
            fileName = fileName + "var";
            ObjectOutputStream oosvar = new ObjectOutputStream(new FileOutputStream(fileName));
            oosvar.writeObject(varMap);
            oosvar.close();

        } 
        catch (IOException err) {
            err.printStackTrace();
        }
    }

    public void openTreeModel(File fileName) {
        if (fileName.exists()) {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName));
                treeModel.reload();
                ois.close();
            } catch (IOException err) {
                err.printStackTrace();
            }
        }
    }

    public void mouseClicked(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {
        // throw new UnsupportedOperationException("Not supported yet."); //To
        // change body of generated methods, choose Tools | Templates.

        if (ODMEEditor.toolMode == "pes")
            return;

        final TreePopup treePopup = new TreePopup(tree);

        if (e.getButton() == MouseEvent.BUTTON1 ) {
            System.out.println("BUTTON1");
            TreePath currentSelection = tree.getSelectionPath();
            if (currentSelection != null) {
                DefaultMutableTreeNode currentNode =
                        (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
               
                // -------------------------------------------------------
                TreeNode[] nodes = currentNode.getPath();
                String[] nodesToSelectedNode = new String[100];
                
                int b = 0;
                for (TreePath key : varMap.keySet()) {
                    int a = 0;

                    for (String value : varMap.get(key)) {
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

                ODMEEditor.scenarioVariable
                        .showNodeValuesInTable(currentNode.toString(), nodesToSelectedNode);
            }

        } else if ( (e.getButton() == MouseEvent.BUTTON3)
                || (e.getButton() == MouseEvent.BUTTON2) ) { // there's no button3 on laptop, use 2 instead

            // #ROY- fixing contextMenu not popping up
            // NOTE put more constraints (like e.isTriggered or whatever)
            // and it won't work anymore (not on my system at least)
            treePopup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void refreshVariableTable(TreePath treePathForVariable) {
        DefaultMutableTreeNode currentNode =
                (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

        TreeNode[] nodes = currentNode.getPath();

        String[] nodesToSelectedNode = new String[100];
        int b = 0;

        for (TreePath key : DynamicTree.varMap.keySet()) {
            int a = 0;

            for (String value : DynamicTree.varMap.get(key)) {
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
                    nodesToSelectedNode[b] = value;
                    b++;
                }
            }
        }
        
        ODMEEditor.scenarioVariable
                .showNodeValuesInTable(currentNode.toString(), nodesToSelectedNode); 
    }

    public void showConstraintsInTable(TreePath treePathForVariable) {

        DefaultMutableTreeNode currentNode =
                (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

        TreeNode[] nodes = currentNode.getPath();

        String[] nodesToSelectedNode = new String[100];

        int b = 0;

        for (TreePath key : DynamicTree.constraintsList.keySet()) {
            int a = 0;

            for (String value : DynamicTree.constraintsList.get(key)) {
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
                    nodesToSelectedNode[b] = value;
                    b++;
                }
            }
        }

        ODMEEditor.scenarioConstraint.showConstraintsInTable(nodesToSelectedNode);
    }
    public void showBehavioursInTable(TreePath treePathForVariable) {

        DefaultMutableTreeNode currentNode =
                (DefaultMutableTreeNode) (treePathForVariable.getLastPathComponent());

        TreeNode[] nodes = currentNode.getPath();

        String[] nodesToSelectedNode = new String[100];
        String nodeName = null;

        int b = 0;
        for (TreePath key : DynamicTree.behavioursList.keySet()) {
            int a = 0;
            for (String value : DynamicTree.behavioursList.get(key)) {

                DefaultMutableTreeNode currentNode2 = (DefaultMutableTreeNode) (key.getLastPathComponent());

                TreeNode[] nodes2 = currentNode2.getPath();

                if (nodes.length == nodes2.length) {
                    int aa = 1;
                    for (int i = 0; i < nodes.length; i++) {
                        if (!nodes[i].toString().equals(nodes2[i].toString())) {
                            nodeName = nodes[i].toString();
                            aa = 0;
                            break;
                        }
                    }
                    a = aa;
                }

                if (a == 1) {

                    nodesToSelectedNode[b] = value;
                    nodeName = currentNode2.getUserObject().toString();
                    b++;
                    ODMEEditor.scenarioBehaviour.showBehavioursInTable(currentNode2.getUserObject().toString(),nodesToSelectedNode);
                }
            }
        }
    }


    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

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
