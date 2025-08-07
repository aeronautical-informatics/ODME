package odme.module.importFromCameo;

import odeme.behaviour.Behaviour;
import odme.jtreetograph.JtreeToGraphImport;
import odme.jtreetograph.JtreeToGraphVariables;
import odme.odmeeditor.*;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/***
 * This class imports projects form Cameo to ODME
 * author: Vadece Kamdem
 * email: vadece3@gmail.com
 **/
public class ImportFromCameo {

    private static String[] relationships = null;
    private static String[] relationshipsSelectionDR = null;
    private static String[] relationshipsSelectionIR = null;
    private static String[] enumsParams = null;
    private static String domainName = null;
    private static String[] enumAffectedEntities = null;

    /**
     ** This function is made to import
     ** a Cameo project by using its CSV files
     **/
    public static void importFromAllCSV(String domainRelationsValue,String operationLinkingValue,String projectName, String instanceRelationsValue,String enums,String defaultProjectLocation) {

        importFromCSVEnums(enums);
        importFromCSVDomainRelations(domainRelationsValue);
        importFromCSVInstanceRelations(instanceRelationsValue);
        importFromCSVOperationLinking(operationLinkingValue);


        JSONObject json = relationshipsToJson(relationships);

        //saves the XML data into a temp file
        JsonToXml.jsonToXml(json,domainName,relationshipsSelectionIR,enumsParams);

//        System.out.printf("%-10s \n %-10s \n %-10s \n %-10s", projectName,domainRelationsValue,operationLinkingValue,defaultProjectLocation);

//        String xmlFileLocation = ODMEEditor.fileLocation + "cameoTemp.xml";

        ODMEEditor.importFileName = "cameoTemp.xml";
        ODMEEditor.importFileLocation = ODMEEditor.fileLocation;
        ODMEEditor.fileLocation = defaultProjectLocation;

        createFunc(projectName);

        //Deleting the cameoTemp.xml file created in the function JsonToXml
        File file = new File(ODMEEditor.fileLocation + "/cameoTemp.xml");
        file.delete();
    }

    public static void importFromCSVDomainRelations(String domainRelationsValue) {
        int num = 1;
        String file = domainRelationsValue;
        BufferedReader reader = null;
        String line = "";
        ArrayList<String[]> rowNum = new ArrayList<>();
        String[] rowHeader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            while((line = reader.readLine()) != null) {
                    rowHeader = line.split(",");
                break;
            }
            while((line = reader.readLine()) != null){
                String[] row = line.split(",");
                rowNum.add(row);
            }

            //get domain name
            domainName = rowHeader[1].split("\\[")[0].trim().replace("\"","");

        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for( int x = 0 ; x < rowNum.size() ; x++ ) {
            String[] rowContent = rowNum.get(x);

            for( int xx = 0 ; xx < rowContent.length ; xx++ ) {
                if(rowContent[xx].equals("\"->\"")) {
                    String content = null;
                    for(String enumAffectedEntity : enumAffectedEntities) {

                        content = rowHeader[xx].replace("\"", "").split(" \\[E")[0];
                        if(content.equals(enumAffectedEntity)) {
                            content = rowHeader[xx].replace("\"", "").split(" \\[E")[0]+"ThisIsAnEnumEntity";
                            break;
                        }
                    }

                    String tempRelations = rowHeader[x+1].replace("\"", "")+" -> "+content;
                    relationships = addValueToArray(relationships, tempRelations);

                    tempRelations = rowHeader[x+1].replace("\"", "").split(" \\[E")[0]+" -> "+rowHeader[xx].replace("\"", "").split(" \\[E")[0];
//                    System.out.print(num+"-"+tempRelations);
//                    System.out.println();
                    relationshipsSelectionDR = addValueToArray(relationshipsSelectionDR, tempRelations);
                    num++;
                }
            }
        }
    }

    public static void importFromCSVOperationLinking(String operationLinkingValue) {
        String file = operationLinkingValue;
        BufferedReader reader = null;
        String line = "";
        ArrayList<String[]> rowNum = new ArrayList<>();
        String[] rowHeader = null;
        ArrayList<String> rowName = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(file));

            while((line = reader.readLine()) != null) {
                rowHeader = line.split(",");
                break;
            }
            while((line = reader.readLine()) != null){
                String[] row = line.split(",");
                rowNum.add(row);

                rowName.add(row[0]);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for( int x = 0 ; x < rowNum.size() ; x++ ) {
            String[] rowContent = rowNum.get(x);
            for( int xx = 0 ; xx < rowContent.length ; xx++ ) {
                if(rowContent[xx].equals("\"<-\"")) {
                   String tempRelations = rowName.get(x).replace("\"", "")+" -> "+rowHeader[xx].replace("\"", "");
                   relationships = addValueToArray(relationships, tempRelations);
                }
            }
        }
    }

