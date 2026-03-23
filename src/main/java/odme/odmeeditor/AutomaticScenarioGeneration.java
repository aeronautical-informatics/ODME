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
import odme.sampling.CurrentModelScenarioBuilder;
import odme.sampling.SamplingManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

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
}
