package odme.sampling;

import odme.sampling.distribution.DistributionSampling;
import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the full constrained sampling pipeline:
 * parse YAML → LHS generate → rejection-sample against constraints
 * → scale to real ranges → export CSV.
 */
public class SamplingManager {

    private final ScenarioParser parser = new ScenarioParser();
    private final LatinHypercubeSampler sampler = new LatinHypercubeSampler();
    private final ConstraintEvaluator evaluator = new ConstraintEvaluator();

    /**
     * Generates constrained samples supporting distribution-typed parameters.
     *
     * @param yamlFilePath    path to the scenario .yaml file
     * @param numberOfSamples number of valid samples to produce
     * @param outputCsvPath   path for the output .csv file
     */
    public void generateSamples(String yamlFilePath, int numberOfSamples, String outputCsvPath) throws Exception {
        Scenario scenario = parser.parse(yamlFilePath);
        List<String> constraint = scenario.getConstraint();

        List<Parameter> numericalParams = scenario.getParameters().stream()
                .filter(p -> "int".equals(p.getType()) || "double".equals(p.getType())
                        || "float".equals(p.getType()) || "distribution".equals(p.getType()))
                .collect(Collectors.toList());

        List<Parameter> categoricalParams = scenario.getParameters().stream()
                .filter(p -> "categorical".equals(p.getType()))
                .collect(Collectors.toList());

        System.out.println("Generating base samples...");
        List<double[]> normalizedSamples = sampler.generateNormalizedSamples(numericalParams.size(), numberOfSamples);
        List<Map<String, Double>> finalSamples = new ArrayList<>();

        if (constraint == null || constraint.isEmpty()) {
            System.out.println("No constraint found. Scaling all generated samples.");
            for (double[] normalizedSample : normalizedSamples) {
                finalSamples.add(scaleSample(normalizedSample, numericalParams));
            }
        } else {
            System.out.println("Constraint found. Starting rejection sampling...");
            int attemptCount = 0;
            int maxAttempts = numberOfSamples * 200;

            while (finalSamples.size() < numberOfSamples && attemptCount < maxAttempts) {
                double[] normalizedSample = sampler.generateNormalizedSamples(numericalParams.size(), 1).get(0);
                Map<String, Double> scaledSample = scaleSample(normalizedSample, numericalParams);
                for (String c : constraint) {
                    if (evaluator.evaluate(c, scaledSample)) {
                        finalSamples.add(scaledSample);
                        System.out.printf("Found valid sample %d of %d%n", finalSamples.size(), numberOfSamples);
                    }
                }
                attemptCount++;
            }

            if (finalSamples.size() < numberOfSamples) {
                throw new RuntimeException("Could not generate the required number of valid samples. " +
                        "Only found " + finalSamples.size() + " after " + maxAttempts + " attempts. " +
                        "The constraint may be too restrictive.");
            }
        }

        writeToCsv(finalSamples, numericalParams, categoricalParams, outputCsvPath);
        System.out.println("Successfully wrote " + numberOfSamples + " samples to " + outputCsvPath);
    }

