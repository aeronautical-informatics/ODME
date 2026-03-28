package odme.domain.transform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Translates parsed YAML data (as produced by SnakeYAML) into a Python
 * literal string.
 *
 * <p>Extracted from {@code odme.odmeeditor.Execution} so that the translation
 * logic can be tested and reused without any Swing dependency.</p>
 */
public class YamlToPythonTranslator {

    /**
     * Translates a parsed YAML object into a complete Python source string.
     *
     * @param yamlData the parsed YAML data (typically a {@link Map}, {@link List}, or scalar)
     * @return the Python source string
     * @throws IllegalArgumentException if yamlData is null
     */
    public String translate(Object yamlData) {
        if (yamlData == null) {
            throw new IllegalArgumentException("YAML data is null");
        }
        Object cleaned = cleanYamlData(yamlData);
        StringBuilder pythonCode = new StringBuilder("# Translated YAML to Python\n\n");
        pythonCode.append("config = ");
        generatePythonFromYaml(cleaned, pythonCode, 0);
        pythonCode.append("\n");
        return pythonCode.toString();
    }

    /**
     * Recursively generates Python code from a YAML data structure.
     * Delegates to {@link #handleMap}, {@link #handleList}, or
     * {@link #formatPythonValue} depending on the runtime type.
     *
     * @param yamlData    the YAML data fragment
     * @param pythonCode  accumulator for the generated source
     * @param indentLevel current indentation depth (4 spaces per level)
     */
    void generatePythonFromYaml(Object yamlData, StringBuilder pythonCode, int indentLevel) {
        String indent = "    ".repeat(indentLevel);

        if (yamlData instanceof Map) {
            handleMap((Map<?, ?>) yamlData, pythonCode, indentLevel, indent);
        } else if (yamlData instanceof List) {
            handleList((List<?>) yamlData, pythonCode, indentLevel, indent);
        } else {
            pythonCode.append(formatPythonValue(yamlData));
        }
    }

    /**
     * Renders a {@link Map} as a Python dictionary literal.
     */
    void handleMap(Map<?, ?> map, StringBuilder pythonCode, int indentLevel, String indent) {
        pythonCode.append("{\n");
        boolean firstEntry = true;

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString().trim();
            if (key.startsWith("-")) {
                key = key.substring(1).trim();
            }
            Object value = entry.getValue();

            if (!firstEntry) {
                pythonCode.append(",\n");
            }
            String childIndent = "    ".repeat(indentLevel + 1);

            pythonCode.append(childIndent)
                    .append("'")
                    .append(key.replace("'", "\\'"))
                    .append("': ");
            generatePythonFromYaml(value, pythonCode, indentLevel + 1);
            firstEntry = false;
        }
        pythonCode.append("\n").append(indent).append("}");
    }

    /**
     * Renders a {@link List} as a Python list literal.
     */
    void handleList(List<?> list, StringBuilder pythonCode, int indentLevel, String indent) {
        pythonCode.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            String childIndent = "    ".repeat(indentLevel + 1);
            pythonCode.append(childIndent);
            generatePythonFromYaml(list.get(i), pythonCode, indentLevel + 1);
            if (i < list.size() - 1) {
                pythonCode.append(",");
            }
            pythonCode.append("\n");
        }
        pythonCode.append(indent).append("]");
    }

    /**
     * Formats a scalar value as a Python literal.
     *
     * <ul>
     *   <li>{@code null} becomes {@code None}</li>
     *   <li>Empty maps become {@code {}}</li>
     *   <li>Numeric strings are unquoted</li>
     *   <li>Booleans become {@code True}/{@code False}</li>
     *   <li>Everything else is single-quoted with escaping</li>
     * </ul>
     *
     * @param value the value to format
     * @return the Python literal string
     */
    String formatPythonValue(Object value) {
        if (value == null) return "None";
        if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) return "{}";

        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty()) return "None";
            try {
                Double.parseDouble(str);
                return str;
            } catch (NumberFormatException e) {
                return "'" + str.replace("'", "\\'") + "'";
            }
        }
        if (value instanceof Number) return value.toString();
        if (value instanceof Boolean) return (Boolean) value ? "True" : "False";
        return "'" + value.toString().replace("'", "\\'") + "'";
    }

    /**
     * Recursively cleans YAML data by trimming map keys and removing
     * leading hyphens that are sometimes an artifact of YAML list notation.
     *
     * @param data the raw parsed YAML object
     * @return a cleaned copy of the data
     */
    Object cleanYamlData(Object data) {
        if (data instanceof Map) {
            Map<Object, Object> map = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) data).entrySet()) {
                String key = entry.getKey().toString().trim();
                if (key.startsWith("-")) {
                    key = key.substring(1).trim();
                }
                map.put(key, cleanYamlData(entry.getValue()));
            }
            return map;
        } else if (data instanceof List) {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<?>) data) {
                list.add(cleanYamlData(item));
            }
            return list;
        } else {
            return data;
        }
    }
}
