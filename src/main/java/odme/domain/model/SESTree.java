package odme.domain.model;

import java.time.Instant;
import java.util.*;

/**
 * A System Entity Structure (SES) — the complete, unmodified domain model
 * representing all possible system configurations.
 *
 * <p>An SES is a rooted, labelled tree. From it, one or more
 * {@link PESTree} instances (Possible Entity Structures) can be derived
 * by the pruning operation.</p>
 *
 * <p>This class is the single source of truth for the SES model. Both the
 * JTree view and the mxGraph view are projections of this object.</p>
 */
public class SESTree {

    private final String id;
    private String name;
    private SESNode root;
    private final Instant createdAt;
    private Instant lastModifiedAt;
    private String schemaVersion;

    private static final String CURRENT_SCHEMA_VERSION = "1.0";

    public SESTree(String id, String name) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.createdAt = Instant.now();
        this.lastModifiedAt = this.createdAt;
        this.schemaVersion = CURRENT_SCHEMA_VERSION;
    }

    // ── Identity ─────────────────────────────────────────────────────────────

    public String getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name);
        touch();
    }

    public String getSchemaVersion() { return schemaVersion; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getLastModifiedAt() { return lastModifiedAt; }

    // ── Root management ───────────────────────────────────────────────────────

    public Optional<SESNode> getRoot() {
        return Optional.ofNullable(root);
    }

    public void setRoot(SESNode root) {
        this.root = root;
        if (root != null) {
            root.setParent(null);
        }
        touch();
    }

    public boolean isEmpty() {
        return root == null;
    }

    // ── Node operations ───────────────────────────────────────────────────────

    /**
     * Adds a new child node to a parent node identified by ID.
     *
     * @param parentId the ID of the parent node
     * @param child    the new node to add as a child
     * @throws NoSuchElementException if parentId is not found in the tree
     */
    public void addNode(String parentId, SESNode child) {
        SESNode parent = findById(parentId)
            .orElseThrow(() -> new NoSuchElementException("Node not found: " + parentId));
        parent.addChild(child);
        touch();
    }

    /**
     * Removes a node and its entire subtree.
     *
     * @param nodeId the ID of the node to remove
     * @throws NoSuchElementException if nodeId is not found
     * @throws IllegalStateException  if nodeId refers to the root node
     */
    public void removeNode(String nodeId) {
        SESNode node = findById(nodeId)
            .orElseThrow(() -> new NoSuchElementException("Node not found: " + nodeId));
        if (node.isRoot()) {
            throw new IllegalStateException("Cannot remove the root node");
        }
        node.getParent().removeChild(node);
        touch();
    }

    /**
     * Finds a node by its unique ID anywhere in the tree.
     */
    public Optional<SESNode> findById(String id) {
        if (root == null) return Optional.empty();
        return root.getSubtree().stream()
            .filter(n -> n.getId().equals(id))
            .findFirst();
    }

    /**
     * Finds nodes by name (case-sensitive, may return multiple if names clash).
     */
    public List<SESNode> findByName(String name) {
        if (root == null) return List.of();
        return root.getSubtree().stream()
            .filter(n -> n.getName().equals(name))
            .toList();
    }

    /**
     * Returns all nodes in the tree in pre-order.
     */
    public List<SESNode> getAllNodes() {
        if (root == null) return List.of();
        return root.getSubtree();
    }

    /**
     * Returns all leaf nodes in the tree (entities with no children).
     * Leaf nodes represent concrete system configurations.
     */
    public List<SESNode> getLeafNodes() {
        if (root == null) return List.of();
        return root.getLeafNodes();
    }

    /**
     * Returns the total number of nodes in the tree.
     */
    public int size() {
        return getAllNodes().size();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void touch() {
        this.lastModifiedAt = Instant.now();
    }

    // ── Object overrides ──────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format("SESTree{id='%s', name='%s', nodes=%d}", id, name, size());
    }
}
