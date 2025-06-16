package odme.odmeeditor;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.mxgraph.model.mxCell;
import odeme.behaviour.Behaviour;
import odme.jtreetograph.JtreeToGraphAdd;
import odme.jtreetograph.JtreeToGraphConvert;
import odme.jtreetograph.JtreeToGraphImport;
import odme.jtreetograph.JtreeToGraphVariables;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static odme.jtreetograph.JtreeToGraphVariables.pathToRoot;

/**
 * <h1>ImportProject</h1>
 * <p>
 * This class implements Import Project Window for importing SES project created
 * in other SES IDE and saved as XML file following a certain format
 * </p>
 *
 * @author ---
 * @version ---
 */
public class ImportProject extends JPanel {

	private static final long serialVersionUID = 1L;
	private JFrame frame;
    private JLabel projectNameLabel, defaultRootNameChecker, projectLocationLabel, errorLabelField;
    private JTextField newProjectNameField, newRootNameField, projectLocationField;
    private JButton selectImportProjectLocation, selectProjectLocation, create, cancel;
    private JCheckBox defaultProjectLocationChecker;
    static String variableParams ="";

    public ImportProject() {
        super(new BorderLayout());
        frame = new JFrame();
    }

    /**
     * Import SES project created in other SES IDE. Check the project name and make
     * sure that they are not same showing error while user type project name for
     * the default location.
     */
    public void importProject() {

        projectNameLabel = new JLabel("Project Name:");
        projectNameLabel.setBounds(20, 30, 120, 30);
        newProjectNameField = new JTextField(30);
        newProjectNameField.setBounds(150, 30, 410, 30);

        defaultRootNameChecker = new JLabel("Select Import Project:");
        defaultRootNameChecker.setBounds(20, 70, 120, 30);

        newRootNameField = new JTextField();
        newRootNameField.setBounds(150, 70, 300, 30);
        newRootNameField.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        newRootNameField.setText("");

        selectImportProjectLocation = new JButton("Browse...");
        selectImportProjectLocation.setBounds(460, 70, 100, 30);

        defaultProjectLocationChecker = new JCheckBox("Use Default Location:");
        defaultProjectLocationChecker.setBounds(20, 110, 140, 30);
        defaultProjectLocationChecker.setSelected(true);

        projectLocationLabel = new JLabel("Location:");
        projectLocationLabel.setBounds(20, 250, 120, 30);
        projectLocationLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        projectLocationLabel.setEnabled(false);
        projectLocationField = new JTextField();
        projectLocationField.setBounds(20, 150, 430, 30);
        projectLocationField.setEnabled(false);
        projectLocationField.setText(ODMEEditor.fileLocation);
        selectProjectLocation = new JButton("Browse...");
        selectProjectLocation.setBounds(460, 150, 100, 30);
        selectProjectLocation.setEnabled(false);

        errorLabelField = new JLabel();
        errorLabelField.setForeground(Color.RED);
        errorLabelField.setBounds(20, 210, 320, 30);
        errorLabelField.setVisible(true);

        create = new JButton("Import");
        create.setBounds(350, 250, 100, 30);
        cancel = new JButton("Cancel");
        cancel.setBounds(460, 250, 100, 30);

        JPanel panelTop = new JPanel();
        JPanel panelCenter = new JPanel();
        panelCenter.setLayout(null);
        JPanel panelBottom = new JPanel();

        panelCenter.add(projectNameLabel);
        panelCenter.add(newProjectNameField);

        panelCenter.add(defaultRootNameChecker);
        panelCenter.add(newRootNameField);
        panelCenter.add(selectImportProjectLocation);

        panelCenter.add(defaultProjectLocationChecker);
        panelCenter.add(projectLocationField);
        panelCenter.add(selectProjectLocation);

        panelCenter.add(errorLabelField);
        panelCenter.add(create);
        panelCenter.add(cancel);

        panelTop.setBorder(new EtchedBorder());
        panelCenter.setBorder(new EtchedBorder());
        panelBottom.setBorder(new EtchedBorder());

        create.setToolTipText("Import Project");
        cancel.setToolTipText("Cancel");
        
        addFunctions();
        
        int width = 600;
        int height = 360;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;

        frame.setBounds(x, y, width, height);
        frame.setTitle("Import SES Project");
        frame.pack();
        frame.setSize(width, height);
        frame.setVisible(true);
        frame.add(panelTop, BorderLayout.NORTH);
        frame.add(panelCenter, BorderLayout.CENTER);
        frame.add(panelBottom, BorderLayout.SOUTH);
    }
    
    private void addFunctions() {
    	newProjectNameField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyReleased(KeyEvent e) {
                String name = newProjectNameField.getText().trim();
                File fileName = new File(name);
                if (fileName.exists() && fileName.isDirectory()) {
                    errorLabelField.setVisible(true);
                    errorLabelField.setText("There is a file with the same name. It will be overwritten.");
                } else {
                    errorLabelField.setVisible(false);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {}
        });
    	
//      -------------------------------------
        defaultProjectLocationChecker.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    projectLocationField.setText(ODMEEditor.fileLocation);
                    projectLocationField.setEnabled(false);
                    projectLocationLabel.setEnabled(false);
                    selectProjectLocation.setEnabled(false);
                } else {
                    projectLocationField.setEnabled(true);
                    projectLocationLabel.setEnabled(true);
                    selectProjectLocation.setEnabled(true);
                    projectLocationField.setText("");
                }
            }
        });

