package ses.utils;

import ses.EntityStructure;
import ses.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * Exports an EntityStructure or Node as a Mermaid graph diagram.
 *
 * Paste the output into https://mermaid.live to render the graph.
 *
 * Example output:
 *   graph TD
 *       n0["Vehicle (ENTITY)"]
 *       n1["VehicleDecomposition (ASPECT)"]
 *       n2["PowerSource (ENTITY)"]
 *       n3["Driver (ENTITY)"]
 *       n0 --> n1
 *       n1 --> n2
 *       n1 --> n3
 */
public class MermaidExporter {

    public static void print(EntityStructure es) {
        System.out.println(export(es.getRoot()));
    }

    public static void print(Node node) {
        System.out.println(export(node));
    }

    public static String export(EntityStructure es) {
        return export(es.getRoot());
    }

    public static String export(Node root) {
        List<String> nodeLines = new ArrayList<>();
        List<String> edgeLines = new ArrayList<>();
        int[] counter = {0};
        collectLines(root, counter, nodeLines, edgeLines, -1);
        StringBuilder sb = new StringBuilder("graph TD\n");
        for (String line : nodeLines) sb.append("    ").append(line).append("\n");
        for (String line : edgeLines) sb.append("    ").append(line).append("\n");
        return sb.toString();
    }

    private static void collectLines(Node node, int[] counter, List<String> nodeLines,
                                     List<String> edgeLines, int parentId) {
        int myId = counter[0]++;
        nodeLines.add("n" + myId + "[\"" + node.getName() + " (" + node.getType() + ")\"]");
        if (parentId >= 0) {
            edgeLines.add("n" + parentId + " --> n" + myId);
        }
        for (Node child : node.getChildren()) {
            collectLines(child, counter, nodeLines, edgeLines, myId);
        }
    }
}
