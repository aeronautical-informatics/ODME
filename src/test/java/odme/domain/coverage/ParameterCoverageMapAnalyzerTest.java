package odme.domain.coverage;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterCoverageMapAnalyzerTest {

    private final ParameterCoverageMapAnalyzer analyzer = new ParameterCoverageMapAnalyzer();

    @Test
    void triangularKernelUsesFixedAdequacyTargetRatherThanIndependentNormalization() {
        ParameterCoverageMapAnalyzer.Configuration configuration = configuration(
                List.of(0.0, 5.0, 10.0), 2.0, 2.0,
                ParameterCoverageMapAnalyzer.Kernel.TRIANGULAR,
                ParameterCoverageMapAnalyzer.Weighting.GEOMETRIC, List.of());

        ParameterCoverageMapAnalyzer.Result result = analyzer.analyze(configuration,
                List.of(new ParameterCoverageMapAnalyzer.Sample("one", 5.0)));

        ParameterCoverageMapAnalyzer.HeatPoint centre = result.heatField().stream()
                .min(java.util.Comparator.comparingDouble(point -> Math.abs(point.x() - 5.0))).orElseThrow();
        assertEquals(1.0, centre.heat(), 1e-9);
        assertEquals(0.5, centre.coverage(), 1e-9);
    }

    @Test
    void unequalRegionsAreWeightedByTheirPhysicalWidths() {
        ParameterCoverageMapAnalyzer.Configuration configuration = configuration(
                List.of(0.0, 1.0, 10.0), 0.1, 1.0,
                ParameterCoverageMapAnalyzer.Kernel.BINARY,
                ParameterCoverageMapAnalyzer.Weighting.GEOMETRIC, List.of());
        ParameterCoverageMapAnalyzer.Result result = analyzer.analyze(configuration,
                List.of(new ParameterCoverageMapAnalyzer.Sample("low", 0.5)));

        assertEquals(0.1, result.regions().get(0).weight(), 1e-9);
        assertEquals(0.9, result.regions().get(1).weight(), 1e-9);
    }

    @Test
    void calculatesPriorityWeightedScoreAndColdRegionMetrics() {
        ParameterCoverageMapAnalyzer.Configuration configuration = configuration(
                List.of(0.0, 5.0, 10.0), 1.0, 1.0,
                ParameterCoverageMapAnalyzer.Kernel.BINARY,
                ParameterCoverageMapAnalyzer.Weighting.PRIORITY, List.of(0.8, 0.2));
        ParameterCoverageMapAnalyzer.Result result = analyzer.analyze(configuration,
                List.of(new ParameterCoverageMapAnalyzer.Sample("cold-side", 1.0)));

        assertEquals(ParameterCoverageMapAnalyzer.Classification.UNCOVERED, result.regions().get(1).classification());
        // Binary support covers [0,2] of the first five-unit region: 0.8 * 2/5.
        assertEquals(0.32, result.parameterScore(), 0.01);
        assertEquals(List.of("cold-side"), result.regions().get(0).sampleIds());
    }

    @Test
    void validatesPriorityWeightsAndEdges() {
        IllegalArgumentException invalidWeight = assertThrows(IllegalArgumentException.class, () -> configuration(List.of(0.0, 4.0, 10.0), 1.0, 1.0,
                ParameterCoverageMapAnalyzer.Kernel.TRIANGULAR,
                ParameterCoverageMapAnalyzer.Weighting.PRIORITY, List.of(0.4, 0.4)));
        assertTrue(invalidWeight.getMessage().contains("sum to 1"));
        IllegalArgumentException invalidEdges = assertThrows(IllegalArgumentException.class, () -> configuration(List.of(0.0, 6.0, 6.0, 10.0), 1.0, 1.0,
                ParameterCoverageMapAnalyzer.Kernel.TRIANGULAR,
                ParameterCoverageMapAnalyzer.Weighting.GEOMETRIC, List.of()));
        assertTrue(invalidEdges.getMessage().contains("strictly increasing"));
    }

    private ParameterCoverageMapAnalyzer.Configuration configuration(
            List<Double> edges, double bandwidth, double target, ParameterCoverageMapAnalyzer.Kernel kernel,
            ParameterCoverageMapAnalyzer.Weighting weighting, List<Double> weights) {
        return new ParameterCoverageMapAnalyzer.Configuration(0.0, 10.0, edges, kernel, bandwidth,
                target, 0.25, weighting, weights);
    }
}
