package odme.core;

import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.Enumeration;


/**
 * <h1>FindByName</h1>
 * <p>
 * This class used find the path of the selected node from the root node.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class FindByName {
    public static TreePath path;

    public FindByName(JTree tree, String[] names) {
    	TreeNode root = (TreeNode) tree.getModel().getRoot();
    	path = find(tree, new TreePath(root), names, 0);
    }

    @SuppressWarnings("rawtypes")
	private TreePath find(JTree tree, TreePath parent, Object[] nodes, int depth) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        String o = node.toString();

        if (o.equals(nodes[depth])) {
            if (depth == nodes.length - 1) {
                return parent;
            }
            if (node.getChildCount() >= 0) {
                for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                    TreeNode n = (TreeNode) e.nextElement();
                    TreePath path = parent.pathByAddingChild(n);
                    TreePath result = find(tree, path, nodes, depth + 1);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }
        return null;
    }
}
