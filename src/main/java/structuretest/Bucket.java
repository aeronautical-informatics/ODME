package structuretest;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class Bucket {
//    double start;
    private TreePath key;      // The TreePath key to identify the source of the bucket
    private String variableName;
    private List<String> buckets;

    private Set<String> coveredBuckets; // Track covered bucket ranges

    //    double end;
//    boolean isCovered;

    // Constructor
    public Bucket(TreePath key, String variableName) {
        this.key = key;
        this.variableName = variableName;
        this.buckets = new ArrayList<>();
        this.coveredBuckets = new HashSet<>();
    }

    public void setBucketList(List<String> buckets){
        this.buckets = buckets;
    }
    // Add a bucket range
    public void addBucketRange(double start, double end) {
        String range = String.format("[%.2f - %.2f]", start, end);
        this.buckets.add(range);
    }

    // Check if a value falls within any of the bucket ranges
    public boolean contains(double value) {


        for (String range : this.buckets) {

            String[] parts = range.replace("[", "").replace("]", "").split(" - ");
            try {
                // Parse start and end values from the range
                double start = Double.parseDouble(parts[0].replace(",", ".").trim());
                double end = Double.parseDouble(parts[1].replace(",", ".").trim());
//                System.out.println("Start value = " + start);
//                System.out.println("End value = " + end);
                // Check if the value lies within the range
                if (value >= start && value <= end) {
                    coveredBuckets.add(range); // Mark this bucket as covered

                    return true;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid range format: " + range);
            }
        }
        return false;
    }

    public boolean containsInt(int value) {


        for (String range : this.buckets) {

            String[] parts = range.replace("[", "").replace("]", "").split(" - ");
            try {
                // Parse start and end values from the range
                int start = Integer.parseInt(parts[0].replace(",", ".").trim());
                int end = Integer.parseInt(parts[1].replace(",", ".").trim());
//                System.out.println("Start value = " + start);
//                System.out.println("End value = " + end);
                // Check if the value lies within the range
                if (value >= start && value <= end) {
                    coveredBuckets.add(range); // Mark this bucket as covered

                    return true;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid range format: " + range);
            }
        }
        return false;
    }


    public int getCoveredBuckets() {
        return coveredBuckets.size();
    }
    // Getters

    public int getUncoveredBuckets() {
        return getTotalBuckets() - getCoveredBuckets();
    }
    public TreePath getKey() {
        return key;
    }

    public String getVariableName() {
        return variableName;
    }

    public List<String> getBuckets() {
        return buckets;
    }
    public int getTotalBuckets() {
        return buckets.size();
    }

    // Override toString for debugging
    @Override
    public String toString() {
        return String.format("Bucket[key=%s, variable=%s, buckets=%s]",
                key, variableName, buckets);
    }
}
