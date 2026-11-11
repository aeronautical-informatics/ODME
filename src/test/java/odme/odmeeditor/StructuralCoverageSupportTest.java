package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import odme.odmeeditor.StructuralCoverageSupport.CoverageBin;
import odme.odmeeditor.StructuralCoverageSupport.CoverageContext;
import odme.odmeeditor.StructuralCoverageSupport.CoverageReport;
import odme.odmeeditor.StructuralCoverageSupport.ParameterCoverageResult;
import odme.odmeeditor.StructuralCoverageSupport.ParameterDefinition;
import odme.odmeeditor.StructuralCoverageSupport.ParameterSelectionRow;
import odme.odmeeditor.StructuralCoverageSupport.ScenarioReference;
import odme.odmeeditor.StructuralCoverageSupport.StructuralOption;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.tree.TreePath;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static odme.odmeeditor.StructuralCoverageSupport.buildUniformBins;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuralCoverageSupportTest {
    @ParameterizedTest
    @CsvSource({
            "0, 10, 1, 10",
            "0, 1, 0.1, 10",
            "0, 3, 0.3, 10",
            "-90, 90, 18, 10",
            "0, 0.7, 0.1, 7",
            "0, 0.30000000000000004, 0.1, 3",
            "5, 5, 1, 1",
            "0, 10000, 1, 10000"
    })
    void buildUniformBins_preservesCountAndEdges(double min, double max, double binSize, int expectedCount) {
        assertBinEdges(min, max, expectedCount, buildUniformBins(min, max, binSize));
    }

    @Test
    void buildUniformBins_keepsShorterLastBin() {
        List<CoverageBin> bins = buildUniformBins(0, 10, 3);

        assertBinEdges(0, 10, 4, bins);
        assertEquals(3.0, bins.get(0).upper());
        assertEquals(6.0, bins.get(1).upper());
        assertEquals(9.0, bins.get(2).upper());
    }

    @Test
    void buildUniformBins_rejectsMoreThanTenThousandBins() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> buildUniformBins(0, 10001, 1));

        assertTrue(exception.getMessage().contains("10000"));
    }

    @ParameterizedTest
    @CsvSource({
            "2.0, 2, [2 - 3)",
            "1.9999, 1, [1 - 2)",
            "0.0, 0, [0 - 1)",
            "10.0, 9, [9 - 10]"
    })
    void buildUniformBins_assignsValuesToOneBin(double value, int binIndex, String label) {
        List<CoverageBin> bins = buildUniformBins(0, 10, 1);
        CoverageBin expected = bins.get(binIndex);

        assertEquals(List.of(expected), bins.stream().filter(bin -> bin.matchesAny(List.of(value))).toList());
        assertEquals(label, expected.label());
    }

    @Test
    void buildUniformBins_integerSamplesCoverEveryBinWithoutDoubleCounting() {
        List<CoverageBin> bins = buildUniformBins(0, 10, 1);
        List<Double> values = IntStream.rangeClosed(0, 10).mapToObj(value -> (double) value).toList();

        assertTrue(bins.stream().allMatch(bin -> bin.matchesAny(values)));
        for (double value : values) {
            assertEquals(1, bins.stream().filter(bin -> bin.matchesAny(List.of(value))).count(), "Value " + value);
        }
    }

    @Test
    void buildUniformBins_twoBoundarySamplesCoverTwoBins() {
        List<CoverageBin> bins = buildUniformBins(0, 10, 2);

        assertEquals(2, bins.stream().filter(bin -> bin.matchesAny(List.of(2.0, 4.0))).count());
    }

    @Test
    void buildUniformBins_equalBoundsProduceOneClosedBin() {
        List<CoverageBin> bins = buildUniformBins(5, 5, 1);

        assertEquals(1, bins.size());
        CoverageBin bin = bins.get(0);
        assertTrue(bin.upperClosed());
        assertTrue(bin.matchesAny(List.of(5.0)));
        assertFalse(bin.matchesAny(List.of(4.0, 6.0)));
        assertEquals("[5 - 5]", bin.label());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0-2;2-8;8-10", "8-10;0-2;2-8"})
    void analyze_assignsCustomBoundaryValuesToOneBin(String customBins, @TempDir Path directory) throws IOException {
        ParameterCoverageResult atTwo = assertCustomBinMatch(directory, customBins, 2, "[2 - 8)");

        assertEquals(3, atTwo.totalBins());
        assertEquals("[0 - 2); [8 - 10]", atTwo.uncoveredBinLabels());
        assertCustomBinMatch(directory, customBins, 8, "[8 - 10]");
        assertCustomBinMatch(directory, customBins, 10, "[8 - 10]");
    }

    @ParameterizedTest
    @CsvSource({
            "0-5;3-8, [0 - 5), [3 - 8)",
            "0-5;5-5, [0 - 5), [5 - 5]",
            "5-8;5-5, [5 - 8), [5 - 5]"
    })
    void analyze_rejectsOverlappingCustomBins(String customBins, String firstBin, String secondBin,
                                               @TempDir Path directory) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> analyzeParameter(directory, customBins));

        assertTrue(exception.getMessage().contains(firstBin));
        assertTrue(exception.getMessage().contains(secondBin));
    }

    @Test
    void analyze_customSingletonMatchesOnlyItsValue(@TempDir Path directory) throws IOException {
        ParameterCoverageResult result = assertCustomBinMatch(directory, "5-5", 5, "[5 - 5]");

        assertEquals(1, result.totalBins());
        assertEquals(0, analyzeParameter(directory, "5-5", 4, 6).coveredParameterBins());
    }

    @Test
    void analyze_keepsCustomGapsAndClosesNonFinalSingleton(@TempDir Path directory) throws IOException {
        String customBins = "0-2;5-5;8-10";

        assertEquals(0, analyzeParameter(directory, customBins, 2, 3, 7).coveredParameterBins());
        assertCustomBinMatch(directory, customBins, 5, "[5 - 5]");
    }

    @Test
    void analyze_countsIntegerSamplesInOneBinEach(@TempDir Path directory) throws IOException {
        CoverageReport report = analyzeParameter(directory, "", 1, 2);

        assertEquals(10, report.totalParameterBins());
        assertEquals(2, report.coveredParameterBins());
        assertEquals(2, report.parameterResults().get(0).coveredBins());
        assertEquals("[1 - 2); [2 - 3)", report.parameterResults().get(0).coveredBinLabels());
    }

    @ParameterizedTest
    @CsvSource({
            "60.0, 10, 44.1, 1404, 52.05",
            "0.0, 0, 70.0, 100, 70.0",
            "80.0, 5, 0.0, 0, 80.0",
            "0.0, 0, 0.0, 0, 0.0"
    })
    void overallCoverage_averagesOnlyComponentsWithItems(double structuralPercent, int structuralTotal,
                                                          double parameterPercent, int parameterTotal, double expected) {
        assertEquals(expected, StructuralCoverageSupport.overallCoverage(
                structuralPercent, structuralTotal, parameterPercent, parameterTotal), 1e-9);
    }

    @Test
    void analyze_averagesPruningAndParameterCoverage(@TempDir Path directory) throws IOException {
        List<StructuralOption> options = List.of(
                new StructuralOption("ModeSpec=A", "ModeSpec", "A", "Root > ModeSpec > A", Set.of("Root>ModeSpec>A")),
                new StructuralOption("ModeSpec=B", "ModeSpec", "B", "Root > ModeSpec > B", Set.of("Root>ModeSpec>B"))
        );
        CoverageReport report = analyzeCoverage(directory, "", options, Set.of("ModeSpec=A"), 1, 2);

        assertEquals(50.0, report.structuralCoveragePercent());
        assertEquals(20.0, report.parameterCoveragePercent());
        assertEquals(35.0, report.overallCoveragePercent(), 1e-9);
        assertNotEquals(25.0, report.overallCoveragePercent(), 1e-9); // Pooled coverage was 3 of 12.
    }

    private static ParameterCoverageResult assertCustomBinMatch(Path directory, String customBins, int sample,
                                                                 String label) throws IOException {
        ParameterCoverageResult result = analyzeParameter(directory, customBins, sample).parameterResults().get(0);
        assertEquals(1, result.coveredBins());
        assertEquals(label, result.coveredBinLabels());
        return result;
    }

    private static CoverageReport analyzeParameter(Path directory, String customBins, int... samples) throws IOException {
        return analyzeCoverage(directory, customBins, List.of(), Set.of(), samples);
    }

    private static CoverageReport analyzeCoverage(Path directory, String customBins, List<StructuralOption> options,
                                                  Set<String> selectedStructuralKeys, int... samples) throws IOException {
        List<ScenarioReference> scenarios = new ArrayList<>();
        for (int i = 0; i < samples.length; i++) {
            String scenarioName = "Scenario" + i;
            Path scenarioDirectory = Files.createDirectories(directory.resolve(scenarioName));
            ArrayListMultimap<TreePath, String> variables = ArrayListMultimap.create();
            variables.put(new TreePath("Root"), "count,int," + samples[i]);
            try (ObjectOutputStream output = new ObjectOutputStream(
                    Files.newOutputStream(scenarioDirectory.resolve("Demo.ssdvar")))) {
                output.writeObject(variables);
            }
            scenarios.add(new ScenarioReference(scenarioName, selectedStructuralKeys));
        }

        ParameterDefinition definition = new ParameterDefinition("count", "Root", "count", "int", 0, 10, Set.of("Root"));
        CoverageContext context = new CoverageContext("Demo", directory, List.of(definition), options, scenarios);
        ParameterSelectionRow selection = new ParameterSelectionRow("count", "Root", "count", "int", 0, 10, "1", customBins);
        return StructuralCoverageSupport.analyze(context, List.of(selection));
    }

    private static void assertBinEdges(double min, double max, int expectedCount, List<CoverageBin> bins) {
        assertEquals(expectedCount, bins.size());
        assertEquals(min, bins.get(0).lower());
        assertEquals(max, bins.get(bins.size() - 1).upper());
        for (int i = 1; i < bins.size(); i++) {
            assertEquals(bins.get(i - 1).upper(), bins.get(i).lower(), "Edge " + i);
        }
    }
}
