package structuretest;


import com.google.common.collect.Multimap;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.ODMEEditor;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;


public class VariableCoverageTest {

    public static Multimap<TreePath, String> varMap = DynamicTree.varMap;
    private final Map<String, Set<Integer>> keyBucketCoverage = new HashMap<>(); // Track covered buckets for each key
    private final Set<String> processedScenarioKeyPairs = new HashSet<>(); // Track processed scenario-key pairs

    private int totalBuckets = 0;
    private int totalCoveredBuckets = 0;
    private int totalUnCoveredBuckets = 0;

    // Class-level variable to store the coverage summary
    private final Map<String, Map<String, Object>> coverageSummary = new HashMap<>();

    public VariableCoverageTest(List<String[]> scenariosList) {
        String input = JOptionPane.showInputDialog(null, "Enter step size (positive decimal):",
                "Step Size Input", JOptionPane.QUESTION_MESSAGE);
        double stepSize;
        try {
            if (input == null) {
                JOptionPane.showMessageDialog(null, "Step size input cancelled. Exiting.", "Input Cancelled", JOptionPane.WARNING_MESSAGE);
                throw new IllegalArgumentException("User cancelled the input.");
            }
            stepSize = Double.parseDouble(input.trim());
            if (stepSize <= 0.0) {
                throw new IllegalArgumentException("Step size must be positive.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }

        for (String[] scenario : scenariosList) {
            try {
                String path = ODMEEditor.fileLocation + "/" + scenario[0] + "/" + ODMEEditor.projName + ".ssdvar";
                File file = new File(path);

                if (!file.exists()) {
                    System.out.println("File does not exist");
                } else {
                    ObjectInputStream oisvar = new ObjectInputStream(new FileInputStream(file));
                    Multimap<TreePath, String> scenarioMap = (Multimap<TreePath, String>) oisvar.readObject();
                    oisvar.close();

                    matchKeys(varMap, scenarioMap, scenario[0], stepSize);
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
        }
    }

    private void matchKeys(Multimap<TreePath, String> dynamicMap, Multimap<TreePath, String> scenarioMap,
                           String scenarioName, double stepSize) {

        if (scenarioName.equals("InitScenario")) return;

        System.out.println("Scenario = " + scenarioName);

        for (TreePath scenarioKey : scenarioMap.keySet()) {
            boolean keyMatched = false;

            for (TreePath dynamicKey : dynamicMap.keySet()) {
                if (isMatchingTreePath(dynamicKey, scenarioKey)) {
                    keyMatched = true;

                    // getting the matched key values from scenarioMap
                    String[] matchedNodeValues = fetchNodeValues(scenarioKey, scenarioMap);
                    String[] matchedNodesValuesDynamicVar = fetchNodeValues(dynamicKey,dynamicMap);

                    //No use List<Strings>
                    List s = NodeValues(scenarioKey,scenarioMap);
                    List d = NodeValues(dynamicKey,dynamicMap);

                    System.out.println("Scenario Key = " + scenarioKey);
                    System.out.println("List Scenario values" + s);

                    System.out.println("Dynamic Key = " + dynamicKey);
                    System.out.println("List Dynamic values" + d);

                    var result = s.equals(d);

                    System.out.println("Matched result = " + result);

                    /*
                    //Now check these values are exact same or not
                    for (String value : matchedNodeValues) {
                        // Compare element by element
                        //skipp if values are same
                        boolean areEqual = Arrays.equals(matchedNodeValues, matchedNodesValuesDynamicVar);
                        if (areEqual){
                            System.out.println("Values are exact same so skipping that key ");
                        }

                        else {
                            // checking value is int,float or double type
                            if (value != null && isNumericType(value)) {
                                if (value.contains("float") || value.contains("double")){
                                    // now check exact same value should be skipped because it is not pruned
//                                defineBuckets(scenarioKey.toString(), matchedNodeValues, stepSize, scenarioName);
                                    System.out.println("Matched float or double values " + value);
                                }
                                if (value.contains("int")){
                                    System.out.println("Matched int values " + value);
                                }
                            }
                        }


                    }

                     */
                }
            }

//            if (!keyMatched) {
//                System.out.println("No match found for Key: " + scenarioKey);
//            }
        }
    }

    private boolean isNumericType(String value) {
        String[] parts = value.split(",");
        return parts.length >= 2 && (parts[1].trim().equalsIgnoreCase("float")
                || parts[1].trim().equalsIgnoreCase("int")
                || parts[1].trim().equalsIgnoreCase("double"));
    }

    private boolean isMatchingTreePath(TreePath dynamicKey, TreePath scenarioKey) {
        TreeNode[] dynamicNodes = ((DefaultMutableTreeNode) dynamicKey.getLastPathComponent()).getPath();
        TreeNode[] scenarioNodes = ((DefaultMutableTreeNode) scenarioKey.getLastPathComponent()).getPath();
        if (dynamicNodes.length != scenarioNodes.length) return false;
        for (int i = 0; i < dynamicNodes.length; i++) {
            if (!dynamicNodes[i].toString().equals(scenarioNodes[i].toString())) return false;
        }
        return true;
    }

    private List<String> NodeValues(TreePath matchingKey, Multimap<TreePath, String> varMap) {
        List<String> nodeValues = new ArrayList<>();
        for (TreePath key : varMap.keySet()) {
            if (key.equals(matchingKey)) {
                nodeValues.addAll(varMap.get(key));
            }
        }
        return nodeValues;
    }
    private String[] fetchNodeValues(TreePath matchingKey, Multimap<TreePath, String> varMap) {
        List<String> nodeValues = new ArrayList<>();
        for (TreePath key : varMap.keySet()) {
            if (key.equals(matchingKey)) {
                nodeValues.addAll(varMap.get(key));
            }
        }
        return nodeValues.toArray(new String[0]);
    }

    private void defineBuckets(String key, String[] values, double stepSize, String scenarioName) {
        try {
            for (String value : values) {
                String scenarioKeyPair = scenarioName + "-" + key + "-" + value;

                // Skip if this pair is already processed
                if (processedScenarioKeyPairs.contains(scenarioKeyPair)) {
                    continue;
                }

                processedScenarioKeyPairs.add(scenarioKeyPair);

                if (value.contains("float") || value.contains("double") || value.contains("int")) {
                    // Extract the relevant numeric values
                    String[] parts = value.split(",");
                    double targetValue = Double.parseDouble(parts[2].trim()); // Target value
                    double lowerBound = Double.parseDouble(parts[3].trim()); // Lower bound
                    double upperBound = Double.parseDouble(parts[4].trim()); // Upper bound

                    // Calculate the number of buckets based on step size
                    int bucketCount = (int) Math.ceil((upperBound - lowerBound) / stepSize);

                    System.out.println("Key: " + key + ", Lower Bound: " + lowerBound +
                            ", Upper Bound: " + upperBound + ", Step Size: " + stepSize);
                    System.out.println("Buckets:");

                    // Define buckets and determine the bucket for the target value
                    double start, end;
                    int bucketNumber = 0;
                    for (int i = 0; i < bucketCount; i++) { // Loop through the calculated bucket count
                        start = lowerBound + (i * stepSize);
                        end = Math.min(start + stepSize, upperBound); // Ensure the last bucket does not exceed upper bound

                        System.out.println("Bucket " + (i + 1) + ": [" + start + " - " + end + "]");

                        // Check if the target value falls within the bucket range
                        if (targetValue >= start && targetValue < end) {
                            bucketNumber = i + 1; // Buckets are 1-based

                            // Track covered bucket for this key
                            keyBucketCoverage
                                    .computeIfAbsent(key, k -> new HashSet<>())
                                    .add(bucketNumber);

                            System.out.println("Scenario: " + scenarioName + ", Target Value: " + targetValue +
                                    " lies in Bucket " + bucketNumber);

                            totalCoveredBuckets ++ ;

                        }
                        totalBuckets ++;
                    }

                    // Handle the case where the target value does not fall into any bucket
                    if (bucketNumber == 0) {
//                        totalUnCoveredBuckets++;
                        System.out.println("Target Value: " + targetValue +
                                " does not lie in any defined bucket for Key: " + key);
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            System.out.println("Error parsing numeric values: " + nfe.getMessage());
        } catch (Exception e) {
            System.out.println("Error defining buckets for key: " + key + " - " + e.getMessage());
        }
    }

    public int getTotalBuckets() {
        return totalBuckets;
    }

    public int getTotalCoveredBuckets() {
        return totalCoveredBuckets;
    }

    public int getTotalUnCoveredBuckets() {
        totalUnCoveredBuckets = totalBuckets - totalCoveredBuckets;

        return totalUnCoveredBuckets;
    }
}
