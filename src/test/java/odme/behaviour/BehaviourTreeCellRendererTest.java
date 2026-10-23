package odme.behaviour;

import org.junit.jupiter.api.Test;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviourTreeCellRendererTest {

    @Test
    void rendersBehaviourLeavesWithADifferentIconThanEntities() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        DefaultMutableTreeNode entity = new DefaultMutableTreeNode("CaptureSpecification");
        DefaultMutableTreeNode behaviour = new DefaultMutableTreeNode("validateCaptureQuality");
        root.add(entity);
        entity.add(behaviour);
        JTree tree = new JTree(root);
        BehaviourTreeCellRenderer renderer = new BehaviourTreeCellRenderer();

        renderer.getTreeCellRendererComponent(tree, entity, false, true, false, 1, false);
        Icon entityIcon = renderer.getIcon();
        renderer.getTreeCellRendererComponent(tree, behaviour, false, false, true, 2, false);
        Icon behaviourIcon = renderer.getIcon();

        assertThat(entityIcon).isNotNull();
        assertThat(behaviourIcon).isNotNull().isNotSameAs(entityIcon);
    }
}
