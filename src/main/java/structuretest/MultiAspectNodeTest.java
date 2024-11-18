package structuretest;

import odme.odmeeditor.ODMEEditor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static odme.odmeeditor.ODMEEditor.fileLocation;

public class MultiAspectNodeTest {


    // Map to store parsed nodes, with parent nodes as keys and lists of combinations as values
    private Map<String, List<String>> parsedNodes;
    private int multiAspectNodeCount;

    public MultiAspectNodeTest() {
        parsedNodes = new HashMap<>();
        multiAspectNodeCount = 0;

    }


    /**
     * Parses nodes containing a specified tag pattern from the given XML file.
     *
     * @paramPath The path to the XML file to be parsed.
     * @paramtagPattern  The pattern to match in tag names (e.g., "Spec", "MAsp").
     */


    public void parseNodes(String xmlFilePath ) {
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

                // Check if the tag matches the specified pattern (e.g., "Spec" or "MAsp")
                if (tagName.contains(tagPattern)) {
                    String parentNodeName = element.getParentNode().getNodeName();
                    NodeList childNodes = element.getChildNodes();

                    // Track the child-parent combinations
                    for (int j = 0; j < childNodes.getLength(); j++) {
                        if (childNodes.item(j) instanceof Element) {
                            Element childElement = (Element) childNodes.item(j);
                            String childNodeName = childElement.getTagName();
                            String combination = childNodeName + "_" + parentNodeName;

                            // Store the combination in the parsedNodes map
                            parsedNodes.computeIfAbsent(parentNodeName, k -> new ArrayList<>()).add(combination);
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



    private void checkCodeCoverageMultiAspect(List<String[]> scenariosList){

        scenariosList.forEach(scenario -> {
            Scanner in = null;
            try{

                String path = fileLocation +"/" + scenario[0] + "/graphxml.xml";

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


                }

            }catch (Exception e){
                    e.printStackTrace();
                }

            });


            /*
            if (file.exists()) {
                //first read all node that contains Specialization keyword
                MAScanner = new Scanner(file);

                while (MAScanner.hasNext()){
                    String line = MAScanner.nextLine();

                    if (line.contains("<") && line.contains("MAsp") && !line.contains("/")) {
                        insideMAspecNode = true;
                    } else if (line.contains("</") && line.contains("MAsp")){
                        insideMAspecNode = false;
                    } else if (insideMAspecNode) {
                        MAspecNodes.add(line.replaceAll("<|>|/", "").trim());
                    }
                }

                System.out.println("Node names below <MAspec>: " + MAspecNodes);

            } else {
                System.out.println("File not found ");
            }

             */
    }

}
