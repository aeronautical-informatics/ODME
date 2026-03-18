package odme.jtreetograph;

import odme.core.EditorContext;
import javax.swing.JOptionPane;
import static odme.jtreetograph.JtreeToGraphVariables.*;
import static odme.behaviour.BehaviourToTree.selectedScenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphModify {

    /**
     * This method will change the generated XML file according to requirement.
     */
    public static void modifiyBehaviourXML(){
        PrintWriter f0 = null;
        try
        {
            String path = new String();
            path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() +  "/" + selectedScenario   + "/OutputBehaviourxml.xml";
            f0 = new PrintWriter(new FileWriter(path));

        }catch (IOException e1){
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Scanner in = null;
        try {

            String path = new String();

            path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() +  "/" + selectedScenario   + "/behaviourxml.xml";
            File file = new File (path);
            if (!file.exists()) {
                file.createNewFile();
            }

            in = new Scanner(file);
            while (in.hasNext()) { // Iterates each line in the file
                String line = in.nextLine();
                System.out.println("line = " + line);
                if (line.contains("start")){
                    String replace = "<root main_tree_to_execute>  <MainTree>";
                    String second = "<BehaviorTree ID>  <MainTree>";
                    f0.println(replace);
                    f0.println(second);
                }else {
                    f0.println(line);
                }
            }
            in.close();
            f0.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

    }
        /**
     * Modifying the generated graphxml.xml output by removing <start> and </start>
     * tag from the file and making single line element <node1/> two double lines
     * <node1> </node1>
     */
    public static void modifyXmlOutput() {
        PrintWriter f0 = null;
        try {
            String path = new String();
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/" + EditorContext.getInstance().getNewFileName() + ".xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/" + EditorContext.getInstance().getProjName() + ".xml";

            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Scanner in = null;
        try {
            String path = new String();
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/graphxml.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/graphxml.xml";

            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
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
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/" + EditorContext.getInstance().getNewFileName() + "Project.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/" + EditorContext.getInstance().getProjName() + "Project.xml";

            f0 = new PrintWriter(new FileWriter(path));

        } 
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Scanner in = null;
        try {
            String path = new String();
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/projectTree.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/projectTree.xml";

            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (in != null) {
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
            }
        }
        if (in != null) in.close();
        if (f0 != null) f0.close();
    }
    
    /**
     * Modifying the generated output by making single line element <node/> two
     * double lines <node> </node>
     */
    public static void modifyXmlOutputFixForSameNameNode() {
        PrintWriter f0 = null;
        try {
            String path = new String();
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/outputgraphxmlforxsdvar.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/outputgraphxmlforxsdvar.xml";

            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Scanner in = null;
        try {
            String path = new String();
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/outputgraphxmlforxsd.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/outputgraphxmlforxsd.xml";

            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
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
                else if (result.endsWith("Behaviour")) { // Author: Vadece Kamdem
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
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/outputgraphxmlforxsd.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/outputgraphxmlforxsd.xml";

            f0 = new PrintWriter(new FileWriter(path));
        } 
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Scanner in = null;
        try {
            String path = new String();
            if ("ses".equals(EditorContext.getInstance().getToolMode()))
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/outputgraphxmlforxsdvar.xml";
            else
                path = EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getCurrentScenario() + "/outputgraphxmlforxsdvar.xml";

            in = new Scanner(new File(path));
        } 
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        while (in.hasNext()) { // Iterates each line in the file
            String line = in.nextLine();
            f0.println(line);
        }

        in.close();
        f0.close();
    }
}
