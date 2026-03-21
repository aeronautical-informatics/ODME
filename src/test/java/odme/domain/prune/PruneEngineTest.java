package odme.domain.prune;

import odme.domain.graph.SESGraph;
import odme.domain.graph.SESGraphNode;
import odme.domain.model.SESNodeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link PruneEngine}.
 */
class PruneEngineTest {

    private PruneEngine engine;

    // ── In-memory SESGraph implementation for testing ─────────────────────────

    /**
     * Simple in-memory graph implementation backed by parent-child maps.
     */
    static class InMemoryGraph implements SESGraph {
        private SESGraphNode root;
        private final Map<SESGraphNode, List<SESGraphNode>> children = new LinkedHashMap<>();
        private final Map<SESGraphNode, SESGraphNode> parents = new LinkedHashMap<>();
        private final Map<SESGraphNode, String> values = new LinkedHashMap<>();

        void setRoot(SESGraphNode node) {
            this.root = node;
            values.put(node, node.getName());
        }

        void addChild(SESGraphNode parent, SESGraphNode child) {
            children.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
            parents.put(child, parent);
            values.put(child, child.getName());
        }

        @Override
        public SESGraphNode getRoot() {
            return root;
        }

        @Override
        public List<SESGraphNode> getChildren(SESGraphNode node) {
            return Collections.unmodifiableList(
                    children.getOrDefault(node, List.of()));
        }

        @Override
        public SESGraphNode getParent(SESGraphNode node) {
            return parents.get(node);
        }

        @Override
        public String getValue(SESGraphNode node) {
            return values.getOrDefault(node, node.getName());
        }

        @Override
        public SESNodeType getNodeType(SESGraphNode node) {
            return SESNodeType.fromLabel(getValue(node));
        }

        @Override
        public boolean isConnectedToRoot(SESGraphNode node) {
            SESGraphNode current = node;
            while (current != null) {
                if (current.equals(root)) return true;
                current = parents.get(current);
            }
            return false;
        }

        @Override
        public List<SESGraphNode> getSubtree(SESGraphNode node) {
            List<SESGraphNode> result = new ArrayList<>();
            collectSubtree(node, result);
            return Collections.unmodifiableList(result);
        }

        private void collectSubtree(SESGraphNode node, List<SESGraphNode> acc) {
            acc.add(node);
            for (SESGraphNode child : children.getOrDefault(node, List.of())) {
                collectSubtree(child, acc);
            }
        }

        @Override
        public void setValue(SESGraphNode node, String value) {
            values.put(node, value);
        }
    }

    @BeforeEach
    void setUp() {
        engine = new PruneEngine();
    }

    // ── Helper to build a spec tree ──────────────────────────────────────────

    /**
     * Builds: Vehicle -> VehicleSpec -> [Car, Truck, Motorcycle]
     */
    private InMemoryGraph buildSpecializationTree() {
        InMemoryGraph graph = new InMemoryGraph();

        SESGraphNode vehicle = new SESGraphNode("1", "Vehicle");
        SESGraphNode vehicleSpec = new SESGraphNode("2", "VehicleSpec");
        SESGraphNode car = new SESGraphNode("3", "Car");
        SESGraphNode truck = new SESGraphNode("4", "Truck");
        SESGraphNode motorcycle = new SESGraphNode("5", "Motorcycle");

        graph.setRoot(vehicle);
        graph.addChild(vehicle, vehicleSpec);
        graph.addChild(vehicleSpec, car);
        graph.addChild(vehicleSpec, truck);
        graph.addChild(vehicleSpec, motorcycle);

        return graph;
    }

    // ── pruneSpecialization ──────────────────────────────────────────────────