    public static void importFromCSVInstanceRelations(String instanceRelationsValue) {
        int num = 1;
        String file = instanceRelationsValue;
        BufferedReader reader = null;
        String line = "";
        ArrayList<String[]> rowNum = new ArrayList<>();
        String[] rowHeader = null;
        ArrayList<String> rowName = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(file));

            while((line = reader.readLine()) != null) {
                rowHeader = line.split(",");
                break;
            }
            while((line = reader.readLine()) != null){
                String[] row = line.split(",");
                rowNum.add(row);

                rowName.add(row[0]);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for( int x = 0 ; x < rowNum.size() ; x++ ) {
            String[] rowContent = rowNum.get(x);
            for( int xx = 0 ; xx < rowContent.length ; xx++ ) {
                if(rowContent[xx].equals("\"<-\"")) {

                    String[] entityNameRawList = rowName.get(x).replace("\"", "").split(" \\[E")[0].split("::");
                    String entityName = entityNameRawList[entityNameRawList.length-1];

                    String[] entityInstanceRawList = rowHeader[xx].replace("\"", "").split(" \\[E")[0].split("=");
                    String entityInstance;
                    if (!entityInstanceRawList[0].isEmpty()) {
                        entityInstance = rowHeader[xx].replace("\"", "").split(" \\[E")[0];
                        String tempRelations = entityName+" -> "+entityInstance;
//                        System.out.print(num+"-"+tempRelations);
//                        System.out.println();
                        relationshipsSelectionIR = addValueToArray(relationshipsSelectionIR, tempRelations);
                        num++;
                    }
                }
            }
        }
    }


