package odme.domain.model;

import java.util.*;

/**
 * A node in a System Entity Structure (SES) tree.
 *
 * <p>This is the central domain object replacing the implicit node representation
 * that previously existed only as JTree {@code DefaultMutableTreeNode} instances
 * with type information encoded in the label string.</p>
 *
 * <p>SES nodes form a tree: each node has exactly one parent (except the root)
 * and zero or more children. The tree structure is managed by {@link SESTree}.</p>
 */
public class SESNode {

    private final String id;
    private String name;
    private SESNodeType type;
    private SESNode parent;
    private final List<SESNode> children;
    private final Map<String, String> variables;
    private final List<String> behaviours;
    private final List<String> constraints;
    private final Map<String, String> flags;

    /**
     * Creates a new SES node.
     *
     * @param id   unique identifier within the tree (typically matches the name for now)
     * @param name the human-readable name (the "base name" without type suffix)
     * @param type the SES node type
     */
    public SESNode(String id, String name, SESNodeType type) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.children = new ArrayList<>();
        this.variables = new LinkedHashMap<>();
        this.behaviours = new ArrayList<>();
        this.constraints = new ArrayList<>();
        this.flags = new LinkedHashMap<>();
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    public String getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public SESNodeType getType() { return type; }

    public void setType(SESNodeType type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    /**
     * Returns the label as it appears in the JTree (name + type suffix).
     * E.g., for an ASPECT node named "Propulsion", returns "PropulsionDec".
     */
    public String getLabel() {
        return type.toLabel(name);
    }

    // ── Tree structure ────────────────────────────────────────────────────────

    public SESNode getParent() { return parent; }

    void setParent(SESNode parent) { this.parent = parent; }

    public List<SESNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isRoot() { return parent == null; }

    public boolean isLeaf() { return children.isEmpty(); }

    public int getDepth() {
        int depth = 0;
        SESNode current = this;
        while (current.parent != null) {
            depth++;
            current = current.parent;
        }
        return depth;
    }

    /**
     * Returns the full path from root to this node as a "/" separated string.
     */
    public String getPath() {
        if (isRoot()) return "/" + name;
        return parent.getPath() + "/" + name;
    }

    void addChild(SESNode child) {
        children.add(child);
        child.setParent(this);
    }

    void removeChild(SESNode child) {
        children.remove(child);
        child.setParent(null);
    }

    // ── Metadata ──────────────────────────────────────────────────────────────

    public Map<String, String> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public void putVariable(String name, String value) {
        variables.put(
            Objects.requireNonNull(name, "variable name must not be null"),
            Objects.requireNonNull(value, "variable value must not be null")
        );
    }

    public void removeVariable(String name) {
        variables.remove(name);
    }

    public List<String> getBehaviours() {
        return Collections.unmodifiableList(behaviours);
    }

    public void addBehaviour(String behaviour) {
        if (behaviour != null && !behaviour.isBlank()) {
            behaviours.add(behaviour);
        }
    }

    public List<String> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    public void addConstraint(String constraint) {
        if (constraint != null && !constraint.isBlank()) {
            constraints.add(constraint);
        }
    }

    public Map<String, String> getFlags() {
        return Collections.unmodifiableMap(flags);
    }

    public void putFlag(String key, String value) {
        flags.put(key, value);
    }

    // ── Traversal helpers ─────────────────────────────────────────────────────

    /**
     * Returns all leaf nodes in the subtree rooted at this node (inclusive).
     */
    public List<SESNode> getLeafNodes() {
        List<SESNode> leaves = new ArrayList<>();
        collectLeaves(this, leaves);
        return leaves;
    }

    private void collectLeaves(SESNode node, List<SESNode> leaves) {
        if (node.isLeaf()) {
            leaves.add(node);
        } else {
            for (SESNode child : node.children) {
                collectLeaves(child, leaves);
            }
        }
    }

    /**
     * Returns all nodes in the subtree rooted at this node (inclusive), in pre-order.
     */
    public List<SESNode> getSubtree() {
        List<SESNode> result = new ArrayList<>();
        collectSubtree(this, result);
        return result;
    }

    private void collectSubtree(SESNode node, List<SESNode> result) {
        result.add(node);
        for (SESNode child : node.children) {
            collectSubtree(child, result);
        }
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SESNode other)) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("SESNode{id='%s', name='%s', type=%s, children=%d}",
            id, name, type, children.size());
    }
}
