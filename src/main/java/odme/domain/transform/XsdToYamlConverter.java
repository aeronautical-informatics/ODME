package odme.domain.transform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Converts XSD data (as parsed by {@link XsdParser}) into a human-readable
 * YAML representation.
 *
 * <p>Extracted from {@code odme.odmeeditor.ODDManager} so that the conversion
 * logic can be tested and reused without any Swing dependency.</p>
 */
public class XsdToYamlConverter {

    /**
     * Converts a list of XSD row data into a YAML string.
     *
     * <p>Each row is a {@code String[]} where:
     * <ul>
     *   <li>[0] = component name (with leading spaces indicating depth)</li>
     *   <li>[1] = type ("Node" or "Variable")</li>
     *   <li>[2] = data type (for variables)</li>
     *   <li>[3] = lower bound / min (for variables, may be null)</li>
     *   <li>[4] = upper bound / max (for variables, may be null)</li>
     * </ul>
     *
     * @param xsdRows the parsed XSD data rows
     * @return the YAML string
     */
    public String convert(List<String[]> xsdRows) {
        StringBuilder yaml = new StringBuilder();

        // Stack holds pair: [xsdIndent, yamlIndent]
        Deque<int[]> indentStack = new ArrayDeque<>();
        indentStack.push(new int[]{-1, -1});

        for (String[] row : xsdRows) {
            int xsdIndent = countLeadingSpaces(row[0]) / 2;
            String componentName = row[0].replaceAll("^\\s*-?", "").trim();
            if (componentName.isEmpty()) {
                continue;
            }
            String type = row[1].trim();

            // Pop until the current xsdIndent is greater than the one at stack top.
            while (!indentStack.isEmpty() && indentStack.peek()[0] >= xsdIndent) {
                indentStack.pop();
            }

            int parentYamlIndent = indentStack.isEmpty() ? -1 : indentStack.peek()[1];
            int currentYamlIndent = parentYamlIndent + 1;

            if (type.equals("Node")) {
                yaml.append(getIndent(currentYamlIndent))
                        .append(componentName)
                        .append(":\n");
                indentStack.push(new int[]{xsdIndent, currentYamlIndent});
            } else if (type.equals("Variable")) {
                yaml.append(getIndent(currentYamlIndent))
                        .append("- ").append(componentName).append(":\n")
                        .append(getIndent(currentYamlIndent + 1))
                        .append("type: ").append(row[2]).append("\n");

                if (row[3] != null && !row[3].isEmpty()) {
                    yaml.append(getIndent(currentYamlIndent + 1))
                            .append("min: ").append(row[3]).append("\n");
                }
                if (row[4] != null && !row[4].isEmpty()) {
                    yaml.append(getIndent(currentYamlIndent + 1))
                            .append("max: ").append(row[4]).append("\n");
                }
            }
        }

        return yaml.toString();
    }

    /**
     * Counts the number of leading space characters in a string.
     *
     * @param s the input string
     * @return the number of leading spaces
     */
    public static int countLeadingSpaces(String s) {
        return (int) s.chars().takeWhile(c -> c == ' ').count();
    }

    /**
     * Returns a YAML indentation string for the given level (2 spaces per level).
     *
     * @param level the indentation level (0-based)
     * @return the indentation string
     */
    public static String getIndent(int level) {
        return "  ".repeat(Math.max(0, level));
    }
}
