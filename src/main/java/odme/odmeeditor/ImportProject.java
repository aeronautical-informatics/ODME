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

import odme.jtreetograph.JtreeToGraphImport;
import odme.jtreetograph.JtreeToGraphVariables;

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
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        ODMEEditor.treePanel.ssdFileFlag = new File(String.format("%s/%s/%s.ssdflag",
        		ODMEEditor.fileLocation, ODMEEditor.projName, newProjectName));

        ProjectTree.projectName = newProjectName;

        ODMEEditor.projectPanel
                .changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);
        
        

        Variable.setNullToAllRows();
        Constraint.setNullToAllRows();

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
        // below function is working. Have to make a xml file like projectName.xml for
        // example Main.xml
        JtreeToGraphImport.importExistingProjectIntoGraph();
    }
}
