package odme.domain.prune;

import odme.domain.graph.SESGraph;
import odme.domain.graph.SESGraphNode;
import odme.domain.model.SESNodeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Implements the SES pruning algorithms against the {@link SESGraph}
 * abstraction, free of any UI or mxGraph dependency.
 *
 * <p>Pruning is the process of converting an SES (System Entity Structure)
 * into a PES (Pruned Entity Structure) by selecting one alternative at
 * each specialization node and choosing a count for each multi-aspect
 * node.</p>
 *
 * <p>Extracted from {@code odme.jtreetograph.JtreeToGraphPrune}.</p>
 */
public class PruneEngine {

    /**
     * Result of a pruning operation, containing the nodes that were kept,
     * the nodes that were removed, and any variables/behaviours that need
     * to be transferred.
     */
    public static class PruneResult {
        private final SESGraphNode selectedChild;
        private final List<SESGraphNode> removedNodes;
        private final List<String> transferredVariables;
        private final List<String> transferredBehaviours;
        private final String newParentName;

        public PruneResult(SESGraphNode selectedChild,
                           List<SESGraphNode> removedNodes,
                           List<String> transferredVariables,
                           List<String> transferredBehaviours,
                           String newParentName) {
            this.selectedChild = selectedChild;
            this.removedNodes = List.copyOf(removedNodes);
            this.transferredVariables = List.copyOf(transferredVariables);
            this.transferredBehaviours = List.copyOf(transferredBehaviours);
            this.newParentName = newParentName;
        }

        /** The child that was selected (kept) during pruning. */
        public SESGraphNode getSelectedChild() { return selectedChild; }

        /** Nodes that should be removed from the graph. */
        public List<SESGraphNode> getRemovedNodes() { return removedNodes; }

        /** Variable definitions to transfer to the parent. */
        public List<String> getTransferredVariables() { return transferredVariables; }

        /** Behaviour definitions to transfer to the parent. */
        public List<String> getTransferredBehaviours() { return transferredBehaviours; }

        /** The new name for the parent node after pruning. */
        public String getNewParentName() { return newParentName; }
    }

    /**
     * Result of a multi-aspect pruning operation.
     */
    public static class MultiAspectPruneResult {
        private final int instanceCount;
        private final List<String> instanceNames;
        private final String aspectNodeName;

        public MultiAspectPruneResult(int instanceCount,
                                       List<String> instanceNames,
                                       String aspectNodeName) {
            this.instanceCount = instanceCount;
            this.instanceNames = List.copyOf(instanceNames);
            this.aspectNodeName = aspectNodeName;
        }

        /** The number of instances requested. */
        public int getInstanceCount() { return instanceCount; }

        /** The generated names for each instance (e.g. "wheel_1", "wheel_2"). */
        public List<String> getInstanceNames() { return instanceNames; }

        /** The aspect node name created for the multi-aspect decomposition. */
        public String getAspectNodeName() { return aspectNodeName; }
    }

