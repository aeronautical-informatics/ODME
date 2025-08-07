package odme.module.importFromCameo;

import odme.odmeeditor.ODMEEditor;
import org.json.JSONObject;
import org.json.JSONTokener;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static odme.module.importFromCameo.ImportFromCameo.addValueToArray;

/***
 * This class helps in the conversion of json data format
 * to XML format and creates a file to store the
 * XML data temporally
 ***/
public class JsonToXml {

    private static String[] relationshipsSelectionIR = null;
    private static String[] nodeNames = null;
    private static String[] variableDataTemp = null;
    private static String[] variableData = null;

    public static void jsonToXml(JSONObject jsonObject,String domainName,String[] relationshipsSelectionIR,String[] enumsParams) {
        JsonToXml.relationshipsSelectionIR = relationshipsSelectionIR;

        //rearranging the variable data for better sorting
        String nodeName_AndVariableName_ = null;
        String nodeName__ = null;
        String nodeName_ = null;
        String variableName__ = null;
        String variableName_ = null;
        StringBuilder finalData = null;
        for(String nodeNameAndVariableParams : relationshipsSelectionIR) {

            String nodeNameRAW = nodeNameAndVariableParams.split(" -> ")[0];

            if (nodeNameRAW.split(" : ").length == 1) {
                nodeName__ = nodeNameRAW;
                variableName__ = nodeNameAndVariableParams.split(" -> ")[1];
                variableName__ = variableName__.split(" = ")[0] + " == "+variableName__.split(" = ")[1];
                if (variableName__.split(" == ")[1].split("\\.")[0].equals(domainName.toLowerCase())) {
                    variableName__ = variableName__.split(" == ")[0] + " == none";
                }
                finalData = new StringBuilder(nodeName__ + " -> " + variableName__);
                variableDataTemp = addValueToArray(variableDataTemp, finalData.toString());

            } else {
                nodeName__ = nodeNameRAW.split(" : ")[1];

                if (!nodeName__.equals("Range")) {
                    variableName__ = nodeNameAndVariableParams.split(" -> ")[1];
                    variableName__ = variableName__.split(" = ")[0] + " == "+variableName__.split(" = ")[1];
                    if (variableName__.split(" == ")[1].split("\\.")[0].equals(domainName.toLowerCase())) {
                        variableName__ = variableName__.split(" == ")[0] + " == none";
                    }
                    finalData = new StringBuilder(nodeName__ + " -> " + variableName__);
                    variableDataTemp = addValueToArray(variableDataTemp, finalData.toString());
                }
            }

        }

        for(String variableDataTemp : variableDataTemp) {
            nodeName__ = variableDataTemp.split(" -> ")[0];
            variableName__ = variableDataTemp.split(" -> ")[1];
            finalData = new StringBuilder(nodeName__ + " -> " + variableName__);


            for (String nodeNameAndVariableParams : relationshipsSelectionIR) {

                String nodeNameRAW = nodeNameAndVariableParams.split(" -> ")[0];
                String variableNameRAW = nodeNameAndVariableParams.split(" -> ")[1];

                if (nodeNameRAW.split(" : ").length > 1) {
                    String nodeName___ = nodeNameRAW.split(" : ")[1];

                    if (nodeName___.equals("Range")) {

                        nodeName_AndVariableName_ = nodeNameRAW.split(" : ")[0];
                        nodeName_ = nodeName_AndVariableName_.split("\\.")[nodeNameRAW.split(" : ")[0].split("\\.").length - 2];
                        variableName_ = nodeName_AndVariableName_.split("\\.")[nodeNameRAW.split(" : ")[0].split("\\.").length - 1];

                        if(nodeName_.equalsIgnoreCase(nodeName__) && variableName_.equalsIgnoreCase(variableName__.split(" == ")[0])) {
                            finalData.append(" ->> ").append(variableNameRAW);
                        }
                    }
                }
            }

//            System.out.print(finalData);
//            System.out.println();
            variableData = addValueToArray(variableData, finalData.toString());

        }

        for(String variableData : variableData) {
            String nodeName = variableData.split(" -> ")[0];
            nodeNames = addValueToArray(nodeNames,nodeName);
        }

        if (jsonObject != null) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.newDocument();

                // Main aspect
                Element mainAspect = doc.createElement("aspect");
                mainAspect.setAttribute("name", domainName+"Dec");
                doc.appendChild(mainAspect);
                for(String nodeName : nodeNames) {
                    if(nodeName.equals(domainName)) {
                        doc.removeChild(mainAspect);
                        mainAspect = doc.createElement("specialization");
                        mainAspect.setAttribute("name", domainName+"Spec");
                        doc.appendChild(mainAspect);
                        break;
                    } else {
                        doc.removeChild(mainAspect);
                        doc.appendChild(mainAspect);
                    }
                }


                // Process JSON
                processJsonObject(doc, mainAspect, jsonObject);

                // Remove empty paragraphs before saving
                removeEmptyParagraphs(doc);

                // Output the XML content and save in file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");


                // Transform to string to remove empty lines
                 StringWriter writer = new StringWriter();
                 transformer.transform(new DOMSource(doc), new StreamResult(writer));
                 String xmlString = writer.getBuffer().toString();

                 // Remove empty lines
                String cleanedXmlString = xmlString.replaceAll("(?m)^[ \t]*\r?\n", "");

                // Write to file
                try (FileWriter fileWriter = new FileWriter(ODMEEditor.fileLocation + "\\cameoTemp.xml")) {
                    fileWriter.write(cleanedXmlString);
                }

                //add the root element
                String path = ODMEEditor.fileLocation + "\\cameoTemp.xml";
                String newLine = "<entity xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"ses.xsd\" name=\""+domainName+"\">";
                String newLineAfterLast = "</entity>";
                AddLineAfterFirst(path, newLine, newLineAfterLast);
                AddVariableLine(path);

            } catch (ParserConfigurationException | TransformerException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static JSONObject readJsonFile(String filePath) {
        StringBuilder jsonData = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                jsonData.append(line);
            }
            return new JSONObject(new JSONTokener(jsonData.toString()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void processJsonObject(Document doc, Element parent, JSONObject jsonObject) {
        for (String key : jsonObject.keySet()) {

            /*
             * Check if it is an entity, a behaviour or a variable
             * Distinguishes between aspects, multiAspects and specializations
             *
             */
            Element entity = doc.createElement("entity");
            Element behaviour = doc.createElement("behaviour");
            String keyValue = key;

            if(key.endsWith("ThisIsAnEnumEntity")) {
                keyValue = keyValue.substring(0, keyValue.length() - 18);
            }
            entity.setAttribute("name", keyValue);
            parent.appendChild(entity);

            // Closing the entity tag explicitly
            entity.appendChild(doc.createTextNode("\n"));

            //for the variables
            String nodeName = null;
            for(String nodeNameAndVariableParams : relationshipsSelectionIR) {

                String nodeNameRAW = nodeNameAndVariableParams.split(" -> ")[0];

                if(nodeNameRAW.split(" : ").length==1) {
                    nodeName = nodeNameRAW;
                } else {
                    nodeName = nodeNameRAW.split(" : ")[1];
                }

                if(nodeName.equals(key)) {

                    Element variable = doc.createElement("VAR"+key);
                    entity.appendChild(variable);
                    // Closing the variable tag explicitly
                    variable.appendChild(doc.createTextNode(""));
                }
            }

            JSONObject childObject = jsonObject.optJSONObject(key);
            if (childObject != null && !childObject.isEmpty()) {

                //for the behaviour
                String nextKey = childObject.keys().next();
                if(nextKey.endsWith("()")) {
                    String newKey = nextKey.substring(0, nextKey.length() - 2);
                    behaviour.setAttribute("name", newKey);
                    entity.appendChild(behaviour);
                    // Closing the behaviour tag explicitly
                    behaviour.appendChild(doc.createTextNode(" "));
                    continue;
                }

                //for the relationtypes (aspect, specialization and MultiAspect)
                Element nextRelationType = doc.createElement("aspect");
                nextRelationType.setAttribute("name", key + "Dec");
                if(key.endsWith("ThisIsAnEnumEntity")) {
                    String value = key.substring(0, key.length() - 18);
                    nextRelationType = doc.createElement("specialization");
                    nextRelationType.setAttribute("name", value + "Spec");
                }
                entity.appendChild(nextRelationType);
                processJsonObject(doc, nextRelationType, childObject);
            } else {
                //for multiAspect
                for(String nodeNameAndVariableParams : relationshipsSelectionIR) {

                    String nodeNameRAW = nodeNameAndVariableParams.split(" -> ")[0];

                    if(nodeNameRAW.split(" : ").length==1) {
                        nodeName = nodeNameRAW;
                    } else {
                        nodeName = nodeNameRAW.split(" : ")[1];
                    }

                    String variableNameAndVariableValue = nodeNameAndVariableParams.split(" -> ")[1];
                    String variableName = variableNameAndVariableValue.split(" = ")[0];

                    if(nodeName.equals(key)) {
                        if(variableName.equals("NumberEntities")) {
                            Element nextRelationType = doc.createElement("multiAspect");
                            nextRelationType.setAttribute("name", key + "MAsp");

                            Element entity_multiAspect = doc.createElement("entity");
                            entity_multiAspect.setAttribute("name", key);
                            nextRelationType.appendChild(entity_multiAspect);

                            // Closing the entity tag explicitly
                            entity_multiAspect.appendChild(doc.createTextNode("\n"));

                            entity.appendChild(nextRelationType);

                            // Closing the entity tag explicitly
                            entity.appendChild(doc.createTextNode("\n"));
                        }
                    }
                }
            }
        }
    }

    private static void removeEmptyParagraphs(Document doc) {
        NodeList paragraphs = doc.getElementsByTagName("paragraph");
        for (int i = 0; i < paragraphs.getLength(); i++) {
            Element paragraph = (Element) paragraphs.item(i);
            if (paragraph.getTextContent().trim().isEmpty()) {
                paragraph.getParentNode().removeChild(paragraph);
            }
        }
    }

    public static void AddLineAfterFirst(String filePath, String newLine, String newLineAfterLast) {
        try {
            // Read the file contents into a list
            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }

            // Add the new line after the first line
            if (lines.size() > 0) {
                lines.add(1, newLine);
            }

            // Add the new line after the last line
            if (lines.size() > 1) {
                lines.add(lines.size(), newLineAfterLast);
            }

            // Write the updated contents back to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (String line : lines) {
                    writer.write(line);
                    writer.newLine();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void AddVariableLine(String path) {

        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < lines.size(); i++) {
            String nodeName = null;
            String variableName = null;
            String variableNameAndVariableValue = null;
            String replaceString = null;
            for(String nodeNameAndVariableParams : variableData) {

                nodeName = nodeNameAndVariableParams.split(" -> ")[0];
                variableNameAndVariableValue = nodeNameAndVariableParams.split(" -> ")[1];
                variableName = variableNameAndVariableValue.split(" == ")[0];
                String variableValues = variableNameAndVariableValue.split(" == ")[1];

                String searchString = "<VAR"+nodeName+"/>";
                replaceString = "<var name=\""+variableName+"\" type=\"string\" default=\""+variableValues+"\"> </var>"; // I just set the default type to string


//                System.out.print(variableNameAndVariableValue);
//                System.out.println("NEXT!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
//                System.out.println();


                if(variableValues.split(" ->> ").length>1) {
                    String[] varValue = variableValues.split(" ->> ");

                    String defaultVal = varValue[0];
                    String maxVal = varValue[1].split(" = ")[1];
                    String minVal = varValue[2].split(" = ")[1];

                    replaceString = "<var name=\""+variableName+"\" type=\"float\" default=\""+defaultVal+"\" lower=\""+minVal+"\" upper=\""+maxVal+"\"> </var>";
                }


                if (lines.get(i).contains(searchString)) {
                    lines.set(i, lines.get(i).replace(searchString, replaceString));


                    //delete item from array
                    String target = nodeNameAndVariableParams; // The element we're looking for

                    // Get the index of the target element
                    int index = -1;
                    for (int ii = 0; ii < variableData.length; ii++) {
                        if (variableData[ii] == target) {
                            index = ii;
                            break;
                        }
                    }

                    // Delete the element if it was found
                    if (index != -1) {
                        String[] newArray = new String[variableData.length - 1];
                        for (int ii = 0, j = 0; ii < variableData.length; ii++) {
                            if (ii != index) {
                                newArray[j++] = variableData[ii];
                            }
                        }
                        variableData = newArray;
                    }
                    break;
                }
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, false))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




