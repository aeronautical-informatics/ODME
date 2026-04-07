package ses.utils;

import ses.EntityStructure;
import ses.Node;

/**
 * Prints an EntityStructure or Node as an ASCII tree to stdout.
 *
 * Example output:
 *   Vehicle [ENTITY]
 *   └── VehicleDecomposition [ASPECT]
 *       ├── PowerSource [ENTITY]
 *       └── Driver [ENTITY]
 */
public class TreePrinter {

    public static void print(EntityStructure es) {
        System.out.println(es.getName());
        printNode(es.getRoot(), "", true);
    }

    public static void print(Node node) {
        printNode(node, "", true);
    }

    private static void printNode(Node node, String prefix, boolean isLast) {
        String connector = isLast ? "└── " : "├── ";
        System.out.println(prefix + connector + node.getName() + " [" + node.getType() + "]");

        String childPrefix = prefix + (isLast ? "    " : "│   ");
        for (int i = 0; i < node.getChildren().size(); i++) {
            boolean lastChild = i == node.getChildren().size() - 1;
            printNode(node.getChildren().get(i), childPrefix, lastChild);
        }
    }
}
