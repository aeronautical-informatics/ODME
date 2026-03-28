package odme.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link SESTree} — the core domain model replacing the implicit
 * JTree/mxGraph dual-representation.
 */
class SESTreeTest {

    private SESTree tree;
    private SESNode aircraft;

    @BeforeEach
    void setUp() {
        tree = new SESTree("ses-001", "UAM_ODD");
        aircraft = new SESNode("aircraft", "Aircraft", SESNodeType.ENTITY);
        tree.setRoot(aircraft);
    }

    // ── Basic structure ───────────────────────────────────────────────────────

    @Test
    void newTree_hasCorrectIdAndName() {
        assertThat(tree.getId()).isEqualTo("ses-001");
        assertThat(tree.getName()).isEqualTo("UAM_ODD");
    }

    @Test
    void setRoot_rootIsAccessible() {
        assertThat(tree.getRoot()).isPresent();
        assertThat(tree.getRoot().get().getName()).isEqualTo("Aircraft");
    }

    @Test
    void emptyTree_isEmptyReturnsTrue() {
        SESTree empty = new SESTree("e", "Empty");
        assertThat(empty.isEmpty()).isTrue();
    }

    @Test
    void treeWithRoot_isEmptyReturnsFalse() {
        assertThat(tree.isEmpty()).isFalse();
    }

    // ── addNode ───────────────────────────────────────────────────────────────

    @Test
    void addNode_toRoot_increasesSize() {
        SESNode aspect = new SESNode("prop-dec", "PropulsionDec", SESNodeType.ASPECT);
        tree.addNode("aircraft", aspect);

        assertThat(tree.size()).isEqualTo(2);
        assertThat(aircraft.getChildren()).containsExactly(aspect);
    }

    @Test
    void addNode_unknownParent_throwsException() {
        SESNode node = new SESNode("x", "X", SESNodeType.ENTITY);

        assertThatThrownBy(() -> tree.addNode("nonexistent", node))
            .isInstanceOf(NoSuchElementException.class)
            .hasMessageContaining("nonexistent");
    }

    // ── removeNode ────────────────────────────────────────────────────────────

    @Test
    void removeNode_removesFromParent() {
        SESNode child = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        tree.addNode("aircraft", child);

        tree.removeNode("engine");

        assertThat(tree.size()).isEqualTo(1);
        assertThat(aircraft.getChildren()).isEmpty();
    }

    @Test
    void removeNode_root_throwsException() {
        assertThatThrownBy(() -> tree.removeNode("aircraft"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("root");
    }

    @Test
    void removeNode_removesSubtree() {
        SESNode aspect = new SESNode("dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode engine = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        tree.addNode("aircraft", aspect);
        tree.addNode("dec", engine);

        assertThat(tree.size()).isEqualTo(3);

        tree.removeNode("dec");

        assertThat(tree.size()).isEqualTo(1); // only root remains
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_existingNode_returnsNode() {
        SESNode engine = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        tree.addNode("aircraft", engine);

        assertThat(tree.findById("engine")).isPresent();
        assertThat(tree.findById("engine").get().getName()).isEqualTo("Engine");
    }

    @Test
    void findById_missingNode_returnsEmpty() {
        assertThat(tree.findById("nonexistent")).isEmpty();
    }

    // ── getLeafNodes ──────────────────────────────────────────────────────────

    @Test
    void getLeafNodes_singleRoot_returnsRoot() {
        assertThat(tree.getLeafNodes()).containsExactly(aircraft);
    }

    @Test
    void getLeafNodes_withChildren_returnsOnlyLeaves() {
        SESNode aspect = new SESNode("dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode engine = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        SESNode battery = new SESNode("battery", "Battery", SESNodeType.ENTITY);
        tree.addNode("aircraft", aspect);
        tree.addNode("dec", engine);
        tree.addNode("dec", battery);

        assertThat(tree.getLeafNodes()).containsExactlyInAnyOrder(engine, battery);
        assertThat(tree.getLeafNodes()).doesNotContain(aircraft, aspect);
    }

    // ── getAllNodes ───────────────────────────────────────────────────────────

    @Test
    void getAllNodes_returnsAllInPreOrder() {
        SESNode aspect = new SESNode("dec", "PropulsionDec", SESNodeType.ASPECT);
        SESNode engine = new SESNode("engine", "Engine", SESNodeType.ENTITY);
        tree.addNode("aircraft", aspect);
        tree.addNode("dec", engine);

        assertThat(tree.getAllNodes()).containsExactly(aircraft, aspect, engine);
    }
}
