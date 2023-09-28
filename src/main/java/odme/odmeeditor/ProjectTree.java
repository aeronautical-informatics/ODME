package odme.odmeeditor;

import com.google.common.io.Files;

import odme.contextmenus.ProjectTreePopup;
import odme.core.CustomIconRendererProject;
import odme.core.XmlJTree;
import odme.jtreetograph.JtreeToGraphVariables;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Scanner;


/**
 * <h1>ProjectTree</h1>
 * <p>
 * This class is used to implements to show project file structure as JTree
 * format. Current project name and its added modules are displayed with
 * different icon in this tree.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class ProjectTree extends JPanel implements MouseListener {

	private static final long serialVersionUID = 1L;
	public static String projectName;
    private DefaultMutableTreeNode projectRoot, mainModule, addedModule;
    private DefaultMutableTreeNode projectXmlFile;
    private DefaultTreeModel projectTreeModel;
    public JTree projectTree;
    private Toolkit toolkit;
    @SuppressWarnings("unused")
	private int clickControl;
    private File ssdFileProject;

    public ProjectTree() {
    	
        super(new GridLayout(1, 0));
        
        toolkit = Toolkit.getDefaultToolkit();
        clickControl = 0;
        ssdFileProject = new File(String.format("%s/%sProject.xml", ODMEEditor.projName, JtreeToGraphVariables.newFileName));

        if (ssdFileProject.exists()) {
            // restoring jtree from xml
            XmlJTree myTree =
                    new XmlJTree(ODMEEditor.projName + "/" + JtreeToGraphVariables.newFileName + "Project.xml");
            projectTreeModel = myTree.dtModel;
            projectTreeModel.addTreeModelListener(new ProjectTreeModelListener());
            projectTree = new JTree(projectTreeModel);

            projectRoot = (DefaultMutableTreeNode) projectTree.getModel().getRoot();
            Enumeration<?> enumeration = projectRoot.breadthFirstEnumeration();
            
            while (enumeration.hasMoreElements()) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
                if ("AddedModule".equals(node.getUserObject().toString())) {
                    addedModule = node;
                }
            }

        } else {
            projectName = JtreeToGraphVariables.newFileName + ".xml";
            projectRoot = new DefaultMutableTreeNode("Project");
            mainModule = new DefaultMutableTreeNode("MainModule");
            addedModule = new DefaultMutableTreeNode("AddedModule");
            projectXmlFile = new DefaultMutableTreeNode(projectName);
            projectTreeModel = new DefaultTreeModel(projectRoot);
            projectTreeModel.addTreeModelListener(new ProjectTreeModelListener());
            projectTree = new JTree(projectTreeModel);
            projectTreeModel.insertNodeInto(mainModule, projectRoot, projectRoot.getChildCount());
            projectTreeModel.insertNodeInto(addedModule, projectRoot, projectRoot.getChildCount());
            projectTreeModel.insertNodeInto(projectXmlFile, mainModule, mainModule.getChildCount());
        }

        projectTree.setEditable(true);
        projectTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        projectTree.setShowsRootHandles(true);
        projectTree.addMouseListener(this);

        // for cursor change
        projectTree.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int x = (int) e.getPoint().getX();
                int y = (int) e.getPoint().getY();
                TreePath path = projectTree.getPathForLocation(x, y);
                if (path == null) {
                    projectTree.setCursor(Cursor.getDefaultCursor());
                    clickControl = 0;
                } 
                else {
                    projectTree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    clickControl = 1;
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(projectTree);
        add(scrollPane);

        CustomIconRendererProject customIconRenderer = new CustomIconRendererProject();
        projectTree.setCellRenderer(new CustomIconRendererProject());
        projectTree.setCellRenderer(customIconRenderer);
        expandTree();
    }

    public static void showXSDtoXMLViewer(String fileName) {

        Scanner in = null;
        try {
        	System.out.println(ODMEEditor.projName + "/" + fileName);
            in = new Scanner(new File(ODMEEditor.projName + "/" + fileName));
        } 
        catch (FileNotFoundException e) {
        	return;
            
        }
        catch (NullPointerException e) {
        	return;
            
        }

        StringBuilder xmlcontent = new StringBuilder();

        while (in.hasNext()) {

            String line = in.nextLine();

            xmlcontent.append(line);
            xmlcontent.append("\n");
        }

        in.close();
    }

    public void expandTree() {
        for (int i = 0; i < projectTree.getRowCount(); i++) {
            projectTree.expandRow(i);
        }
    }

    public void addModueFile(String fileName) {
        fileName = Files.getNameWithoutExtension(fileName); // using google guava deleting file extension
        projectXmlFile = new DefaultMutableTreeNode(fileName + ".xml");
        projectTreeModel.insertNodeInto(projectXmlFile, addedModule, addedModule.getChildCount());
        expandTree();
    }

    public void removeCurrentNode() {
        TreePath currentSelection = projectTree.getSelectionPath();
        if (currentSelection != null) {
            DefaultMutableTreeNode currentNode =
                    (DefaultMutableTreeNode) (currentSelection.getLastPathComponent());
            MutableTreeNode parent = (MutableTreeNode) (currentNode.getParent());
            if (parent != null) {

                if (currentNode.toString().equals("MainModule") || currentNode.toString()
                        .equals("AddedModule") || currentNode.toString()
                            .equals(ODMEEditor.projName + ".xml")) {
                    toolkit.beep();
                } else {
                    projectTreeModel.removeNodeFromParent(currentNode);
                }

                return;
            }
        }
    }

    public void changeCurrentProjectFileName(String fileName, String oldProjectTreeProjectName) {

        projectRoot = (DefaultMutableTreeNode) projectTree.getModel().getRoot();
        Enumeration<?> enumeration = projectRoot.breadthFirstEnumeration();

        while (enumeration.hasMoreElements()) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if ((oldProjectTreeProjectName + ".xml").equals(node.getUserObject().toString())) {
                projectTreeModel.removeNodeFromParent(node);
            }
        }

        projectRoot = (DefaultMutableTreeNode) projectTree.getModel().getRoot();
        enumeration = projectRoot.breadthFirstEnumeration();

        while (enumeration.hasMoreElements()) {

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) enumeration.nextElement();
            if ("MainModule".equals(node.getUserObject().toString())) {
                mainModule = node;
            }
        }

        projectXmlFile = new DefaultMutableTreeNode(fileName + ".xml");
        projectTreeModel.insertNodeInto(projectXmlFile, mainModule, mainModule.getChildCount());
        expandTree();
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
    	if (ODMEEditor.toolMode == "pes")
    		return;
    	
        if (arg0.getClickCount() == 2) // double click
        {
            String name = projectTree.getSelectionPath().getLastPathComponent().toString();
            showXSDtoXMLViewer(name);
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {}

    @Override
    public void mouseExited(MouseEvent arg0) {}

    @Override
    public void mousePressed(MouseEvent arg0) {}

    @Override
    public void mouseReleased(MouseEvent e) {
    	if (ODMEEditor.toolMode == "pes")
    		return;
    	
        final ProjectTreePopup treePopup = new ProjectTreePopup(projectTree);
        
        // #ROY - fixing contextMenu not popping up
        if ( (e.getButton() == MouseEvent.BUTTON3) 
        	|| (e.getButton() == MouseEvent.BUTTON2)  ) { // on laptop there's no button3, catch 2 instead	
        	System.out.println("\ttriggered!"); // debug!
            treePopup.show(e.getComponent(), e.getX(), e.getY());
       }
    }

    // Internal Class for Project Tree Handling
    class ProjectTreeModelListener implements TreeModelListener {

        public void treeNodesChanged(TreeModelEvent e) {
            DefaultMutableTreeNode node;
            node = (DefaultMutableTreeNode) (e.getTreePath().getLastPathComponent());

            int index = e.getChildIndices()[0];
            node = (DefaultMutableTreeNode) (node.getChildAt(index));
        }

        public void treeNodesInserted(TreeModelEvent e) {}

        public void treeNodesRemoved(TreeModelEvent e) {}

        public void treeStructureChanged(TreeModelEvent e) {}
    }
}
