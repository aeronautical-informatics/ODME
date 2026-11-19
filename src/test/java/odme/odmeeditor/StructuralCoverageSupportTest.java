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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static odme.odmeeditor.StructuralCoverageSupport.buildUniformBins;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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

    @Test
    void binCoverageMap_usesTheBinsOfTheCoverageEstimate(@TempDir Path directory) throws IOException {
        CoverageContext context = writeIntegerScenarios(directory, 1, 2, 2, 7);
        ParameterSelectionRow selection = new ParameterSelectionRow("count", "Root", "count", "int", 0, 10, "1", "");

        CoverageReport report = StructuralCoverageSupport.analyze(context, List.of(selection));
        StructuralCoverageSupport.BinMapRow row =
                StructuralCoverageSupport.analyzeBinCoverageMaps(context, List.of(selection)).get(0);

        assertEquals(report.parameterResults().get(0).totalBins(), row.bins().size());
        assertEquals(report.parameterResults().get(0).coveredBins(), row.coveredBins());
        assertArrayEquals(new int[]{0, 1, 2, 0, 0, 0, 0, 1, 0, 0}, row.counts());
        assertEquals(4, row.sampleCount());
    }

    @Test
    void binCoverageMap_colours_emptyRed_sampleGreen_farYellow() {
        CoverageBin bin = new CoverageBin(0, 10, false);

        assertEquals(CoverageMapOverviewPanel.EMPTY, CoverageMapOverviewPanel.colourAt(5, bin, List.of()));
        assertEquals(CoverageMapOverviewPanel.AT_SAMPLE, CoverageMapOverviewPanel.colourAt(2, bin, List.of(2.0)));
        assertEquals(CoverageMapOverviewPanel.FAR, CoverageMapOverviewPanel.colourAt(7, bin, List.of(2.0)));
        assertEquals(CoverageMapOverviewPanel.FAR, CoverageMapOverviewPanel.colourAt(9.9, bin, List.of(2.0)));
        CoverageBin unit = new CoverageBin(1, 2, false);
        assertEquals(CoverageMapOverviewPanel.AT_SAMPLE, CoverageMapOverviewPanel.colourAt(1.9, unit, List.of(1.0), true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0-0.25;0.5-0.5;0.75-1"})
    void binCoverageMap_mixedParameterTotalsMatchReport(String customBins, @TempDir Path directory)
            throws IOException {
        List<ScenarioReference> scenarios = new ArrayList<>();
        double[][] samples = {{1, 0.25}, {2, 0.5}, {10, 1.0}};
        for (int i = 0; i < samples.length; i++) {
            String name = "Scenario" + i;
            Path scenarioDirectory = Files.createDirectories(directory.resolve(name));
            ArrayListMultimap<TreePath, String> variables = ArrayListMultimap.create();
            variables.put(new TreePath("Root"), "count,int," + samples[i][0]);
            variables.put(new TreePath("Root"), "distance,double," + samples[i][1]);
            try (ObjectOutputStream output = new ObjectOutputStream(
                    Files.newOutputStream(scenarioDirectory.resolve("Demo.ssdvar")))) {
                output.writeObject(variables);
            }
            scenarios.add(new ScenarioReference(name, Set.of()));
        }
        List<ParameterDefinition> definitions = List.of(
                new ParameterDefinition("count", "Root", "count", "int", 0, 10, Set.of("Root")),
                new ParameterDefinition("distance", "Root", "distance", "double", 0, 1, Set.of("Root")));
        CoverageContext context = new CoverageContext("Demo", directory, definitions, List.of(), scenarios);
        List<ParameterSelectionRow> selections = List.of(
                new ParameterSelectionRow("count", "Root", "count", "int", 0, 10, "1", ""),
                new ParameterSelectionRow("distance", "Root", "distance", "double", 0, 1, "0.25", customBins));

        CoverageReport report = StructuralCoverageSupport.analyze(context, selections);
        List<StructuralCoverageSupport.BinMapRow> rows =
                StructuralCoverageSupport.analyzeBinCoverageMaps(context, selections);

        assertEquals(2, rows.size());
        assertEquals(customBins.isEmpty() ? 14 : 13, report.totalParameterBins());
        assertEquals(customBins.isEmpty() ? 6 : 5, report.coveredParameterBins());
        assertEquals(report.totalParameterBins(), rows.stream().mapToInt(row -> row.bins().size()).sum());
        assertEquals(report.coveredParameterBins(), rows.stream()
                .mapToInt(StructuralCoverageSupport.BinMapRow::coveredBins).sum());
    }

    @Test
    void binCoverageMap_integerUnitsOneAndTwoAreCovered(@TempDir Path directory) throws IOException {
        CoverageContext context = writeIntegerScenarios(directory, 1, 2);
        ParameterSelectionRow selection = new ParameterSelectionRow("count", "Root", "count", "int", 0, 10, "1", "");
        StructuralCoverageSupport.BinMapRow row =
                StructuralCoverageSupport.analyzeBinCoverageMaps(context, List.of(selection)).get(0);

        assertEquals(2, row.coveredBins());
        assertArrayEquals(new int[]{0, 1, 1, 0, 0, 0, 0, 0, 0, 0}, row.counts());
        BufferedImage image = paintBinMap(row);
        for (int value = 0; value <= 10; value++) {
            Color expected = value == 1 || value == 2
                    ? CoverageMapOverviewPanel.AT_SAMPLE : CoverageMapOverviewPanel.EMPTY;
            assertEquals(expected.getRGB(), image.getRGB(4 + value * 100 + 25, 20), "Unit " + value);
            assertEquals(expected.getRGB(), image.getRGB(4 + value * 100 + 75, 20), "Unit " + value);
        }
    }

    @Test
    void binCoverageMap_integerUnitColourIsConstantBetweenSamples() {
        CoverageBin bin = new CoverageBin(0, 5, false);

        assertEquals(CoverageMapOverviewPanel.colourAt(2.1, bin, List.of(1.0), true),
                CoverageMapOverviewPanel.colourAt(2.9, bin, List.of(1.0), true));
    }

    @Test
    void binCoverageMap_integerSingletonAndMaximumAreVisible(@TempDir Path directory) throws IOException {
        CoverageContext context = writeIntegerScenarios(directory, 5, 10);
        ParameterSelectionRow selection = new ParameterSelectionRow(
                "count", "Root", "count", "int", 0, 10, "1", "0-2;5-5;8-10");
        StructuralCoverageSupport.BinMapRow row =
                StructuralCoverageSupport.analyzeBinCoverageMaps(context, List.of(selection)).get(0);

        assertArrayEquals(new int[]{0, 1, 1}, row.counts());
        BufferedImage image = paintBinMap(row);
        assertEquals(CoverageMapOverviewPanel.AT_SAMPLE.getRGB(), image.getRGB(554, 20));
        assertEquals(CoverageMapOverviewPanel.AT_SAMPLE.getRGB(), image.getRGB(1054, 20));
    }

    private static BufferedImage paintBinMap(StructuralCoverageSupport.BinMapRow row) {
        CoverageMapOverviewPanel.Strip strip = new CoverageMapOverviewPanel.Strip(row);
        strip.setSize(1108, 40);
        BufferedImage image = new BufferedImage(1108, 40, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            strip.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static CoverageContext writeIntegerScenarios(Path directory, int... samples) throws IOException {
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
            scenarios.add(new ScenarioReference(scenarioName, Set.of()));
        }
        ParameterDefinition definition = new ParameterDefinition("count", "Root", "count", "int", 0, 10, Set.of("Root"));
        return new CoverageContext("Demo", directory, List.of(definition), List.of(), scenarios);
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
