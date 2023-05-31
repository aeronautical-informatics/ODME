package odme.core;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import odme.odmeeditor.ODMEEditor;

import java.awt.Component;


/**
 * <h1>CustomIconRenderer</h1>
 * <p>
 * To identify System Entity Structure elements uniquely different icons are
 * used. CustomIconRenderer class is used to set the icon of the elements based
 * on the name of the elements.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class CustomIconRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = 1L;
	private Icon entityIcon;
    private Icon specIcon;
    private Icon maspIcon;
    private Icon aspIcon;

    public CustomIconRenderer() { // throws MalformedURLException
        entityIcon = new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/en.png"));
        specIcon = new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/sp.png"));
        maspIcon = new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/ma.png"));
        aspIcon = new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/as16.png"));
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        Object nodeObj = ((DefaultMutableTreeNode) value).getUserObject();
        String nodeName = nodeObj.toString();

        if ((nodeName != null) && (!nodeName.trim().isEmpty())) {
            if (nodeName.startsWith("~")) {
                setIcon(null);
            } 
            else if (nodeName.endsWith("Spec")) {
                setIcon(specIcon);
            } 
            else if (nodeName.endsWith("MAsp")) {
                setIcon(maspIcon);
            } 
            else if (nodeName.endsWith("Dec")) {
                setIcon(aspIcon);
            }
            else {
                setIcon(entityIcon);
            }
        }
        return this;
    }
}
