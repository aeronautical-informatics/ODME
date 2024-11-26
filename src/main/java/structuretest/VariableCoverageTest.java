package structuretest;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.*;

import odme.odmeeditor.DynamicTree;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.swing.tree.TreePath;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.util.*;

/*
public class VariableCoverageTest {

    public static Multimap<javax.swing.tree.TreePath, String> varMap = DynamicTree.varMap;

    public static void main(String[] args) {
        // Populate the DynamicTree.varMap
        populateDynamicTreeVarMap();

        // Simulate scenario Multimaps
        List<Multimap<TreePath, String>> scenarioMaps = new ArrayList<>();
        scenarioMaps.add(createScenario1());
        scenarioMaps.add(createScenario2());

        // Prompt user for bucket size
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the bucket size for analysis: ");
        double bucketSize = scanner.nextDouble();

        // Compare varMap with scenario maps
        System.out.println("\nComparing DynamicTree.varMap with scenario maps...");
//        for (Multimap<javax.swing.tree.TreePath, String> scenarioMap : scenarioMaps) {
//            compareWithScenario(varMap, scenarioMap, bucketSize);
//        }
    }

    // Compare DynamicTree.varMap with a scenario map
    private static void compareWithScenario(Multimap<javax.swing.tree.TreePath, String> dynamicMap,
                                            Multimap<javax.swing.tree.TreePath, String> scenarioMap,
                                            double bucketSize) {
        for (javax.swing.tree.TreePath key : dynamicMap.keySet()) {
            if (scenarioMap.containsKey(key)) {
                List<String> dynamicValues = new ArrayList<>(dynamicMap.get(key));
                List<String> scenarioValues = new ArrayList<>(scenarioMap.get(key));

                // Ensure both have 5 elements
                if (dynamicValues.size() == 5 && scenarioValues.size() == 5) {
                    String id = dynamicValues.get(0);
                    String type = dynamicValues.get(1);
                    double dynamicDefault = Double.parseDouble(dynamicValues.get(2));
                    double dynamicLower = Double.parseDouble(dynamicValues.get(3));
                    double dynamicUpper = Double.parseDouble(dynamicValues.get(4));

                    double scenarioDefault = Double.parseDouble(scenarioValues.get(2));

                    // Calculate total buckets
                    int totalBuckets = (int) Math.ceil((dynamicUpper - dynamicLower) / bucketSize);
                    int scenarioBucket = (int) Math.floor((scenarioDefault - dynamicLower) / bucketSize);

                    // Check if the scenarioDefault is within valid range
                    boolean isInBucket = scenarioDefault >= dynamicLower && scenarioDefault <= dynamicUpper;

                    // Print the comparison
                    System.out.printf("Key: %s%n", key);
                    System.out.printf("DynamicTree Default: %.2f, Lower: %.2f, Upper: %.2f%n", dynamicDefault, dynamicLower, dynamicUpper);
                    System.out.printf("Scenario Default: %.2f%n", scenarioDefault);
                    System.out.printf("Bucket Size: %.2f, Total Buckets: %d, Scenario Bucket: %d%n", bucketSize, totalBuckets, scenarioBucket);
                    System.out.printf("Is Scenario Default in Range: %b%n%n", isInBucket);
                } else {
                    System.err.printf("Invalid data for key: %s. Dynamic Values: %s, Scenario Values: %s%n",
                            key, dynamicValues, scenarioValues);
                }
            } else {
                System.out.printf("Key: %s not found in scenario map.%n", key);
            }
        }
    }

    // Populate the DynamicTree.varMap
    private static void populateDynamicTreeVarMap() {
        varMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "id");
        varMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "float");
        varMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "1");
        varMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "0");
        varMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "10");

        varMap.put(new TreePath("Thing, node5Dec, second"), "secId");
        varMap.put(new TreePath("Thing, node5Dec, second"), "int");
        varMap.put(new TreePath("Thing, node5Dec, second"), "0");
        varMap.put(new TreePath("Thing, node5Dec, second"), "1");
        varMap.put(new TreePath("Thing, node5Dec, second"), "20");
    }

    // Create a simulated scenario 1
    private static Multimap<TreePath, String> createScenario1() {
        Multimap<TreePath, String> scenarioMap = ArrayListMultimap.create();
        scenarioMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "id");
        scenarioMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "float");
        scenarioMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "2.2");
        scenarioMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "0");
        scenarioMap.put(new TreePath("Thing, node7Dec, MainMultipAspect"), "10");
        return scenarioMap;
    }

    // Create a simulated scenario 2
    private static Multimap<TreePath, String> createScenario2() {
        Multimap<TreePath, String> scenarioMap = ArrayListMultimap.create();
        scenarioMap.put(new TreePath("Thing, node5Dec, second"), "secId");
        scenarioMap.put(new TreePath("Thing, node5Dec, second"), "int");
        scenarioMap.put(new TreePath("Thing, node5Dec, second"), "5");
        scenarioMap.put(new TreePath("Thing, node5Dec, second"), "1");
        scenarioMap.put(new TreePath("Thing, node5Dec, second"), "20");
        return scenarioMap;
    }

    // Mock TreePath class for this example
    static class TreePath {
        private final String path;

        public TreePath(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreePath treePath = (TreePath) o;
            return Objects.equals(path, treePath.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }
}

 */



