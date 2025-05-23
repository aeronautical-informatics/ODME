package structuretest;

import odme.odmeeditor.ODMEEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.*;

public class MultiAspectNodeTest {


    // Map to store parsed nodes, with parent nodes as keys and lists of combinations as values
    private Map<String, List<String>> parsedNodes;
    private int multiAspectNodeCount;

    private int totalCoveredNodes = 0;
    private int totalUncoveredNodes = 0;
    private double totalPercentage = 0;

    public MultiAspectNodeTest() {
        parsedNodes = new HashMap<>();
        multiAspectNodeCount = 0;
    }


    /**
     * Parses nodes containing a specified tag pattern from the given XML file.
     *
     * @paramPath The path to the XML file to be parsed.
     * @paramtagPattern The pattern to match in tag names (e.g., "Spec", "MAsp").
     */


    public void parseNodes(String xmlFilePath) {
        try {

            String tagPattern = "MAsp";
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();


            // Get all elements in the document
            NodeList allNodes = doc.getElementsByTagName("*");

            for (int i = 0; i < allNodes.getLength(); i++) {
                Element element = (Element) allNodes.item(i);
                String tagName = element.getTagName();

                // Check if the tag matches the specified pattern (e.g.,  "MAsp")
                if (tagName.contains(tagPattern)) {
                    String parentNodeName = element.getParentNode().getNodeName();
                    NodeList childNodes = element.getChildNodes();

                    // Track the child-parent combinations
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        if (childNodes.item(j) instanceof Element) {
                            Element childElement = (Element) childNodes.item(j);
                            String childNodeName = childElement.getTagName();
//                            String combination = childNodeName + "_" + parentNodeName;

                            // Store the combination in the parsedNodes map
//                            parsedNodes.computeIfAbsent(parentNodeName, k -> new ArrayList<>()).add(combination);
                            parsedNodes.computeIfAbsent(parentNodeName, k -> new ArrayList<>()).add(childNodeName);
                        }
                    }
                    // Increment count for "MAsp" or other specified tag pattern
                    if (tagName.contains("MAsp")) {
                        multiAspectNodeCount++;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, List<String>> getParsedNodes() {
        return parsedNodes;
    }

    public int getMultiAspectNodeCount() {
        return multiAspectNodeCount;
    }

    public void clearParsedNodes() {
        parsedNodes.clear();
        multiAspectNodeCount = 0;
    }


    public void checkCodeCoverageMultiAspect(List<String[]> scenariosList) {

        // Define the file location for the MultiAspect limits file
        File ssdFileLimit = new File(String.format("%s.ssdLimit",
                ODMEEditor.fileLocation + "/" + ODMEEditor.projName));

        final Map<TreePath, String> limits = new HashMap<>();

        // Check if the file exists
        if (ssdFileLimit.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ssdFileLimit))) {
                limits.putAll((Map<TreePath, String>) ois.readObject());
            } catch (Exception e) {
                System.out.println("Failed to read existing file " + e.getMessage());
            }
        } else {
            System.out.println("MultiAspect Limit file does not exist");
            return;
        }


        Map<String, Integer> globalChildCounts = new HashMap<>();
        Set<String> processedNodes = new HashSet<>(); // Track nodes processed as pruned in any scenario

        // Process each scenario
        scenariosList.forEach(scenario -> {
            try {
                String path = ODMEEditor.fileLocation + "/" + scenario[0] + "/graphxml.xml";
                File file = new File(path);

                if (file.exists()) {
                    // Parse the XML file
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(file);
                    doc.getDocumentElement().normalize();

                    // Get all elements in the document
                    NodeList allNodes = doc.getElementsByTagName("*");

                    // Temporary storage for pruned nodes in this scenario
                    Map<String, Integer> localChildCounts = new HashMap<>();

                    // Iterate over all nodes and check for pruned nodes
                    for (int i = 0; i < allNodes.getLength(); i++) {
                        Element element = (Element) allNodes.item(i);
                        String tagName = element.getTagName();

                        if (tagName.contains("Dec")) {
                            String parentNode = element.getParentNode().getNodeName();
                            System.out.println("Parent node " + parentNode);

                            if (parsedNodes.containsKey(parentNode)) {
                                processedNodes.add(parentNode); //
                                NodeList childNodes = element.getChildNodes();

                                // Count child nodes for this MultiAspect node
                                int count = 0;

                                for (int j = 0; j < childNodes.getLength(); j++) {
                                    if (childNodes.item(j) instanceof Element) {
                                        System.out.println("Child name = " + childNodes.item(j).getNodeName());
                                        count++;
                                    }
                                }

                                // Update the count for this pruned parent node in the current scenario
                                localChildCounts.put(parentNode, localChildCounts.getOrDefault(parentNode, 0) + count);
                            }
                        }
                    }

                    // Merge localChildCounts into globalChildCounts
                    for (Map.Entry<String, Integer> entry : localChildCounts.entrySet()) {
                        globalChildCounts.put(entry.getKey(),
                                globalChildCounts.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }

                } else {
                    System.out.println("Scenario graphxml does not exist: " + path);
                }
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
        });

        // Add non-pruned nodes (nodes in parsedNodes but not in processedNodes) with zero child count
        for (String node : parsedNodes.keySet()) {
            if (!processedNodes.contains(node)) {
//                System.out.printf("Non-pruned node detected: %s%n", node);
                globalChildCounts.putIfAbsent(node, 0);
            }
        }

        // Variables for tracking parent node-level coverage
        int totalNodes = 0;
        int coveredNodes = 0;
        int uncoveredNodes = 0;

        // Variables for calculating overall coverage
        int totalCoveredChildren = 0;
        int totalLimit = 0;

        // Calculate and classify parent nodes as covered/uncovered
        for (Map.Entry<String, Integer> childCountEntry : globalChildCounts.entrySet()) {
            String nodeName = childCountEntry.getKey();
            int childCount = childCountEntry.getValue();

            // Increment total nodes
            totalNodes++;

            // Classify as covered or uncovered
            if (childCount > 0) {
                coveredNodes++;
            } else {
                uncoveredNodes++;
            }

            // Find the corresponding limit for this node
            int limit = 0;
            for (Map.Entry<TreePath, String> entry : limits.entrySet()) {
                if (entry.getKey().toString().contains(nodeName)) {
                    limit = Integer.parseInt(entry.getValue());
                    break;
                }
            }

            if (limit > 0) {
                // Calculate node-level coverage
                double coverage = Math.min((double) childCount / limit * 100, 100.0);
                System.out.printf("Node: %s, Child Count: %d, Limit: %d, Coverage: %.2f%%%n",
                        nodeName, childCount, limit, coverage);

                // Update global totals for overall coverage
                totalCoveredChildren += childCount;
                totalLimit += limit;
            } else {
                System.out.printf("Node: %s, Child Count: %d, Limit: Not Found%n", nodeName, childCount);
            }
        }

        // Calculate overall coverage for all child nodes
        double overallCoverage = (totalLimit > 0) ? (double) totalCoveredChildren / totalLimit * 100 : 0;

        // Calculate parent node coverage percentage
//        double parentNodeCoveragePercentage = (totalNodes > 0) ? (double) coveredNodes / totalNodes * 100 : 0;

        totalUncoveredNodes = uncoveredNodes;

        totalCoveredNodes = coveredNodes;

        totalPercentage = overallCoverage;


        // Output overall coverage and parent node stats
        System.out.printf("Overall Coverage for MultiAspect Nodes: %.2f%%%n", overallCoverage);
        System.out.printf("Total Nodes: %d%n", totalNodes);
        System.out.printf("Covered Nodes: %d%n", coveredNodes);
        System.out.printf("Uncovered Nodes: %d%n", uncoveredNodes);
//        System.out.printf("Parent Node Coverage Percentage: %.2f%%%n", parentNodeCoveragePercentage);
    }

    // Getter methods to access global totals
    public int getTotalCoveredChildren() {
        return totalCoveredNodes;
    }

    public int getTotalUncoveredChildren() {
        return totalUncoveredNodes;
    }

    public double getTotalPercentage() {
        return totalPercentage;
    }


}