//      -------------------------------------
        selectImportProjectLocation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter xmlfilter = new FileNameExtensionFilter("xml files (*.xml)", "xml");
                fileChooser.setFileFilter(xmlfilter);
                fileChooser.setCurrentDirectory(new File(ODMEEditor.repFslas));
                int result = fileChooser.showOpenDialog(Main.frame);

                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    ODMEEditor.importFileName = selectedFile.getName();
                    ODMEEditor.importFileLocation = selectedFile.getParentFile().getAbsolutePath();
                    newRootNameField.setText(ODMEEditor.importFileLocation);
                }
            }
        });

//      -------------------------------------
        selectProjectLocation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setCurrentDirectory(new File(ODMEEditor.repFslas));
                int result = fileChooser.showOpenDialog(Main.frame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    ODMEEditor.fileLocation = selectedFile.getAbsolutePath();
                    projectLocationField.setText(ODMEEditor.fileLocation);
                }
            }
        });
        
//      -------------------------------------
        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	createFunc();
            }
        });
        
//      -------------------------------------
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });
    }
    
    private void createFunc() {
        DynamicTree.varMap.clear();
        DynamicTree.behavioursList.clear();
        DynamicTree.constraintsList.clear();

    	String newProjectName = newProjectNameField.getText();
    	if (newProjectName.trim().length() == 0 || errorLabelField.isVisible() == true) {
    		JOptionPane.showMessageDialog(this, "Project name should be not empty and not alrady exist!", "Import Error!",
                    JOptionPane.WARNING_MESSAGE);
    		return;
    	}
    	
        //String newRootName = null;

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
        frame.dispose();

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
            else if (line.startsWith("<var") && line.endsWith("/var>")) { // Author: Vadece Kamdem
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

    // Author:Vadece Kamdem
    public static void importBehaviour(Object positionBehaviour, String nodeName) {
            try {
                String filePath = ODMEEditor.importFileLocation + "/" + ODMEEditor.importFileName;
                // Parse the XML file
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document document = builder.parse(new File(filePath));
                document.getDocumentElement().normalize();

                // Find TreePaths to <behaviour> elements
                List<String> path = new ArrayList<>();
                List<TreePath> behaviourPaths = new ArrayList<>();

                // List to store TreePaths of behaviour elements
                findBehaviourPaths(document.getDocumentElement(), path, behaviourPaths, nodeName);

                // Print the TreePaths of behaviour elements
                for (TreePath treePathForVariable : behaviourPaths) {
                    Object[] pathComponents = treePathForVariable.getPath();
                    String behaviourName = (String) pathComponents[pathComponents.length - 1];
//                    System.out.println(treePathForVariable + " - " + behaviourName);

                    JtreeToGraphAdd.addBehaviourFromImport(behaviourName, positionBehaviour);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    // Author:Vadece Kamdem
    private static void findBehaviourPaths(Node node, List<String> path, List<TreePath> behaviourPaths, String nodeName) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (element.hasAttribute("name")) {
                path.add(element.getAttribute("name"));
            } else {
                path.add(element.getTagName());
            }
            if (element.getTagName().equals("behaviour")) {
                TreePath treePath = new TreePath(path.toArray());
                Object[] pathComponents = treePath.getPath();
                String behaviourName = (String) pathComponents[pathComponents.length - 1]+"BevOr";

                if(behaviourName.equals(nodeName)) {
                    behaviourPaths.add(treePath);// Add the TreePath to the list
                }
            }
            NodeList nodeList = element.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                findBehaviourPaths(nodeList.item(i), new ArrayList<>(path), behaviourPaths,nodeName);
            }
        }
    }

    // Author:Vadece Kamdem
    public static void importVariable(Object positionVariable, String nodeNameAndParams) {
        try {
            String filePath = ODMEEditor.importFileLocation + "/" + ODMEEditor.importFileName;
            // Parse the XML file
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(filePath));
            document.getDocumentElement().normalize();

            // Find TreePaths to <behaviour> elements
            List<String> path = new ArrayList<>();
            List<TreePath> variablePaths = new ArrayList<>();

            System.out.println("PRINT---"+nodeNameAndParams);
            System.out.println();
            String[] nodeParams = nodeNameAndParams.split(",");

            for (String value : nodeParams) {
                if (value.equals(nodeParams[0])) {
                    variableParams = value;
                    continue;
                }
                variableParams = variableParams + "," + value;
            }

            String nodeName = nodeParams[0];
            // List to store TreePaths of variable elements
            findVariablePaths(document.getDocumentElement(), path, variablePaths, nodeName);

            // Print the TreePaths of variable elements
            for (TreePath treePathForVariable : variablePaths) {
                Object[] pathComponents = treePathForVariable.getPath();
                String variableName = (String) pathComponents[pathComponents.length - 1];
//                System.out.println(treePathForVariable + " - " + variableName);
                System.out.println(treePathForVariable + " - " + variableName);

                JtreeToGraphAdd.addVariableFromImport(variableParams, positionVariable);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("FINISHED");
//        System.out.println();
    }

    // Author:Vadece Kamdem
    private static void findVariablePaths(Node node, List<String> path, List<TreePath> variablePaths, String nodeName) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;
            if (element.hasAttribute("name")) {
                path.add(element.getAttribute("name"));
            } else {
                path.add(element.getTagName());
            }
            if (element.getTagName().equals("var")) {
                TreePath treePath = new TreePath(path.toArray());
                Object[] pathComponents = treePath.getPath();
                String variableName = (String) pathComponents[pathComponents.length - 1];

                if(variableName.equals(nodeName)) {
                    variablePaths.add(treePath);// Add the TreePath to the list
                }
            }
            NodeList nodeList = element.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                findVariablePaths(nodeList.item(i), new ArrayList<>(path), variablePaths,nodeName);
            }
        }
    }
}
