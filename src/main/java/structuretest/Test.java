package structuretest;

import com.google.common.collect.Multimap;
import odme.odmeeditor.DynamicTree;
import odme.odmeeditor.ODMEEditor;

import javax.swing.tree.TreePath;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;
import java.util.regex.Pattern;




public class Test {

    public static Multimap<TreePath, String> varMap = DynamicTree.varMap;
    private final Map<String, Set<Integer>> keyBucketCoverage = new HashMap<>(); // Track covered buckets for each key

    public Test(List<String[]> scenariosList) {
        // Prompt user for bucket size
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter bucket size: ");
        int bucketSize = scanner.nextInt();
        for (String[] scenario : scenariosList) {
            try {
                String path = ODMEEditor.fileLocation + "/" + scenario[0] + "/Main.ssdvar";
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
        scanner.close();

        // Calculate overall coverage for each key
        calculateOverallCoverage(bucketSize);
    }

    private void matchKeys(Multimap<TreePath, String> dynamicMap, Multimap<TreePath, String> scenarioMap, String scenarioName, int bucketSize) {
        if (scenarioName.equals("InitScenario")) return;

        // Convert keys to Lists of Strings
        List<String> dynamicKeyList = new ArrayList<>();
        List<String> scenarioKeyList = new ArrayList<>();

        for (TreePath key : dynamicMap.keySet()) {
            dynamicKeyList.add(key.toString());
        }

        for (TreePath key : scenarioMap.keySet()) {
            scenarioKeyList.add(key.toString());
        }

        // Find matching keys
        List<String> matchingKeys = new ArrayList<>(dynamicKeyList);
        matchingKeys.retainAll(scenarioKeyList); // Keep only keys that exist in both lists

        if (!matchingKeys.isEmpty()) {
            for (String matchingKey : matchingKeys) {
                for (TreePath key : scenarioMap.keySet()) {
                    if (key.toString().equals(matchingKey)) {
                        Collection<String> values = scenarioMap.get(key);

                        for (String value : values) {
                            try {
                                if (value.contains("float") || value.contains("double")) {
                                    Collection<String> dynamicValues = null;
                                    for (TreePath dynamicKey : dynamicMap.keySet()) {
                                        if (dynamicKey.toString().equals(matchingKey)) {
                                            dynamicValues = dynamicMap.get(dynamicKey);
                                            break;
                                        }
                                    }

                                    System.out.println("Matched Key: " + matchingKey +
                                            ", Numeric Value in ScenarioMap: " + value +
                                            ", Values in DynamicMap: " + dynamicValues +
                                            ", Scenario: " + scenarioName);

                                    // Call defineBuckets for the matched key
                                    defineBuckets(matchingKey, values, bucketSize, scenarioName);
                                }
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("No matching keys found.");
        }
    }

    private void defineBuckets(String key, Collection<String> values, int bucketSize, String scenarioName) {
        try {
            for (String value : values) {
                if (value.contains("float") || value.contains("double")) {
                    // Extract the relevant numeric value and upper limit from the value string
                    String[] parts = value.split(",");
                    double targetValue = Double.parseDouble(parts[2].trim()); // Assuming the third value is the target value
                    double upperLimit = Double.parseDouble(parts[parts.length - 1].trim()); // Assuming the last part is the upper limit

                    // Calculate bucket size
                    double bucketRange = upperLimit / bucketSize;

                    System.out.println("Key: " + key + ", Upper Limit: " + upperLimit + ", Bucket Size: " + bucketSize);
                    System.out.println("Buckets:");

                    // Define buckets and determine the bucket for the target value
                    double start, end;
                    int bucketNumber = 0;
                    for (int i = 0; i < bucketSize; i++) {
                        start = i * bucketRange;
                        end = (i + 1) * bucketRange;

                        System.out.println("Bucket " + (i + 1) + ": [" + start + " - " + end + "]");

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
                }
            }
        } catch (Exception e) {
            System.out.println("Error defining buckets for key: " + key + " - " + e.getMessage());
        }
    }

    private void calculateOverallCoverage(int bucketSize) {
        System.out.println("\nOverall Coverage:");
        for (Map.Entry<String, Set<Integer>> entry : keyBucketCoverage.entrySet()) {
            String key = entry.getKey();
            Set<Integer> coveredBuckets = entry.getValue();

            double coverage = (coveredBuckets.size() / (double) bucketSize) * 100;
            System.out.println("Key: " + key + ", Covered Buckets: " + coveredBuckets +
                    ", Overall Code Coverage: " + coverage + "%");
        }
    }
}






/*
public class Test {

    public static Multimap<TreePath, String> varMap = DynamicTree.varMap;

    public Test(List<String[]> scenariosList){

        // Prompt user for bucket size
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter bucket size: ");
        int bucketSize = scanner.nextInt();
        for (String[] scenario : scenariosList) {

            try{

                String path = ODMEEditor.fileLocation + "/" + scenario[0] + "/Main.ssdvar";
                File file = new File(path);

                if (!file.exists()) {

                    System.out.println("File does not exist");

                }
                else {
                    ObjectInputStream oisvar = new ObjectInputStream(new FileInputStream(file));
                    Multimap<TreePath, String> scenarioMap = (Multimap<TreePath, String>) oisvar.readObject();
                    oisvar.close();

                    matchKeys(varMap,scenarioMap , scenario[0] , bucketSize);
                }

            }catch (Exception  e){
                System.out.println("exception " + e.getMessage());
            }
        }
    }

    private void matchKeys(Multimap<TreePath, String> dynamicMap, Multimap<TreePath, String> scenarioMap, String scenarioName, int bucketSize) {
        if (scenarioName.equals("InitScenario")) return;

        // Convert keys to Lists of Strings
        List<String> dynamicKeyList = new ArrayList<>();
        List<String> scenarioKeyList = new ArrayList<>();

        for (TreePath key : dynamicMap.keySet()) {
            dynamicKeyList.add(key.toString());
        }

        for (TreePath key : scenarioMap.keySet()) {
            scenarioKeyList.add(key.toString());
        }

        // Find matching keys
        List<String> matchingKeys = new ArrayList<>(dynamicKeyList);
        matchingKeys.retainAll(scenarioKeyList); // Keep only keys that exist in both lists

        if (!matchingKeys.isEmpty()) {
            for (String matchingKey : matchingKeys) {
                for (TreePath key : scenarioMap.keySet()) {
                    if (key.toString().equals(matchingKey)) {
                        Collection<String> values = scenarioMap.get(key);

                        for (String value : values) {
                            try {
                                if (value.contains("float") || value.contains("double")) {
                                    Collection<String> dynamicValues = null;
                                    for (TreePath dynamicKey : dynamicMap.keySet()) {
                                        if (dynamicKey.toString().equals(matchingKey)) {
                                            dynamicValues = dynamicMap.get(dynamicKey);
                                            break;
                                        }
                                    }

                                    System.out.println("Matched Key: " + matchingKey +
                                            ", Numeric Value in ScenarioMap: " + value +
                                            ", Values in DynamicMap: " + dynamicValues +
                                            ", Scenario: " + scenarioName);

                                    // Call defineBuckets for the matched key
                                    defineBuckets(matchingKey, values, bucketSize);
                                }
                            } catch (Exception e) {
                                System.out.println(e.getMessage());
                            }
                        }
                    }
                }
            }
        } else {
            System.out.println("No matching keys found.");
        }
    }
    private void defineBuckets(String key, Collection<String> values, int bucketSize) {
        try {
            for (String value : values) {
                if (value.contains("float") || value.contains("double")) {
                    // Extract the relevant numeric value and upper limit from the value string
                    String[] parts = value.split(",");
                    double targetValue = Double.parseDouble(parts[2].trim()); // Assuming the third value is the target value
                    double upperLimit = Double.parseDouble(parts[parts.length - 1].trim()); // Assuming the last part is the upper limit

                    // Calculate bucket size
                    double bucketRange = upperLimit / bucketSize;

                    System.out.println("Key: " + key + ", Upper Limit: " + upperLimit + ", Bucket Size: " + bucketSize);
                    System.out.println("Buckets:");

                    // Define buckets and determine the bucket for the target value
                    double start, end;
                    int bucketNumber = 0;
                    for (int i = 0; i < bucketSize; i++) {
                        start = i * bucketRange;
                        end = (i + 1) * bucketRange;

                        System.out.println("Bucket " + (i + 1) + ": [" + start + " - " + end + "]");

                        if (targetValue >= start && targetValue < end) {
                            bucketNumber = i + 1; // Buckets are 1-based
                        }
                    }

                    // Calculate coverage percentage
                    double coverage = 100.0 / bucketSize;
                    System.out.println("Target Value: " + targetValue + " lies in Bucket " + bucketNumber +
                            " with Code Coverage: " + coverage + "%");
                }
            }
        } catch (Exception e) {
            System.out.println("Error defining buckets for key: " + key + " - " + e.getMessage());
        }
    }
}
#
 */
