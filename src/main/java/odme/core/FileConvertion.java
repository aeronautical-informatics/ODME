package odme.core;

import javax.swing.tree.TreePath;

import odme.jtreetograph.JtreeToGraphGeneral;
import odme.odmeeditor.ODMEEditor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * <h1>FileConvertion</h1>
 * <p>
 * This class is used to convert files to desired formats which are used in the
 * project for various purposes.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class FileConvertion {

    // for modifying the generated xml output
    public void xmlToXSDConversion() {

        int entiyAfterMAsp = 0;

        PrintWriter f0 = null;
        try {
        	
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/xsdfromxml.xsd";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/xsdfromxml.xsd";
        	
            f0 = new PrintWriter(
                    new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
        	
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsd.xml";
        	
        	
            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (in.hasNext()) {

            String mod = null;
            String line = in.nextLine();
            String backConstraints = line;

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
                f0.println("<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" "
                           + "\n xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\" "
                           + "\n elementFormDefault=\"qualified\" vc:minVersion=\"1.1\" >");
                f0.println(); // for one line gap

            } 
            else if (line.startsWith("</")) {
                String result = line.replaceAll("[</>]", "");

                if (result.endsWith("Dec")) {
                    mod = "</xs:sequence>";
                    f0.println(mod);

                } 
                else if (result.endsWith("MAsp")) {

                    mod = "</xs:sequence>";
                    f0.println(mod);

                } 
                else if (result.endsWith("Spec")) {
                    mod = "</xs:choice>";
                    f0.println(mod);

                } 
                else if (result.endsWith("Seq")) {
                    mod = "</xs:sequence>";
                    f0.println(mod);
                } 
                else {
                    f0.println("<xs:attribute name=\"name\" use=\"optional\"/> ");
                    f0.println("</xs:complexType>");
                    mod = "</xs:element>";
                    f0.println(mod);
                }

            } 
            else if (line.startsWith("<")) {
                if (line.endsWith("/>")) {
                    String result = line.replaceAll("[</>]", "");

                    if (result.endsWith("Var")) {
                        String novarresult = result.replace("Var", "");

                        // variable with proper style
                        String[] properties = novarresult.split(",");
                        if (properties[1].equals("string") || properties[1].equals("boolean")) {
                            f0.println(
                                    "<xs:attribute name=\"" + properties[0] + "\" type=\"" + properties[1]
                                            + "\" default=\"" + properties[2]
                                    + "\">"); // Author: Vadece Kamdem--> I added "\" type=\"" + properties[1] + so that the type should be seen
                            f0.println("</xs:attribute>");
                        } 
                        else {
                            f0.println(
                                    "<xs:attribute name=\"" + properties[0] + "\" type=\"" + properties[1] + "\" default=\"" + properties[2]
                                    + "\">");
                            f0.println("<xs:simpleType>");
                            f0.println("<xs:restriction base=\"xs:" + properties[1] + "\">");
                            f0.println("<xs:type  value=\"" + properties[1] + "\"/>");
                            f0.println("<xs:minInclusive  value=\"" + properties[3] + "\"/>");
                            f0.println("<xs:maxInclusive value=\"" + properties[4] + "\"/>");
                            f0.println("</xs:restriction>");
                            f0.println("</xs:simpleType>");
                            f0.println("</xs:attribute>");
                        }

                    }
                    else if (result.endsWith("Behaviour")) {
                        String nobresult = result.replace("Behaviour", "");

                        // behaviour with proper style
                        String[] properties = nobresult.split(",");
                        f0.println("<xs:attribute name=\"" + properties[0] + "\">");
                        f0.println("</xs:attribute>");

                    }
                    else if (result.endsWith("Con")) {
                        String resultCon =
                                backConstraints.replaceAll("[<>]", ""); // to keep the space backConstraints
                        // is used here otherwise else and
                        // true() will be elsetrue();
                        String nonconresult =
                                resultCon.replace("Con/", ""); // with Con also / added here then Con/ for
                        // replacement. because xpath query itself
                        // contain /, so can not all / from query
                        f0.println("<xs:assert test=\"" + nonconresult + "\" />");
                    } 
                    else if (result.endsWith("RefNode")) {
                        String noRefNoderesult = result.replace("RefNode", "");

                        if (noRefNoderesult.endsWith("Dec") || noRefNoderesult.endsWith("MAsp")) {
                            f0.println("<xs:sequence ref=\"" + noRefNoderesult + "\"/>");
                        } 
                        else if (noRefNoderesult.endsWith("Spec")) {
                            f0.println("<xs:choice ref=\"" + noRefNoderesult + "\"/>");
                        } 
                        else {
                            f0.println("<xs:element ref=\"" + noRefNoderesult + "\"/>");
                        }

                    } 
                    else {
                        mod = "<xs:element name=\"" + result + "\"/>";
                        f0.println(mod);
                    }

                } 
                else {
                    String result = line.replaceAll("[</>]", "");

                    if (result.endsWith("Dec")) {
                        mod = "<xs:sequence id=\"" + result + "\">";
                        f0.println(mod);

                    } 
                    else if (result.endsWith("MAsp")) {
                        mod = "<xs:sequence id=\"" + result + "\">";
                        f0.println(mod);
                        entiyAfterMAsp = 1;

                    } 
                    else if (result.endsWith("Spec")) {
                        mod = "<xs:choice id=\"" + result + "\">";
                        f0.println(mod);
                    } 
                    else {
                        if (entiyAfterMAsp == 1) {
                            mod = "<xs:element name=\"" + result
                                  + "\" minOccurs=\"0\" maxOccurs=\"unbounded\">";
                            f0.println(mod);
                            f0.println("<xs:complexType>");

                            entiyAfterMAsp = 0;
                        } 
                        else if (result.endsWith("Seq")) {
                            mod = "<xs:sequence>";
                            f0.println(mod);
                        } 
                        else {
                            mod = "<xs:element name=\"" + result + "\">";
                            f0.println(mod);
                            f0.println("<xs:complexType>");
                        }
                    }
                }
            }
        }

        f0.println("</xs:schema>");
        in.close();
        f0.close();
    }

    public void behaviourAdditionToNode(TreePath key, String variableName) { // Author: Vadece Kamdem
        Object[] stringArrayRev = key.getPath();

        int len = stringArrayRev.length;
        int count = 0;

        Scanner in = null;
        try {
            String path = new String();
            if (ODMEEditor.toolMode == "ses")
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml";
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsd.xml";

            in = new Scanner(new File(path));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintWriter f0 = null;
        try {
            String path = new String();
            if (ODMEEditor.toolMode == "ses")
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml";
            else
                path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsdvar.xml";

            f0 = new PrintWriter(new FileWriter(path));
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }

        while (in.hasNext()) {
            String line = in.nextLine();
            String backLine = line;

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            }
            else if (line.startsWith("<if(")) {
                f0.println(backLine);
            }
            else if (line.startsWith("<")) {
                String result = line.replaceAll("[</>]", "");

                if (count < len) {
                    if (result.equals(stringArrayRev[count].toString())) {
                        count++;
                    }
                    if (count == len) {
                        if (line.endsWith("/>")) {
                            f0.println("<" + result + ">");
                            f0.println(variableName + "Behaviour");
                            f0.println("</" + result + ">");
                        }
                        else if (line.startsWith("<")) {
                            f0.println("<" + result + ">");
                            f0.println(variableName + "Behaviour");
                        }
                        else {
                            f0.println(line);
                        }
                    }
                    else {
                        f0.println(line);
                    }
                }
                else {
                    f0.println(line);
                }
            }
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
        copyFileToExistingOne();
    }

    public void variableAdditionToNode(TreePath key, String variableName) {
        Object[] stringArrayRev = key.getPath();

        int len = stringArrayRev.length;
        int count = 0;

        Scanner in = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsd.xml";
        	
            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintWriter f0 = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsdvar.xml";
        	
            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        while (in.hasNext()) {
            String line = in.nextLine();
            String backLine = line;

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            } 
            else if (line.startsWith("<if(")) {
                f0.println(backLine);
            } 
            else if (line.startsWith("<")) {
                String result = line.replaceAll("[</>]", "");

                if (count < len) {
                    if (result.equals(stringArrayRev[count].toString())) {
                        count++;
                    }
                    if (count == len) {
                        if (line.endsWith("/>")) {
                            f0.println("<" + result + ">");
                            f0.println(variableName + "Var");
                            f0.println("</" + result + ">");
                        }
                        else if (line.startsWith("<")) {
                            f0.println("<" + result + ">");
                            f0.println(variableName + "Var");
                        } 
                        else {
                            f0.println(line);
                        }
                    } 
                    else {
                        f0.println(line);
                    }
                } 
                else {
                    f0.println(line);
                }
            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
        copyFileToExistingOne();
    }

    public void constraintAdditionToNode(String selectedNode, String variableName) {
        Scanner in = null;
        try {
            in = new Scanner(new File(ODMEEditor.fileLocation + "/" + "/outputgraphxmlforxsd.xml"));
//                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml"));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter( ODMEEditor.fileLocation + "/" + "/outputgraphxmlforxsdvar.xml"));
//                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml"));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            } 
            else if (line.startsWith("</")) {
                String result = line.replaceAll("[</>]", "");

                if (result.equals(selectedNode)) {
                    f0.println(line);
                    f0.println(variableName + "Con");
                    //System.out.println(selectedNode + ": \n" + variableName);
                } 
                else {
                    f0.println(line);
                }
            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
        copyFileToExistingOne();
    }

    /**
     * Add constraint to the aspect node in the SES XML structure. To do this take
     * the aspect node path from root and the constraint as arguments.
     *
     * @param sesNodesInPath
     * @param constraint
     */
    public void addConstraintToSESStructure(String[] sesNodesInPath, String constraint) {
        int len = sesNodesInPath.length;
        int count = 0;
        System.out.println("file path  = "+ODMEEditor.fileLocation + "/");
        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(                    ODMEEditor.fileLocation + "/testcon.xml"));
//                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/testcon.xml")); // outputgraphxmlforxsdvar
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
            in = new Scanner(new File(ODMEEditor.fileLocation  + "/xmlforxsd.xml"));
//                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/xmlforxsd.xml"));

        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (in.hasNext()) {
            String line = in.nextLine();

            if (line.startsWith("<?")) {
                f0.println(line);
            } 
            else if (line.startsWith("</")) {
                f0.println(line);
            } 
            else if (line.startsWith("<")) {
                String node = line.replaceAll("[<>]", "");
                String[] splited = node.split("\\s+");

                if (count < len) {
                    if (splited[0].equals(sesNodesInPath[count])) {
                        count++;
                    }
                    if (count == len) {
                        f0.println("<" + splited[0] + " " + splited[1] + " " + "constraint=\"" + constraint
                                   + "\" " + ">");
                    } 
                    else {
                        f0.println(line);
                    }
                } 
                else {
                    f0.println(line);
                }

            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
    }

    public void placeAssertInRightPosition() {
        PrintWriter f0 = null;

        try {
            f0 = new PrintWriter(new FileWriter(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml"));

        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
            in = new Scanner(new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/xsdfromxml.xsd"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        boolean finishChaningLinePosition = false;
        boolean deleteExtraAtrributeLineBelowAssert = false;

        while (in.hasNext()) {
            String line = in.nextLine();
            if (line.startsWith("<xs:assert") && !finishChaningLinePosition) {

                f0.println("<xs:attribute name=\"name\" use=\"optional\"/> ");
                f0.println(line);
                finishChaningLinePosition = true;

            } 
            else if (line.startsWith("<xs:attribute") && !deleteExtraAtrributeLineBelowAssert
                       && finishChaningLinePosition) {
                deleteExtraAtrributeLineBelowAssert = true;
            } 
            else {
                f0.println(line);
            }
        }

        in.close();
        f0.close();

        copyChangedXSDtoOldOne(); // copy this output to existing one
        ///////////////////////////////////////////////////////
        copyxsdfromxmlToRootNodeNameXSD();
    }

    public void constraintAdditionToNode(TreePath key, String variableName) {
        Object[] stringArrayRev = key.getPath();

        int len = stringArrayRev.length;
        int count = 0;

        Scanner in = null;
        try {
            in = new Scanner(new File(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml"));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml"));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();
            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            } 
            else if (line.startsWith("<")) {
                String result = line.replaceAll("[</>]", "");

                if (count < len) {
                    if (result.equals(stringArrayRev[count].toString())) {
                        count++;
                    }
                    if (count == len) {
                        if (line.endsWith("/>")) {
                            f0.println(line);
                            f0.println(variableName + "Con");
                        } 
                        else {
                            f0.println(line);
                        }

                    } 
                    else {
                        f0.println(line);
                    }
                } 
                else {
                    f0.println(line);
                }
            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
        copyFileToExistingOne(); // copy this output to existing one
    }

    public void addingUniformityRefNodeToXML(String[] stringArrayRev, String cellName) {
        int len = stringArrayRev.length;
        int count = 0;

        Scanner in = null;
        try {
            in = new Scanner(new File(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml"));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml"));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();
            String backline = line; // for assert statement

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            } 
            else if (line.startsWith("if")) {
                f0.println(backline);
            } 
            else if (line.startsWith("</")) {
                f0.println(line);
            } 
            else if (line.startsWith("<")) {
                String result = line.replaceAll("[</>]", "");

                if (count < len) {
                    if (result.equals(stringArrayRev[count])) {
                        count++;
                    }
                    if (count == len) {
                        f0.println(line);
                        f0.println("<" + cellName + "RefNode/>");
                    } 
                    else {
                        f0.println(line);
                    }
                } else {
                    f0.println(line);
                }
            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();

        copyFileToExistingOne();
    }

    // this file is used when in one node there will be multiple aspect or
    // combination of aspect and specialization or multiaspect. It add Seq node and
    // for this i get <entity name="Seq">
    // to remove <entity name="Seq"> i have added Seq ignore in xmlOutputForXSD
    // conversion function in JtreToGraph class
    //
    public void fixingSequenceProblem(String[] stringArrayRev) {
        int len = stringArrayRev.length;
        int count = 0;

        Scanner in = null;
        try {
            in = new Scanner(new File(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml"));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(
                    ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdseq.xml"));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        String seqNode = "";

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();
            String backline = line;

            if (line.startsWith("<?")) { // have to solve space problem for this line
                f0.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>");
            } 
            else if (line.startsWith("if")) {
                f0.println(backline);

            } 
            else if (line.startsWith("</")) {

                String result = line.replaceAll("[</>]", "");
                if (result.equals(seqNode)) {
                    f0.println("</Seq>");
                    f0.println(line);
                } 
                else {
                    f0.println(line);
                }

            } 
            else if (line.startsWith("<")) {
                String result = line.replaceAll("[</>]", "");

                if (count < len) {
                    if (result.equals(stringArrayRev[count])) {
                        count++;
                    }
                    if (count == len) {
                        f0.println(line);
                        seqNode = result;
                        f0.println("<Seq>");
                    } 
                    else {
                        f0.println(line);
                    }
                } 
                else {
                    f0.println(line);
                }
            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();

        copyfixingSequenceFileToExistingOne();
    }

    public void modifyXmlOutputForRefNode() {
    	copyModifyHelper("/outputgraphxmlforxsd.xml", "/outputgraphxmlforxsdvar.xml", "/>");
    }

    /**
     * Modify the generated graphxml.xml output from graph to remove <start> and
     * </start> tag from the file.
     */
    public void modifyXmlOutputForXSD() {
    	copyModifyHelper("/outputgraphxmlforxsd.xml", "/graphxmluniformity.xml", "start>");
    }

    public void copyChangedXSDtoOldOne() {
    	copyModifyHelper("/xsdfromxml.xsd", "/outputgraphxmlforxsdvar.xml", null);
    }
    
    public void copyFileToExistingOne() {
    	copyModifyHelper("/outputgraphxmlforxsd.xml", "/outputgraphxmlforxsdvar.xml", null);
    }
    
    public void copyfixingSequenceFileToExistingOne() {
    	copyModifyHelper("/outputgraphxmlforxsd.xml", "/outputgraphxmlforxsdseq.xml", null);
    }

    public void copyxsdfromxmlToRootNodeNameXSD() {
        String rootNodeName = JtreeToGraphGeneral.rootNodeName();
        copyModifyHelper("/" + rootNodeName + ".xsd", "/outputgraphxmlforxsdvar.xml", null);
    }
    
    private void copyModifyHelper(String file1, String file2, String modify) {
        PrintWriter f0 = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + file1;
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + file1;
        	
            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }
        
        Scanner in = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + file2;
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + file2;
        	
            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();
            if (modify != null) {
            	if (line.endsWith(modify))
            		continue;
            }
            f0.println(line);
        }
        in.close();
        f0.close();
    }

    public void createSES(String path) {
        PrintWriter f0 = null;
        try {
            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        // writing to the file
        String ses = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                     + "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" elementFormDefault=\"qualified\"\r\n"
                     + "    xmlns:vc=\"http://www.w3.org/2007/XMLSchema-versioning\" vc:minVersion=\"1.1\">\r\n"
                     + "    \r\n" + "        <xs:complexType name=\"aspectType\">\r\n"
                     + "        <xs:sequence>\r\n"
                     + "            <xs:element minOccurs=\"0\" maxOccurs=\"unbounded\" ref=\"entity\"/>\r\n"
                     + "        </xs:sequence>\r\n"
                     + "        <xs:attribute name=\"name\" use=\"required\"/>\r\n"
                     + "    </xs:complexType>\r\n" + "\r\n"
                     + "    <xs:complexType name=\"multiAspectType\">\r\n" + "        <xs:sequence>\r\n"
                     + "            <xs:element minOccurs=\"0\" maxOccurs=\"unbounded\" ref=\"entity\"/>\r\n"
                     + "        </xs:sequence>\r\n"
                     + "        <xs:attribute name=\"name\" use=\"required\"/>\r\n"
                     + "        <xs:attribute name=\"constraint\" use=\"optional\"/>\r\n"
                     + "    </xs:complexType>\r\n" + "\r\n"
                     + "    <xs:complexType name=\"specializationType\">\r\n" + "        <xs:sequence>\r\n"
                     + "            <xs:element minOccurs=\"0\" maxOccurs=\"unbounded\" ref=\"entity\"/>\r\n"
                     + "        </xs:sequence>\r\n"
                     + "        <xs:attribute name=\"name\" use=\"required\"/>\r\n"
                     + "    </xs:complexType>\r\n" + "\r\n" + "\r\n"
                     + "    <xs:complexType name=\"varType\"> \r\n" + "        <xs:sequence>\r\n"
                     + "            <xs:element minOccurs=\"0\" maxOccurs=\"unbounded\" ref=\"entity\"/>\r\n"
                     + "        </xs:sequence>\r\n"
                     + "        <xs:attribute name=\"name\" use=\"required\"/>\r\n"
                     + "        <xs:attribute name=\"type\" use=\"optional\"/>\r\n"
                     + "        <xs:attribute name=\"default\" use=\"optional\"/>\r\n"
                     + "        <xs:attribute name=\"lower\" use=\"optional\"/>\r\n"
                     + "        <xs:attribute name=\"upper\" use=\"optional\"/>\r\n" + "        \r\n"
                     + "    </xs:complexType>\r\n" + "\r\n" + "\r\n"
                     + "    <xs:complexType name=\"behaviourType\"> \r\n" + "        <xs:sequence>\r\n"
                     + "            <xs:element minOccurs=\"0\" maxOccurs=\"unbounded\" ref=\"entity\"/>\r\n"
                     + "        </xs:sequence>\r\n"
                     + "        <xs:attribute name=\"name\" use=\"required\"/>\r\n" + "        \r\n"
                     + "    </xs:complexType>\r\n" + "\r\n" + "\r\n" + "    <xs:element name=\"entity\">\r\n"
                     + "        <xs:complexType>\r\n" + "            <xs:sequence>\r\n"
                     + "                <xs:choice minOccurs=\"0\" maxOccurs=\"unbounded\">\r\n"
                     + "                    <xs:element ref=\"aspect\"/>\r\n"
                     + "                    <xs:element ref=\"specialization\"/>\r\n"
                     + "                    <xs:element ref=\"multiAspect\"/>\r\n"
                     + "                    <xs:element ref=\"var\"/>\r\n"
                     + "                    <xs:element ref=\"behaviour\"/>\r\n" + "                    \r\n"
                     + "                </xs:choice>            \r\n" + "                \r\n"
                     + "            </xs:sequence>\r\n" + "\r\n"
                     + "            <xs:attribute name=\"name\" use=\"required\"/>\r\n"
                     + "            <xs:attribute name=\"ref\" use=\"optional\"/>\r\n" + "          \r\n"
                     + "            <xs:assert test=\"every $x in .//entity satisfies empty($x//*[@name = $x/@name])\"/> \r\n"
                     + "            <xs:assert test=\"every $x in .//entity satisfies count(*[$x/@name = $x/following-sibling::*/@name]) = 0\"/>                \r\n"
                     + "            <xs:assert test=\"every $x in .//var satisfies count(*[@name = following-sibling::*/@name]) = 0\"/>                                     \r\n"
                     + "            \r\n" + "        </xs:complexType>      \r\n" + "        \r\n"
                     + "      \r\n" + "    </xs:element>\r\n" + "\r\n"
                     + "    <xs:element name=\"aspect\" type=\"aspectType\"/>\r\n"
                     + "    <xs:element name=\"multiAspect\" type=\"multiAspectType\"/>\r\n"
                     + "    <xs:element name=\"specialization\" type=\"specializationType\"/>\r\n"
                     + "    <xs:element name=\"var\" type=\"varType\"/>\r\n"
                     + "    <xs:element name=\"behaviour\" type=\"behaviourType\"/>   \r\n" + "\r\n" + "     \r\n"
                     + "</xs:schema>\r\n" + "";

        f0.println(ses);
        f0.close();
    }
}
