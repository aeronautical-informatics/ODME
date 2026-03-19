package odme.domain.operations;

import odme.domain.model.SESNodeType;
import odme.domain.model.SESNode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NodeFactoryTest {

    @Test
    void create_returnsNodeWithCorrectNameAndType() {
        NodeFactory factory = new NodeFactory();
        SESNode node = factory.create("Aircraft", SESNodeType.ENTITY);

        assertThat(node.getName()).isEqualTo("Aircraft");
        assertThat(node.getType()).isEqualTo(SESNodeType.ENTITY);
    }

    @Test
    void create_generatesUniqueIds() {
        NodeFactory factory = new NodeFactory();
        SESNode n1 = factory.create("Engine", SESNodeType.ENTITY);
        SESNode n2 = factory.create("Engine", SESNodeType.ENTITY);

        assertThat(n1.getId()).isNotEqualTo(n2.getId());
    }

    @Test
    void create_idContainsNameAndType() {
        NodeFactory factory = new NodeFactory();
        SESNode node = factory.create("Propulsion", SESNodeType.ASPECT);

        assertThat(node.getId()).contains("Propulsion");
        assertThat(node.getId()).contains("aspect");
    }
}
