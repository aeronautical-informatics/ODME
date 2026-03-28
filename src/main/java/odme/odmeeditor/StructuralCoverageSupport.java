package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.core.EditorContext;
import odme.core.XmlJTree;
import odme.sampling.CurrentModelScenarioBuilder;
import odme.sampling.model.Parameter;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StructuralCoverageSupport {

    private static final Pattern CUSTOM_BIN_PATTERN =
            Pattern.compile("^\\s*([-+]?\\d*\\.?\\d+)\\s*-\\s*([-+]?\\d*\\.?\\d+)\\s*$");
    private static final String AUTO_GENERATED_REMARKS_PREFIX =
            "Automatically generated specialization combination:";

    private StructuralCoverageSupport() {
    }

    static CoverageContext loadCoverageContext(List<String[]> scenarioRows) throws Exception {
        String projectName = EditorContext.getInstance().getProjName();
        Path projectDirectory = Path.of(EditorContext.getInstance().getFileLocation(), projectName);
        Path projectTreePath = projectDirectory.resolve(projectName + ".xml");

        if (!Files.exists(projectTreePath)) {
            throw new IOException("Domain model not found: " + projectTreePath);
        }

        XmlJTree domainTree = new XmlJTree(projectTreePath.toString());
        if (domainTree.dtModel == null) {
            throw new IOException("Unable to read domain model tree: " + projectTreePath);
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) domainTree.dtModel.getRoot();
        Multimap<TreePath, String> domainVariables = readSerializedMultimap(projectDirectory.resolve(projectName + ".ssdvar").toFile());

        CurrentModelScenarioBuilder builder = new CurrentModelScenarioBuilder();
        CurrentModelScenarioBuilder.SamplingModel samplingModel =
                builder.buildFromMetadata(domainVariables, ArrayListMultimap.create());

        List<ParameterDefinition> parameterDefinitions = buildParameterDefinitions(samplingModel.bindings());
        List<StructuralOption> structuralOptions = new ArrayList<>();
        collectStructuralOptions(root, new ArrayList<>(), structuralOptions);
        List<ScenarioReference> scenarioReferences = buildScenarioReferences(scenarioRows);

        return new CoverageContext(projectName, projectDirectory, parameterDefinitions, structuralOptions, scenarioReferences);
    }

    static List<ParameterSelectionRow> defaultSelectionRows(CoverageContext context) {
        List<ParameterSelectionRow> rows = new ArrayList<>();
        for (ParameterDefinition definition : context.parameterDefinitions()) {
            rows.add(new ParameterSelectionRow(
                    definition.id(),
                    definition.pathDisplay(),
                    definition.variableName(),
                    definition.type(),
                    definition.min(),
                    definition.max(),
                    formatNumber(defaultBinSize(definition)),
                    ""
            ));
        }
        return rows;
    }

    static CoverageReport analyze(CoverageContext context, List<ParameterSelectionRow> selectionRows) {
        List<ScenarioSnapshot> scenarios = loadScenarioSnapshots(
                context.projectDirectory(),
                context.projectName(),
                context.scenarioReferences()
        );
        Map<String, ParameterSelectionRow> selectionsById = new LinkedHashMap<>();
        for (ParameterSelectionRow row : selectionRows) {
            selectionsById.put(row.parameterId(), row);
        }

        List<StructuralCoverageResult> structuralResults = new ArrayList<>();
        int coveredStructural = 0;
        for (StructuralOption option : context.structuralOptions()) {
            boolean covered = scenarios.stream().anyMatch(scenario ->
                    scenario.selectedStructuralKeys().contains(option.key())
                            || intersects(scenario.treePathVariants(), option.coveragePathVariants()));
            if (covered) {
                coveredStructural++;
            }
            structuralResults.add(new StructuralCoverageResult(
                    option.decisionNode(),
                    option.optionLabel(),
                    option.pathDisplay(),
                    covered
            ));
        }

        List<ParameterCoverageResult> parameterResults = new ArrayList<>();
        int totalBins = 0;
        int coveredBins = 0;
        for (ParameterDefinition definition : context.parameterDefinitions()) {
            ParameterSelectionRow row = selectionsById.get(definition.id());
            List<CoverageBin> bins = buildBins(definition, row);

            List<Double> matchingValues = new ArrayList<>();
            for (ScenarioSnapshot scenario : scenarios) {
                for (ScenarioVariableValue value : scenario.variableValues()) {
                    if (!definition.variableName().equals(value.variableName())) {
                        continue;
                    }
                    if (!intersects(definition.coveragePathVariants(), value.pathVariants())) {
                        continue;
                    }
                    matchingValues.add(value.numericValue());
                }
            }

            List<String> coveredLabels = new ArrayList<>();
            List<String> uncoveredLabels = new ArrayList<>();
            for (CoverageBin bin : bins) {
                boolean covered = bin.matchesAny(matchingValues);
                if (covered) {
                    coveredLabels.add(bin.label());
                    coveredBins++;
                } else {
                    uncoveredLabels.add(bin.label());
                }
                totalBins++;
            }

            parameterResults.add(new ParameterCoverageResult(
                    definition.pathDisplay(),
                    definition.variableName(),
                    definition.type(),
                    definition.min(),
                    definition.max(),
                    bins.size(),
                    coveredLabels.size(),
                    percentage(coveredLabels.size(), bins.size()),
                    String.join("; ", coveredLabels),
                    String.join("; ", uncoveredLabels)
            ));
        }

        double structuralCoverage = percentage(coveredStructural, structuralResults.size());
        double parameterCoverage = percentage(coveredBins, totalBins);
        int overallCovered = coveredStructural + coveredBins;
        int overallTotal = structuralResults.size() + totalBins;
        double overallCoverage = percentage(overallCovered, overallTotal);

        return new CoverageReport(
                context.projectName(),
                scenarios.size(),
                structuralResults.size(),
                coveredStructural,
                structuralCoverage,
                totalBins,
                coveredBins,
                parameterCoverage,
                overallCovered,
                overallTotal,
                overallCoverage,
                structuralResults,
                parameterResults
        );
    }

    private static List<ParameterDefinition> buildParameterDefinitions(List<CurrentModelScenarioBuilder.VariableBinding> bindings) {
        List<ParameterDefinition> definitions = new ArrayList<>();
        for (CurrentModelScenarioBuilder.VariableBinding binding : bindings) {
            Parameter parameter = binding.parameter();
            if (!isNumericRangeParameter(parameter)) {
                continue;
            }

            LinkedHashSet<String> pathVariants = new LinkedHashSet<>(buildPathVariants(binding.pathSegments()));
            definitions.add(new ParameterDefinition(
                    parameter.getName(),
                    String.join(" > ", binding.pathSegments()),
                    binding.variableName(),
                    parameter.getType(),
                    parameter.getMin(),
                    parameter.getMax(),
                    pathVariants
            ));
        }
        return definitions;
    }

    private static void collectStructuralOptions(DefaultMutableTreeNode node,
                                                 List<String> parentPath,
                                                 List<StructuralOption> target) {
        String label = node.toString();
        List<String> currentPath = append(parentPath, label);

        if (isStructuralDecision(label)) {
            for (int i = 0; i < node.getChildCount(); i++) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
                List<String> childPath = append(currentPath, child.toString());
                target.add(new StructuralOption(
                        structuralKey(label, child.toString()),
                        label,
                        child.toString(),
                        String.join(" > ", childPath),
                        buildPathVariants(childPath)
                ));
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectStructuralOptions((DefaultMutableTreeNode) node.getChildAt(i), currentPath, target);
        }
    }

    private static List<ScenarioReference> buildScenarioReferences(List<String[]> scenarioRows) {
        List<ScenarioReference> references = new ArrayList<>();
        for (String[] row : scenarioRows) {
            if (row == null || row.length == 0) {
                continue;
            }
            String scenarioName = row[0];
            if (scenarioName == null || scenarioName.isBlank() || "InitScenario".equals(scenarioName)) {
                continue;
            }
            String remarks = row.length > 2 ? row[2] : "";
            references.add(new ScenarioReference(
                    scenarioName,
                    parseSelectedStructuralKeys(remarks)
            ));
        }
        return references;
    }

    private static List<ScenarioSnapshot> loadScenarioSnapshots(Path projectDirectory,
                                                               String projectName,
                                                               List<ScenarioReference> scenarioReferences) {
        List<ScenarioSnapshot> scenarios = new ArrayList<>();
        for (ScenarioReference reference : scenarioReferences) {
            if (reference == null || reference.scenarioName() == null || reference.scenarioName().isBlank()) {
                continue;
            }

            Path scenarioDirectory = projectDirectory.resolve(reference.scenarioName());
            if (!Files.isDirectory(scenarioDirectory)) {
                continue;
            }

            LinkedHashSet<String> pathVariants = new LinkedHashSet<>();
            if (reference.selectedStructuralKeys().isEmpty()) {
                Path scenarioTreePath = scenarioDirectory.resolve(projectName + ".xml");
                if (Files.exists(scenarioTreePath)) {
                    try {
                        XmlJTree scenarioTree = new XmlJTree(scenarioTreePath.toString());
                        if (scenarioTree.dtModel != null) {
                            DefaultMutableTreeNode root = (DefaultMutableTreeNode) scenarioTree.dtModel.getRoot();
                            collectScenarioPathVariants(root, new ArrayList<>(), pathVariants);
                        }
                    } catch (Exception ignored) {
                        // Keep coverage estimation resilient for partially generated/manual scenarios.
                    }
                }
            }

            Multimap<TreePath, String> scenarioVariables;
            try {
                scenarioVariables = readSerializedMultimap(scenarioDirectory.resolve(projectName + ".ssdvar").toFile());
            } catch (IOException | ClassNotFoundException ex) {
                continue;
            }

            List<ScenarioVariableValue> values = new ArrayList<>();
            for (Map.Entry<TreePath, Collection<String>> entry : scenarioVariables.asMap().entrySet()) {
                Set<String> variants = buildPathVariants(toSegments(entry.getKey()));
                for (String rawVariable : entry.getValue()) {
                    ScenarioVariableValue value = toScenarioVariableValue(rawVariable, variants);
                    if (value != null) {
                        values.add(value);
                    }
                }
            }

            scenarios.add(new ScenarioSnapshot(reference.scenarioName(), reference.selectedStructuralKeys(), pathVariants, values));
        }
        return scenarios;
    }

    private static void collectScenarioPathVariants(DefaultMutableTreeNode node,
                                                    List<String> parentPath,
                                                    Set<String> target) {
        List<String> currentPath = append(parentPath, node.toString());
        target.addAll(buildPathVariants(currentPath));
        for (int i = 0; i < node.getChildCount(); i++) {
            collectScenarioPathVariants((DefaultMutableTreeNode) node.getChildAt(i), currentPath, target);
        }
    }

    private static ScenarioVariableValue toScenarioVariableValue(String rawVariable, Set<String> pathVariants) {
        if (rawVariable == null || rawVariable.isBlank()) {
            return null;
        }

        String[] parts = rawVariable.split(",", -1);
        if (parts.length < 3) {
            return null;
        }

        String type = parts[1].trim().toLowerCase(Locale.ROOT);
        if (!isNumericType(type)) {
            return null;
        }

        try {
            return new ScenarioVariableValue(parts[0].trim(), pathVariants, Double.parseDouble(parts[2].trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Set<String> parseSelectedStructuralKeys(String remarks) {
        if (remarks == null || remarks.isBlank()) {
            return Collections.emptySet();
        }

        String trimmed = remarks.trim();
        int prefixIndex = trimmed.indexOf(AUTO_GENERATED_REMARKS_PREFIX);
        if (prefixIndex < 0) {
            return Collections.emptySet();
        }

        String selections = trimmed.substring(prefixIndex + AUTO_GENERATED_REMARKS_PREFIX.length()).trim();
        int sampleIndex = selections.indexOf(" Variable sample ");
        if (sampleIndex >= 0) {
            selections = selections.substring(0, sampleIndex).trim();
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String token : selections.split(";")) {
            String selection = token == null ? "" : token.trim();
            if (selection.isEmpty()) {
                continue;
            }

            int separatorIndex = selection.indexOf('=');
            if (separatorIndex <= 0 || separatorIndex >= selection.length() - 1) {
                continue;
            }

            String decisionNode = selection.substring(0, separatorIndex).trim();
            String optionLabel = selection.substring(separatorIndex + 1).trim();
            if (!decisionNode.isEmpty() && !optionLabel.isEmpty()) {
                keys.add(structuralKey(decisionNode, optionLabel));
            }
        }
        return keys;
    }

    private static List<CoverageBin> buildBins(ParameterDefinition definition, ParameterSelectionRow row) {
        if (row != null && row.customBins() != null && !row.customBins().trim().isEmpty()) {
            return parseCustomBins(definition.variableName(), row.customBins());
        }

        String rawBinSize = row == null ? "" : row.binSize();
        double binSize;
        try {
            binSize = rawBinSize == null || rawBinSize.trim().isEmpty()
                    ? defaultBinSize(definition)
                    : Double.parseDouble(rawBinSize.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bin size for " + definition.variableName() + ": " + rawBinSize);
        }

        if (binSize <= 0.0) {
            throw new IllegalArgumentException("Bin size must be positive for " + definition.variableName());
        }

        return buildUniformBins(definition.min(), definition.max(), binSize);
    }

    private static List<CoverageBin> buildUniformBins(double min, double max, double binSize) {
        if (max < min) {
            throw new IllegalArgumentException("Upper bound must be >= lower bound.");
        }

        List<CoverageBin> bins = new ArrayList<>();
        if (Double.compare(min, max) == 0) {
            bins.add(new CoverageBin(min, max, formatRange(min, max)));
            return bins;
        }

        double cursor = min;
        int guard = 0;
        while (cursor < max && guard < 10000) {
            guard++;
            double next = Math.min(max, cursor + binSize);
            bins.add(new CoverageBin(cursor, next, formatRange(cursor, next)));
            if (next == max) {
                break;
            }
            cursor = next;
        }

        if (bins.isEmpty()) {
            bins.add(new CoverageBin(min, max, formatRange(min, max)));
        }

        return bins;
    }

    private static List<CoverageBin> parseCustomBins(String variableName, String rawCustomBins) {
        List<CoverageBin> bins = new ArrayList<>();
        String[] tokens = rawCustomBins.split("[;\\n\\r]+");
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            Matcher matcher = CUSTOM_BIN_PATTERN.matcher(trimmed);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(
                        "Invalid custom bin for " + variableName + ": '" + trimmed + "'. Use format 0-2;2-8;8-10");
            }

            double lower = Double.parseDouble(matcher.group(1));
            double upper = Double.parseDouble(matcher.group(2));
            if (upper < lower) {
                throw new IllegalArgumentException(
                        "Custom bin upper bound must be >= lower bound for " + variableName + ": '" + trimmed + "'");
            }
            bins.add(new CoverageBin(lower, upper, formatRange(lower, upper)));
        }

        if (bins.isEmpty()) {
            throw new IllegalArgumentException("At least one custom bin is required when custom bins are provided.");
        }

        return bins;
    }

    private static double defaultBinSize(ParameterDefinition definition) {
        double range = definition.max() - definition.min();
        if (range <= 0.0) {
            return 1.0;
        }
        if ("int".equals(definition.type())) {
            return Math.max(1.0, Math.ceil(range / 5.0));
        }
        return range / 5.0;
    }

    private static boolean isNumericRangeParameter(Parameter parameter) {
        if (parameter == null || parameter.getType() == null) {
            return false;
        }
        if (!isNumericType(parameter.getType())) {
            return false;
        }
        return parameter.getMax() > parameter.getMin();
    }

    private static boolean isNumericType(String type) {
        return switch (type) {
            case "int", "float", "double", "distribution" -> true;
            default -> false;
        };
    }

    private static boolean isStructuralDecision(String label) {
        return label.endsWith("Spec") || label.endsWith("MAsp");
    }

    private static Set<String> buildPathVariants(List<String> pathSegments) {
        LinkedHashSet<String> variants = new LinkedHashSet<>();
        variants.add(pathKey(pathSegments));
        variants.add(pathKey(normalizePrunedPath(pathSegments)));
        return variants;
    }

    private static List<String> normalizePrunedPath(List<String> pathSegments) {
        List<String> normalized = new ArrayList<>();
        for (String segment : pathSegments) {
            if (segment == null) {
                continue;
            }
            if (segment.isBlank() || "start".equalsIgnoreCase(segment)) {
                continue;
            }
            if (segment.endsWith("Spec") || segment.endsWith("MAsp") || segment.endsWith("Dec")) {
                continue;
            }
            normalized.add(segment);
        }
        return normalized;
    }

    private static List<String> append(List<String> path, String label) {
        List<String> next = new ArrayList<>(path);
        next.add(label);
        return next;
    }

    private static String pathKey(List<String> pathSegments) {
        return String.join(">", pathSegments);
    }

    private static String structuralKey(String decisionNode, String optionLabel) {
        return decisionNode.trim() + "=" + optionLabel.trim();
    }

    private static List<String> toSegments(TreePath path) {
        Object[] rawSegments = path.getPath();
        List<String> segments = new ArrayList<>(rawSegments.length);
        for (Object rawSegment : rawSegments) {
            segments.add(String.valueOf(rawSegment));
        }
        return segments;
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        return !Collections.disjoint(left, right);
    }

    private static double percentage(int covered, int total) {
        return total <= 0 ? 0.0 : (covered * 100.0) / total;
    }

    private static String formatRange(double lower, double upper) {
        return "[" + formatNumber(lower) + " - " + formatNumber(upper) + "]";
    }

    private static String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 1e-9) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.4f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    @SuppressWarnings("unchecked")
    private static Multimap<TreePath, String> readSerializedMultimap(File file) throws IOException, ClassNotFoundException {
        if (!file.exists()) {
            return ArrayListMultimap.create();
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            return (Multimap<TreePath, String>) objectInputStream.readObject();
        }
    }

    record CoverageContext(String projectName,
                           Path projectDirectory,
                           List<ParameterDefinition> parameterDefinitions,
                           List<StructuralOption> structuralOptions,
                           List<ScenarioReference> scenarioReferences) {
    }

    record ParameterDefinition(String id,
                               String pathDisplay,
                               String variableName,
                               String type,
                               double min,
                               double max,
                               Set<String> coveragePathVariants) {
    }

    record ParameterSelectionRow(String parameterId,
                                 String pathDisplay,
                                 String variableName,
                                 String type,
                                 double min,
                                 double max,
                                 String binSize,
                                 String customBins) {
    }

    record StructuralOption(String key,
                            String decisionNode,
                            String optionLabel,
                            String pathDisplay,
                            Set<String> coveragePathVariants) {
    }

    record ScenarioReference(String scenarioName,
                             Set<String> selectedStructuralKeys) {
    }

    record ScenarioSnapshot(String scenarioName,
                            Set<String> selectedStructuralKeys,
                            Set<String> treePathVariants,
                            List<ScenarioVariableValue> variableValues) {
    }

    record ScenarioVariableValue(String variableName,
                                 Set<String> pathVariants,
                                 double numericValue) {
    }

    record CoverageBin(double lowerInclusive, double upperInclusive, String label) {
        boolean matchesAny(List<Double> values) {
            for (Double value : values) {
                if (value == null) {
                    continue;
                }
                if (value >= lowerInclusive && value <= upperInclusive) {
                    return true;
                }
            }
            return false;
        }
    }

    record StructuralCoverageResult(String decisionNode,
                                    String optionLabel,
                                    String pathDisplay,
                                    boolean covered) {
    }

    record ParameterCoverageResult(String pathDisplay,
                                   String variableName,
                                   String type,
                                   double min,
                                   double max,
                                   int totalBins,
                                   int coveredBins,
                                   double coveragePercent,
                                   String coveredBinLabels,
                                   String uncoveredBinLabels) {
    }

    record CoverageReport(String projectName,
                          int scenarioCount,
                          int totalStructuralOptions,
                          int coveredStructuralOptions,
                          double structuralCoveragePercent,
                          int totalParameterBins,
                          int coveredParameterBins,
                          double parameterCoveragePercent,
                          int overallCoveredItems,
                          int overallTotalItems,
                          double overallCoveragePercent,
                          List<StructuralCoverageResult> structuralResults,
                          List<ParameterCoverageResult> parameterResults) {
        int uncoveredStructuralOptions() {
            return Math.max(0, totalStructuralOptions - coveredStructuralOptions);
        }

        int uncoveredParameterBins() {
            return Math.max(0, totalParameterBins - coveredParameterBins);
        }
    }
}
