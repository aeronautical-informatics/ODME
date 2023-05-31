package odme.core;

import javax.swing.event.UndoableEditListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

/**
 * <code>DefaultTreeModel</code> implementation that supports undo and redo
 * the <code>javax.swing.undo</code> package.  In order to take advantage of
 * the undo/redo support, all changes to the model should be performed using
 * the <code>insertNodeInto</code>, <code>removeNodeFromParent</code>, and
 * <code>valueForPathChanged</code> methods.
 *
 * @author ---
 * @ModifiedBy ---
 */
public class UndoableTreeModel extends DefaultTreeModel {

	private static final long serialVersionUID = 1L;
	private final UndoableEditSupport editListeners = new UndoableEditSupport();

    /**
     * Creates a tree specifying whether any node can have children, or
     * whether only certain nodes can have children.
     *
     * @param root               - a TreeNode object that is the root of the tree
     * @param asksAllowsChildren - a boolean, false if any node can have
     *                           children, true if each node is asked
     *                           to see if it can have children
     */
    public UndoableTreeModel(TreeNode root, boolean asksAllowsChildren) {
        super(root, asksAllowsChildren);
    }

    /**
     * Creates a tree in which any node can have children
     *
     * @param root - a TreeNode object that is the root of the tree
     */
    public UndoableTreeModel(TreeNode root) {
        super(root);
    }

    /**
     * Registers an <code>UndoableEditListener</code> to receive undo-able
     * events from this tree model.
     *
     * @param l - listener to receive undo-able events from this tree model
     */
    public void addUndoableEditListener(UndoableEditListener l) {
        editListeners.addUndoableEditListener(l);
    }

    /**
     * Unregisters an <code>UndoableEditListener</code>.
     *
     * @param l - listener to unregister from this tree model
     */
    public void removeUndoableEditListener(UndoableEditListener l) {
        editListeners.removeUndoableEditListener(l);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.DefaultTreeModel#insertNodeInto(javax.swing.tree.MutableTreeNode, javax.swing.tree.MutableTreeNode, int)
     */
    @Override
    public void insertNodeInto(MutableTreeNode newChild, MutableTreeNode parent, int index) {
        // Perform the insertion
        super.insertNodeInto(newChild, parent, index);

        // Now create and post an UndoableEdit for this change
        UndoableEdit edit = new NodeAddEdit(parent, newChild, index);
        editListeners.postEdit(edit);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.DefaultTreeModel#removeNodeFromParent(javax.swing.tree.MutableTreeNode)
     */
    @Override
    public void removeNodeFromParent(MutableTreeNode node) {
        // First, get a reference to the child's index and parent
        MutableTreeNode parent = (MutableTreeNode) node.getParent();
        int index = (parent != null) ? parent.getIndex(node) : -1;

        // Now remove the node from its parent
        super.removeNodeFromParent(node);
        
        // Finally, create and post an UndoableEdit for this removal
        UndoableEdit edit = new NodeRemoveEdit(parent, node, index);
        editListeners.postEdit(edit);
    }

    /* (non-Javadoc)
     * @see javax.swing.tree.DefaultTreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
     */
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // Save the old value of the node being changed
        MutableTreeNode aNode = (MutableTreeNode) path.getLastPathComponent();
        Object oldValue = ((DefaultMutableTreeNode) aNode).getUserObject();

        // Create an undoable edit object for this change
        UndoableEdit edit = new NodeChangeEdit(path, oldValue, newValue);
        editListeners.postEdit(edit);

        // Actually change the value of the tree node
        super.valueForPathChanged(path, newValue);
    }

    
    /**
     * Private inner class that represents the undo-able (and redo-able)
     * addition of a tree node to the tree model.
     */
    private class NodeAddEdit extends AbstractUndoableEdit {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final MutableTreeNode child;
        private final MutableTreeNode parent;
        private final int index;

        public NodeAddEdit(MutableTreeNode parent, MutableTreeNode child, int index) {
            this.child = child;
            this.parent = parent;
            this.index = index;
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#getPresentationName()
         */
        @Override
        public String getPresentationName() {
            return "Add " + child + " to " + parent;
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#redo()
         */
        @Override
        public void redo() throws CannotRedoException {
            // First, invoke super.redo() to make sure we can redo
            super.redo();

            // Re-add the child to the parent at the specified index
            parent.insert(child, index);

            // Notify any listeners that the node was readded
            int[] childIndices = {index};
            nodesWereInserted(parent, childIndices);
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#undo()
         */
        @Override
        public void undo() throws CannotUndoException {
            // First, invoke super.undo() to make sure we can undo
            super.undo();

            // Re-add the child to the parent at the specified index
            parent.remove(index);

            // Notify any listeners that the node was readded
            int[] childIndices = {index};
            Object[] removedChildren = {child};
            nodesWereRemoved(parent, childIndices, removedChildren);
        }

    }

    
    /**
     * Private inner class that represents the undo-able (and redo-able)
     * changing of data stored in a tree node.
     */
    private class NodeChangeEdit extends AbstractUndoableEdit {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final TreePath path;
        private final Object oldValue;
        private final Object newValue;

        public NodeChangeEdit(TreePath path, Object oldValue, Object newValue) {
            this.path = path;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#getPresentationName()
         */
        @Override
        public String getPresentationName() {
            return "Change " + path.getLastPathComponent();
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#redo()
         */
        @Override
        public void redo() throws CannotRedoException {
            // First, invoke super.redo() to make sure we can redo
            super.redo();

            // Set the node's user object to the its new value
            MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
            node.setUserObject(newValue);

            // Notify any listeners that the tree has changed
            nodeChanged(node);
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#undo()
         */
        @Override
        public void undo() throws CannotUndoException {
            // First, invoke super.undo() to make sure we can undo
            super.undo();

            // Set the node's user object to its old value
            MutableTreeNode node = (MutableTreeNode) path.getLastPathComponent();
            node.setUserObject(oldValue);

            // Notify any listeners that the tree has changed
            nodeChanged(node);
        }
    }

    
    /**
     * Private inner class that represents the undo-able (and redo-able)
     * removal of a tree node to the tree model.
     */
    private class NodeRemoveEdit extends AbstractUndoableEdit {

        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final MutableTreeNode parent;
        private final MutableTreeNode child;
        private final int index;

        public NodeRemoveEdit(MutableTreeNode parent, MutableTreeNode child, int index) {
            this.parent = parent;
            this.child = child;
            this.index = index;
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#getPresentationName()
         */
        @Override
        public String getPresentationName() {
            return "Remove " + child;
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#redo()
         */
        @Override
        public void redo() throws CannotRedoException {
            // First, invoke super.redo() to make sure we can redo
            super.redo();

            // Remove the child from its parent, again
            parent.remove(index);

            // Notify all listeners that the child was removed again
            int[] childIndex = {index};
            Object[] childObj = {child};
            nodesWereRemoved(parent, childIndex, childObj);
        }

        /* (non-Javadoc)
         * @see javax.swing.undo.AbstractUndoableEdit#undo()
         */
        @Override
        public void undo() throws CannotUndoException {
            // First, invoke super.undo() to make sure we can undo
            super.undo();

            // Put the child back with its parent
            parent.insert(child, index);

            // Notify all listeners that the node was readded
            int[] childIndex = {index};
            nodesWereInserted(parent, childIndex);
        }
    }
}

