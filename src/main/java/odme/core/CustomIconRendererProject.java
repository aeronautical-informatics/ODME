package odme.core;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import odme.odmeeditor.ODMEEditor;

import java.awt.Component;

/**
 * <h1>CustomIconRendererProject</h1>
 * <p>
 * To change the icon of the project window tree specific icons are used. This
 * class is used to set the folder icon and xml file icon for the project files.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class CustomIconRendererProject extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;
	private Icon moduleIcon;
    private Icon xmlIcon;

    public CustomIconRendererProject() { // throws MalformedURLException
        moduleIcon = new ImageIcon(
                ODMEEditor.class.getClassLoader().getResource("images/folder164.png"));
        xmlIcon = new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/projtreeleaf.png"));
    }

    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
                                                  final boolean expanded, final boolean leaf, final int row,
                                                  final boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        Object nodeObj = ((DefaultMutableTreeNode) value).getUserObject();
        String nodeName = nodeObj.toString();
        if ((nodeName != null) && (!nodeName.trim().isEmpty())) {
            if (nodeName.endsWith("Module")) {
                setIcon(moduleIcon);
            } else if (nodeName.endsWith("xml")) {
                setIcon(xmlIcon);
            } else {
                setIcon(moduleIcon);
            }
        }
        return this;
    }
}
