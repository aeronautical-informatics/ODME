package odme.sampling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style unit tests for {@link SamplingManager}.
 * Uses small in-memory YAML files written to a temp directory.
 */
class SamplingManagerTest {

    @TempDir
    Path tempDir;

    private final SamplingManager manager = new SamplingManager();

    // ────────────────────────────────────────────────────────────────────────
    // Helpers
    // ────────────────────────────────────────────────────────────────────────

    private Path writeYaml(String content) throws IOException {
        Path file = tempDir.resolve("sc_" + System.nanoTime() + ".yaml");
        Files.writeString(file, content);
        return file;
    }

    private Path outputCsv() {
        return tempDir.resolve("out_" + System.nanoTime() + ".csv");
    }

    // ────────────────────────────────────────────────────────────────────────
    // generateSamples — unconstrained
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void generateSamples_noConstraint_producesCorrectRowCount() throws Exception {
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  EgoAC:\n" +
                "    Speed:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 300\n" +
                "    Altitude:\n" +
                "      type: double\n" +
                "      min: 100\n" +
                "      max: 10000\n");

        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 10, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals(11, lines.size(), "Expected header + 10 data rows");
    }

    @Test
    void generateSamples_noConstraint_headerContainsParameterNames() throws Exception {
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  EgoAC:\n" +
                "    Speed:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 300\n");

        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 5, csv.toString());

        String header = Files.readAllLines(csv).get(0);
        assertTrue(header.contains("EgoAC_Speed"), "Header must contain parameter name");
    }

    @Test
    void generateSamples_noConstraint_valuesWithinBounds() throws Exception {
        double min = 10.0, max = 50.0;
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  Sensor:\n" +
                "    Range:\n" +
                "      type: double\n" +
                "      min: " + min + "\n" +
                "      max: " + max + "\n");

        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 20, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        for (int i = 1; i < lines.size(); i++) {
            double v = Double.parseDouble(lines.get(i).split(",")[0]);
            assertTrue(v >= min && v <= max,
                    "Value " + v + " is outside [" + min + ", " + max + "]");
        }
    }

    @Test
    void generateSamples_withCategoricalParam_categoricalColumnPresent() throws Exception {
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  Weather:\n" +
                "    Condition:\n" +
                "      options: [Clear, Rainy, Foggy]\n");

        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 5, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertTrue(lines.get(0).contains("Weather_Condition"),
                "Header must include categorical parameter");
        // All data rows should have one of the allowed options
        for (int i = 1; i < lines.size(); i++) {
            String val = lines.get(i).trim();
            assertTrue(val.equals("Clear") || val.equals("Rainy") || val.equals("Foggy"),
                    "Unexpected categorical value: " + val);
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // generateSamplesforDomainModel
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void generateSamplesforDomainModel_noConstraint_producesCorrectRowCount() throws Exception {
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  Traffic:\n" +
                "    Count:\n" +
                "      type: int\n" +
                "      min: 0\n" +
                "      max: 50\n");

        Path csv = outputCsv();
        manager.generateSamplesforDomainModel(yaml.toString(), 8, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals(9, lines.size(), "Expected header + 8 data rows");
    }

    @Test
    void generateSamplesforDomainModel_distributionParamExcluded() throws Exception {
        // Distribution-typed params should be excluded from domain-model sampling
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  EgoAC:\n" +
                "    Speed:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 300\n" +
                "    Noise:\n" +
                "      type: distribution\n" +
                "      min: 0\n" +
                "      max: 1\n" +
                "      distributionName: normalDistribution\n" +
                "      distributionDetails: mean=0.5___stdDev=0.1\n");

        Path csv = outputCsv();
        manager.generateSamplesforDomainModel(yaml.toString(), 5, csv.toString());

        String header = Files.readAllLines(csv).get(0);
        assertTrue(header.contains("EgoAC_Speed"), "Speed must be included");
        assertFalse(header.contains("EgoAC_Noise"), "Distribution param must be excluded from domain model");
    }

    // ────────────────────────────────────────────────────────────────────────
    // generateSamples — with constraint (rejection sampling)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void generateSamples_withAlwaysTrueConstraint_producesCorrectRowCount() throws Exception {
        // Constraint: if(speed > -1) then (altitude > -1) else true → always true
        Path yaml = writeYaml(
                "Scenario:\n" +
                "  EgoAC:\n" +
                "    Speed:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 100\n" +
                "    Altitude:\n" +
                "      type: double\n" +
                "      min: 0\n" +
                "      max: 1000\n" +
                "    HasConstraint:\n" +
                "      IntraConstraint: if(@EgoACSpeed > -1) then (@EgoACAltitude > -1) else true\n");

        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 5, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals(6, lines.size(), "Expected header + 5 valid samples");
    }
}
