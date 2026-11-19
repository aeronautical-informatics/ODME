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
     *   <li>[0] = component name (with leading spaces indicating depth; variables start with "-")</li>
     *   <li>[1] = type ("Node" or "Variable")</li>
     *   <li>[2] = data type (for variables)</li>
     *   <li>[3] = lower bound / min (for variables, may be null)</li>
     *   <li>[4] = upper bound / max (for variables, may be null)</li>
     *   <li>[5] = optional comment (ODD annotation)</li>
     * </ul>
     *
     * <p>The output is a YAML mapping that {@code odme.sampling.ScenarioParser} reads back: every node is a key,
     * and every variable is a key inside the node it belongs to, with {@code type}, {@code min}, {@code max} and
     * {@code comment} as its details. A variable row belongs to the node row directly above its block, which is how
     * {@link XsdParser} emits them (attributes right after their element).</p>
     *
     * @param xsdRows the parsed XSD data rows
     * @return the YAML string
     */
    public String convert(List<String[]> xsdRows) {
        StringBuilder yaml = new StringBuilder();
        // Stack holds pairs: [leading spaces of the node row, YAML level of the node]
        Deque<int[]> nodeStack = new ArrayDeque<>();
        int lastNodeLevel = -1;

        for (String[] row : xsdRows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            String componentName = row[0].replaceAll("^\\s*-?", "").trim();
            if (componentName.isEmpty()) {
                continue;
            }
            String type = row[1] == null ? "" : row[1].trim();

            if (type.equals("Node")) {
                int spaces = countLeadingSpaces(row[0]);
                while (!nodeStack.isEmpty() && nodeStack.peek()[0] >= spaces) {
                    nodeStack.pop();
                }
                int level = nodeStack.isEmpty() ? 0 : nodeStack.peek()[1] + 1;
                yaml.append(getIndent(level)).append(key(componentName)).append(":\n");
                String comment = cell(row, 5);
                if (!comment.isEmpty()) {
                    yaml.append(getIndent(level + 1)).append("comment: ").append(quote(comment)).append("\n");
                }
                nodeStack.push(new int[]{spaces, level});
                lastNodeLevel = level;
            } else if (type.equals("Variable")) {
                int level = lastNodeLevel + 1;
                yaml.append(getIndent(level)).append(key(componentName)).append(":");
                StringBuilder details = new StringBuilder();
                String dataType = cell(row, 2);
                appendDetail(details, level + 1, "type", dataType.isEmpty() ? "string" : dataType, false);
                appendDetail(details, level + 1, "min", cell(row, 3), true);
                appendDetail(details, level + 1, "max", cell(row, 4), true);
                appendDetail(details, level + 1, "comment", cell(row, 5), false);
                yaml.append(details.length() == 0 ? " {}\n" : "\n" + details);
            }
        }

        return yaml.toString();
    }

    private static void appendDetail(StringBuilder out, int level, String name, String value, boolean numeric) {
        if (value.isEmpty()) {
            return;
        }
        out.append(getIndent(level)).append(name).append(": ")
                .append(numeric && isNumber(value) ? value : (name.equals("type") && isPlain(value) ? value : quote(value)))
                .append("\n");
    }

    private static String cell(String[] row, int index) {
        return index < row.length && row[index] != null ? row[index].trim() : "";
    }

    private static boolean isNumber(String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isPlain(String value) {
        return value.matches("[A-Za-z_][A-Za-z0-9_.-]*")
                && !value.matches("(?i)true|false|yes|no|on|off|null|~");
    }

    private static String key(String name) {
        return isPlain(name) ? name : quote(name);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\"";
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
