package odme.jtreetograph;

import odme.core.EditorContext;
import odme.domain.transform.XmlTransformRules;

import javax.swing.JOptionPane;
import static odme.jtreetograph.JtreeToGraphVariables.*;
import static odme.behaviour.BehaviourToTree.selectedScenario;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphModify {

    private static final XmlTransformRules transformRules = new XmlTransformRules();

    /**
     * Modifying the generated graphxml.xml output by removing <start> and </start>
     * tag from the file and making single line element <node1/> two double lines
     * <node1> </node1>
     */
    public static void modifyXmlOutput() {
        // Read input file
        List<String> inputLines = new ArrayList<>();
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/graphxml.xml";

            Scanner in = new Scanner(new File(path));
            while (in.hasNext()) {
                inputLines.add(in.nextLine());
            }
            in.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Apply transformation rules
        List<String> outputLines = transformRules.applyModifyXmlOutputRules(inputLines);

        // Write output file
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/" + EditorContext.getInstance().getProjName() + ".xml";

            PrintWriter f0 = new PrintWriter(new FileWriter(path));
            for (String line : outputLines) {
                f0.println(line);
            }
            f0.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

 // for modifying the generated xml output from the project tree. it is not using
    // for graph tree now
    // graph tree are generating from mxGraph files
    public static void modifyXmlOutputSES() {
        // Read input file
        List<String> inputLines = new ArrayList<>();
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/projectTree.xml";

            Scanner in = new Scanner(new File(path));
            while (in.hasNext()) {
                inputLines.add(in.nextLine());
            }
            in.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Apply transformation rules (same rules as modifyXmlOutput - drops start> lines and expands self-closing tags)
        List<String> outputLines = transformRules.applyModifyXmlOutputRules(inputLines);

        // Write output file
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/" + EditorContext.getInstance().getProjName() + "Project.xml";

            PrintWriter f0 = new PrintWriter(new FileWriter(path));
            for (String line : outputLines) {
                f0.println(line);
            }
            f0.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Modifying the generated output by making single line element <node/> two
     * double lines <node> </node>
     */
    public static void modifyXmlOutputFixForSameNameNode() {
        // Read input file
        List<String> inputLines = new ArrayList<>();
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/outputgraphxmlforxsd.xml";

            Scanner in = new Scanner(new File(path));
            while (in.hasNext()) {
                inputLines.add(in.nextLine());
            }
            in.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Apply transformation rules
        List<String> outputLines = transformRules.applyModifyXmlOutputFixForSameNameNode(inputLines);

        // Write output file
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/outputgraphxmlforxsdvar.xml";

            PrintWriter f0 = new PrintWriter(new FileWriter(path));
            for (String line : outputLines) {
                f0.println(line);
            }
            f0.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        copyFixForSameNameNodeToOther();
    }

    private static void copyFixForSameNameNodeToOther() {
        PrintWriter f0 = null;
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/outputgraphxmlforxsd.xml";
            f0 = new PrintWriter(new FileWriter(path));
        }
        catch (IOException e1) {
            e1.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Scanner in = null;
        try {
            String path = EditorContext.getInstance().getWorkingDir() + "/outputgraphxmlforxsdvar.xml";
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
