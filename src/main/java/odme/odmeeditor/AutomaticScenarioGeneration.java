package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mxgraph.io.mxCodec;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import odme.core.EditorContext;
import odme.core.FlagVariables;
import odme.core.XmlJTree;
import odme.jtreetograph.JtreeToGraphSave;
import odme.sampling.ConstraintEvaluator;
import odme.sampling.CurrentModelScenarioBuilder;
import odme.sampling.SamplingManager;
import odme.sampling.model.Parameter;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Generates PES scenario folders by exhaustively pruning every specialization
 * combination from the saved domain model.
 *
 * <p>The current ODME project format persists specialization structure, node
 * variables, behaviours, and constraints, but it does not yet persist the
 * rule-based SES variable interface required for automatic multi-aspect
 * expansion. For that reason, this first implementation supports exhaustive
 * specialization pruning and rejects projects that still contain multi-aspect
 * nodes.</p>
 */
public final class AutomaticScenarioGeneration {

    private static final String DEFAULT_PREFIX = "AutoScenario";
    private static final int MAX_AUTO_SCENARIOS = 10000;
    private static final double X_GAP = 140.0;
    private static final double Y_GAP = 90.0;
    private static final double ROOT_X = 80.0;
    private static final double ROOT_Y = 20.0;

    private AutomaticScenarioGeneration() {}

    public static int maxGeneratedScenarioModels() {
        return MAX_AUTO_SCENARIOS;
    }

    public static GenerationPreview inspectProject() throws IOException, ClassNotFoundException {
        ProjectModel projectModel = loadProjectModel();
        return inspectProject(projectModel);
    }

    private static GenerationPreview inspectProject(ProjectModel projectModel) {

        List<SpecializationDescriptor> specializations = new ArrayList<>();
        collectSpecializations(projectModel.root, new ArrayList<>(), specializations);

        int multiAspectCount = countNodesWithSuffix(projectModel.root, "MAsp");
        long totalCombinations = 1L;
        for (SpecializationDescriptor descriptor : specializations) {
            int optionCount = Math.max(1, descriptor.optionLabels.size());
            if (totalCombinations > MAX_AUTO_SCENARIOS / optionCount) {
                totalCombinations = (long) MAX_AUTO_SCENARIOS + 1;
                break;
            }
            totalCombinations *= optionCount;
        }

        return new GenerationPreview(
                projectModel.projectName,
                specializations,
                multiAspectCount,
                totalCombinations == 0 ? 1 : totalCombinations
        );
    }

    public static GenerationResult generateAll(String requestedPrefix)
            throws Exception {
        return generateAll(requestedPrefix, 1);
    }

    public static GenerationResult generateAll(String requestedPrefix, int samplesPerCombination)
            throws Exception {
        ProjectModel projectModel = loadProjectModel();
        GenerationPreview preview = inspectProject(projectModel);

        if (preview.multiAspectCount() > 0) {
            throw new IllegalStateException(
                    "Automatic scenario generation currently supports specialization pruning only. "
                            + "Detected " + preview.multiAspectCount() + " multi-aspect node(s) in the domain model.");
        }

        if (samplesPerCombination <= 0) {
            throw new IllegalArgumentException("Samples per specialization combination must be positive.");
        }

        if (preview.exceedsScenarioLimit(samplesPerCombination)) {
            throw new IllegalStateException(
                    "The requested generation would create more than " + MAX_AUTO_SCENARIOS
                            + " scenario models. Reduce the samples per combination or refine the domain model.");
        }

        String prefix = sanitizePrefix(requestedPrefix);
        JSONArray scenarioCatalog = loadScenarioCatalog(projectModel.projectDirectory);
        Set<String> existingScenarioNames = loadScenarioNames(scenarioCatalog);
        int nextIndex = nextScenarioIndex(existingScenarioNames, prefix);
        SamplingManager samplingManager = new SamplingManager();
        CurrentModelScenarioBuilder samplingBuilder = new CurrentModelScenarioBuilder();

        List<String> createdScenarioNames = new ArrayList<>();
        int[] createdCounter = {0};

        forEachCombination(preview.specializations(), 0, new LinkedHashMap<>(), selectedChildren -> {
            GenerationContext generationContext = new GenerationContext(
                    projectModel.metadataBundle,
                    selectedChildren
            );
            DefaultMutableTreeNode prunedRoot = pruneTree(projectModel.root, null, new ArrayList<>(), generationContext);
            CurrentModelScenarioBuilder.SamplingModel samplingModel =
                    samplingBuilder.buildFromMetadata(
                            generationContext.generatedVariables,
                            generationContext.generatedConstraints
                    );
            List<Map<String, String>> sampleRows =
                    generateSampleRows(samplingManager, samplingModel, generationContext.generatedVariables,
                            samplesPerCombination);
            String baseRemarks = buildRemarks(preview.specializations(), selectedChildren);

            for (int sampleIndex = 0; sampleIndex < sampleRows.size(); sampleIndex++) {
                String scenarioName = nextAvailableScenarioName(
                        prefix,
                        existingScenarioNames,
                        projectModel.projectDirectory,
                        nextIndex + createdCounter[0]
                );
                createdCounter[0]++;

                Path scenarioDirectory = projectModel.projectDirectory.resolve(scenarioName);
                Files.createDirectories(scenarioDirectory);

                Multimap<TreePath, String> sampledVariables =
                        applySampleValues(generationContext.generatedVariables, samplingModel, sampleRows.get(sampleIndex));
                writeScenarioFiles(projectModel.projectName, scenarioDirectory, prunedRoot,
                        sampledVariables, generationContext.generatedConstraints, generationContext.generatedBehaviours);
                appendScenarioRecord(scenarioCatalog, scenarioName,
                        buildSampleRemarks(baseRemarks, sampleIndex + 1, sampleRows.size()));

                existingScenarioNames.add(scenarioName);
                createdScenarioNames.add(scenarioName);
            }
        });

        saveScenarioCatalog(projectModel.projectDirectory, scenarioCatalog);

        return new GenerationResult(
                projectModel.projectName,
                preview.totalCombinations(),
                samplesPerCombination,
                createdScenarioNames
        );
    }

