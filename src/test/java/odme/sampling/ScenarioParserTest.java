package odme.sampling;

import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ScenarioParser}.
 * Each test writes a small YAML snippet to a temp file, then parses it.
 */
class ScenarioParserTest {

    @TempDir
    Path tempDir;

    private final ScenarioParser parser = new ScenarioParser();

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private Path writeYaml(String content) throws IOException {
        Path file = tempDir.resolve("scenario_" + System.nanoTime() + ".yaml");
        Files.writeString(file, content);
        return file;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Numerical parameters
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void parse_numericalParameter_extractsMinMax() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  EgoAC:\n" +
                "    Altitude:\n" +
                "      type: double\n" +
                "      min: 100\n" +
                "      max: 5000\n");

        Scenario scenario = parser.parse(f.toString());

        assertEquals(1, scenario.getParameters().size());
        Parameter p = scenario.getParameters().get(0);
        assertEquals("EgoAC_Altitude", p.getName());
        assertEquals("double", p.getType());
        assertEquals(100.0, p.getMin(), 1e-9);
        assertEquals(5000.0, p.getMax(), 1e-9);
    }

    @Test
    void parse_intParameter_typePreserved() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  Sensor:\n" +
                "    Resolution:\n" +
                "      type: int\n" +
                "      min: 200\n" +
                "      max: 800\n");

        Scenario scenario = parser.parse(f.toString());
        assertEquals(1, scenario.getParameters().size());
        Parameter p = scenario.getParameters().get(0);
        assertEquals("int", p.getType());
        assertEquals(200.0, p.getMin(), 1e-9);
        assertEquals(800.0, p.getMax(), 1e-9);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Categorical parameters
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void parse_categoricalWithOptions_extractsOptions() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  Weather:\n" +
                "    Condition:\n" +
                "      options: [Clear, Rainy, Foggy]\n");

        Scenario scenario = parser.parse(f.toString());
        assertEquals(1, scenario.getParameters().size());
        Parameter p = scenario.getParameters().get(0);
        assertEquals("categorical", p.getType());
        assertEquals(List.of("Clear", "Rainy", "Foggy"), p.getOptions());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Distribution parameters
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void parse_distributionParameter_extractsDistributionFields() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  Traffic:\n" +
                "    Density:\n" +
                "      type: distribution\n" +
                "      min: 0\n" +
                "      max: 100\n" +
                "      distributionName: normalDistribution\n" +
                "      distributionDetails: mean=50___stdDev=10\n");

        Scenario scenario = parser.parse(f.toString());
        assertEquals(1, scenario.getParameters().size());
        Parameter p = scenario.getParameters().get(0);
        assertEquals("distribution", p.getType());
        assertEquals("normalDistribution", p.getDistributionName());
        assertEquals("mean=50___stdDev=10", p.getDistributionDetails());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Constraints
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void parse_intraConstraint_addedToConstraintList() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  Entity:\n" +
                "    HasConstraint:\n" +
                "      IntraConstraint: if(@speed > 100) then (@altitude < 500) else true\n");

        Scenario scenario = parser.parse(f.toString());
        assertNotNull(scenario.getConstraint());
        assertEquals(1, scenario.getConstraint().size());
        assertTrue(scenario.getConstraint().get(0).contains("speed"));
    }

    @Test
    void parse_interConstraint_addedToConstraintList() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  Entity:\n" +
                "    HasConstraint:\n" +
                "      InterConstraint: if(@AC@speed > 50) then (@AC@altitude > 100) else true\n");

        Scenario scenario = parser.parse(f.toString());
        assertEquals(1, scenario.getConstraint().size());
        assertTrue(scenario.getConstraint().get(0).contains("AC@speed"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Multiple parameters
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void parse_multipleEntitiesAndParams_allExtracted() throws Exception {
        Path f = writeYaml(
                "Scenario:\n" +
                "  EgoAC:\n" +
                "    Speed:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 300\n" +
                "    Altitude:\n" +
                "      type: double\n" +
                "      min: 100\n" +
                "      max: 10000\n" +
                "  Traffic:\n" +
                "    Count:\n" +
                "      type: int\n" +
                "      min: 0\n" +
                "      max: 50\n");

        Scenario scenario = parser.parse(f.toString());
        assertEquals(3, scenario.getParameters().size());

        List<String> names = scenario.getParameters().stream()
                .map(Parameter::getName)
                .toList();
        assertTrue(names.contains("EgoAC_Speed"));
        assertTrue(names.contains("EgoAC_Altitude"));
        assertTrue(names.contains("Traffic_Count"));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void parse_emptyScenarioBlock_returnsEmptyParameters() throws Exception {
        Path f = writeYaml("Scenario:\n");
        Scenario scenario = parser.parse(f.toString());
        assertNotNull(scenario.getParameters());
        assertTrue(scenario.getParameters().isEmpty());
    }

    @Test
    void parse_constraintListResetBetweenParses() throws Exception {
        // First parse adds a constraint
        Path f1 = writeYaml(
                "Scenario:\n" +
                "  E:\n" +
                "    HasConstraint:\n" +
                "      IntraConstraint: if(@x > 0) then (@y < 10) else true\n");
        parser.parse(f1.toString());

        // Second parse has no constraints — list must be empty, not carry over
        Path f2 = writeYaml(
                "Scenario:\n" +
                "  E:\n" +
                "    Val:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 1\n");
        Scenario scenario2 = parser.parse(f2.toString());
        assertTrue(scenario2.getConstraint() == null || scenario2.getConstraint().isEmpty(),
                "Constraint list must be reset between parse() calls");
    }

    @Test
    void parse_nonExistentFile_throwsException() {
        assertThrows(Exception.class, () ->
                parser.parse(tempDir.resolve("does_not_exist.yaml").toString()));
    }
}