    public static void importFromCSVEnums(String enumValue) {
        enumAffectedEntities = null;
        String file = enumValue;
        BufferedReader reader = null;
        String line = "";
        ArrayList<String[]> rowNum = new ArrayList<>();
        String[] rowHeader = null;
        ArrayList<String> rowName = new ArrayList<>();

        try {
            reader = new BufferedReader(new FileReader(file));

            while((line = reader.readLine()) != null) {

                String[] row = line.split("\",\"");
                rowNum.add(row);

                rowName.add(row[1]);
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        for( int x = 1 ; x < rowNum.size() ; x++ ) {
            String[] rowContent = rowNum.get(x);
            for( int xx = 2 ; xx < rowContent.length ; xx++ ) {

                String[] contents = rowContent[xx].replace("\"", "").split(",");
                for(String content : contents) {
                    enumAffectedEntities = addValueToArray(enumAffectedEntities,rowName.get(x).replace("\"", ""));
                    String tempRelations = rowName.get(x).replace("\"", "")+"ThisIsAnEnumEntity -> "+content;
                    relationships = addValueToArray(relationships, tempRelations);

//                    System.out.print(tempRelations);
//                    System.out.println();
                    enumsParams = addValueToArray(enumsParams, tempRelations);
                }
            }
        }
    }


    public static String[] addValueToArray(String[] array, String value) {
        if(array == null) {
            array = new String[1];
            array[0] = value;
            return array;
        }else {
            // Create a new array with one more element than the original array
            String[] newArray = new String[array.length + 1];

            // Copy the original array elements to the new array
            System.arraycopy(array, 0, newArray, 0, array.length);

            // Add the new value to the end of the new array
            newArray[newArray.length - 1] = value;

            return newArray;
        }
    }

    private static void createFunc(String newProjectName) {
        DynamicTree.varMap.clear();
        DynamicTree.behavioursList.clear();
        DynamicTree.constraintsList.clear();
        String oldProjectTreeProjectName = ODMEEditor.projName;

        ODMEEditor.projName = newProjectName;
        JtreeToGraphVariables.newFileName = newProjectName;
        JtreeToGraphVariables.projectFileNameGraph = newProjectName;

        JtreeToGraphVariables.ssdFileGraph = new File(String.format("%s/%s/%sGraph.xml",
                ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));

        ODMEEditor.treePanel.ssdFile = new File(String.format("%s/%s/%s.xml",
                ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
        ODMEEditor.treePanel.ssdFileVar = new File(String.format("%s/%s/%s.ssdvar",
                ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
        ODMEEditor.treePanel.ssdFileCon = new File(String.format("%s/%s/%s.ssdcon",
                ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));
        ODMEEditor.treePanel.ssdFileBeh = new File(String.format("%s/%s/%s.ssdbeh",
                ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));

        ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
                ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));

        ProjectTree.projectName = newProjectName;

        ODMEEditor.projectPanel
                .changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);



        Variable.setNullToAllRows();
        Constraint.setNullToAllRows();
        Behaviour.setNullToAllRows();

        System.out.println(newProjectName);

        ODMEEditor.newProjectFolderCreation();

        importProjectStart();
    }

    private static void importProjectStart() {
        Scanner in = null;
        try {
            in = new Scanner(new File(ODMEEditor.importFileLocation + "/" + ODMEEditor.importFileName));
        }
        catch (FileNotFoundException e1) {
            JOptionPane.showMessageDialog(Main.frame, "Import error!", "Import Error!",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + ODMEEditor.projName + ".xml"));
            f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        }
        catch (IOException e1) {
            JOptionPane.showMessageDialog(Main.frame, "Import error!", "Import Error!",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Stack<String> stackEntity, stackAspect, stackMultiAspect, stackSpecialization, stackBehaviour, stackVar;
        stackEntity = new Stack<String>();
        stackAspect = new Stack<String>();
        stackMultiAspect = new Stack<String>();
        stackSpecialization = new Stack<String>();
        stackBehaviour = new Stack<String>();
        stackVar = new Stack<String>();

        while (in.hasNext()) {
            String line = in.nextLine();
            String[] partsOfLine = line.split(" ");
            int len = partsOfLine.length;

            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(partsOfLine[len - 1]);
            String element = "";
            while (m.find()) {
                element = m.group(1);
            }

            if (line.startsWith("<entity")) {
                f0.println("<" + element + ">");
                stackEntity.push("</" + element + ">");
            }
            else if (line.startsWith("<aspect")) {
                f0.println("<" + element + ">");
                stackAspect.push("</" + element + ">");
            }
            else if (line.startsWith("<multiAspect")) {
                f0.println("<" + element + ">");
                stackMultiAspect.push("</" + element + ">");
            }
            else if (line.startsWith("<specialization")) {
                f0.println("<" + element + ">");
                stackSpecialization.push("</" + element + ">");
            }
            else if (line.startsWith("</entity")) {
                String pop = (String) stackEntity.pop();
                f0.println(pop);
            }
            else if (line.startsWith("</aspect")) {
                String pop = (String) stackAspect.pop();
                f0.println(pop);
            }
            else if (line.startsWith("</multiAspect")) {
                String pop = (String) stackMultiAspect.pop();
                f0.println(pop);
            }
            else if (line.startsWith("</specialization")) {
                String pop = (String) stackSpecialization.pop();
                f0.println(pop);
            }
            else if (line.startsWith("<behaviour") && line.endsWith("/behaviour>")) { // Author: Vadece Kamdem
                p = Pattern.compile("\"([^\"]*)\"");
                m = p.matcher(partsOfLine[len - 2]);
                element = "";
                while (m.find()) {
                    element = m.group(1);
                }
                f0.println("<" + element + "BevOr>");
                stackBehaviour.push("</" + element + "BevOr>");
                String pop = (String) stackBehaviour.pop();
                f0.println(pop);
            }
            else if (line.startsWith("<var") && line.endsWith("/var>")) {
                p = Pattern.compile("\"([^\"]*)\"");
                if (len==5) {
                    m = p.matcher(partsOfLine[len - 4]);
                    Matcher mType = p.matcher(partsOfLine[len - 3]);
                    Matcher mDefault = p.matcher(partsOfLine[len - 2]);
                    element = "";
                    String elementDefault = "";
                    String elementType = "";
                    while (m.find()) {
                        element = m.group(1);
                    }
                    while (mType.find()) {
                        elementType = mType.group(1);
                    }
                    while (mDefault.find()) {
                        elementDefault = mDefault.group(1);
                    }
//                    System.out.println(element+"-"+elementType+"-"+elementDefault);
                    f0.println("<" + element + "," + elementType + "," + elementDefault + "VarLe>");
                    stackVar.push("</" + element + "VarLe>");
                    String pop = (String) stackVar.pop();
                    f0.println(pop);
                }
                else {
                    m = p.matcher(partsOfLine[len - 6]);
                    Matcher mType = p.matcher(partsOfLine[len - 5]);
                    Matcher mDefault = p.matcher(partsOfLine[len - 4]);
                    Matcher mLower = p.matcher(partsOfLine[len - 3]);
                    Matcher mUpper = p.matcher(partsOfLine[len - 2]);
                    element = "";
                    String elementDefault = "";
                    String elementType = "";
                    String elementLower = "";
                    String elementUpper = "";
                    while (m.find()) {
                        element = m.group(1);
                    }
                    while (mType.find()) {
                        elementType = mType.group(1);
                    }
                    while (mDefault.find()) {
                        elementDefault = mDefault.group(1);
                    }
                    while (mLower.find()) {
                        elementLower = mLower.group(1);
                    }
                    while (mUpper.find()) {
                        elementUpper = mUpper.group(1);
                    }
//                    System.out.println(element+"-"+elementType+"-"+elementDefault+"-"+elementLower+"-"+elementUpper);
                    f0.println("<" + element + "," + elementType + "," + elementDefault + "," + elementLower + "," + elementUpper + "VarLe>");
                    stackVar.push("</" + element + "VarLe>");
                    String pop = (String) stackVar.pop();
                    f0.println(pop);
                }
            }
        }
        in.close();
        f0.close();

        // below function is working. Have to make a xml file like projectName.xml for
        // example Main.xml
        JtreeToGraphImport.importExistingProjectIntoGraph();

    }

    public static JSONObject relationshipsToJson(String[] relationships) {
        Map<String, JSONObject> nodes = new HashMap<>();

        for(String relationship : relationships){

            String[] parts = relationship.split("->");
            String parent = parts[0].trim();
            String child = parts[1].trim();
            String parentName = parent.split("\\[")[0].trim();
            String childName = child.split("\\[")[0].trim();

            if (!nodes.containsKey(parentName)) {
                JSONObject parentJson = new JSONObject();
                nodes.put(parentName, parentJson);
            }

            if (!nodes.containsKey(childName)) {
                JSONObject childJson = new JSONObject();
                nodes.put(childName, childJson);
            }

            nodes.get(parentName).put(childName, nodes.get(childName));
        }

        return nodes.get(domainName);
    }

    private static void rest_importProjectStart() {
        Scanner in = null;
        try {
            in = new Scanner(new File(ODMEEditor.importFileLocation + "/" + ODMEEditor.importFileName));
        }
        catch (FileNotFoundException e1) {
            JOptionPane.showMessageDialog(Main.frame, "Import error!", "Import Error!",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + ODMEEditor.projName + ".xml"));
            f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
        }
        catch (IOException e1) {
            JOptionPane.showMessageDialog(Main.frame, "Import error!", "Import Error!",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Stack<String> stackEntity, stackAspect, stackMultiAspect, stackSpecialization;
        stackEntity = new Stack<String>();
        stackAspect = new Stack<String>();
        stackMultiAspect = new Stack<String>();
        stackSpecialization = new Stack<String>();

        while (in.hasNext()) {
            String line = in.nextLine();
            String[] partsOfLine = line.split(" ");
            int len = partsOfLine.length;

            Pattern p = Pattern.compile("\"([^\"]*)\"");
            Matcher m = p.matcher(partsOfLine[len - 1]);
            String element = "";
            while (m.find()) {
                element = m.group(1);
            }

            if (line.startsWith("<entity")) {
                f0.println("<" + element + ">");
                stackEntity.push("</" + element + ">");
            }
            else if (line.startsWith("<aspect")) {
                f0.println("<" + element + ">");
                stackAspect.push("</" + element + ">");
            }
            else if (line.startsWith("<multiAspect")) {
                f0.println("<" + element + ">");
                stackMultiAspect.push("</" + element + ">");
            }
            else if (line.startsWith("<specialization")) {
                f0.println("<" + element + ">");
                stackSpecialization.push("</" + element + ">");
            }
            else if (line.startsWith("</entity")) {
                String pop = (String) stackEntity.pop();
                f0.println(pop);
            }
            else if (line.startsWith("</aspect")) {
                String pop = (String) stackAspect.pop();
                f0.println(pop);
            }
            else if (line.startsWith("</multiAspect")) {
                String pop = (String) stackMultiAspect.pop();
                f0.println(pop);
            }
            else if (line.startsWith("</specialization")) {
                String pop = (String) stackSpecialization.pop();
                f0.println(pop);
            }
        }
        in.close();
        f0.close();

        //Deleting the cameoTemp.xml file created in the function JsonToXml
        File file = new File(ODMEEditor.fileLocation + "/cameoTemp.xml");
//        file.delete();


        // below function is working. Have to make a xml file like projectName.xml for
        // example Main.xml
        JtreeToGraphImport.importExistingProjectIntoGraph();
    }

    /**
     * Just a sample function to import
     * a CSV file and print in the console
     **/
    public static void importFromCSVSample() {
        String file = ODMEEditor.fileLocation+"\\ODME-main\\src\\DomainRelations.csv";
        BufferedReader reader = null;
        String line = "";
        Integer num = 0;

        try {
            reader = new BufferedReader(new FileReader(file));
            while((line = reader.readLine()) != null){

                String[] row = line.split(",");

                for(String index : row) {
                    System.out.printf("%-10s", index);
                }
                System.out.println();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            try {
                reader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}