    /**
     * Like {@link #generateSamples} but excludes distribution-typed parameters
     * (intended for domain-model-based generation from XSD-derived YAML).
     */
    public void generateSamplesforDomainModel(String yamlFilePath, int numberOfSamples, String outputCsvPath) throws Exception {
        Scenario scenario = parser.parse(yamlFilePath);
        List<String> constraint = scenario.getConstraint();

        List<Parameter> numericalParams = scenario.getParameters().stream()
                .filter(p -> "int".equals(p.getType()) || "double".equals(p.getType()) || "float".equals(p.getType()))
                .collect(Collectors.toList());

        List<Parameter> categoricalParams = scenario.getParameters().stream()
                .filter(p -> "categorical".equals(p.getType()))
                .collect(Collectors.toList());

        System.out.println("Generating base samples (domain model)...");
        List<double[]> normalizedSamples = sampler.generateNormalizedSamples(numericalParams.size(), numberOfSamples);
        List<Map<String, Double>> finalSamples = new ArrayList<>();

        if (constraint == null || constraint.isEmpty()) {
            for (double[] normalizedSample : normalizedSamples) {
                finalSamples.add(scaleSample(normalizedSample, numericalParams));
            }
        } else {
            int attemptCount = 0;
            int maxAttempts = numberOfSamples * 200;
            while (finalSamples.size() < numberOfSamples && attemptCount < maxAttempts) {
                double[] normalizedSample = sampler.generateNormalizedSamples(numericalParams.size(), 1).get(0);
                Map<String, Double> scaledSample = scaleSample(normalizedSample, numericalParams);
                for (String c : constraint) {
                    if (evaluator.evaluate(c, scaledSample)) {
                        finalSamples.add(scaledSample);
                    }
                }
                attemptCount++;
            }
            if (finalSamples.size() < numberOfSamples) {
                throw new RuntimeException("Could not generate the required number of valid samples. " +
                        "Only found " + finalSamples.size() + " after " + maxAttempts + " attempts.");
            }
        }

        writeToCsv(finalSamples, numericalParams, categoricalParams, outputCsvPath);
        System.out.println("Successfully wrote " + numberOfSamples + " samples to " + outputCsvPath);
    }

    private Map<String, Double> scaleSample(double[] normalizedSample, List<Parameter> numericalParams) {
        Map<String, Double> scaledSample = new HashMap<>();
        for (int i = 0; i < numericalParams.size(); i++) {
            Parameter param = numericalParams.get(i);
            if (param.getDistributionDetails() != null) {
                if ("normalDistribution".equals(param.getDistributionName())) {
                    String[] parts = param.getDistributionDetails().split("___");
                    double mean = Double.parseDouble(parts[0].split("=")[1]);
                    double stdDev = Double.parseDouble(parts[1].split("=")[1]);
                    scaledSample.put(param.getName(), DistributionSampling.normalDistributionSample(mean, stdDev, 1));
                } else if ("uniformDistribution".equals(param.getDistributionName())) {
                    String[] parts = param.getDistributionDetails().split("___");
                    double minVal = Double.parseDouble(parts[0].split("=")[1]);
                    double maxVal = Double.parseDouble(parts[1].split("=")[1]);
                    scaledSample.put(param.getName(), DistributionSampling.uniformDistributionSample(minVal, maxVal));
                }
            } else {
                double scaledValue = param.getMin() + normalizedSample[i] * (param.getMax() - param.getMin());
                scaledSample.put(param.getName(), scaledValue);
            }
        }
        return scaledSample;
    }

    private void writeToCsv(List<Map<String, Double>> samples,
                            List<Parameter> numericalParams,
                            List<Parameter> categoricalParams,
                            String outputCsvPath) throws IOException {

        if ((samples == null || samples.isEmpty()) && (categoricalParams == null || categoricalParams.isEmpty())) {
            System.out.println("No samples or parameters to write!");
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(outputCsvPath, StandardCharsets.UTF_8))) {
            List<String> headers = new ArrayList<>();
            for (Parameter p : numericalParams) headers.add(p.getName());
            for (Parameter p : categoricalParams) headers.add(p.getName());
            writer.println(String.join(",", headers));

            for (Map<String, Double> sample : samples) {
                List<String> row = new ArrayList<>();
                for (Parameter p : numericalParams) {
                    Double val = sample.get(p.getName());
                    row.add(val != null ? String.valueOf(val) : "");
                }
                for (Parameter p : categoricalParams) {
                    if (p.getOptions() != null && !p.getOptions().isEmpty()) {
                        int idx = (int) (Math.random() * p.getOptions().size());
                        row.add(p.getOptions().get(idx));
                    } else {
                        row.add("");
                    }
                }
                writer.println(String.join(",", row));
            }
            writer.flush();
        }
        System.out.println("CSV successfully written to: " + outputCsvPath);
    }
}