    public static CartesianGenerationPreview inspectCartesianProject(int levelsPerVariable)
            throws Exception {
        if (levelsPerVariable <= 0) {
            throw new IllegalArgumentException("Levels per variable must be positive.");
        }

        ProjectModel projectModel = loadProjectModel();
        GenerationPreview preview = inspectProject(projectModel);
        return inspectCartesianProject(projectModel, preview, levelsPerVariable);
    }

    public static GenerationResult generateCartesian(String requestedPrefix, int levelsPerVariable)
            throws Exception {
        if (levelsPerVariable <= 0) {
            throw new IllegalArgumentException("Levels per variable must be positive.");
        }

        ProjectModel projectModel = loadProjectModel();
        GenerationPreview preview = inspectProject(projectModel);

        if (preview.multiAspectCount() > 0) {
            throw new IllegalStateException(
                    "Automatic scenario generation currently supports specialization pruning only. "
                            + "Detected " + preview.multiAspectCount() + " multi-aspect node(s) in the domain model.");
        }

        CartesianGenerationPreview cartesianPreview =
                inspectCartesianProject(projectModel, preview, levelsPerVariable);
        if (cartesianPreview.exceedsScenarioLimit()) {
            throw new IllegalStateException(
                    "The requested Cartesian generation would create more than " + MAX_AUTO_SCENARIOS
                            + " scenario models. Reduce the levels per variable or refine the domain model.");
        }

        String prefix = sanitizePrefix(requestedPrefix);
        JSONArray scenarioCatalog = loadScenarioCatalog(projectModel.projectDirectory);
        Set<String> existingScenarioNames = loadScenarioNames(scenarioCatalog);
        int nextIndex = nextScenarioIndex(existingScenarioNames, prefix);
        CurrentModelScenarioBuilder samplingBuilder = new CurrentModelScenarioBuilder();

        List<String> createdScenarioNames = new ArrayList<>();
        int[] createdCounter = {0};

        forEachCombination(preview.specializations(), 0, new LinkedHashMap<>(), selectedChildren -> {
            GenerationContext generationContext = new GenerationContext(
                    projectModel.metadataBundle,
                    selectedChildren
            );
            DefaultMutableTreeNode prunedRoot = pruneTree(projectModel.root, null, new ArrayList<>(), generationContext);
            CurrentModelScenarioBuilder.SamplingModel samplingModel =
                    samplingBuilder.buildFromMetadata(
                            generationContext.generatedVariables,
                            generationContext.generatedConstraints
                    );
            List<Map<String, String>> sampleRows = generateCartesianRows(samplingModel, levelsPerVariable);
            String baseRemarks = buildRemarks(preview.specializations(), selectedChildren);

            for (int rowIndex = 0; rowIndex < sampleRows.size(); rowIndex++) {
                String scenarioName = nextAvailableScenarioName(
                        prefix,
                        existingScenarioNames,
                        projectModel.projectDirectory,
                        nextIndex + createdCounter[0]
                );
                createdCounter[0]++;

                Path scenarioDirectory = projectModel.projectDirectory.resolve(scenarioName);
                Files.createDirectories(scenarioDirectory);

                Multimap<TreePath, String> sampledVariables =
                        applySampleValues(generationContext.generatedVariables, samplingModel, sampleRows.get(rowIndex));
                writeScenarioFiles(projectModel.projectName, scenarioDirectory, prunedRoot,
                        sampledVariables, generationContext.generatedConstraints, generationContext.generatedBehaviours);
                appendScenarioRecord(scenarioCatalog, scenarioName,
                        buildCartesianRemarks(baseRemarks, rowIndex + 1, sampleRows.size(), levelsPerVariable));

                existingScenarioNames.add(scenarioName);
                createdScenarioNames.add(scenarioName);
            }
        });

        saveScenarioCatalog(projectModel.projectDirectory, scenarioCatalog);

        return new GenerationResult(
                projectModel.projectName,
                preview.totalCombinations(),
                levelsPerVariable,
                createdScenarioNames
        );
    }

    private static ProjectModel loadProjectModel() throws IOException, ClassNotFoundException {
        String projectName = EditorContext.getInstance().getProjName();
        Path projectDirectory = Path.of(EditorContext.getInstance().getFileLocation(), projectName);
        Path projectTreePath = projectDirectory.resolve(projectName + ".xml");

        if (!Files.exists(projectTreePath)) {
            throw new FileNotFoundException("Domain model not found: " + projectTreePath);
        }

        XmlJTree xmlJTree = new XmlJTree(projectTreePath.toString());
        if (xmlJTree.dtModel == null) {
            throw new IOException("Unable to read domain model tree: " + projectTreePath);
        }

        DefaultMutableTreeNode root = (DefaultMutableTreeNode) xmlJTree.dtModel.getRoot();
        MetadataBundle metadataBundle = new MetadataBundle(
                toPathMap(loadSerializedMultimap(projectDirectory.resolve(projectName + ".ssdvar").toFile())),
                toPathMap(loadConstraintsWithFallback(projectDirectory, projectName)),
                toPathMap(loadSerializedMultimap(projectDirectory.resolve(projectName + ".ssdbeh").toFile()))
        );

        return new ProjectModel(projectName, projectDirectory, root, metadataBundle);
    }