    /**
     * Prunes a specialization node by selecting one child and removing
     * the specialization node along with all non-selected siblings.
     *
     * <p>Algorithm (from JtreeToGraphPrune.pruneNodeFromGraphPopup):
     * <ol>
     *   <li>Identify the parent of the specialization node.</li>
     *   <li>Find the selected child among the specialization's children.</li>
     *   <li>Determine if the selected child has its own children (subtree).</li>
     *   <li>Collect all variables and behaviours from the selected child.</li>
     *   <li>Compute the new parent name: "{selectedChild}_{parentName}".</li>
     *   <li>Mark the specialization node and non-selected siblings for removal.</li>
     * </ol>
     *
     * @param graph             the SES graph
     * @param specializationNode the specialization node to prune
     * @param selectedChildName  the name of the child to keep
     * @return the pruning result describing what to keep, remove, and transfer
     * @throws IllegalArgumentException if the node or child is not found
     */
    public PruneResult pruneSpecialization(SESGraph graph,
                                           SESGraphNode specializationNode,
                                           String selectedChildName) {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(specializationNode, "specializationNode must not be null");
        Objects.requireNonNull(selectedChildName, "selectedChildName must not be null");

        SESGraphNode parent = graph.getParent(specializationNode);
        if (parent == null) {
            throw new IllegalArgumentException("Specialization node has no parent");
        }

        List<SESGraphNode> children = graph.getChildren(specializationNode);
        SESGraphNode selectedChild = null;
        List<SESGraphNode> removedNodes = new ArrayList<>();

        for (SESGraphNode child : children) {
            if (graph.getValue(child).equals(selectedChildName)) {
                selectedChild = child;
            }
        }

        if (selectedChild == null) {
            throw new IllegalArgumentException(
                    "Child '" + selectedChildName + "' not found under specialization node");
        }

        // All nodes in the specialization subtree except the selected child's subtree
        // are marked for removal
        for (SESGraphNode child : children) {
            if (!child.equals(selectedChild)) {
                removedNodes.addAll(graph.getSubtree(child));
            }
        }
        // The specialization node itself is also removed
        removedNodes.add(specializationNode);

        // The parent gets renamed: selectedChild + "_" + parent
        String newParentName = selectedChildName + "_" + graph.getValue(parent);

        // Variables and behaviours would be collected from the selected child
        // and transferred to the parent. The actual variable/behaviour data
        // is UI-layer concern; we return empty lists here as placeholders.
        List<String> transferredVariables = new ArrayList<>();
        List<String> transferredBehaviours = new ArrayList<>();

        return new PruneResult(selectedChild, removedNodes,
                transferredVariables, transferredBehaviours, newParentName);
    }

    /**
     * Prunes a multi-aspect node by replacing it with a specified number
     * of entity instances under a new aspect node.
     *
     * <p>Algorithm (from JtreeToGraphPrune.pruneMAspNodeFromGraphPopup):
     * <ol>
     *   <li>Remove the "MAsp" suffix to get the base name.</li>
     *   <li>Create an aspect node named "{baseName}Dec" under the parent.</li>
     *   <li>Create {@code count} entity instances under the aspect node,
     *       named "{baseName}_{1}", "{baseName}_{2}", etc.</li>
     *   <li>Transfer all variables and behaviours from the original
     *       multi-aspect node to each new instance.</li>
     * </ol>
     *
     * @param graph          the SES graph
     * @param multiAspectNode the multi-aspect node to prune
     * @param count           the number of entity instances to create (must be >= 1)
     * @return the pruning result describing the new structure
     * @throws IllegalArgumentException if count is less than 1
     */
    public MultiAspectPruneResult pruneMultiAspect(SESGraph graph,
                                                    SESGraphNode multiAspectNode,
                                                    int count) {
        Objects.requireNonNull(graph, "graph must not be null");
        Objects.requireNonNull(multiAspectNode, "multiAspectNode must not be null");
        if (count < 1) {
            throw new IllegalArgumentException("Instance count must be at least 1, got: " + count);
        }

        String maspName = graph.getValue(multiAspectNode);
        String baseName = NamingConventions.removeSuffix(maspName);
        String aspectNodeName = baseName + NamingConventions.ASPECT_SUFFIX;

        List<String> instanceNames = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            instanceNames.add(baseName + "_" + i);
        }

        return new MultiAspectPruneResult(count, instanceNames, aspectNodeName);
    }

    /**
     * Determines whether a node requires pruning (i.e. it is a specialization
     * or multi-aspect node).
     *
     * @param graph the SES graph
     * @param node  the node to check
     * @return {@code true} if the node should be pruned
     */
    public boolean requiresPruning(SESGraph graph, SESGraphNode node) {
        SESNodeType type = graph.getNodeType(node);
        return type == SESNodeType.SPECIALIZATION || type == SESNodeType.MULTI_ASPECT;
    }

    /**
     * Finds all nodes in the graph that require pruning (all specialization
     * and multi-aspect nodes).
     *
     * @param graph the SES graph
     * @return a list of nodes that need to be pruned
     */
    public List<SESGraphNode> findPrunableNodes(SESGraph graph) {
        Objects.requireNonNull(graph, "graph must not be null");
        SESGraphNode root = graph.getRoot();
        if (root == null) {
            return List.of();
        }
        List<SESGraphNode> result = new ArrayList<>();
        for (SESGraphNode node : graph.getSubtree(root)) {
            if (requiresPruning(graph, node)) {
                result.add(node);
            }
        }
        return result;
    }
}
