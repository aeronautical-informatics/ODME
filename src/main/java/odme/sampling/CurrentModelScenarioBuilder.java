package odme.sampling;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import odme.core.EditorContext;
import odme.sampling.model.Parameter;
import odme.sampling.model.Scenario;

import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a sampling {@link Scenario} directly from the currently opened ODME
 * model by reading its serialized variable and constraint metadata.
 */
public class CurrentModelScenarioBuilder {

    private static final Pattern CONSTRAINT_TOKEN_PATTERN = Pattern.compile("@([A-Za-z0-9_@\\-]+)");

    public Scenario buildFromCurrentContext() throws IOException, ClassNotFoundException {
        return build(Path.of(EditorContext.getInstance().getWorkingDir()), EditorContext.getInstance().getProjName());
    }

    Scenario build(Path workingDir, String modelName) throws IOException, ClassNotFoundException {
        File variableFile = workingDir.resolve(modelName + ".ssdvar").toFile();
        if (!variableFile.exists()) {
            throw new FileNotFoundException("Current model variables were not found: " + variableFile);
        }

        Multimap<TreePath, String> variablesByPath = readSerializedMultimap(variableFile);
        Multimap<TreePath, String> constraintsByPath = loadConstraintsWithFallback(workingDir, modelName);

        return buildScenario(variablesByPath, constraintsByPath);
    }

