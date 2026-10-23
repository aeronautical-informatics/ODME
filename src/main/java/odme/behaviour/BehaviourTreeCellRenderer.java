package odme.behaviour;

import odme.odmeeditor.ODMEEditor;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

/** Renders behavior leaves as executable system actions, distinct from SES entities. */
final class BehaviourTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final long serialVersionUID = 1L;

    private final Icon entityIcon = icon("images/en.png");
    private final Icon specializationIcon = icon("images/sp.png");
    private final Icon multiAspectIcon = icon("images/ma.png");
    private final Icon aspectIcon = icon("images/as16.png");
    private final Icon behaviourIcon = icon("images/executionIcon.png");

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        String name = String.valueOf(((DefaultMutableTreeNode) value).getUserObject());
        if (name == null || name.isBlank() || name.startsWith("~")) {
            setIcon(null);
        } else if (leaf) {
            setIcon(behaviourIcon);
        } else if (name.endsWith("Spec")) {
            setIcon(specializationIcon);
        } else if (name.endsWith("MAsp")) {
            setIcon(multiAspectIcon);
        } else if (name.endsWith("Dec")) {
            setIcon(aspectIcon);
        } else {
            setIcon(entityIcon);
        }
        return this;
    }

    private static Icon icon(String resource) {
        return new ImageIcon(ODMEEditor.class.getClassLoader().getResource(resource));
    }
}
