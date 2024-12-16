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

    public  Multimap<TreePath, String> varMap = DynamicTree.varMap;
    private final Map<String, List<Bucket>> bucketMap = new HashMap<>();

    // A map to store all Bucket objects for later use
    Map<TreePath, List<Bucket>> bucketStorage = new HashMap<>();


    public Test(List<String[]> scenariosList){
        //define buckets for all variables
        String input = JOptionPane.showInputDialog(null, "Enter step size for Continuous variables",
                "Step Size Input", JOptionPane.QUESTION_MESSAGE);
        String inputDiscrete = JOptionPane.showInputDialog(null, "Enter step size for discreate variables",
                "Step Size Input", JOptionPane.QUESTION_MESSAGE);
        double stepSize;
        int stepSizeDiscrete;

        try {
            if (input == null || inputDiscrete == null) {
                JOptionPane.showMessageDialog(null, "Step size input cancelled. Exiting.",
                        "Input Cancelled", JOptionPane.WARNING_MESSAGE);
                throw new IllegalArgumentException("User cancelled the input.");
            }
            stepSizeDiscrete = Integer.parseInt(inputDiscrete.trim());
            stepSize = Double.parseDouble(input.trim());
            if (stepSize <= 0.0 || stepSizeDiscrete <=0 ) {
                throw new IllegalArgumentException("Step size must be positive.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return;
        }
        readVarFile(scenariosList , stepSize , stepSizeDiscrete);
    }

    private void readVarFile(List<String[]> scenariosList, double stepSize , int stepSizeDiscrete){


        for (TreePath key : varMap.keySet()) {
            int a = 0;

            System.out.println("Key = " + key);

            for (String value : DynamicTree.varMap.get(key)){

                List<Bucket> bucketList = bucketStorage.computeIfAbsent(key, k -> new ArrayList<>());


                if (value.contains("float") || value.contains("double")){
                    String[] valueArray = value.split(",");

                    // Extract variable details
                    String variableName = valueArray[0];
                    double lowerBound = Double.parseDouble(valueArray[3].trim());
                    double upperBound = Double.parseDouble(valueArray[4].trim());

                    // Create a new Bucket for the variable
                    Bucket bucket = new Bucket(key, variableName);

                    List<String> buckets = defineBuckets(lowerBound, upperBound, stepSize);

                    bucket.setBucketList(buckets);

                    // Add the Bucket to the list for this key
                    bucketList.add(bucket);
                }
                if (value.contains("int")){

                    String[] valueArray = value.split(",");

                    // Extract variable details
                    String variableName = valueArray[0];
                    int lowerBound = Integer.parseInt(valueArray[3].trim());
                    int upperBound = Integer.parseInt(valueArray[4].trim());

                    // Create a new Bucket for the variable
                    Bucket bucket = new Bucket(key, variableName);

                    List<String> buckets = defineBucketsForInt(lowerBound, upperBound, stepSizeDiscrete);

                    bucket.setBucketList(buckets);

                    // Add the Bucket to the list for this key
                    bucketList.add(bucket);
                }
            }
        }

        // Print bucketStorage contents for verification
        System.out.println("Bucket Storage Contents:");
        for (Map.Entry<TreePath, List<Bucket>> entry : bucketStorage.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            for (Bucket bucket : entry.getValue()) {
                System.out.println(bucket);
            }
        }
        checkVariables(scenariosList);
    }


    //Now read all scenarios var ssd file and check which bucket is fill or not

    public void checkVariables(List<String[]> scenariosList){


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

                    System.out.println("Scenario  name  = " + scenario[0]);

                    if (scenario[0].equals("InitScenario")){
                        continue;
                    }


                    for (TreePath scenarioKey : scenarioMap.keySet()) {

                        System.out.println("scenarioKey = " + scenarioKey);

                        for (String values : scenarioMap.get(scenarioKey)){

                            if (values.contains("float") || values.contains("double")){

                                String[] valueArray = values.split(",");

                                if (isNumericType(values)){

                                    try
                                    {
                                        double targetValue = Double.parseDouble(valueArray[2].trim());
                                        String variableName = valueArray[0];

                                        System.out.println("target = " + targetValue);

                                        boolean bucketFound = false;

                                        // Compare scenarioKey with bucketStorage keys
                                        for (TreePath bucketKey : bucketStorage.keySet()) {
                                            if (isMatchingTreePath(bucketKey, scenarioKey)) {

                                                List<Bucket> buckets = bucketStorage.get(bucketKey);

                                                for (Bucket bucket : buckets) {

                                                    if (bucket.getVariableName().equals(variableName)){

                                                        //That is function inside BucketClass to check values inside any bucket range
                                                        if (bucket.contains(targetValue)){

                                                            System.out.println("Matched value record ");
                                                            System.out.println("Variable = " + variableName);
                                                            System.out.println("Target value = " + targetValue);
                                                            bucketFound = true;
                                                        }

                                                        // Print bucket statistics
                                                        System.out.printf("Total buckets: %d%n", bucket.getTotalBuckets());
                                                        System.out.printf("Covered buckets: %d%n", bucket.getCoveredBuckets());
                                                        System.out.printf("Uncovered buckets: %d%n", bucket.getUncoveredBuckets());
                                                    }
                                                }
                                                break; // Stop searching after finding the matching bucketKey
                                            }
                                        }

                                        if (!bucketFound) {
                                            System.out.printf("Target value %.2f does not fall in any bucket for variable %s and key %s%n",
                                                    targetValue, variableName, scenarioKey);
                                        }

                                    }catch ( Exception e){
                                        System.out.println("Exception catch in isNumeric " + e.getMessage());
                                    }

                                }
                            }


                            //for int type
                            if (values.contains("int")){

                                String[] valueArray = values.split(",");

                                if (isNumericType(values)){

                                    int targetValue = Integer.parseInt(valueArray[2].trim());

                                    System.out.println("targetValue = "+ targetValue);

                                    String variableName = valueArray[0];

                                    boolean bucketFound = false;

                                    // Compare scenarioKey with bucketStorage keys
                                    for (TreePath bucketKey : bucketStorage.keySet()) {
                                        if (isMatchingTreePath(bucketKey, scenarioKey)) {

                                            List<Bucket> buckets = bucketStorage.get(bucketKey);

                                            for (Bucket bucket : buckets) {

                                                if (bucket.getVariableName().equals(variableName)){

                                                    //That is function inside BucketClass to check values inside any bucket range
                                                    if (bucket.containsInt(targetValue)){

                                                        System.out.println("Matched value record ");
                                                        System.out.println("Variable = " + variableName);
                                                        System.out.println("Target value = " + targetValue);
                                                        bucketFound = true;
                                                    }

                                                    // Print bucket statistics
                                                    System.out.printf("Total buckets: %d%n", bucket.getTotalBuckets());
                                                    System.out.printf("Covered buckets: %d%n", bucket.getCoveredBuckets());
                                                    System.out.printf("Uncovered buckets: %d%n", bucket.getUncoveredBuckets());
                                                }
                                            }
//                                        break; // Stop searching after finding the matching bucketKey
                                        }
                                    }

                                    if (!bucketFound) {
                                        System.out.printf("Target value % does not fall in any bucket for variable %s and key %s%n",
                                                targetValue, variableName, scenarioKey);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception:  in main " + e.getMessage());
            }
        }
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
    // Function to define buckets
    public static List<String> defineBuckets(double lowerBound, double upperBound, double stepSize) {

        List<String> buckets = new ArrayList<>();
        int bucketCount = (int) Math.ceil((upperBound - lowerBound) / stepSize);

        for (int i = 0; i < bucketCount; i++) {
            double start = lowerBound + (i * stepSize);
            double end = Math.min(start + stepSize, upperBound); // Ensure it doesn't exceed upper bound
            buckets.add(String.format("[%.2f - %.2f]", start, end));
        }

        return buckets;
    }

    private boolean isNumericType(String value) {
        String[] parts = value.split(",");
        return parts.length >= 2 && (parts[1].trim().equalsIgnoreCase("float")
                || parts[1].trim().equalsIgnoreCase("int")
                || parts[1].trim().equalsIgnoreCase("double"));
    }

    public static List<String> defineBucketsForInt(int lowerBound, int upperBound, int stepSize) {
        List<String> buckets = new ArrayList<>();
        int bucketCount = (int) Math.ceil((double) (upperBound - lowerBound) / stepSize);

        for (int i = 0; i < bucketCount; i++) {
            int start = lowerBound + (i * stepSize);
            int end = Math.min(start + stepSize, upperBound); // Ensure it doesn't exceed the upper bound
            buckets.add(String.format("[%d - %d]", start, end));
        }

        return buckets;
    }

    public void printBucketStatistics() {
        int totalBuckets = 0;
        int totalCoveredBuckets = 0;

        System.out.println("Aggregated Bucket Statistics:");
        for (Map.Entry<TreePath, List<Bucket>> entry : bucketStorage.entrySet()) {
            System.out.println("Key: " + entry.getKey());
            for (Bucket bucket : entry.getValue()) {
                int bucketCount = bucket.getTotalBuckets();
                int coveredCount = bucket.getCoveredBuckets();

                totalBuckets += bucketCount;
                totalCoveredBuckets += coveredCount;

                // Print statistics for each bucket
                System.out.printf("Variable: %s%n", bucket.getVariableName());
                System.out.printf("  Total Buckets: %d%n", bucketCount);
                System.out.printf("  Covered Buckets: %d%n", coveredCount);
                System.out.printf("  Uncovered Buckets: %d%n", bucket.getUncoveredBuckets());
            }
        }

        // Print aggregated totals
        System.out.println("---------------------------------------------------");
        System.out.printf("Total Buckets Across All Variables: %d%n", totalBuckets);
        System.out.printf("Total Covered Buckets: %d%n", totalCoveredBuckets);
        System.out.printf("Total Uncovered Buckets: %d%n", totalBuckets - totalCoveredBuckets);
    }


    /*
    private void defineBuckets(String key, String[] values, double stepSize) {
        try {
            for (String value : values) {
                if (value.contains("float") || value.contains("double")) {
                    String[] parts = value.split(",");
                    double lowerBound = Double.parseDouble(parts[3].trim());
                    double upperBound = Double.parseDouble(parts[4].trim());

                    int bucketCount = (int) Math.ceil((upperBound - lowerBound) / stepSize);


                    System.out.println("Key: " + key + ", Lower Bound: " + lowerBound +
                            ", Upper Bound: " + upperBound + ", Step Size: " + stepSize);
                    System.out.println("Buckets:" + bucketCount);

                    List<Bucket> buckets = new ArrayList<>();


                    // Define buckets starting from lowerBound and ensuring the last bucket ends at upperBound
                    double start, end;

                    for (int i = 0; i < bucketCount; i++){
                        start = lowerBound + (i * stepSize);
                        end = Math.min(start + stepSize, upperBound); // Ensure the last bucket does not exceed upper bound
                        buckets.add(new Bucket(start, end));
                    }
                    bucketMap.put(key, buckets);
                }
            }
        } catch (NumberFormatException e) {
            System.out.println("Error parsing numeric values: " + e.getMessage());
        }
    }
     */

}