    @SuppressWarnings("unchecked")
    private Multimap<TreePath, String> readSerializedMultimap(File file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file))) {
            return (Multimap<TreePath, String>) objectInputStream.readObject();
        }
    }

    private Multimap<TreePath, String> loadConstraintsWithFallback(Path workingDir, String modelName)
            throws IOException, ClassNotFoundException {
        File combinedConstraints = workingDir.resolve(modelName + ".ssdcon").toFile();
        if (combinedConstraints.exists()) {
            return readSerializedMultimap(combinedConstraints);
        }

        Multimap<TreePath, String> mergedConstraints = ArrayListMultimap.create();
        File interConstraints = workingDir.resolve(modelName + ".ssdintercon").toFile();
        File intraConstraints = workingDir.resolve(modelName + ".ssdintracons").toFile();

        if (interConstraints.exists()) {
            mergedConstraints.putAll(readSerializedMultimap(interConstraints));
        }
        if (intraConstraints.exists()) {
            mergedConstraints.putAll(readSerializedMultimap(intraConstraints));
        }
        return mergedConstraints;
    }

    private Scenario buildScenario(Multimap<TreePath, String> variablesByPath,
                                   Multimap<TreePath, String> constraintsByPath) {
        List<VariableBinding> bindings = new ArrayList<>();
        for (Map.Entry<TreePath, Collection<String>> entry : variablesByPath.asMap().entrySet()) {
            for (String rawVariable : entry.getValue()) {
                VariableBinding binding = toBinding(entry.getKey(), rawVariable);
                if (binding != null) {
                    bindings.add(binding);
                }
            }
        }

        assignCanonicalNames(bindings);
        Map<String, List<VariableBinding>> aliasIndex = buildAliasIndex(bindings);

        Scenario scenario = new Scenario();
        List<Parameter> parameters = bindings.stream()
                .map(VariableBinding::parameter)
                .toList();
        scenario.setParameters(parameters);

        List<String> constraints = new ArrayList<>();
        for (Map.Entry<TreePath, Collection<String>> entry : constraintsByPath.asMap().entrySet()) {
            List<String> contextPath = toSegments(entry.getKey());
            for (String rawConstraint : entry.getValue()) {
                if (rawConstraint == null || rawConstraint.trim().isEmpty()) {
                    continue;
                }
                constraints.add(canonicalizeConstraint(rawConstraint, contextPath, bindings, aliasIndex));
            }
        }
        scenario.setConstraint(constraints);
        return scenario;
    }

    private VariableBinding toBinding(TreePath path, String rawVariable) {
        if (rawVariable == null || rawVariable.trim().isEmpty()) {
            return null;
        }

        String[] parts = rawVariable.split(",", -1);
        if (parts.length < 3) {
            return null;
        }

        String variableName = parts[0].trim();
        String type = parts[1].trim().toLowerCase(Locale.ROOT);
        String defaultValue = parts[2].trim();

        Parameter parameter = new Parameter();
        parameter.setDefaultValue(defaultValue);

        switch (type) {
            case "int", "float", "double" -> {
                parameter.setType(type);
                parameter.setMin(parseDouble(parts, 3, defaultValue));
                parameter.setMax(parseDouble(parts, 4, defaultValue));
            }
            case "boolean" -> parameter.setType("boolean");
            case "distribution" -> {
                parameter.setType("distribution");
                parameter.setMin(parseDouble(parts, 3, defaultValue));
                parameter.setMax(parseDouble(parts, 4, defaultValue));
            }
            default -> parameter.setType("fixed");
        }

        return new VariableBinding(path, toSegments(path), variableName, parameter);
    }

    private double parseDouble(String[] parts, int index, String fallback) {
        String candidate = index < parts.length ? parts[index].trim() : fallback;
        if (candidate == null || candidate.isBlank() || "none".equalsIgnoreCase(candidate)) {
            candidate = fallback;
        }
        return Double.parseDouble(candidate);
    }

    private void assignCanonicalNames(List<VariableBinding> bindings) {
        List<VariableBinding> unresolved = new ArrayList<>(bindings);
        int maxDepth = bindings.stream().mapToInt(binding -> binding.pathSegments.size()).max().orElse(1);

        for (int depth = 1; depth <= maxDepth && !unresolved.isEmpty(); depth++) {
            Map<String, List<VariableBinding>> candidateGroups = new LinkedHashMap<>();
            for (VariableBinding binding : unresolved) {
                String candidate = buildCandidateName(binding, depth);
                candidateGroups.computeIfAbsent(candidate, ignored -> new ArrayList<>()).add(binding);
            }

            List<VariableBinding> resolvedInThisRound = new ArrayList<>();
            for (Map.Entry<String, List<VariableBinding>> entry : candidateGroups.entrySet()) {
                if (entry.getValue().size() == 1) {
                    VariableBinding binding = entry.getValue().get(0);
                    binding.parameter.setName(entry.getKey());
                    resolvedInThisRound.add(binding);
                }
            }
            unresolved.removeAll(resolvedInThisRound);
        }

        Map<String, Integer> collisionCount = new LinkedHashMap<>();
        for (VariableBinding binding : unresolved) {
            String fallbackName = buildCandidateName(binding, binding.pathSegments.size());
            int count = collisionCount.merge(fallbackName, 1, Integer::sum);
            binding.parameter.setName(count == 1 ? fallbackName : fallbackName + "_" + count);
        }
    }

    private String buildCandidateName(VariableBinding binding, int depth) {
        int start = Math.max(0, binding.pathSegments.size() - depth);
        List<String> suffix = binding.pathSegments.subList(start, binding.pathSegments.size());
        String prefix = suffix.stream()
                .map(CurrentModelScenarioBuilder::safeToken)
                .filter(token -> !token.isBlank())
                .reduce((left, right) -> left + "_" + right)
                .orElse("");
        String variableToken = safeToken(binding.variableName);
        return prefix.isBlank() ? variableToken : prefix + "_" + variableToken;
    }

    private Map<String, List<VariableBinding>> buildAliasIndex(List<VariableBinding> bindings) {
        Map<String, List<VariableBinding>> aliasIndex = new LinkedHashMap<>();
        for (VariableBinding binding : bindings) {
            for (String alias : buildAliases(binding)) {
                String normalized = normalizeReference(alias);
                if (normalized.isBlank()) {
                    continue;
                }
                aliasIndex.computeIfAbsent(normalized, ignored -> new ArrayList<>()).add(binding);
            }
        }
        return aliasIndex;
    }

    private Set<String> buildAliases(VariableBinding binding) {
        Set<String> aliases = new LinkedHashSet<>();
        aliases.add(binding.parameter.getName());
        aliases.add(binding.variableName);
        aliases.add(binding.parameter.getName().replace("_", ""));

        for (int start = 0; start < binding.pathSegments.size(); start++) {
            List<String> suffix = binding.pathSegments.subList(start, binding.pathSegments.size());
            addAliasVariant(aliases, suffix, binding.variableName, "@");
            addAliasVariant(aliases, suffix, binding.variableName, "_");
            addAliasVariant(aliases, suffix, binding.variableName, "");
        }
        return aliases;
    }

    private void addAliasVariant(Set<String> aliases, List<String> pathSegments, String variableName, String delimiter) {
        List<String> sanitizedSegments = pathSegments.stream()
                .map(CurrentModelScenarioBuilder::safeToken)
                .filter(token -> !token.isBlank())
                .toList();
        if (sanitizedSegments.isEmpty()) {
            aliases.add(variableName);
            return;
        }

        String path = String.join(delimiter, sanitizedSegments);
        String variableToken = safeToken(variableName);
        if (delimiter.isEmpty()) {
            aliases.add(path + variableToken);
        } else {
            aliases.add(path + delimiter + variableToken);
        }
    }

    private String canonicalizeConstraint(String rawConstraint,
                                          List<String> contextPath,
                                          List<VariableBinding> bindings,
                                          Map<String, List<VariableBinding>> aliasIndex) {
        Matcher matcher = CONSTRAINT_TOKEN_PATTERN.matcher(rawConstraint);
        StringBuffer rewritten = new StringBuffer();
        List<String> unresolved = new ArrayList<>();

        while (matcher.find()) {
            String tokenBody = matcher.group(1);
            VariableBinding resolved = resolveToken(tokenBody, contextPath, bindings, aliasIndex);
            if (resolved == null) {
                unresolved.add(tokenBody);
                continue;
            }
            if (!supportsConstraintEvaluation(resolved.parameter)) {
                throw new IllegalStateException(
                        "Constraint references non-numeric variable '" + tokenBody + "'. "
                                + "Only numeric and boolean constraints are supported for automatic sampling.");
            }
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement("@" + resolved.parameter.getName()));
        }
        matcher.appendTail(rewritten);

        if (!unresolved.isEmpty()) {
            throw new IllegalStateException(
                    "Constraint references unknown or ambiguous variable(s) " + unresolved + ": " + rawConstraint);
        }

        return rewritten.toString();
    }

    private boolean supportsConstraintEvaluation(Parameter parameter) {
        return switch (parameter.getType()) {
            case "int", "float", "double", "distribution", "boolean" -> true;
            default -> false;
        };
    }

    private VariableBinding resolveToken(String tokenBody,
                                         List<String> contextPath,
                                         List<VariableBinding> bindings,
                                         Map<String, List<VariableBinding>> aliasIndex) {
        String normalizedToken = normalizeReference(tokenBody);
        if (normalizedToken.isBlank()) {
            return null;
        }

        if (!tokenBody.contains("@")) {
            List<VariableBinding> localMatches = bindings.stream()
                    .filter(binding -> binding.pathSegments.equals(contextPath))
                    .filter(binding -> normalizeReference(binding.variableName).equals(normalizedToken))
                    .toList();
            VariableBinding local = uniqueMatch(localMatches);
            if (local != null) {
                return local;
            }
        }

        VariableBinding aliasMatch = uniqueMatch(aliasIndex.get(normalizedToken));
        if (aliasMatch != null) {
            return aliasMatch;
        }

        if (!tokenBody.contains("@")) {
            List<VariableBinding> variableNameMatches = bindings.stream()
                    .filter(binding -> normalizeReference(binding.variableName).equals(normalizedToken))
                    .toList();
            return uniqueMatch(variableNameMatches);
        }

        return null;
    }

    private VariableBinding uniqueMatch(List<VariableBinding> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }

        VariableBinding first = matches.get(0);
        for (VariableBinding match : matches) {
            if (!match.parameter.getName().equals(first.parameter.getName())) {
                return null;
            }
        }
        return first;
    }

    private static List<String> toSegments(TreePath path) {
        Object[] rawSegments = path.getPath();
        List<String> segments = new ArrayList<>(rawSegments.length);
        for (Object rawSegment : rawSegments) {
            segments.add(rawSegment.toString());
        }
        return segments;
    }

    private static String safeToken(String token) {
        return token == null ? "" : token.replaceAll("[^A-Za-z0-9_]", "");
    }

    private static String normalizeReference(String token) {
        return safeToken(token).toLowerCase(Locale.ROOT);
    }

    private record VariableBinding(TreePath path,
                                   List<String> pathSegments,
                                   String variableName,
                                   Parameter parameter) {
    }
}
