package odme.domain.graph;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link SESGraphNode}.
 */
class SESGraphNodeTest {

    // ── Creation ─────────────────────────────────────────────────────────────

    @Test
    void constructor_validArgs_createsNode() {
        SESGraphNode node = new SESGraphNode("id1", "Vehicle");

        assertThat(node.getId()).isEqualTo("id1");
        assertThat(node.getName()).isEqualTo("Vehicle");
    }

    @Test
    void constructor_nullId_throwsNPE() {
        assertThatThrownBy(() -> new SESGraphNode(null, "name"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    void constructor_nullName_throwsNPE() {
        assertThatThrownBy(() -> new SESGraphNode("id", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    // ── equals ───────────────────────────────────────────────────────────────

    @Test
    void equals_sameId_differentName_areEqual() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");
        SESGraphNode b = new SESGraphNode("id1", "Car");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void equals_differentId_notEqual() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");
        SESGraphNode b = new SESGraphNode("id2", "Vehicle");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void equals_sameInstance_equal() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");

        assertThat(a).isEqualTo(a);
    }

    @Test
    void equals_null_notEqual() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");

        assertThat(a).isNotEqualTo(null);
    }

    @Test
    void equals_differentType_notEqual() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");

        assertThat(a).isNotEqualTo("id1");
    }

    // ── hashCode ─────────────────────────────────────────────────────────────

    @Test
    void hashCode_sameId_sameHash() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");
        SESGraphNode b = new SESGraphNode("id1", "DifferentName");

        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void hashCode_differentId_differentHash() {
        SESGraphNode a = new SESGraphNode("id1", "Vehicle");
        SESGraphNode b = new SESGraphNode("id2", "Vehicle");

        // Not strictly required but expected for good hash distribution
        assertThat(a.hashCode()).isNotEqualTo(b.hashCode());
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    void toString_containsIdAndName() {
        SESGraphNode node = new SESGraphNode("abc", "Engine");

        String str = node.toString();
        assertThat(str).contains("abc");
        assertThat(str).contains("Engine");
    }
}
