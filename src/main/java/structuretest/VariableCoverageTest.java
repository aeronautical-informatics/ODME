package structuretest;

import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import java.util.*;

import com.google.common.collect.ArrayListMultimap;

public class VariableCoverageTest {

    // Simulated varMap in DynamicTree
    public static Multimap<TreePath, String> varMap = ArrayListMultimap.create();

    public static void main(String[] args) {
        // Populate the varMap with sample data
        populateVarMap();

        // Prompt user for bucket size
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the bucket size for analysis: ");
        double bucketSize = scanner.nextDouble();

        // Process the varMap
        System.out.println("Processing varMap...");
        for (TreePath key : varMap.keySet()) {
            Collection<String> values = varMap.get(key);

            // Print the key and associated values
            System.out.println("Key: " + key);
            System.out.println("Values: " + values);

            // Convert the Collection<String> to a List<String> for processing
            List<String> valueList = new ArrayList<>(values);

            // Ensure we have exactly 5 elements (id, type, default, lower, upper)
            if (valueList.size() == 5) {
                String id = valueList.get(0);
                String type = valueList.get(1);
                double defaultValue = Double.parseDouble(valueList.get(2));
                double lowerBound = Double.parseDouble(valueList.get(3));
                double upperBound = Double.parseDouble(valueList.get(4));

                // Calculate bucket details
                int totalBuckets = (int) Math.ceil((upperBound - lowerBound) / bucketSize);
                int coveredBuckets = calculateCoveredBuckets(defaultValue, lowerBound, upperBound, bucketSize);

                // Print variable details and coverage
                System.out.printf("Processed Variable: ID=%s, Type=%s, Default=%.2f, Lower=%.2f, Upper=%.2f%n",
                        id, type, defaultValue, lowerBound, upperBound);
                System.out.printf("Bucket Size: %.2f, Total Buckets: %d, Covered Buckets: %d%n",
                        bucketSize, totalBuckets, coveredBuckets);
                System.out.printf("Coverage: %.2f%%%n", (coveredBuckets / (double) totalBuckets) * 100);
            } else {
                System.err.printf("Invalid number of elements for key: %s - Expected 5, found %d. Values: %s%n",
                        key, valueList.size(), valueList);
            }
        }
    }

    // Calculate the number of covered buckets
    private static int calculateCoveredBuckets(double defaultValue, double lowerBound, double upperBound, double bucketSize) {
        // Determine the bucket index of the default value
        int bucketIndex = (int) Math.floor((defaultValue - lowerBound) / bucketSize);
        if (bucketIndex >= 0 && defaultValue <= upperBound) {
            return 1; // Only one bucket is covered
        }
        return 0;
    }

    // Populate the varMap with sample data
    private static void populateVarMap() {
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

        varMap.put(new TreePath("Thing, node5Dec, second, node7MAsp"), "multiAspectID");
        varMap.put(new TreePath("Thing, node5Dec, second, node7MAsp"), "int");
        varMap.put(new TreePath("Thing, node5Dec, second, node7MAsp"), "0");
        varMap.put(new TreePath("Thing, node5Dec, second, node7MAsp"), "1");
        varMap.put(new TreePath("Thing, node5Dec, second, node7MAsp"), "10");

        varMap.put(new TreePath("Thing, node1Dec, AirFrame"), "temp");
        varMap.put(new TreePath("Thing, node1Dec, AirFrame"), "int");
        varMap.put(new TreePath("Thing, node1Dec, AirFrame"), "1");
        varMap.put(new TreePath("Thing, node1Dec, AirFrame"), "0");
        varMap.put(new TreePath("Thing, node1Dec, AirFrame"), "40");
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


