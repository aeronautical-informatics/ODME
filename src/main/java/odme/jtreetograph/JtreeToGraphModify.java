package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphModify {

	/**
     * Modifying the generated graphxml.xml output by removing <start> and </start>
     * tag from the file and making single line element <node1/> two double lines
     * <node1> </node1>
     */
    public static void modifyXmlOutput() {
        PrintWriter f0 = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + projectFileNameGraph + ".xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + ODMEEditor.projName + ".xml";
        	
            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/graphxml.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/graphxml.xml";
        		
            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();

            if (line.endsWith("start>")) {
                continue;
            } 
            else if (line.endsWith("/>")) {
                String result = line.replaceAll("[</>]", "");
                result = result.replaceAll("\\s+", "");
                String line1 = "<" + result + ">";
                String line2 = "</" + result + ">";
                f0.println(line1);
                f0.println(line2);
            } 
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
    }
    
 // for modifying the generated xml output from the project tree. it is not using
    // for graph tree now
    // graph tree are generating from mxGraph files
    public static void modifyXmlOutputSES() {
        PrintWriter f0 = null;
        try {
        	
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/" + newFileName + "Project.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/" + ODMEEditor.projName + "Project.xml";
        	
            f0 = new PrintWriter(new FileWriter(path));	
            	
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/projectTree.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/projectTree.xml";
        	
            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        
        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();

            if (line.endsWith("start>")) {
                continue;
            } 
            else if (line.endsWith("/>")) {
                String result = line.replaceAll("[</>]", "");
                result = result.replaceAll("\\s+", "");
                String line1 = "<" + result + ">";
                String line2 = "</" + result + ">";
                f0.println(line1);
                f0.println(line2);
            }
            else {
                f0.println(line);
            }
        }
        in.close();
        f0.close();
    }
    
    /**
     * Modifying the generated output by making single line element <node/> two
     * double lines <node> </node>
     */
    public static void modifyXmlOutputFixForSameNameNode() {
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

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();

            if (line.endsWith("/>")) {
                String result = line.replaceAll("[</>]", "");
                if (result.endsWith("RefNode")) {
                    f0.println(line);
                }
                else if (result.endsWith("Var")) {
                    f0.println(line);
                } 
                else if (result.endsWith("Con")) {
                    f0.println(line);
                } 
                else {
                    result = result.replaceAll("\\s+", "");
                    String line1 = "<" + result + ">";
                    String line2 = "</" + result + ">";
                    f0.println(line1);
                    f0.println(line2);
                }
            }
            else {
                f0.println(line);
            }
        }

        in.close();
        f0.close();

        copyFixForSameNameNodeToOther();
    }
    
    private static void copyFixForSameNameNodeToOther() {
        PrintWriter f0 = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsd.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsd.xml";
        	
            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/outputgraphxmlforxsdvar.xml";
        	else
        		path = ODMEEditor.fileLocation + "/" + ODMEEditor.currentScenario + "/outputgraphxmlforxsdvar.xml";
        	
            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();
            f0.println(line);
        }

        in.close();
        f0.close();
    }
}
