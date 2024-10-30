package structuretest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static odme.odmeeditor.ODMEEditor.fileLocation;

public class SpecialisationNodeTest {

    private Map<String, List<String>> specialisationNodes = new HashMap<>();

    // Constructor to load XML file and extract specialization relationships
    public SpecialisationNodeTest(String xmlFilePath) {
        parseSpecialisationNodes(xmlFilePath);
    }

    // Method to parse the XML and dynamically extract specialization nodes
    private void parseSpecialisationNodes(String xmlFilePath) {
        try {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Get all elements in the document
            NodeList allNodes = doc.getElementsByTagName("*");  // The "*" gets all elements in the XML

            // Iterate over all nodes to find those containing "Spec"
            for (int i = 0; i < allNodes.getLength(); i++) {
                Element element = (Element) allNodes.item(i);
                String tagName = element.getTagName();

                // Check if the tag name contains "Spec" (e.g., EventSpec, GuardSpec, etc.)
                if (tagName.contains("Spec")) {
                    // The parent node is the node containing this "Spec" element
                    String parentNodeName = element.getParentNode().getNodeName();

                    // Get the child nodes inside the Spec element
                    NodeList childNodes = element.getChildNodes();
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        if (childNodes.item(j) instanceof Element) {
                            Element childElement = (Element) childNodes.item(j);
                            String childNodeName = childElement.getTagName();  // The child node name (e.g., Time, State)

                            // Build the combination (child_parent)
                            String combination = childNodeName + "_" + parentNodeName;

                            // Add the combination to the specializationNodes map
                            specialisationNodes.computeIfAbsent(parentNodeName, k -> new ArrayList<>()).add(combination);
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to get all specialization node relationships
    public Map<String, List<String>> getSpecialisationNodes() {
        return specialisationNodes;
    }

    // Example testing method for checking nodes in scenarios
    public void checkSpecialisationInScenarios(List<String> scenarios) {
        int totalSpecialisationNodes = 0;
        int usedSpecialisationNodes = 0;

        for (Map.Entry<String, List<String>> entry : specialisationNodes.entrySet()) {
            List<String> combinations = entry.getValue();

            for (String combination : combinations) {
                totalSpecialisationNodes++;

                // Check if this combination is used in any scenario
                for (String scenario : scenarios) {
                    if (scenario.contains(combination)) {
                        usedSpecialisationNodes++;
                        break;
                    }
                }
            }
        }

        // Print the result
        System.out.println(usedSpecialisationNodes + " / " + totalSpecialisationNodes + " specialization nodes are used.");
    }

    public void checkMatchedNodes(List<String[]> scenariosList){


        // Counters for total and matched nodes
        AtomicInteger totalSpecialNodes = new AtomicInteger();
        AtomicInteger matchedSpecialNodes = new AtomicInteger();

        int totalValues = countTotalValues(specialisationNodes);
//        System.out.println("Total number of values: " + totalValues);
        scenariosList.forEach(scenario -> {

            Scanner in = null;
            try
            {
                String path = fileLocation +"/" + scenario[0] + "/graphxml.xml";
//                System.out.println("Path of scenario = " + path);

                File file = new File(path);
                if (file.exists()) {
                    in = new Scanner(file);

                    // Parse the XML file
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(file);
                    doc.getDocumentElement().normalize();

                    // Get all elements in the document
                    NodeList allNodes = doc.getElementsByTagName("*");  // "*" gets all elements in the XML

                    // Increment the total specialization node counter
                    totalSpecialNodes.getAndIncrement();

                    // Iterate over all nodes and check if any of the tag names match values in the map
                    for (int i = 0; i < allNodes.getLength(); i++) {
                        Element element = (Element) allNodes.item(i);
                        String tagName = element.getTagName();  // Get the tag name from the XML element

                        // Check if this tag name matches any value in the specialisationNodes map
                        if (matchesWithSpecialisationNodes(tagName, specialisationNodes)) {
                            System.out.println("Match found for tag: " + tagName);
                            matchedSpecialNodes.getAndIncrement();
                        } else {
                            System.out.println("No match found for tag: " + tagName);
                        }
                    }
                } else {
                    System.out.println("File not found: " + path);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        });

        // Calculate the percentage of matched nodes
        double percentageMatched = (totalValues > 0) ? ((double) matchedSpecialNodes.get() / totalValues) * 100 : 0;

//        String message = "Total Specialization Nodes: " + totalValues + "\nMatched Specialization Nodes: " + matchedSpecialNodes;
        // Show dialog with the total, matched nodes, and percentage
        String message =
//                "Total Specialization Nodes: " + totalValues +
//                "\nMatched Specialization Nodes: " + matchedSpecialNodes +
                "Match Percentage: " + String.format("%.2f", percentageMatched) + "%";

        JOptionPane.showMessageDialog(null, message, "Specialization Node Matching Results", JOptionPane.INFORMATION_MESSAGE);
    }

    // Function to check if a given tag name matches any value in the specialisationNodes map
    private boolean matchesWithSpecialisationNodes(String tagName, Map<String, List<String>> specialisationNodes) {
        for (Map.Entry<String, List<String>> entry : specialisationNodes.entrySet()) {
            List<String> values = entry.getValue();

            // Check if the tagName is present in the list of values for this entry
            if (values.contains(tagName)) {
                return true;
            }
        }
        return false;
    }

    private int countTotalValues(Map<String, List<String>> specialisationNodes) {
        int totalCount = 0;

        // Iterate through each entry in the map
        for (Map.Entry<String, List<String>> entry : specialisationNodes.entrySet()) {
            List<String> values = entry.getValue(); // Get the list of values for this key
            totalCount += values.size(); // Add the size of the list to the total count
        }

        return totalCount;
    }
}

