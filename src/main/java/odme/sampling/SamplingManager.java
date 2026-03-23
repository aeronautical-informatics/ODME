package odme.sampling;

import odme.sampling.distribution.DistributionSampling;
import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Orchestrates constrained Latin Hypercube Sampling and CSV export.
 */
public class SamplingManager {

    private final ScenarioParser parser = new ScenarioParser();
    private final LatinHypercubeSampler sampler = new LatinHypercubeSampler();
    private final ConstraintEvaluator evaluator = new ConstraintEvaluator();
    private final CurrentModelScenarioBuilder currentModelBuilder = new CurrentModelScenarioBuilder();

    public void generateSamples(String yamlFilePath, int numberOfSamples, String outputCsvPath) throws Exception {
        Scenario scenario = parser.parse(yamlFilePath);
        generateSamples(scenario, numberOfSamples, outputCsvPath, true);
    }

    public void generateSamplesforDomainModel(String yamlFilePath, int numberOfSamples, String outputCsvPath)
            throws Exception {
        Scenario scenario = parser.parse(yamlFilePath);
        generateSamples(scenario, numberOfSamples, outputCsvPath, false);
    }

    public void generateSamplesForCurrentModel(int numberOfSamples, String outputCsvPath) throws Exception {
        Scenario scenario = currentModelBuilder.buildFromCurrentContext();
        generateSamples(scenario, numberOfSamples, outputCsvPath, false);
    }

    void generateSamplesForCurrentModel(Path workingDir, String modelName, int numberOfSamples, String outputCsvPath)
            throws Exception {
        Scenario scenario = currentModelBuilder.build(workingDir, modelName);
        generateSamples(scenario, numberOfSamples, outputCsvPath, false);
    }

    private void generateSamples(Scenario scenario,
                                 int numberOfSamples,
                                 String outputCsvPath,
                                 boolean includeDistributionParameters) throws Exception {
        if (numberOfSamples <= 0) {
            throw new IllegalArgumentException("Number of samples must be positive.");
        }

        List<Parameter> outputParameters = scenario.getParameters().stream()
                .filter(parameter -> includeInOutput(parameter, includeDistributionParameters))
                .collect(Collectors.toList());
        if (outputParameters.isEmpty()) {
            throw new IllegalStateException("No sampleable or exportable parameters were found.");
        }

        List<Parameter> sampledParameters = outputParameters.stream()
                .filter(parameter -> isSampledParameter(parameter, includeDistributionParameters))
                .collect(Collectors.toList());
        List<String> constraints = scenario.getConstraint() == null
                ? List.of()
                : scenario.getConstraint().stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(constraint -> !constraint.isEmpty())
                        .toList();

        List<SampleRow> finalSamples = new ArrayList<>();
        int maxAttempts = Math.max(numberOfSamples * 200, 1000);
        int attemptCount = 0;
        int batchSize = Math.max(numberOfSamples, 16);

        while (finalSamples.size() < numberOfSamples && attemptCount < maxAttempts) {
            int remainingNeeded = numberOfSamples - finalSamples.size();
            int currentBatchSize = Math.min(Math.max(remainingNeeded * 2, batchSize), maxAttempts - attemptCount);
            if (currentBatchSize <= 0) {
                break;
            }

            List<double[]> normalizedSamples = generateNormalizedBatch(sampledParameters.size(), currentBatchSize);
            for (double[] normalizedSample : normalizedSamples) {
                SampleRow sampleRow = buildSampleRow(outputParameters, sampledParameters, normalizedSample);
                if (constraints.isEmpty() || satisfiesAllConstraints(constraints, sampleRow.constraintValues())) {
                    finalSamples.add(sampleRow);
                    if (finalSamples.size() == numberOfSamples) {
                        break;
                    }
                }
            }

            attemptCount += currentBatchSize;
            batchSize = Math.min(Math.max(batchSize * 2, 32), Math.max(numberOfSamples * 4, 32));
        }

        if (finalSamples.size() < numberOfSamples) {
            throw new RuntimeException("Could not generate the required number of valid samples. Only found "
                    + finalSamples.size() + " after " + attemptCount + " attempts. "
                    + "The constraints may be too restrictive or reference unsupported values.");
        }

        writeToCsv(finalSamples, outputParameters, outputCsvPath);
    }

    private boolean includeInOutput(Parameter parameter, boolean includeDistributionParameters) {
        return includeDistributionParameters || !"distribution".equals(normalizeType(parameter));
    }

    private boolean isSampledParameter(Parameter parameter, boolean includeDistributionParameters) {
        return switch (normalizeType(parameter)) {
            case "int", "float", "double" -> true;
            case "distribution" -> includeDistributionParameters;
            case "categorical" -> false;
            case "boolean" -> false;
            default -> false;
        };
    }