    @Test
    void pruneSpecialization_selectsOneChild_removesOthers() {
        InMemoryGraph graph = buildSpecializationTree();
        SESGraphNode vehicleSpec = new SESGraphNode("2", "VehicleSpec");

        PruneEngine.PruneResult result = engine.pruneSpecialization(
                graph, vehicleSpec, "Car");

        assertThat(result.getSelectedChild().getName()).isEqualTo("Car");
        // The spec node plus Truck and Motorcycle should be removed
        assertThat(result.getRemovedNodes()).hasSize(3);
        List<String> removedNames = result.getRemovedNodes().stream()
                .map(SESGraphNode::getName)
                .toList();
        assertThat(removedNames).contains("Truck", "Motorcycle", "VehicleSpec");
        assertThat(removedNames).doesNotContain("Car");
    }

    @Test
    void pruneSpecialization_computesNewParentName() {
        InMemoryGraph graph = buildSpecializationTree();
        SESGraphNode vehicleSpec = new SESGraphNode("2", "VehicleSpec");

        PruneEngine.PruneResult result = engine.pruneSpecialization(
                graph, vehicleSpec, "Car");

        assertThat(result.getNewParentName()).isEqualTo("Car_Vehicle");
    }

    @Test
    void pruneSpecialization_childNotFound_throwsException() {
        InMemoryGraph graph = buildSpecializationTree();
        SESGraphNode vehicleSpec = new SESGraphNode("2", "VehicleSpec");

        assertThatThrownBy(() ->
                engine.pruneSpecialization(graph, vehicleSpec, "Bicycle"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Bicycle");
    }

    @Test
    void pruneSpecialization_noParent_throwsException() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode root = new SESGraphNode("1", "RootSpec");
        graph.setRoot(root);

        assertThatThrownBy(() ->
                engine.pruneSpecialization(graph, root, "Child"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no parent");
    }

    @Test
    void pruneSpecialization_nullGraph_throwsNPE() {
        SESGraphNode node = new SESGraphNode("1", "Spec");
        assertThatThrownBy(() ->
                engine.pruneSpecialization(null, node, "child"))
                .isInstanceOf(NullPointerException.class);
    }

    // ── pruneSpecialization preserves subtree of selected child ──────────────

    @Test
    void pruneSpecialization_selectedChildWithSubtree_preservesSubtree() {
        InMemoryGraph graph = buildSpecializationTree();
        // Add subtree under Car
        SESGraphNode carEngine = new SESGraphNode("6", "CarEngine");
        graph.addChild(new SESGraphNode("3", "Car"), carEngine);

        SESGraphNode vehicleSpec = new SESGraphNode("2", "VehicleSpec");
        PruneEngine.PruneResult result = engine.pruneSpecialization(
                graph, vehicleSpec, "Car");

        // Car's subtree should not be in removed nodes
        List<String> removedIds = result.getRemovedNodes().stream()
                .map(SESGraphNode::getId)
                .toList();
        assertThat(removedIds).doesNotContain("3", "6");
    }

    // ── pruneMultiAspect ─────────────────────────────────────────────────────

    @Test
    void pruneMultiAspect_createsCorrectInstanceNames() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode parent = new SESGraphNode("1", "Vehicle");
        SESGraphNode wheelMAsp = new SESGraphNode("2", "WheelMAsp");
        graph.setRoot(parent);
        graph.addChild(parent, wheelMAsp);

        PruneEngine.MultiAspectPruneResult result =
                engine.pruneMultiAspect(graph, wheelMAsp, 4);

        assertThat(result.getInstanceCount()).isEqualTo(4);
        assertThat(result.getInstanceNames())
                .containsExactly("Wheel_1", "Wheel_2", "Wheel_3", "Wheel_4");
        assertThat(result.getAspectNodeName()).isEqualTo("WheelDec");
    }

    @Test
    void pruneMultiAspect_singleInstance_works() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode root = new SESGraphNode("1", "Root");
        SESGraphNode sensorMAsp = new SESGraphNode("2", "SensorMAsp");
        graph.setRoot(root);
        graph.addChild(root, sensorMAsp);

        PruneEngine.MultiAspectPruneResult result =
                engine.pruneMultiAspect(graph, sensorMAsp, 1);

        assertThat(result.getInstanceNames()).containsExactly("Sensor_1");
    }

    @Test
    void pruneMultiAspect_zeroCount_throwsException() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode node = new SESGraphNode("1", "ThingMAsp");
        graph.setRoot(node);

        assertThatThrownBy(() ->
                engine.pruneMultiAspect(graph, node, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 1");
    }

    // ── requiresPruning ──────────────────────────────────────────────────────

    @Test
    void requiresPruning_specializationNode_returnsTrue() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode spec = new SESGraphNode("1", "VehicleSpec");
        graph.setRoot(spec);

        assertThat(engine.requiresPruning(graph, spec)).isTrue();
    }

    @Test
    void requiresPruning_multiAspectNode_returnsTrue() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode masp = new SESGraphNode("1", "WheelMAsp");
        graph.setRoot(masp);

        assertThat(engine.requiresPruning(graph, masp)).isTrue();
    }

