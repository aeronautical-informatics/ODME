package odme.domain.graph;

import odme.domain.model.SESNodeType;

import java.util.List;

/**
 * Abstracts the SES graph structure so that domain operations (pruning,
 * validation, traversal) can be expressed without depending on mxGraph or
 * any other UI toolkit.
 *
 * <p>Implementations bridge this interface to whatever concrete graph
 * representation is in use (e.g. mxGraph, an in-memory adjacency list,
 * or a test double).</p>
 */
public interface SESGraph {

    /**
     * Returns the root node of the SES tree.
     *
     * @return the root node, or {@code null} if the graph is empty
     */
    SESGraphNode getRoot();

    /**
     * Returns the immediate children of the given node.
     *
     * @param node the parent node
     * @return an unmodifiable list of child nodes (never null, may be empty)
     */
    List<SESGraphNode> getChildren(SESGraphNode node);

    /**
     * Returns the parent of the given node.
     *
     * @param node the child node
     * @return the parent node, or {@code null} if {@code node} is the root
     */
    SESGraphNode getParent(SESGraphNode node);

    /**
     * Returns the display value (label) of a node.
     *
     * @param node the node to query
     * @return the string label of the node
     */
    String getValue(SESGraphNode node);

    /**
     * Determines the SES node type of the given node based on naming conventions.
     *
     * @param node the node to classify
     * @return the {@link SESNodeType} of the node
     */
    SESNodeType getNodeType(SESGraphNode node);

    /**
     * Checks whether the given node has a path back to the root.
     *
     * @param node the node to check
     * @return {@code true} if the node is reachable from the root
     */
    boolean isConnectedToRoot(SESGraphNode node);

    /**
     * Returns all nodes in the subtree rooted at the given node,
     * including the node itself, in depth-first order.
     *
     * @param node the subtree root
     * @return an unmodifiable list of all nodes in the subtree
     */
    List<SESGraphNode> getSubtree(SESGraphNode node);

    /**
     * Updates the display value (label) of a node.
     *
     * @param node  the node to update
     * @param value the new label value
     */
    void setValue(SESGraphNode node, String value);
}
