package odeme.behaviour;

import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import behaviourtreetograph.JtreeToGraphSave;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import odeme.behaviour.ODMEBehaviourEditor;
import odme.core.CustomIconRenderer;

import odme.core.UndoableTreeModel;
import odme.odmeeditor.ODMEEditor;
import org.fife.util.DynamicIntArray;

public class BehaviourToTree  extends JPanel implements MouseListener{


	public  Multimap<TreePath, String> behavioursList;

	public File ssdFileBeh;
	public static String selectedScenario;
	public static DefaultMutableTreeNode root;
	public  JTree  tree;

	private int clickControl;

	@SuppressWarnings("unchecked")
	public BehaviourToTree(){
		super(new GridLayout(1, 0));

		//now construct tree of behaviours
		ssdFileBeh = new File(ODMEEditor.fileLocation + "/"+ ODMEEditor.projName +"/"+ selectedScenario + "/"+ ODMEEditor.projName+".ssdbeh" );

//		System.out.println("Path  = "+ODMEEditor.fileLocation + "/"+ ODMEEditor.projName +"/"+ selectedScenario + "/"+ ODMEEditor.projName+".ssdbeh");
		behavioursList = ArrayListMultimap.create();

		if(ssdFileBeh.exists()) {
			//read Node from the  file
			try {
//				  behavioursList = null;
				ObjectInputStream oisbeh = new ObjectInputStream(new FileInputStream(ssdFileBeh));
				behavioursList = (Multimap<TreePath , String>) oisbeh.readObject();
				oisbeh.close();

				System.out.println("behavioursList = " + behavioursList.toString());

				//first sort this List so that i can get parent node
				Comparator<TreePath> pathLengthComparator = Comparator.comparingInt(path -> path.getPathCount());

				// Sort the behavioursList based on TreePath length
				List<TreePath> sortedKeys = new ArrayList<>(behavioursList.keySet());

				HashSet<TreePath> sortedSet = new HashSet<>(behavioursList.keySet());

				Collections.sort(sortedKeys, pathLengthComparator);

				root = treeify(convert(behavioursList));

				tree = new JTree(root);
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

//	              // for cursor change

				tree.addMouseListener(this);

				JScrollPane scrollPane = new JScrollPane(tree);
				add(scrollPane);

			}catch(Exception e) {
			}
		} else {
			System.out.println("Behaviour file does not exist");
		}
	}

	public static String[][] convert(Multimap<TreePath ,String> multiMap) {

		Map<TreePath, Collection<String>> map =   multiMap.asMap();
//		  System.out.println("Converted values "+map.toString());

		String[][] resultArray = convertMapToArray(map);
		// Sort the array using a custom comparator
		Arrays.sort(resultArray, Comparator.comparing(strings -> strings[0]));
		return resultArray;

	}

	public static String[][] convertMapToArray(Map<TreePath, Collection<String>> map) {
		List<String[]> resultList = new ArrayList<>();

		for (Map.Entry<TreePath, Collection<String>> entry : map.entrySet()) {
			TreePath treePath = entry.getKey();
			Collection<String> values = entry.getValue();

			String[] row = new String[treePath.getPathCount() + values.size()];

			int index = 0;
			for (Object pathComponent : treePath.getPath()) {
				row[index++] = pathComponent.toString();
			}

			for (String value : values) {
				row[index++] = value;
			}

			resultList.add(row);
		}

		return resultList.toArray(new String[0][0]);
	}


	public static DefaultMutableTreeNode treeify(String[][] paths) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
		for ( String[] path : paths) {
			DefaultMutableTreeNode curr = root;

			for ( String value : path){
//			    	 System.out.println(value.toString());
				DefaultMutableTreeNode next = null;
				Enumeration ce = curr.children();
				while (ce.hasMoreElements()){
					DefaultMutableTreeNode kid = (DefaultMutableTreeNode) ce.nextElement();
					if (((String)kid.getUserObject()).equals(value)){

						next = kid;
//		                    System.out.println(next.getUserObject().toString());
						break;
					}
				}
				if (next == null){
					next = new DefaultMutableTreeNode(value);
					curr.add(next);
				}
				curr = next;
			}
		}
		return root;
	}

	public void saveTreeModel() {
		JtreeToGraphSave.saveBehaviourGraph();
	}

	@Override
	public void mouseClicked(MouseEvent e) {

        int x = (int) e.getPoint().getX();
        int y = (int) e.getPoint().getY();
        TreePath path = tree.getPathForLocation(x, y);
        
        if (path == null) {
            tree.setCursor(Cursor.getDefaultCursor());
            clickControl = 0;

        } else {
            tree.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            clickControl = 1;
            String name = path.getLastPathComponent().toString();
                       
            ODMEBehaviourEditor.nodeBehaviour = name;
            System.out.println(name);
        }
	}


	public void expandTree() {
		for (int i = 0; i < tree.getRowCount(); i++) {
			tree.expandRow(i);
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub

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
