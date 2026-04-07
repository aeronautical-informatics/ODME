package ses.testdata;

import ses.EntityStructure;
import ses.Node;
import ses.NodeType;
import ses.Variable;
import ses.VariableType;

/**
 * Pre-built trees for use across test classes.
 * Call the static method to get a fresh instance.
 */
public class TestData {

    // -------------------------------------------------------------------------
    // Valid trees
    // -------------------------------------------------------------------------

    /** A well-formed SES tree. Used for happy-path and serializer round-trip tests. */
    public static EntityStructure validVehicleTree() {
        Node root = new Node("Vehicle", NodeType.ENTITY)
            .withChild(new Node("VehicleDecomposition", NodeType.ASPECT)
                .withChild(new Node("PowerSource", NodeType.ENTITY)
                    .withChild(new Node("PowerSourceType", NodeType.SPECIALIZATION)
                        .withChild(new Node("Electric", NodeType.ENTITY))
                        .withChild(new Node("Combustion", NodeType.ENTITY))))
                .withChild(new Node("Driver", NodeType.ENTITY)));
        return new EntityStructure("VehicleModel", root);
    }

    /** A tree containing all four node types. Used for serializer type round-trip tests. */
    public static EntityStructure allNodeTypes() {
        Node root = new Node("System", NodeType.ENTITY)
            .withChild(new Node("SystemDecomposition", NodeType.ASPECT)
                .withChild(new Node("Component", NodeType.ENTITY)
                    .withChild(new Node("ComponentVariants", NodeType.SPECIALIZATION)
                        .withChild(new Node("VariantA", NodeType.ENTITY)))
                    .withChild(new Node("ComponentInstances", NodeType.MULTI_ASPECT)
                        .withChild(new Node("Instance", NodeType.ENTITY)))));
        return new EntityStructure("AllTypesModel", root);
    }

    /** A tree with an INT variable with lower and upper bounds. */
    public static EntityStructure withVariableAndBounds() {
        Variable speed = new Variable("speed", VariableType.INT);
        speed.setDefaultValue("60");
        speed.setLowerBound(0.0);
        speed.setUpperBound(200.0);

        Node root = new Node("Vehicle", NodeType.ENTITY);
        root.addVariable(speed);
        return new EntityStructure("VariableModel", root);
    }

    // -------------------------------------------------------------------------
    // Axiom violation trees
    // -------------------------------------------------------------------------

    /** Two siblings share the same name — violates Valid Brothers. */
    public static Node duplicateSiblings() {
        return new Node("Vehicle", NodeType.ENTITY)
            .withChild(new Node("VehicleDecomposition", NodeType.ASPECT)
                .withChild(new Node("Engine", NodeType.ENTITY))
                .withChild(new Node("Engine", NodeType.ENTITY)));
    }

    /** An Entity is a direct child of another Entity — violates Alternating Mode. */
    public static Node entityChildOfEntity() {
        return new Node("Vehicle", NodeType.ENTITY)
            .withChild(new Node("Engine", NodeType.ENTITY));
    }

    /** The same name appears twice on a root-to-leaf path — violates Strict Hierarchy. */
    public static Node nameRepeatedOnPath() {
        return new Node("Vehicle", NodeType.ENTITY)
            .withChild(new Node("VehicleDecomposition", NodeType.ASPECT)
                .withChild(new Node("Vehicle", NodeType.ENTITY)));
    }

    /** Same name in two branches, different subtrees — violates Uniformity. */
    public static Node nonIsomorphicSameName() {
        return new Node("System", NodeType.ENTITY)
            .withChild(new Node("SystemDecomposition", NodeType.ASPECT)
                .withChild(new Node("Component", NodeType.ENTITY)
                    .withChild(new Node("ComponentParts", NodeType.ASPECT)
                        .withChild(new Node("Part", NodeType.ENTITY))))
                .withChild(new Node("Component", NodeType.ENTITY)));
                // Second "Component" has no children — non-isomorphic with the first
    }

    /** One node has two variables with the same name — violates Attached Variables. */
    public static Node duplicateVariable() {
        Node root = new Node("Vehicle", NodeType.ENTITY);
        root.addVariable(new Variable("speed", VariableType.INT));
        root.addVariable(new Variable("speed", VariableType.FLOAT));
        return root;
    }
}
