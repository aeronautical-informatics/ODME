package odme.domain.coverage;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates parameter-wise marginal coverage fields for continuous ODD
 * parameters.  This deliberately does not claim multidimensional ODD
 * coverage: every parameter is evaluated independently.
 */
public final class ParameterCoverageMapAnalyzer {

    private static final int MIN_PROBES_PER_REGION = 24;
    private static final int BASE_PROBES_ACROSS_RANGE = 320;

    public enum Kernel {
        TRIANGULAR, GAUSSIAN, BINARY
    }

    public enum Weighting {
        GEOMETRIC, EQUAL_REGION, PRIORITY
    }

    public enum Classification {
        HOT, PARTIALLY_REPRESENTED, COLD, UNCOVERED
    }

    public record Sample(String id, double value) {
        public Sample {
            Objects.requireNonNull(id, "id");
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Sample value must be finite.");
            }
        }
    }

    public record Configuration(double minimum, double maximum, List<Double> regionEdges,
                                Kernel kernel, double bandwidth, double adequacyTarget,
                                double coldThreshold, Weighting weighting,
                                List<Double> priorityWeights) {
        public Configuration {
            Objects.requireNonNull(regionEdges, "regionEdges");
            Objects.requireNonNull(kernel, "kernel");
            Objects.requireNonNull(weighting, "weighting");
            priorityWeights = priorityWeights == null ? List.of() : List.copyOf(priorityWeights);
            validate(minimum, maximum, regionEdges, bandwidth, adequacyTarget, coldThreshold,
                    weighting, priorityWeights);
            regionEdges = List.copyOf(regionEdges);
        }

        private static void validate(double minimum, double maximum, List<Double> edges, double bandwidth,
                                     double adequacyTarget, double coldThreshold, Weighting weighting,
                                     List<Double> priorityWeights) {
            if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || maximum <= minimum) {
                throw new IllegalArgumentException("Parameter maximum must be greater than its minimum.");
            }
            if (!Double.isFinite(bandwidth) || bandwidth <= 0.0) {
                throw new IllegalArgumentException("Coverage bandwidth must be positive.");
            }
            if (!Double.isFinite(adequacyTarget) || adequacyTarget <= 0.0) {
                throw new IllegalArgumentException("Adequacy target must be positive.");
            }
            if (!Double.isFinite(coldThreshold) || coldThreshold < 0.0 || coldThreshold > 1.0) {
                throw new IllegalArgumentException("Cold threshold must be between 0 and 1.");
            }
            if (edges.size() < 2) {
                throw new IllegalArgumentException("At least two ordered region edges are required.");
            }
            if (Math.abs(edges.get(0) - minimum) > 1e-9 || Math.abs(edges.get(edges.size() - 1) - maximum) > 1e-9) {
                throw new IllegalArgumentException("Region edges must start at the parameter minimum and end at its maximum.");
            }
            double previous = Double.NEGATIVE_INFINITY;
            for (Double edge : edges) {
                if (edge == null || !Double.isFinite(edge) || edge <= previous) {
                    throw new IllegalArgumentException("Region edges must be finite and strictly increasing.");
                }
                previous = edge;
            }
            if (weighting == Weighting.PRIORITY) {
                if (priorityWeights.size() != edges.size() - 1) {
                    throw new IllegalArgumentException("A priority weight is required for every coverage region.");
                }
                double total = 0.0;
                for (Double weight : priorityWeights) {
                    if (weight == null || !Double.isFinite(weight) || weight < 0.0) {
                        throw new IllegalArgumentException("Priority weights must be finite and non-negative.");
                    }
                    total += weight;
                }
                if (Math.abs(total - 1.0) > 1e-6) {
                    throw new IllegalArgumentException("Priority weights must sum to 1.");
                }
            }
        }
    }

    public record HeatPoint(double x, double heat, double coverage) { }

    public record RegionResult(double start, double end, double width, int sampleCount,
                               int influencingSampleCount, double meanCoverage, double minimumCoverage,
                               double coldFraction, double hotFraction, double weight,
                               Classification classification, List<String> sampleIds,
                               List<String> influencingSampleIds) { }

    public record Result(double parameterScore, Weighting weighting, int inRangeSampleCount,
                         int excludedSampleCount, List<Sample> samples, List<HeatPoint> heatField,
                         List<RegionResult> regions) { }

    public Result analyze(Configuration configuration, List<Sample> samples) {
        Objects.requireNonNull(configuration, "configuration");
        Objects.requireNonNull(samples, "samples");
        List<Sample> inRange = new ArrayList<>();
        int excluded = 0;
        for (Sample sample : samples) {
            if (sample.value() < configuration.minimum() || sample.value() > configuration.maximum()) {
                excluded++;
            } else {
                inRange.add(sample);
            }
        }

        List<HeatPoint> heatField = buildHeatField(configuration, inRange);
        List<RegionResult> regions = new ArrayList<>();
        for (int index = 0; index < configuration.regionEdges().size() - 1; index++) {
            double start = configuration.regionEdges().get(index);
            double end = configuration.regionEdges().get(index + 1);
            regions.add(summarizeRegion(configuration, inRange, start, end, index));
        }
        return new Result(score(configuration, regions), configuration.weighting(), inRange.size(), excluded,
                List.copyOf(inRange), List.copyOf(heatField), List.copyOf(regions));
    }

    private List<HeatPoint> buildHeatField(Configuration configuration, List<Sample> samples) {
        int probes = Math.max(BASE_PROBES_ACROSS_RANGE, configuration.regionEdges().size() * MIN_PROBES_PER_REGION);
        List<HeatPoint> result = new ArrayList<>(probes + 1);
        double range = configuration.maximum() - configuration.minimum();
        for (int i = 0; i <= probes; i++) {
            double x = configuration.minimum() + range * i / probes;
            double heat = heatAt(x, configuration, samples);
            result.add(new HeatPoint(x, heat, normalized(heat, configuration.adequacyTarget())));
        }
        return result;
    }

    private RegionResult summarizeRegion(Configuration configuration, List<Sample> samples,
                                         double start, double end, int index) {
        double width = end - start;
        int probes = Math.max(MIN_PROBES_PER_REGION,
                (int) Math.ceil(BASE_PROBES_ACROSS_RANGE * width / (configuration.maximum() - configuration.minimum())));
        double totalCoverage = 0.0;
        double minimumCoverage = Double.POSITIVE_INFINITY;
        int cold = 0;
        int hot = 0;
        boolean hasHeat = false;
        for (int i = 0; i < probes; i++) {
            double x = start + width * (i + 0.5) / probes;
            double heat = heatAt(x, configuration, samples);
            hasHeat |= heat > 0.0;
            double coverage = normalized(heat, configuration.adequacyTarget());
            totalCoverage += coverage;
            minimumCoverage = Math.min(minimumCoverage, coverage);
            if (coverage < configuration.coldThreshold()) {
                cold++;
            }
            if (coverage >= 0.75) {
                hot++;
            }
        }
        List<String> directIds = new ArrayList<>();
        List<String> influencingIds = new ArrayList<>();
        for (Sample sample : samples) {
            if (sample.value() >= start && (sample.value() < end || sample.value() == configuration.maximum())) {
                directIds.add(sample.id());
            }
            if (influences(sample.value(), start, end, configuration)) {
                influencingIds.add(sample.id());
            }
        }
        double mean = totalCoverage / probes;
        double coldFraction = (double) cold / probes;
        Classification classification = !hasHeat ? Classification.UNCOVERED
                : mean < 0.25 || coldFraction > 0.40 ? Classification.COLD
                : mean >= 0.75 && coldFraction <= 0.10 ? Classification.HOT
                : Classification.PARTIALLY_REPRESENTED;
        return new RegionResult(start, end, width, directIds.size(), influencingIds.size(), mean,
                minimumCoverage, coldFraction, (double) hot / probes, weight(configuration, index), classification,
                List.copyOf(directIds), List.copyOf(influencingIds));
    }

    private double score(Configuration configuration, List<RegionResult> regions) {
        if (regions.isEmpty()) {
            return 0.0;
        }
        if (configuration.weighting() == Weighting.EQUAL_REGION) {
            return regions.stream().mapToDouble(RegionResult::meanCoverage).average().orElse(0.0);
        }
        if (configuration.weighting() == Weighting.PRIORITY) {
            return regions.stream().mapToDouble(region -> region.weight() * region.meanCoverage()).sum();
        }
        double range = configuration.maximum() - configuration.minimum();
        return regions.stream().mapToDouble(region -> region.width() / range * region.meanCoverage()).sum();
    }

    private double weight(Configuration configuration, int index) {
        if (configuration.weighting() == Weighting.PRIORITY) {
            return configuration.priorityWeights().get(index);
        }
        if (configuration.weighting() == Weighting.EQUAL_REGION) {
            return 1.0 / (configuration.regionEdges().size() - 1);
        }
        return (configuration.regionEdges().get(index + 1) - configuration.regionEdges().get(index))
                / (configuration.maximum() - configuration.minimum());
    }

    private boolean influences(double value, double start, double end, Configuration configuration) {
        if (configuration.kernel() == Kernel.GAUSSIAN) {
            return true;
        }
        return value + configuration.bandwidth() >= start && value - configuration.bandwidth() <= end;
    }

    private double heatAt(double x, Configuration configuration, List<Sample> samples) {
        double heat = 0.0;
        for (Sample sample : samples) {
            double u = Math.abs(x - sample.value()) / configuration.bandwidth();
            heat += switch (configuration.kernel()) {
                case TRIANGULAR -> Math.max(0.0, 1.0 - u);
                case GAUSSIAN -> Math.exp(-0.5 * u * u);
                case BINARY -> u <= 1.0 ? 1.0 : 0.0;
            };
        }
        return heat;
    }

    private double normalized(double heat, double adequacyTarget) {
        return Math.min(1.0, heat / adequacyTarget);
    }
}