    @Test
    void requiresPruning_entityNode_returnsFalse() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode entity = new SESGraphNode("1", "Vehicle");
        graph.setRoot(entity);

        assertThat(engine.requiresPruning(graph, entity)).isFalse();
    }

    @Test
    void requiresPruning_aspectNode_returnsFalse() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode aspect = new SESGraphNode("1", "VehicleDec");
        graph.setRoot(aspect);

        assertThat(engine.requiresPruning(graph, aspect)).isFalse();
    }

    // ── findPrunableNodes ────────────────────────────────────────────────────

    @Test
    void findPrunableNodes_mixedTree_returnsOnlyPrunableNodes() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode vehicle = new SESGraphNode("1", "Vehicle");
        SESGraphNode vehicleDec = new SESGraphNode("2", "VehicleDec");
        SESGraphNode powerSpec = new SESGraphNode("3", "PowerSpec");
        SESGraphNode wheelMAsp = new SESGraphNode("4", "WheelMAsp");
        SESGraphNode engineNode = new SESGraphNode("5", "Engine");

        graph.setRoot(vehicle);
        graph.addChild(vehicle, vehicleDec);
        graph.addChild(vehicleDec, powerSpec);
        graph.addChild(vehicleDec, wheelMAsp);
        graph.addChild(vehicleDec, engineNode);

        List<SESGraphNode> prunable = this.engine.findPrunableNodes(graph);

        assertThat(prunable).hasSize(2);
        List<String> names = prunable.stream()
                .map(SESGraphNode::getName)
                .toList();
        assertThat(names).containsExactlyInAnyOrder("PowerSpec", "WheelMAsp");
    }

    @Test
    void findPrunableNodes_emptyGraph_returnsEmptyList() {
        InMemoryGraph graph = new InMemoryGraph();
        // root is null

        List<SESGraphNode> prunable = engine.findPrunableNodes(graph);

        assertThat(prunable).isEmpty();
    }

    @Test
    void findPrunableNodes_noPrunableNodes_returnsEmptyList() {
        InMemoryGraph graph = new InMemoryGraph();
        SESGraphNode vehicle = new SESGraphNode("1", "Vehicle");
        SESGraphNode vehicleDec = new SESGraphNode("2", "VehicleDec");
        graph.setRoot(vehicle);
        graph.addChild(vehicle, vehicleDec);

        List<SESGraphNode> prunable = engine.findPrunableNodes(graph);

        assertThat(prunable).isEmpty();
    }

    // ── PruneResult immutability ─────────────────────────────────────────────

    @Test
    void pruneResult_listsAreImmutable() {
        InMemoryGraph graph = buildSpecializationTree();
        SESGraphNode vehicleSpec = new SESGraphNode("2", "VehicleSpec");

        PruneEngine.PruneResult result = engine.pruneSpecialization(
                graph, vehicleSpec, "Car");

        assertThatThrownBy(() -> result.getRemovedNodes().add(new SESGraphNode("x", "x")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
