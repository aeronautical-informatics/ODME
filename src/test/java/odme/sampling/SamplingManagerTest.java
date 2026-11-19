package odme.sampling;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration-style unit tests for {@link SamplingManager}.
 * Uses small in-memory YAML files written to a temp directory.
 */
class SamplingManagerTest {

    @TempDir
    Path tempDir;

    private final SamplingManager manager = new SamplingManager();

    @Test
    void seededYamlSamplingReproducesMixedConstrainedCsv() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Count: {type: int, min: 0, max: 3}
                    Range: {type: double, min: 0, max: 1}
                    Mode: {options: [A, B, C]}
                    Flag: {type: boolean}
                    HasConstraint:
                      IntraConstraint: '@Flag == 1 && @Count > 0'
                """);
        Path first = tempDir.resolve("first.csv");
        Path replay = tempDir.resolve("replay.csv");
        Path different = tempDir.resolve("different.csv");
        SamplingManager seeded = new SamplingManager(123L);
        seeded.generateSamples(yaml.toString(), 32, first.toString());
        new SamplingManager(123L).generateSamples(yaml.toString(), 32, replay.toString());
        new SamplingManager(456L).generateSamples(yaml.toString(), 32, different.toString());

        assertEquals(123L, seeded.getSeed());
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(replay));
        assertNotEquals(Files.readString(first), Files.readString(different));
    }

    @Test
    void oneSeededManagerReplaysConsecutivePesBatches() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Range: {type: double, min: 0, max: 1}
                    Flag: {type: boolean}
                """);
        Scenario scenario = new ScenarioParser().parse(yaml.toString());
        SamplingManager first = new SamplingManager(123L);
        SamplingManager replay = new SamplingManager(123L);
        List<Map<String, String>> previous = List.of();
        for (int pes = 0; pes < 3; pes++) {
            List<Map<String, String>> rows = first.generateSampleRows(scenario, 10, false);
            assertEquals(rows, replay.generateSampleRows(scenario, 10, false));
            assertNotEquals(previous, rows);
            previous = rows;
        }
    }

    @Test
    void seededCurrentModelSamplingIsStableAcrossMetadataReloads() throws Exception {
        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        variables.put(new TreePath(new String[]{"Scenario", "B"}), "Altitude,double,0,0,100");
        variables.put(new TreePath(new String[]{"Scenario", "A"}), "Speed,double,0,0,100");
        writeSerializedMultimap(tempDir.resolve("Model.ssdvar"), variables);
        Path first = tempDir.resolve("current-first.csv");
        Path replay = tempDir.resolve("current-replay.csv");

        new SamplingManager(123L).generateSamplesForCurrentModel(tempDir, "Model", 20, first.toString());
        new SamplingManager(123L).generateSamplesForCurrentModel(tempDir, "Model", 20, replay.toString());

        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(replay));
        assertEquals("A_Speed,B_Altitude", Files.readAllLines(first).get(0));
    }

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

    private void writeSerializedMultimap(Path file, Multimap<TreePath, String> data) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(file))) {
            objectOutputStream.writeObject(data);
        }
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
    void generateSamples_noConstraint_samplesEachIntegerEqually() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Traffic:
                    Count:
                      type: int
                      min: 0
                      max: 3
                """);
        Path csv = outputCsv();

        manager.generateSamples(yaml.toString(), 400, csv.toString());

        Map<String, Long> counts = Files.readAllLines(csv).stream().skip(1)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(Map.of("0", 100L, "1", 100L, "2", 100L, "3", 100L), counts);
    }

    @Test
    void generateSamples_noConstraint_usesEveryRealValuedStratumExactlyOnce() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Range:
                      type: double
                      min: 0
                      max: 1
                """);
        Path csv = outputCsv();
        int sampleCount = 400;

        manager.generateSamples(yaml.toString(), sampleCount, csv.toString());

        List<String> rows = Files.readAllLines(csv).stream().skip(1).toList();
        assertEquals(sampleCount, rows.size());
        Set<Integer> occupiedStrata = new HashSet<>();
        for (String row : rows) {
            double value = Double.parseDouble(row);
            int stratum = (int) Math.floor(value * sampleCount);
            assertTrue(stratum >= 0 && stratum < sampleCount, "Value outside [0, 1): " + value);
            occupiedStrata.add(stratum);
        }
        assertEquals(sampleCount, occupiedStrata.size(), "Expected one sample in each stratum");
    }

    @Test
    void generateSamples_booleanAndDouble_preserveBalancedDesign() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Flag:
                      type: boolean
                    Range:
                      type: double
                      min: 0
                      max: 1
                """);
        Path csv = outputCsv();
        int sampleCount = 200;
        manager.generateSamples(yaml.toString(), sampleCount, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals("Sensor_Flag,Sensor_Range", lines.get(0));
        List<String[]> rows = lines.stream().skip(1).map(row -> row.split(",")).toList();
        assertEquals(sampleCount, rows.size());
        Map<String, Long> counts = rows.stream()
                .collect(Collectors.groupingBy(row -> row[0], Collectors.counting()));
        assertEquals(Map.of("false", 100L, "true", 100L), counts);

        Set<Integer> occupiedStrata = new HashSet<>();
        for (String[] row : rows) {
            double value = Double.parseDouble(row[1]);
            int stratum = (int) Math.floor(value * sampleCount);
            assertTrue(stratum >= 0 && stratum < sampleCount, "Value outside [0, 1): " + value);
            occupiedStrata.add(stratum);
        }
        assertEquals(sampleCount, occupiedStrata.size(), "Expected one sample in each double stratum");
    }

    @Test
    void generateSampleRows_booleanWithTrueDefault_stillProducesBothValues() throws Exception {
        Parameter flag = new Parameter();
        flag.setName("Flag");
        flag.setType("boolean");
        flag.setDefaultValue("true");
        Scenario scenario = new Scenario();
        scenario.setParameters(List.of(flag));

        List<Map<String, String>> rows = manager.generateSampleRows(scenario, 10, false);

        Set<String> values = rows.stream().map(row -> row.get("Flag")).collect(Collectors.toSet());
        assertEquals(Set.of("false", "true"), values);
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

    @Test
    void generateSamples_withCategoricalAndDouble_balancesThreeOptions() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Mode:
                      options: [A, B, C]
                    Range:
                      type: double
                      min: 0
                      max: 1
                """);
        Path csv = outputCsv();

        manager.generateSamples(yaml.toString(), 300, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals("Sensor_Mode,Sensor_Range", lines.get(0));
        Map<String, Long> counts = lines.stream().skip(1)
                .collect(Collectors.groupingBy(row -> row.split(",", 2)[0], Collectors.counting()));
        assertEquals(Map.of("A", 100L, "B", 100L, "C", 100L), counts);
    }

    @Test
    void generateSamples_withFourOptions_samplesEachOptionOnce() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Mode:
                      options: [A, B, C, D]
                """);
        Path csv = outputCsv();

        manager.generateSamples(yaml.toString(), 4, csv.toString());

        Map<String, Long> counts = Files.readAllLines(csv).stream().skip(1)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        assertEquals(Map.of("A", 1L, "B", 1L, "C", 1L, "D", 1L), counts);
    }

    @Test
    void generateSampleRows_withoutCategoricalOptions_preservesDefaultAndDoubleStrata() throws Exception {
        Parameter mode = new Parameter();
        mode.setName("Mode");
        mode.setType("categorical");
        mode.setOptions(List.of());
        mode.setDefaultValue("Fallback");
        Parameter range = new Parameter();
        range.setName("Range");
        range.setType("double");
        range.setMin(0.0);
        range.setMax(1.0);
        Scenario scenario = new Scenario();
        scenario.setParameters(List.of(mode, range));
        int sampleCount = 300;

        List<Map<String, String>> rows = manager.generateSampleRows(scenario, sampleCount, false);

        assertEquals(sampleCount, rows.size());
        Set<Integer> occupiedStrata = new HashSet<>();
        for (Map<String, String> row : rows) {
            assertEquals("Fallback", row.get("Mode"));
            double value = Double.parseDouble(row.get("Range"));
            int stratum = (int) Math.floor(value * sampleCount);
            assertTrue(stratum >= 0 && stratum < sampleCount, "Value outside [0, 1): " + value);
            occupiedStrata.add(stratum);
        }
        assertEquals(sampleCount, occupiedStrata.size(), "Expected one sample in each double stratum");
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
    void generateSamples_rejectsInvalidYamlConstraintBeforeSampling() throws Exception {
        String constraint = "if(@Traffic_Count > 1) then (true)";
        Path yaml = writeYaml("""
                Scenario:
                  Traffic:
                    Count:
                      type: int
                      min: 0
                      max: 3
                    HasConstraint:
                      IntraConstraint: %s
                """.formatted(constraint));
        Path csv = outputCsv();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.generateSamples(yaml.toString(), 5, csv.toString()));

        assertTrue(error.getMessage().contains(constraint));
        assertTrue(error.getMessage().contains("else fallback"));
        assertFalse(Files.exists(csv));
    }

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

    @Test
    void generateSamples_integerConstraintUsesTheValueWrittenToCsv() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Traffic:
                    Count:
                      type: int
                      min: 0
                      max: 3
                    HasConstraint:
                      IntraConstraint: if(@Traffic_Count < 3) then (0) else 1
                """);
        Path csv = outputCsv();

        manager.generateSamples(yaml.toString(), 8, csv.toString());

        List<String> rows = Files.readAllLines(csv).stream().skip(1).toList();
        assertEquals(8, rows.size());
        assertTrue(rows.stream().allMatch("3"::equals));
    }

    @Test
    void generateSamples_booleanConstraintUsesTheValueWrittenToCsv() throws Exception {
        Path yaml = writeYaml("""
                Scenario:
                  Sensor:
                    Flag:
                      type: boolean
                    HasConstraint:
                      IntraConstraint: '@Flag == 1'
                """);
        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 10, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals("Sensor_Flag", lines.get(0));
        List<String> rows = lines.subList(1, lines.size());
        assertEquals(10, rows.size());
        assertTrue(rows.stream().allMatch("true"::equals));
    }

    @Test
    void generateSamples_withMultipleConstraints_requiresAllConstraints() throws Exception {
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
                "      max: 100\n" +
                "    HasConstraint:\n" +
                "      IntraConstraint: if(@EgoACSpeed > -1) then (@EgoACAltitude > 40) else true\n" +
                "      InterConstraint: if(@EgoACSpeed > -1) then (@EgoACAltitude < 60) else true\n");

        Path csv = outputCsv();
        manager.generateSamples(yaml.toString(), 12, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            double altitude = Double.parseDouble(values[1]);
            assertTrue(altitude > 40 && altitude < 60, "Altitude must satisfy both constraints.");
        }
    }

    @Test
    void generateSamplesForCurrentModel_samplesBooleanFromSerializedMetadata() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scenario");
        DefaultMutableTreeNode aircraft = new DefaultMutableTreeNode("Aircraft");
        root.add(aircraft);
        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        variables.put(new TreePath(aircraft.getPath()), "Flag,boolean,false");
        writeSerializedMultimap(tempDir.resolve("Model.ssdvar"), variables);

        Path csv = outputCsv();
        manager.generateSamplesForCurrentModel(tempDir, "Model", 10, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals("Aircraft_Flag", lines.get(0));
        assertEquals(11, lines.size());
        assertEquals(Set.of("false", "true"), new HashSet<>(lines.subList(1, lines.size())));
    }

    @Test
    void generateSamplesForCurrentModel_readsSerializedVariablesAndConstraints() throws Exception {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Scenario");
        DefaultMutableTreeNode aircraft = new DefaultMutableTreeNode("Aircraft");
        root.add(aircraft);
        TreePath aircraftPath = new TreePath(aircraft.getPath());

        Multimap<TreePath, String> variables = ArrayListMultimap.create();
        variables.put(aircraftPath, "Speed,double,50,0,100");
        variables.put(aircraftPath, "Altitude,double,50,0,100");

        Multimap<TreePath, String> constraints = ArrayListMultimap.create();
        constraints.put(aircraftPath, "if(@Speed > -1) then (@Altitude > 40) else true");
        constraints.put(aircraftPath, "if(@Speed > -1) then (@Altitude < 60) else true");

        writeSerializedMultimap(tempDir.resolve("Model.ssdvar"), variables);
        writeSerializedMultimap(tempDir.resolve("Model.ssdcon"), constraints);

        Path csv = outputCsv();
        manager.generateSamplesForCurrentModel(tempDir, "Model", 10, csv.toString());

        List<String> lines = Files.readAllLines(csv);
        assertEquals(11, lines.size(), "Expected header + 10 data rows");
        assertTrue(lines.get(0).contains("Aircraft_Speed"));
        assertTrue(lines.get(0).contains("Aircraft_Altitude"));

        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split(",");
            double altitude = Double.parseDouble(values[1]);
            assertTrue(altitude > 40 && altitude < 60, "Serialized constraints must be enforced.");
        }
    }
}