    private List<double[]> generateNormalizedBatch(int dimensions, int count) {
        if (dimensions <= 0) {
            List<double[]> emptyRows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                emptyRows.add(new double[0]);
            }
            return emptyRows;
        }
        return sampler.generateNormalizedSamples(dimensions, count);
    }

    private SampleRow buildSampleRow(List<Parameter> outputParameters,
                                     List<Parameter> sampledParameters,
                                     double[] normalizedSample) {
        Map<String, String> csvValues = new LinkedHashMap<>();
        Map<String, Double> constraintValues = new LinkedHashMap<>();
        int sampledIndex = 0;

        for (Parameter parameter : outputParameters) {
            String type = normalizeType(parameter);
            switch (type) {
                case "int", "float", "double" -> {
                    double sampledValue = scaleNumeric(normalizedSample[sampledIndex++], parameter);
                    csvValues.put(parameter.getName(), formatNumericForCsv(type, sampledValue));
                    constraintValues.put(parameter.getName(), sampledValue);
                }
                case "distribution" -> {
                    double sampledValue = sampleDistribution(parameter);
                    csvValues.put(parameter.getName(), Double.toString(sampledValue));
                    constraintValues.put(parameter.getName(), sampledValue);
                }
                case "categorical" -> {
                    String categoricalValue = pickCategoricalValue(parameter);
                    csvValues.put(parameter.getName(), categoricalValue);
                }
                case "boolean" -> {
                    String booleanValue = defaultString(parameter.getDefaultValue(), "false");
                    csvValues.put(parameter.getName(), booleanValue);
                    constraintValues.put(parameter.getName(), "true".equalsIgnoreCase(booleanValue) ? 1.0 : 0.0);
                }
                default -> csvValues.put(parameter.getName(), defaultString(parameter.getDefaultValue(), ""));
            }
        }

        if (sampledIndex != sampledParameters.size()) {
            throw new IllegalStateException("Sample generation drifted from the parameter definition order.");
        }

        return new SampleRow(csvValues, constraintValues);
    }

    private boolean satisfiesAllConstraints(List<String> constraints, Map<String, Double> constraintValues) {
        for (String constraint : constraints) {
            if (!evaluator.evaluate(constraint, constraintValues)) {
                return false;
            }
        }
        return true;
    }

    private double scaleNumeric(double normalizedValue, Parameter parameter) {
        double scaledValue = parameter.getMin() + normalizedValue * (parameter.getMax() - parameter.getMin());
        if ("int".equals(normalizeType(parameter))) {
            return Math.rint(scaledValue);
        }
        return scaledValue;
    }

    private double sampleDistribution(Parameter parameter) {
        if ("normalDistribution".equals(parameter.getDistributionName())) {
            String[] parts = parameter.getDistributionDetails().split("___");
            double mean = Double.parseDouble(parts[0].split("=")[1]);
            double stdDev = Double.parseDouble(parts[1].split("=")[1]);
            return DistributionSampling.normalDistributionSample(mean, stdDev, 1);
        }
        if ("uniformDistribution".equals(parameter.getDistributionName())) {
            String[] parts = parameter.getDistributionDetails().split("___");
            double minValue = Double.parseDouble(parts[0].split("=")[1]);
            double maxValue = Double.parseDouble(parts[1].split("=")[1]);
            return DistributionSampling.uniformDistributionSample(minValue, maxValue);
        }
        throw new IllegalArgumentException("Unsupported distribution: " + parameter.getDistributionName());
    }

    private String pickCategoricalValue(Parameter parameter) {
        if (parameter.getOptions() == null || parameter.getOptions().isEmpty()) {
            return defaultString(parameter.getDefaultValue(), "");
        }
        int index = (int) (Math.random() * parameter.getOptions().size());
        return parameter.getOptions().get(index);
    }

    private String formatNumericForCsv(String type, double value) {
        if ("int".equals(type)) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private String normalizeType(Parameter parameter) {
        return parameter.getType() == null ? "" : parameter.getType().toLowerCase(Locale.ROOT);
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void writeToCsv(List<SampleRow> samples, List<Parameter> outputParameters, String outputCsvPath)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsvPath, StandardCharsets.UTF_8))) {
            List<String> headers = outputParameters.stream()
                    .map(Parameter::getName)
                    .collect(Collectors.toList());
            writer.println(String.join(",", headers));

            for (SampleRow sample : samples) {
                List<String> row = outputParameters.stream()
                        .map(parameter -> escapeCsv(sample.csvValues().getOrDefault(parameter.getName(), "")))
                        .collect(Collectors.toList());
                writer.println(String.join(",", row));
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private record SampleRow(Map<String, String> csvValues, Map<String, Double> constraintValues) {
    }
}
