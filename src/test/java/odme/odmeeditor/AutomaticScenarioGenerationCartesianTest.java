package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.sampling.CurrentModelScenarioBuilder;
import org.junit.jupiter.api.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AutomaticScenarioGenerationCartesianTest {

    private final CurrentModelScenarioBuilder builder = new CurrentModelScenarioBuilder();

    @Test
    void generateCartesianRowsBuildsCrossProductForNumericVariables() {
        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        TreePath aircraftPath = treePath("Scenario", "Aircraft");
        variables.put(aircraftPath, "Speed,double,50,0,100");
        variables.put(aircraftPath, "Altitude,int,1,1,2");

        CurrentModelScenarioBuilder.SamplingModel samplingModel =
                builder.buildFromMetadata(variables, ArrayListMultimap.create());

        List<Map<String, String>> rows = AutomaticScenarioGeneration.generateCartesianRows(samplingModel, 2);

        assertThat(rows).hasSize(4);
        assertThat(rows)
                .extracting(row -> row.get("Aircraft_Speed") + "|" + row.get("Aircraft_Altitude"))
                .containsExactlyInAnyOrder(
                        "0.0|1",
                        "0.0|2",
                        "100.0|1",
                        "100.0|2"
                );
    }

    @Test
    void generateCartesianRowsFiltersInvalidConstraintCombinations() {
        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        Multimap<TreePath, String> constraints = ArrayListMultimap.create();
        TreePath aircraftPath = treePath("Scenario", "Aircraft");
        variables.put(aircraftPath, "Speed,int,0,0,1");
        variables.put(aircraftPath, "Altitude,int,0,0,1");
        constraints.put(aircraftPath, "if(@Speed > -1) then (@Altitude > 0) else true");

        CurrentModelScenarioBuilder.SamplingModel samplingModel =
                builder.buildFromMetadata(variables, constraints);

        List<Map<String, String>> rows = AutomaticScenarioGeneration.generateCartesianRows(samplingModel, 2);

        assertThat(rows).hasSize(2);
        assertThat(rows)
                .extracting(row -> row.get("Aircraft_Altitude"))
                .containsOnly("1");
    }

    @Test
    void generateCartesianRowsExpandsBooleanVariablesWhenLevelsExceedOne() {
        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        TreePath environmentPath = treePath("Scenario", "Environment");
        variables.put(environmentPath, "MotionBlur,boolean,false");

        CurrentModelScenarioBuilder.SamplingModel samplingModel =
                builder.buildFromMetadata(variables, ArrayListMultimap.create());

        List<Map<String, String>> rows = AutomaticScenarioGeneration.generateCartesianRows(samplingModel, 2);

        assertThat(rows).hasSize(2);
        assertThat(rows)
                .extracting(row -> row.get("Environment_MotionBlur"))
                .containsExactly("false", "true");
    }

    private TreePath treePath(String rootName, String childName) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootName);
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(childName);
        root.add(child);
        return new TreePath(child.getPath());
    }
}