    @SuppressWarnings("unchecked")
    private static Multimap<TreePath, String> loadSerializedMultimap(File file)
            throws IOException, ClassNotFoundException {
        if (!file.exists()) {
            return ArrayListMultimap.create();
        }

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            return (Multimap<TreePath, String>) objectInputStream.readObject();
        }
    }

    private static Multimap<TreePath, String> loadConstraintsWithFallback(Path projectDirectory, String projectName)
            throws IOException, ClassNotFoundException {
        File combinedConstraints = projectDirectory.resolve(projectName + ".ssdcon").toFile();
        if (combinedConstraints.exists()) {
            return loadSerializedMultimap(combinedConstraints);
        }

        Multimap<TreePath, String> mergedConstraints = ArrayListMultimap.create();
        File interConstraints = projectDirectory.resolve(projectName + ".ssdintercon").toFile();
        File intraConstraints = projectDirectory.resolve(projectName + ".ssdintracons").toFile();

        if (interConstraints.exists()) {
            mergedConstraints.putAll(loadSerializedMultimap(interConstraints));
        }
        if (intraConstraints.exists()) {
            mergedConstraints.putAll(loadSerializedMultimap(intraConstraints));
        }
        return mergedConstraints;
    }

    private static Map<String, List<String>> toPathMap(Multimap<TreePath, String> source) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (TreePath key : source.keySet()) {
            String pathKey = pathKey(key);
            List<String> values = result.computeIfAbsent(pathKey, ignored -> new ArrayList<>());
            values.addAll(source.get(key));
        }
        return result;
    }

    private static void collectSpecializations(DefaultMutableTreeNode node,
                                               List<String> path,
                                               List<SpecializationDescriptor> target) {
        String label = node.toString();
        List<String> currentPath = new ArrayList<>(path);
        currentPath.add(label);

        if (label.endsWith("Spec")) {
            List<String> optionLabels = new ArrayList<>();
            for (int i = 0; i < node.getChildCount(); i++) {
                optionLabels.add(((DefaultMutableTreeNode) node.getChildAt(i)).toString());
            }
            target.add(new SpecializationDescriptor(pathKey(currentPath), label, optionLabels));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            collectSpecializations((DefaultMutableTreeNode) node.getChildAt(i), currentPath, target);
        }
    }

    private static int countNodesWithSuffix(DefaultMutableTreeNode node, String suffix) {
        int count = node.toString().endsWith(suffix) ? 1 : 0;
        for (int i = 0; i < node.getChildCount(); i++) {
            count += countNodesWithSuffix((DefaultMutableTreeNode) node.getChildAt(i), suffix);
        }
        return count;
    }

    private interface CombinationConsumer {
        void accept(Map<String, String> selectedChildren) throws Exception;
    }

    private static void forEachCombination(List<SpecializationDescriptor> descriptors,
                                           int index,
                                           LinkedHashMap<String, String> currentSelection,
                                           CombinationConsumer consumer)
            throws Exception {
        if (descriptors.isEmpty()) {
            consumer.accept(Collections.emptyMap());
            return;
        }

        if (index >= descriptors.size()) {
            consumer.accept(new LinkedHashMap<>(currentSelection));
            return;
        }

        SpecializationDescriptor descriptor = descriptors.get(index);
        if (descriptor.optionLabels.isEmpty()) {
            currentSelection.put(descriptor.pathKey, "");
            forEachCombination(descriptors, index + 1, currentSelection, consumer);
            currentSelection.remove(descriptor.pathKey);
            return;
        }

        for (String optionLabel : descriptor.optionLabels) {
            currentSelection.put(descriptor.pathKey, optionLabel);
            forEachCombination(descriptors, index + 1, currentSelection, consumer);
        }
        currentSelection.remove(descriptor.pathKey);
    }

    private static DefaultMutableTreeNode pruneTree(DefaultMutableTreeNode original,
                                                    DefaultMutableTreeNode newParent,
                                                    List<String> oldParentPath,
                                                    GenerationContext context) {
        String label = original.toString();
        List<String> oldPath = appendPath(oldParentPath, label);

        if (label.endsWith("Spec")) {
            String selectedChildLabel = context.selectedChildrenBySpecPath.get(pathKey(oldPath));
            DefaultMutableTreeNode selectedChild = findSelectedChild(original, selectedChildLabel);
            return pruneTree(selectedChild, newParent, oldPath, context);
        }

        DefaultMutableTreeNode copy = new DefaultMutableTreeNode(label);
        if (newParent != null) {
            newParent.add(copy);
        }
        copyMetadata(context.metadataBundle.variablesByPath, oldPath, context.generatedVariables, copy);
        copyMetadata(context.metadataBundle.constraintsByPath, oldPath, context.generatedConstraints, copy);
        copyMetadata(context.metadataBundle.behavioursByPath, oldPath, context.generatedBehaviours, copy);

        for (int i = 0; i < original.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) original.getChildAt(i);
            pruneTree(child, copy, oldPath, context);
        }

        return copy;
    }

    private static DefaultMutableTreeNode findSelectedChild(DefaultMutableTreeNode specializationNode,
                                                            String selectedChildLabel) {
        if (specializationNode.getChildCount() == 0) {
            throw new IllegalStateException("Specialization node has no options: " + specializationNode);
        }
        if (selectedChildLabel == null || selectedChildLabel.isBlank()) {
            return (DefaultMutableTreeNode) specializationNode.getChildAt(0);
        }
        for (int i = 0; i < specializationNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) specializationNode.getChildAt(i);
            if (selectedChildLabel.equals(child.toString())) {
                return child;
            }
        }
        throw new IllegalStateException(
                "Selected specialization child '" + selectedChildLabel + "' was not found under " + specializationNode);
    }

    private static void copyMetadata(Map<String, List<String>> source,
                                     List<String> oldPath,
                                     Multimap<TreePath, String> target,
                                     DefaultMutableTreeNode newNode) {
        List<String> values = source.get(pathKey(oldPath));
        if (values == null || values.isEmpty()) {
            return;
        }
        TreePath newPath = new TreePath(newNode.getPath());
        for (String value : values) {
            target.put(newPath, value);
        }
    }

    private static List<Map<String, String>> generateSampleRows(SamplingManager samplingManager,
                                                                CurrentModelScenarioBuilder.SamplingModel samplingModel,
                                                                Multimap<TreePath, String> generatedVariables,
                                                                int samplesPerCombination) throws Exception {
        if (generatedVariables.isEmpty() || samplingModel.scenario().getParameters().isEmpty()) {
            List<Map<String, String>> defaultRows = new ArrayList<>(samplesPerCombination);
            for (int i = 0; i < samplesPerCombination; i++) {
                defaultRows.add(new LinkedHashMap<>());
            }
            return defaultRows;
        }
        return samplingManager.generateSampleRows(samplingModel.scenario(), samplesPerCombination, false);
    }

    static List<Map<String, String>> generateCartesianRows(
            CurrentModelScenarioBuilder.SamplingModel samplingModel,
            int levelsPerVariable) {
        if (levelsPerVariable <= 0) {
            throw new IllegalArgumentException("Levels per variable must be positive.");
        }

        List<CartesianParameterDomain> parameterDomains = buildCartesianDomains(samplingModel, levelsPerVariable);
        List<String> constraints = normalizedConstraints(samplingModel);
        ConstraintEvaluator constraintEvaluator = new ConstraintEvaluator();
        List<Map<String, String>> rows = new ArrayList<>();
        buildCartesianRows(
                parameterDomains,
                constraints,
                constraintEvaluator,
                0,
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                rows
        );
        return rows;
    }

    static long projectedCartesianRowCount(CurrentModelScenarioBuilder.SamplingModel samplingModel,
                                           int levelsPerVariable) {
        if (levelsPerVariable <= 0) {
            throw new IllegalArgumentException("Levels per variable must be positive.");
        }

        long total = 1L;
        for (CartesianParameterDomain parameterDomain : buildCartesianDomains(samplingModel, levelsPerVariable)) {
            total = safeMultiply(total, parameterDomain.values().size());
        }
        return total;
    }

    static int expandedCartesianVariableCount(CurrentModelScenarioBuilder.SamplingModel samplingModel,
                                              int levelsPerVariable) {
        int count = 0;
        for (CartesianParameterDomain parameterDomain : buildCartesianDomains(samplingModel, levelsPerVariable)) {
            if (parameterDomain.values().size() > 1) {
                count++;
            }
        }
        return count;
    }

    private static List<CartesianParameterDomain> buildCartesianDomains(
            CurrentModelScenarioBuilder.SamplingModel samplingModel,
            int levelsPerVariable) {
        Map<String, CurrentModelScenarioBuilder.VariableBinding> bindingsByParameterName =
                samplingModel.bindingByParameterName();
        List<CartesianParameterDomain> parameterDomains = new ArrayList<>();

        for (Parameter parameter : samplingModel.scenario().getParameters()) {
            CurrentModelScenarioBuilder.VariableBinding binding = bindingsByParameterName.get(parameter.getName());
            if (binding == null) {
                continue;
            }
            parameterDomains.add(new CartesianParameterDomain(
                    parameter.getName(),
                    buildCartesianValues(parameter, levelsPerVariable)
            ));
        }

        return parameterDomains;
    }

    private static List<CartesianDomainValue> buildCartesianValues(Parameter parameter, int levelsPerVariable) {
        String type = normalizeParameterType(parameter);
        return switch (type) {
            case "int" -> buildNumericCartesianValues(parameter, levelsPerVariable, true);
            case "float", "double", "distribution" -> buildNumericCartesianValues(parameter, levelsPerVariable, false);
            case "boolean" -> buildBooleanCartesianValues(parameter, levelsPerVariable);
            default -> List.of(new CartesianDomainValue(defaultString(parameter.getDefaultValue(), ""), null));
        };
    }

    private static List<CartesianDomainValue> buildNumericCartesianValues(Parameter parameter,
                                                                          int levelsPerVariable,
                                                                          boolean integerType) {
        double min = parameter.getMin();
        double max = parameter.getMax();
        if (max < min) {
            double swap = min;
            min = max;
            max = swap;
        }

        if (levelsPerVariable <= 1 || Math.abs(max - min) < 1e-9) {
            double singleValue = clamp(parseDefaultNumeric(parameter, min), min, max);
            if (integerType) {
                long rounded = Math.round(singleValue);
                return List.of(new CartesianDomainValue(Long.toString(rounded), (double) rounded));
            }
            return List.of(new CartesianDomainValue(Double.toString(singleValue), singleValue));
        }

        List<CartesianDomainValue> values = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (int index = 0; index < levelsPerVariable; index++) {
            double normalized = levelsPerVariable == 1 ? 0.0 : (double) index / (levelsPerVariable - 1);
            double rawValue = min + normalized * (max - min);
            if (integerType) {
                long rounded = Math.round(rawValue);
                String csvValue = Long.toString(rounded);
                if (seen.add(csvValue)) {
                    values.add(new CartesianDomainValue(csvValue, (double) rounded));
                }
            } else {
                String csvValue = Double.toString(rawValue);
                if (seen.add(csvValue)) {
                    values.add(new CartesianDomainValue(csvValue, rawValue));
                }
            }
        }

        if (values.isEmpty()) {
            double fallback = clamp(parseDefaultNumeric(parameter, min), min, max);
            if (integerType) {
                long rounded = Math.round(fallback);
                return List.of(new CartesianDomainValue(Long.toString(rounded), (double) rounded));
            }
            return List.of(new CartesianDomainValue(Double.toString(fallback), fallback));
        }

        return values;
    }

    private static List<CartesianDomainValue> buildBooleanCartesianValues(Parameter parameter, int levelsPerVariable) {
        boolean defaultValue = Boolean.parseBoolean(defaultString(parameter.getDefaultValue(), "false"));
        if (levelsPerVariable <= 1) {
            return List.of(new CartesianDomainValue(Boolean.toString(defaultValue), defaultValue ? 1.0 : 0.0));
        }

        List<CartesianDomainValue> values = new ArrayList<>(2);
        values.add(new CartesianDomainValue(Boolean.toString(defaultValue), defaultValue ? 1.0 : 0.0));
        values.add(new CartesianDomainValue(Boolean.toString(!defaultValue), !defaultValue ? 1.0 : 0.0));
        return values;
    }

    private static void buildCartesianRows(List<CartesianParameterDomain> parameterDomains,
                                           List<String> constraints,
                                           ConstraintEvaluator constraintEvaluator,
                                           int index,
                                           LinkedHashMap<String, String> currentValues,
                                           LinkedHashMap<String, Double> currentConstraintValues,
                                           List<Map<String, String>> rows) {
        if (index >= parameterDomains.size()) {
            if (constraints.isEmpty() || satisfiesAllConstraints(constraints, currentConstraintValues, constraintEvaluator)) {
                rows.add(new LinkedHashMap<>(currentValues));
            }
            return;
        }

        CartesianParameterDomain parameterDomain = parameterDomains.get(index);
        for (CartesianDomainValue value : parameterDomain.values()) {
            currentValues.put(parameterDomain.parameterName(), value.csvValue());
            Double previousConstraintValue = currentConstraintValues.get(parameterDomain.parameterName());
            if (value.constraintValue() != null) {
                currentConstraintValues.put(parameterDomain.parameterName(), value.constraintValue());
            } else {
                currentConstraintValues.remove(parameterDomain.parameterName());
            }

            buildCartesianRows(parameterDomains, constraints, constraintEvaluator, index + 1,
                    currentValues, currentConstraintValues, rows);

            currentValues.remove(parameterDomain.parameterName());
            if (previousConstraintValue != null) {
                currentConstraintValues.put(parameterDomain.parameterName(), previousConstraintValue);
            } else {
                currentConstraintValues.remove(parameterDomain.parameterName());
            }
        }
    }

    private static boolean satisfiesAllConstraints(List<String> constraints,
                                                   Map<String, Double> currentConstraintValues,
                                                   ConstraintEvaluator constraintEvaluator) {
        for (String constraint : constraints) {
            if (!constraintEvaluator.evaluate(constraint, currentConstraintValues)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> normalizedConstraints(CurrentModelScenarioBuilder.SamplingModel samplingModel) {
        List<String> constraints = new ArrayList<>();
        for (String constraint : samplingModel.scenario().getConstraint()) {
            if (constraint == null) {
                continue;
            }
            String trimmed = constraint.trim();
            if (!trimmed.isEmpty()) {
                constraints.add(trimmed);
            }
        }
        return constraints;
    }

    private static double parseDefaultNumeric(Parameter parameter, double fallback) {
        String defaultValue = parameter.getDefaultValue();
        if (defaultValue == null || defaultValue.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(defaultValue);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String normalizeParameterType(Parameter parameter) {
        return parameter.getType() == null ? "" : parameter.getType().toLowerCase(Locale.ROOT);
    }

    private static String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Multimap<TreePath, String> applySampleValues(
            Multimap<TreePath, String> generatedVariables,
            CurrentModelScenarioBuilder.SamplingModel samplingModel,
            Map<String, String> sampleValues) {
        Multimap<TreePath, String> sampledVariables = ArrayListMultimap.create();
        Map<String, CurrentModelScenarioBuilder.VariableBinding> bindingsByParameterName =
                samplingModel.bindingByParameterName();
        Map<String, String> sampledValuesByBindingKey = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : sampleValues.entrySet()) {
            CurrentModelScenarioBuilder.VariableBinding binding = bindingsByParameterName.get(entry.getKey());
            if (binding == null) {
                continue;
            }
            sampledValuesByBindingKey.put(variableBindingKey(binding.path(), binding.variableName()), entry.getValue());
        }

        for (Map.Entry<TreePath, Collection<String>> entry : generatedVariables.asMap().entrySet()) {
            TreePath path = entry.getKey();
            for (String rawVariable : entry.getValue()) {
                String variableName = extractVariableName(rawVariable);
                String sampledValue = sampledValuesByBindingKey.get(variableBindingKey(path, variableName));
                sampledVariables.put(path, sampledValue == null ? rawVariable : replaceDefaultValue(rawVariable, sampledValue));
            }
        }

        return sampledVariables;
    }

    private static String extractVariableName(String rawVariable) {
        String[] parts = rawVariable.split(",", -1);
        return parts.length == 0 ? "" : parts[0].trim();
    }

    private static String replaceDefaultValue(String rawVariable, String sampledValue) {
        String[] parts = rawVariable.split(",", -1);
        if (parts.length < 3) {
            return rawVariable;
        }
        parts[2] = sampledValue == null ? "" : sampledValue;
        return String.join(",", parts);
    }

    private static String variableBindingKey(TreePath path, String variableName) {
        return pathKey(path) + "|" + variableName;
    }

    private static void writeScenarioFiles(String projectName,
                                           Path scenarioDirectory,
                                           DefaultMutableTreeNode prunedRoot,
                                           Multimap<TreePath, String> variables,
                                           Multimap<TreePath, String> constraints,
                                           Multimap<TreePath, String> behaviours)
            throws IOException, TransformerException {
        writeTreeXml(prunedRoot, scenarioDirectory.resolve(projectName + ".xml"));
        writeGraphXml(prunedRoot, scenarioDirectory.resolve(projectName + "Graph.xml"));
        writeReadableScenarioXml(prunedRoot, variables, scenarioDirectory.resolve("xmlforxsd.xml"));
        writeSerializedMap(variables, scenarioDirectory.resolve(projectName + ".ssdvar"));
        writeSerializedMap(constraints, scenarioDirectory.resolve(projectName + ".ssdcon"));
        writeSerializedMap(behaviours, scenarioDirectory.resolve(projectName + ".ssdbeh"));
        writeFlags(prunedRoot, scenarioDirectory.resolve(projectName + ".ssdflag"));
    }

    private static void writeTreeXml(DefaultMutableTreeNode root, Path target)
            throws TransformerException, IOException {
        try {
            DOMImplementation domImplementation =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            Document document = domImplementation.createDocument(null, "start", null);
            document.getDocumentElement().appendChild(JtreeToGraphSave.saveAllTreeNodes(document, root));
            JtreeToGraphSave.saveToXMLFile(document, target.toString());
        } catch (ParserConfigurationException e) {
            throw new IOException("Failed to build XML document for " + target, e);
        }
    }

    private static void writeReadableScenarioXml(DefaultMutableTreeNode root,
                                                 Multimap<TreePath, String> variables,
                                                 Path target)
            throws TransformerException, IOException {
        try {
            DOMImplementation domImplementation =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();
            Document document = domImplementation.createDocument(null, null, null);
            Element rootElement = buildReadableScenarioElement(document, root, new ArrayList<>(), variables, true);
            document.appendChild(rootElement);
            JtreeToGraphSave.saveToXMLFile(document, target.toString());
        } catch (ParserConfigurationException e) {
            throw new IOException("Failed to build XML document for " + target, e);
        }
    }

    private static Element buildReadableScenarioElement(Document document,
                                                        DefaultMutableTreeNode node,
                                                        List<String> parentPath,
                                                        Multimap<TreePath, String> variables,
                                                        boolean isRoot) {
        String label = node.toString();
        Element element = createReadableElement(document, label, isRoot);
        List<String> currentPath = appendPath(parentPath, label);

        for (String rawVariable : metadataValuesForPath(variables, currentPath)) {
            appendVariableElement(document, element, rawVariable);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            element.appendChild(buildReadableScenarioElement(document, child, currentPath, variables, false));
        }

        return element;
    }

    private static Element createReadableElement(Document document, String label, boolean isRoot) {
        Element element;
        if (label.endsWith("Dec")) {
            element = document.createElement("aspect");
            element.setAttribute("name", label);
            return element;
        }
        if (label.endsWith("MAsp")) {
            element = document.createElement("multiAspect");
            element.setAttribute("name", label);
            return element;
        }
        if (label.endsWith("Spec")) {
            element = document.createElement("specialization");
            element.setAttribute("name", label);
            return element;
        }

        element = document.createElement("entity");
        element.setAttribute("name", label);
        if (isRoot) {
            element.setAttribute("xmlns:vc", "http://www.w3.org/2007/XMLSchema-versioning");
            element.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            element.setAttribute("xsi:noNamespaceSchemaLocation", "ses.xsd");
        }
        return element;
    }

    private static List<String> metadataValuesForPath(Multimap<TreePath, String> source, List<String> pathParts) {
        List<String> values = new ArrayList<>();
        for (TreePath key : source.keySet()) {
            if (pathMatches(key, pathParts)) {
                values.addAll(source.get(key));
            }
        }
        return values;
    }

    private static boolean pathMatches(TreePath path, List<String> pathParts) {
        Object[] rawSegments = path.getPath();
        if (rawSegments.length != pathParts.size()) {
            return false;
        }

        for (int i = 0; i < rawSegments.length; i++) {
            if (!String.valueOf(rawSegments[i]).equals(pathParts.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static void appendVariableElement(Document document, Element parent, String rawVariable) {
        if (rawVariable == null || rawVariable.isBlank()) {
            return;
        }

        String[] parts = rawVariable.split(",", -1);
        if (parts.length < 3) {
            return;
        }

        Element variableElement = document.createElement("var");
        variableElement.setAttribute("name", parts[0].trim());
        variableElement.setAttribute("type", parts[1].trim());
        variableElement.setAttribute("default", parts[2].trim());
        if (parts.length > 3 && !parts[3].trim().isEmpty()) {
            variableElement.setAttribute("lower", parts[3].trim());
        }
        if (parts.length > 4 && !parts[4].trim().isEmpty()) {
            variableElement.setAttribute("upper", parts[4].trim());
        }
        parent.appendChild(variableElement);
    }

    private static void writeGraphXml(DefaultMutableTreeNode root, Path target) throws IOException {
        mxGraph graph = new mxGraph();
        Object parent = graph.getDefaultParent();
        Map<DefaultMutableTreeNode, Double> xPositions = new IdentityHashMap<>();
        double[] nextLeafSlot = {0.0};
        assignXPositions(root, xPositions, nextLeafSlot);
        int[] idCounter = {1};

        graph.getModel().beginUpdate();
        try {
            Object rootVertex = graph.insertVertex(parent, "rootnode", root.toString(),
                    ROOT_X + xPositions.get(root), ROOT_Y, entityWidth(root.toString()), entityHeight(root.toString()),
                    styleForLabel(root.toString()));
            addGraphChildren(graph, parent, rootVertex, root, xPositions, idCounter, 1);
        } finally {
            graph.getModel().endUpdate();
        }

        mxCodec codec = new mxCodec();
        String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
        try (FileWriter fileWriter = new FileWriter(target.toFile())) {
            fileWriter.write(xml);
        }
    }

    private static double assignXPositions(DefaultMutableTreeNode node,
                                           Map<DefaultMutableTreeNode, Double> positions,
                                           double[] nextLeafSlot) {
        if (node.getChildCount() == 0) {
            double x = nextLeafSlot[0] * X_GAP;
            positions.put(node, x);
            nextLeafSlot[0] += 1.0;
            return x;
        }

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            double childX = assignXPositions(child, positions, nextLeafSlot);
            minX = Math.min(minX, childX);
            maxX = Math.max(maxX, childX);
        }

        double x = (minX + maxX) / 2.0;
        positions.put(node, x);
        return x;
    }

    private static void addGraphChildren(mxGraph graph,
                                         Object graphParent,
                                         Object graphParentNode,
                                         DefaultMutableTreeNode treeParent,
                                         Map<DefaultMutableTreeNode, Double> xPositions,
                                         int[] idCounter,
                                         int depth) {
        for (int i = 0; i < treeParent.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) treeParent.getChildAt(i);
            String nodeId = "asg_n_" + idCounter[0]++;
            String edgeId = "asg_e_" + idCounter[0]++;
            Object graphNode = graph.insertVertex(
                    graphParent,
                    nodeId,
                    child.toString(),
                    ROOT_X + xPositions.get(child),
                    ROOT_Y + (depth * Y_GAP),
                    entityWidth(child.toString()),
                    entityHeight(child.toString()),
                    styleForLabel(child.toString())
            );
            graph.insertEdge(graphParent, edgeId, "", graphParentNode, graphNode);
            addGraphChildren(graph, graphParent, graphNode, child, xPositions, idCounter, depth + 1);
        }
    }

    private static double entityWidth(String label) {
        if (label.endsWith("Dec") || label.endsWith("Spec") || label.endsWith("MAsp")) {
            return 30.0;
        }
        return Math.max(80.0, Math.min(260.0, label.length() * 8.5));
    }

    private static double entityHeight(String label) {
        if (label.endsWith("Dec") || label.endsWith("Spec") || label.endsWith("MAsp")) {
            return 30.0;
        }
        return 30.0;
    }

    private static String styleForLabel(String label) {
        if (label.endsWith("MAsp")) {
            return "Multiaspect";
        }
        if (label.endsWith("Dec")) {
            return "Aspect";
        }
        if (label.endsWith("Spec")) {
            return "Specialization";
        }
        return "Entity";
    }

    private static void writeSerializedMap(Multimap<TreePath, String> multimap, Path target) throws IOException {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(target.toFile()))) {
            objectOutputStream.writeObject(multimap);
        }
    }

    private static void writeFlags(DefaultMutableTreeNode root, Path target) throws IOException {
        FlagVariables flagVariables = new FlagVariables();
        flagVariables.nodeNumber = countTreeNodes(root) + 1;
        flagVariables.uniformityNodeNumber = 1;

        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(target.toFile()))) {
            objectOutputStream.writeObject(flagVariables);
        }
    }

    private static int countTreeNodes(DefaultMutableTreeNode node) {
        int count = 1;
        for (int i = 0; i < node.getChildCount(); i++) {
            count += countTreeNodes((DefaultMutableTreeNode) node.getChildAt(i));
        }
        return count;
    }

    private static JSONArray loadScenarioCatalog(Path projectDirectory) throws IOException, ParseException {
        Path scenariosPath = projectDirectory.resolve("scenarios.json");
        if (!Files.exists(scenariosPath)) {
            return new JSONArray();
        }

        JSONParser parser = new JSONParser();
        try (FileReader fileReader = new FileReader(scenariosPath.toFile())) {
            Object parsed = parser.parse(fileReader);
            if (parsed instanceof JSONArray array) {
                return array;
            }
        }
        return new JSONArray();
    }

    private static Set<String> loadScenarioNames(JSONArray catalog) {
        Set<String> result = new LinkedHashSet<>();
        for (Object entry : catalog) {
            if (!(entry instanceof JSONObject entryObject)) {
                continue;
            }
            JSONObject scenarioObject = extractScenarioObject(entryObject);
            Object name = scenarioObject.get("name");
            if (name instanceof String nameString && !nameString.isBlank()) {
                result.add(nameString);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void appendScenarioRecord(JSONArray catalog, String scenarioName, String remarks) {
        JSONObject scenario = new JSONObject();
        scenario.put("name", scenarioName);
        scenario.put("risk", "");
        scenario.put("remarks", remarks);

        JSONObject wrapper = new JSONObject();
        wrapper.put("scenario", scenario);
        catalog.add(wrapper);
    }

    private static JSONObject extractScenarioObject(JSONObject entryObject) {
        Object wrapped = entryObject.get("scenario");
        if (wrapped instanceof JSONObject wrappedScenario) {
            return wrappedScenario;
        }
        return entryObject;
    }

    private static void saveScenarioCatalog(Path projectDirectory, JSONArray catalog) throws IOException {
        try (FileWriter fileWriter = new FileWriter(projectDirectory.resolve("scenarios.json").toFile())) {
            fileWriter.write(catalog.toJSONString());
        }
    }

    private static String buildRemarks(List<SpecializationDescriptor> specializations,
                                       Map<String, String> selectedChildrenBySpecPath) {
        if (specializations.isEmpty()) {
            return "Automatically generated from the domain model.";
        }

        List<String> selections = new ArrayList<>();
        for (SpecializationDescriptor descriptor : specializations) {
            String selectedChild = selectedChildrenBySpecPath.get(descriptor.pathKey);
            if (selectedChild != null && !selectedChild.isBlank()) {
                selections.add(descriptor.label + "=" + selectedChild);
            }
        }
        return "Automatically generated specialization combination: " + String.join("; ", selections);
    }

    private static String buildSampleRemarks(String baseRemarks, int sampleIndex, int totalSamples) {
        return baseRemarks + " Variable sample " + sampleIndex + " of " + totalSamples
                + " via constrained Latin Hypercube sampling.";
    }

    private static String buildCartesianRemarks(String baseRemarks,
                                                int combinationIndex,
                                                int totalCombinations,
                                                int levelsPerVariable) {
        return baseRemarks + " Cartesian variable combination " + combinationIndex + " of " + totalCombinations
                + " using " + levelsPerVariable + " level(s) per sampled variable.";
    }

    private static int nextScenarioIndex(Set<String> existingScenarioNames, String prefix) {
        int maxIndex = 0;
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT) + "_";
        for (String scenarioName : existingScenarioNames) {
            String normalizedName = scenarioName.toLowerCase(Locale.ROOT);
            if (!normalizedName.startsWith(normalizedPrefix)) {
                continue;
            }

            String suffix = scenarioName.substring(prefix.length() + 1);
            try {
                maxIndex = Math.max(maxIndex, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                // Ignore existing names that do not follow the generated numbering scheme.
            }
        }
        return maxIndex + 1;
    }

    private static String nextAvailableScenarioName(String prefix,
                                                    Set<String> existingScenarioNames,
                                                    Path projectDirectory,
                                                    int startIndex) {
        int index = Math.max(1, startIndex);
        String candidate = formatScenarioName(prefix, index);
        while (existingScenarioNames.contains(candidate) || Files.exists(projectDirectory.resolve(candidate))) {
            index++;
            candidate = formatScenarioName(prefix, index);
        }
        return candidate;
    }

    private static String formatScenarioName(String prefix, int index) {
        return prefix + "_" + String.format(Locale.ROOT, "%03d", index);
    }

    private static String sanitizePrefix(String requestedPrefix) {
        String prefix = requestedPrefix == null ? "" : requestedPrefix.trim();
        if (prefix.isEmpty()) {
            prefix = DEFAULT_PREFIX;
        }
        prefix = prefix.replaceAll("[^A-Za-z0-9_-]", "_");
        while (prefix.contains("__")) {
            prefix = prefix.replace("__", "_");
        }
        return prefix;
    }

    private static long projectedScenarioModels(long structuralCombinations, int samplesPerCombination) {
        if (samplesPerCombination <= 0) {
            return 0;
        }
        try {
            return Math.multiplyExact(structuralCombinations, (long) samplesPerCombination);
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    private static boolean exceedsScenarioLimit(long structuralCombinations, int samplesPerCombination) {
        if (samplesPerCombination <= 0) {
            return false;
        }
        return structuralCombinations > MAX_AUTO_SCENARIOS / (long) samplesPerCombination;
    }

    private static long safeMultiply(long left, int right) {
        try {
            return Math.multiplyExact(left, (long) right);
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    private static long safeAdd(long left, long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            return Long.MAX_VALUE;
        }
    }

    private static List<String> appendPath(List<String> parentPath, String label) {
        List<String> result = new ArrayList<>(parentPath);
        result.add(label);
        return result;
    }

    private static String pathKey(List<String> pathParts) {
        return "/" + String.join("/", pathParts);
    }

    private static String pathKey(TreePath path) {
        List<String> parts = new ArrayList<>();
        Object[] nodes = path.getPath();
        for (Object node : nodes) {
            parts.add(String.valueOf(node));
        }
        return pathKey(parts);
    }

    public record GenerationPreview(String projectName,
                                    List<SpecializationDescriptor> specializations,
                                    int multiAspectCount,
                                    long totalCombinations) {
        public long projectedScenarioModels(int samplesPerCombination) {
            return AutomaticScenarioGeneration.projectedScenarioModels(totalCombinations, samplesPerCombination);
        }

        public boolean exceedsScenarioLimit(int samplesPerCombination) {
            return AutomaticScenarioGeneration.exceedsScenarioLimit(totalCombinations, samplesPerCombination);
        }
    }

    public record CartesianGenerationPreview(String projectName,
                                             List<SpecializationDescriptor> specializations,
                                             int multiAspectCount,
                                             long structuralCombinationCount,
                                             long projectedScenarioModels,
                                             int minExpandedVariableCount,
                                             int maxExpandedVariableCount,
                                             int levelsPerVariable) {
        public boolean exceedsScenarioLimit() {
            return projectedScenarioModels > MAX_AUTO_SCENARIOS;
        }
    }

    public record GenerationResult(String projectName,
                                   long structuralCombinationCount,
                                   int samplesPerCombination,
                                   List<String> createdScenarioNames) {
        public int createdCount() {
            return createdScenarioNames.size();
        }

        public long projectedScenarioModels() {
            return AutomaticScenarioGeneration.projectedScenarioModels(structuralCombinationCount, samplesPerCombination);
        }
    }

    public record SpecializationDescriptor(String pathKey, String label, List<String> optionLabels) {}

    private record ProjectModel(String projectName,
                                Path projectDirectory,
                                DefaultMutableTreeNode root,
                                MetadataBundle metadataBundle) {}

    private record MetadataBundle(Map<String, List<String>> variablesByPath,
                                  Map<String, List<String>> constraintsByPath,
                                  Map<String, List<String>> behavioursByPath) {}

    private static final class GenerationContext {
        private final MetadataBundle metadataBundle;
        private final Map<String, String> selectedChildrenBySpecPath;
        private final Multimap<TreePath, String> generatedVariables = ArrayListMultimap.create();
        private final Multimap<TreePath, String> generatedConstraints = ArrayListMultimap.create();
        private final Multimap<TreePath, String> generatedBehaviours = ArrayListMultimap.create();

        private GenerationContext(MetadataBundle metadataBundle,
                                  Map<String, String> selectedChildrenBySpecPath) {
            this.metadataBundle = metadataBundle;
            this.selectedChildrenBySpecPath = selectedChildrenBySpecPath;
        }
    }

    private record CartesianParameterDomain(String parameterName, List<CartesianDomainValue> values) {}

    private record CartesianDomainValue(String csvValue, Double constraintValue) {}

    private static CartesianGenerationPreview inspectCartesianProject(ProjectModel projectModel,
                                                                     GenerationPreview preview,
                                                                     int levelsPerVariable)
            throws Exception {
        CurrentModelScenarioBuilder samplingBuilder = new CurrentModelScenarioBuilder();
        long[] projectedScenarioModels = {0L};
        int[] minExpandedVariables = {Integer.MAX_VALUE};
        int[] maxExpandedVariables = {0};

        forEachCombination(preview.specializations(), 0, new LinkedHashMap<>(), selectedChildren -> {
            GenerationContext generationContext = new GenerationContext(
                    projectModel.metadataBundle,
                    selectedChildren
            );
            pruneTree(projectModel.root, null, new ArrayList<>(), generationContext);
            CurrentModelScenarioBuilder.SamplingModel samplingModel =
                    samplingBuilder.buildFromMetadata(
                            generationContext.generatedVariables,
                            generationContext.generatedConstraints
                    );

            projectedScenarioModels[0] = safeAdd(
                    projectedScenarioModels[0],
                    projectedCartesianRowCount(samplingModel, levelsPerVariable)
            );

            int expandedVariables = expandedCartesianVariableCount(samplingModel, levelsPerVariable);
            minExpandedVariables[0] = Math.min(minExpandedVariables[0], expandedVariables);
            maxExpandedVariables[0] = Math.max(maxExpandedVariables[0], expandedVariables);
        });

        if (minExpandedVariables[0] == Integer.MAX_VALUE) {
            minExpandedVariables[0] = 0;
        }

        return new CartesianGenerationPreview(
                projectModel.projectName,
                preview.specializations(),
                preview.multiAspectCount(),
                preview.totalCombinations(),
                projectedScenarioModels[0],
                minExpandedVariables[0],
                maxExpandedVariables[0],
                levelsPerVariable
        );
    }
}
