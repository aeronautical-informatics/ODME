package ses;

import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the six axioms of a System Entity Structure.
 *
 * The six axioms are:
 *   1. Uniformity         — nodes with the same name have isomorphic subtrees
 *   2. Strict Hierarchy   — a name does not appear more than once on any root-to-leaf path
 *   3. Alternating Mode   — Entity children must be Aspect/Specialization/Multi-Aspect, and vice versa
 *   4. Valid Brothers     — no two siblings share the same name
 *   5. Attached Variables — variable names on the same node must be distinct
 *   6. Inheritance        — Specialization nodes inherit variables and Aspects from their parent Entity
 *                           (semantic rule; enforced at modeling time, not checked structurally here)
 */
public class AxiomValidator {

    public static class Violation {
        private final String axiom;
        private final String message;

        public Violation(String axiom, String message) {
            this.axiom = axiom;
            this.message = message;
        }

        public String getAxiom()   { return axiom; }
        public String getMessage() { return message; }

        @Override
        public String toString() { return "[" + axiom + "] " + message; }
    }

    /** Runs all checkable axioms and returns every violation found. Empty list means valid. */
    public static List<Violation> validate(EntityStructure es) {
        List<Violation> violations = new ArrayList<>();
        violations.addAll(checkUniformity(es.getRoot()));
        violations.addAll(checkStrictHierarchy(es.getRoot()));
        violations.addAll(checkAlternatingMode(es.getRoot()));
        violations.addAll(checkValidBrothers(es.getRoot()));
        violations.addAll(checkAttachedVariables(es.getRoot()));
        return violations;
    }

    // Axiom 1: Uniformity — nodes with the same name must have isomorphic subtrees
    public static List<Violation> checkUniformity(Node root) {
        List<Violation> violations = new ArrayList<>();
        checkUniformityRecursive(root, new HashMap<>(), violations);
        return violations;
    }

    private static void checkUniformityRecursive(Node node, Map<String, Node> seen, List<Violation> violations) {
        if (seen.containsKey(node.getName())) {
            if (!isIsomorphic(seen.get(node.getName()), node)) {
                violations.add(new Violation("Uniformity",
                        "Nodes named '" + node.getName() + "' exist with non-isomorphic subtrees"));
            }
        } else {
            seen.put(node.getName(), node);
        }
        for (Node child : node.getChildren()) {
            checkUniformityRecursive(child, seen, violations);
        }
    }

    private static boolean isIsomorphic(Node a, Node b) {
        if (!a.getName().equals(b.getName()) || a.getType() != b.getType()) return false;
        if (a.getChildren().size() != b.getChildren().size()) return false;
        for (int i = 0; i < a.getChildren().size(); i++) {
            if (!isIsomorphic(a.getChildren().get(i), b.getChildren().get(i))) return false;
        }
        return true;
    }

    // Axiom 2: Strict Hierarchy — a name cannot appear more than once on any root-to-leaf path
    public static List<Violation> checkStrictHierarchy(Node root) {
        List<Violation> violations = new ArrayList<>();
        checkStrictHierarchyRecursive(root, new ArrayDeque<>(), violations);
        return violations;
    }

    private static void checkStrictHierarchyRecursive(Node node, Deque<String> path, List<Violation> violations) {
        if (path.contains(node.getName())) {
            violations.add(new Violation("Strict Hierarchy",
                    "'" + node.getName() + "' appears more than once on a single tree path"));
        }
        path.addLast(node.getName());
        for (Node child : node.getChildren()) {
            checkStrictHierarchyRecursive(child, path, violations);
        }
        path.removeLast();
    }

    // Axiom 3: Alternating Mode — Entity children must be structural nodes, and vice versa
    public static List<Violation> checkAlternatingMode(Node root) {
        List<Violation> violations = new ArrayList<>();
        checkAlternatingModeRecursive(root, violations);
        return violations;
    }

    private static void checkAlternatingModeRecursive(Node node, List<Violation> violations) {
        for (Node child : node.getChildren()) {
            if (node.getType() == NodeType.ENTITY) {
                if (child.getType() == NodeType.ENTITY) {
                    violations.add(new Violation("Alternating Mode",
                            "Entity '" + node.getName() + "' has Entity child '" + child.getName()
                            + "'; expected Aspect, Specialization, or Multi-Aspect"));
                }
            } else {
                if (child.getType() != NodeType.ENTITY) {
                    violations.add(new Violation("Alternating Mode",
                            node.getType() + " '" + node.getName() + "' has non-Entity child '"
                            + child.getName() + "'; expected Entity"));
                }
            }
            checkAlternatingModeRecursive(child, violations);
        }
    }

    // Axiom 4: Valid Brothers — no two siblings share the same name
    public static List<Violation> checkValidBrothers(Node root) {
        List<Violation> violations = new ArrayList<>();
        checkValidBrothersRecursive(root, violations);
        return violations;
    }

    private static void checkValidBrothersRecursive(Node node, List<Violation> violations) {
        Set<String> names = new HashSet<>();
        for (Node child : node.getChildren()) {
            if (!names.add(child.getName())) {
                violations.add(new Violation("Valid Brothers",
                        "Siblings under '" + node.getName() + "' share the name '" + child.getName() + "'"));
            }
            checkValidBrothersRecursive(child, violations);
        }
    }

    // Axiom 5: Attached Variables — variable names on the same node must be distinct
    public static List<Violation> checkAttachedVariables(Node root) {
        List<Violation> violations = new ArrayList<>();
        checkAttachedVariablesRecursive(root, violations);
        return violations;
    }

    private static void checkAttachedVariablesRecursive(Node node, List<Violation> violations) {
        Set<String> names = new HashSet<>();
        for (Variable variable : node.getVariables()) {
            if (!names.add(variable.getName())) {
                violations.add(new Violation("Attached Variables",
                        "Node '" + node.getName() + "' has duplicate variable '" + variable.getName() + "'"));
            }
        }
        for (Node child : node.getChildren()) {
            checkAttachedVariablesRecursive(child, violations);
        }
    }
}