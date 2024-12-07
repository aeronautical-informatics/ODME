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



public class Test {

    public static Multimap<TreePath, String> varMap = DynamicTree.varMap;
    private final Map<String, Set<Integer>> keyBucketCoverage = new HashMap<>(); // Track covered buckets for each key
    private final Set<String> processedScenarioKeyPairs = new HashSet<>(); // Track processed scenario-key pairs

     int totalCoveredBuckets = 0;
     int totalUnCoveredBuckets = 0;
     int totalBuckets = 0;

    // Class-level variable to store the coverage summary
    private final Map<String, Map<String, Object>> coverageSummary = new HashMap<>();

    public Test(List<String[]> scenariosList) {

        String input = JOptionPane.showInputDialog(null, "Enter bucket size (positive integer):",
                "Bucket Size Input", JOptionPane.QUESTION_MESSAGE);
        try{
            if (input == null) {
                JOptionPane.showMessageDialog(null, "Bucket size input cancelled. Exiting.", "Input Cancelled", JOptionPane.WARNING_MESSAGE);
                throw new IllegalArgumentException("User cancelled the input.");
            }
        }catch (Exception e){
            System.out.println(""+e.getMessage());
        }
        // Parse the input to an integer
        int bucketSize = Integer.parseInt(input.trim());

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

                    matchKeys(varMap, scenarioMap, scenario[0], bucketSize);
                }
            } catch (Exception e) {
                System.out.println("exception " + e.getMessage());
            }
        }

        // Calculate overall coverage for each key
        calculateOverallCoverage(bucketSize);
    }


    private void matchKeys(Multimap<TreePath, String> dynamicMap, Multimap<TreePath, String> scenarioMap,
                           String scenarioName, int bucketSize) {

        if (scenarioName.equals("InitScenario")) return;

        //This for loop is on scenario varMap
        for (TreePath scenarioKey : scenarioMap.keySet()) {

            boolean keyMatched = false;


            for (TreePath dynamicKey : dynamicMap.keySet()) {

                // Compare the structure of the TreePath objects
                if (isMatchingTreePath(dynamicKey, scenarioKey)) {

                    keyMatched = true;

                    Collection<String> scenarioValues = scenarioMap.get(scenarioKey);

                    Collection<String> dynamicValues = dynamicMap.get(dynamicKey);


                    // Fetch values for the matched key
                    String[] matchedNodeValuesDynamic = fetchNodeValues(dynamicKey, dynamicMap);

                    String[] matchedNodeValues = fetchNodeValues(scenarioKey, scenarioMap);

                    boolean areSame = Arrays.equals(matchedNodeValuesDynamic, matchedNodeValues);
                    if (areSame){
                        System.out.println("Values are exact same so skip");
                    }else {

                        for (String value : matchedNodeValues){
                            if (value != null){

                                if (value.contains("float") || value.contains("double")){
//                                    System.out.println("value = "+value + "   scenario  = "+ scenarioKey);
                                    // Process matched values
//                                defineBuckets(scenarioKey.toString(), scenarioValues, bucketSize, scenarioName);
                                defineBuckets(scenarioKey.toString(), matchedNodeValues, 0.1, scenarioName);
                                }
                            }
                        }
                    }
                }
            }

            if (!keyMatched) {
                System.out.println("No match found for Key: " + scenarioKey);
            }
        }
    }

    private boolean isNumericType(String value) {
        // Split the value string by commas to extract the type
        String[] parts = value.split(",");
        if (parts.length < 2) {
            return false; // Invalid value format
        }

        String type = parts[1].trim().toLowerCase(); // Extract type (e.g., "float" or "double")

        // Check if the type is float or double
        return type.equals("float") || type.equals("double");
    }
    private boolean isMatchingTreePath(TreePath dynamicKey, TreePath scenarioKey) {
        DefaultMutableTreeNode dynamicNode = (DefaultMutableTreeNode) dynamicKey.getLastPathComponent();
        DefaultMutableTreeNode scenarioNode = (DefaultMutableTreeNode) scenarioKey.getLastPathComponent();

        TreeNode[] dynamicNodes = dynamicNode.getPath();
        TreeNode[] scenarioNodes = scenarioNode.getPath();

        // Compare the length of the paths
        if (dynamicNodes.length != scenarioNodes.length) {
            return false;
        }

        // Compare each node in the paths
        for (int i = 0; i < dynamicNodes.length; i++) {
            if (!dynamicNodes[i].toString().equals(scenarioNodes[i].toString())) {
                return false;
            }
        }

        return true;
    }
    private String[] fetchNodeValues(TreePath matchingKey, Multimap<TreePath, String> varMap) {

        TreeNode[] nodes = ((DefaultMutableTreeNode) matchingKey.getLastPathComponent()).getPath();
        String[] nodesToSelectedNode = new String[100]; // Array to store matched values
        int b = 0;

        for (TreePath key : varMap.keySet()) {
            int a = 0;

            for (String value : varMap.get(key)) {
                DefaultMutableTreeNode currentNode2 = (DefaultMutableTreeNode) key.getLastPathComponent();
                TreeNode[] nodes2 = currentNode2.getPath();

                if (nodes.length == nodes2.length) {
                    int aa = 1;
                    for (int i = 0; i < nodes.length; i++) {
                        if (!nodes[i].toString().equals(nodes2[i].toString())) {
                            aa = 0;
                            break;
                        }
                    }
                    a = aa;
                }

                if (a == 1) {
                    nodesToSelectedNode[b] = value;
                    b++;
                }
            }
        }

        return nodesToSelectedNode;
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

                if (value.contains("float") || value.contains("double")) {
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
                        }
                    }

                    // Handle the case where the target value does not fall into any bucket
                    if (bucketNumber == 0) {
                        totalUnCoveredBuckets++;
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



    /*
    private void defineBuckets(String key, String[] values, double bucketCount, String scenarioName) {
        try {
            for (String value : values) {
                String scenarioKeyPair = scenarioName + "-" + key + "-" + value;

                // Skip if this pair is already processed
                if (processedScenarioKeyPairs.contains(scenarioKeyPair)) {
                    continue;
                }

                processedScenarioKeyPairs.add(scenarioKeyPair);

                if (value.contains("float") || value.contains("double")) {
                    // Extract the relevant numeric values
                    String[] parts = value.split(",");
                    double targetValue = Double.parseDouble(parts[2].trim()); // Target value
                    double lowerBound = Double.parseDouble(parts[3].trim()); // Lower bound
                    double upperBound = Double.parseDouble(parts[4].trim()); // Upper bound

                    // Calculate the step size based on bucket count
                    double stepSize = (upperBound - lowerBound) / bucketCount;

                    System.out.println("Key: " + key + ", Lower Bound: " + lowerBound +
                            ", Upper Bound: " + upperBound + ", Step Size: " + stepSize);
                    System.out.println("Buckets:");

                    // Define buckets and determine the bucket for the target value
                    double start, end;
                    int bucketNumber = 0;
                    for (int i = 0; i < bucketCount; i++) { // Loop through bucket count
                        start = lowerBound + (i * stepSize);
                        end = start + stepSize;

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
                        }
                    }
                    // Handle the case where the target value does not fall into any bucket
                    if (bucketNumber == 0) {
                        totalUnCoveredBuckets++;
                        System.out.println("Target Value: " + targetValue +
                                " does not lie in any defined bucket for Key: " + key);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error defining buckets for key: " + key + " - " + e.getMessage());
        }
    }
 */

    private void calculateOverallCoverage(int bucketSize) {
        for (Map.Entry<String, Set<Integer>> entry : keyBucketCoverage.entrySet()) {

            String key = entry.getKey();
            Set<Integer> coveredBuckets = entry.getValue();

            Set<Integer> uncoveredBuckets = new HashSet<>();
            for (int i = 1; i <= bucketSize; i++) {
                if (!coveredBuckets.contains(i)) {
                    uncoveredBuckets.add(i);
                }
            }

            Map<String, Object> details = new HashMap<>();
            details.put("TotalBuckets", bucketSize);
            details.put("CoveredBuckets", coveredBuckets.size());
            details.put("UncoveredBuckets", uncoveredBuckets);

            coverageSummary.put(key, details);

            // Update overall totals
            totalBuckets += bucketSize;                  // Add the total buckets for this key
            totalCoveredBuckets += coveredBuckets.size();       // Add the covered buckets for this key
            totalUnCoveredBuckets += uncoveredBuckets.size();   // Add the uncovered buckets for this key

        }
    }

    public void print(){
        System.out.println(totalBuckets);
        System.out.println(totalCoveredBuckets);
        System.out.println(totalUnCoveredBuckets);
    }

     public int getTotalBuckets(){
        return totalBuckets;
     }

    public int getTotalCoveredBuckets(){
        return totalCoveredBuckets;
    }
    public int getTotalUnCoveredBuckets(){
        return totalUnCoveredBuckets;
    }
    public Map<String, Map<String, Object>> getCoverageSummary() {
        return coverageSummary;
    }
    public Map<String, Set<Integer>> getCoveredBuckets(){
        return keyBucketCoverage;
    }
}

