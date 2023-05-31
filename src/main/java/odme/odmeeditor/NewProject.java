package odme.odmeeditor;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.tree.DefaultMutableTreeNode;

import odme.jtreetograph.JtreeToGraphAdd;
import odme.jtreetograph.JtreeToGraphDelete;
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

/**
 * <h1>New Project</h1>
 * <p>
 * This class implements New Project Window for creating new SES project with
 * configuration like project name, root name and project location.
 * </p>
 *
 * @author ---
 * @version ---
 */

public class NewProject extends JPanel {

    private static final long serialVersionUID = 1L;
    private JFrame frame;
    
    private JLabel projectNameLabel, RootLabel, projectLocationLabel, errorLabelField;
    private JTextField newProjectNameField, newRootNameField, projectLocationField;
    private JCheckBox defaultProjectLocationChecker;
    private JButton selectProjectLocation, create, cancel;

    public NewProject() {
        super(new BorderLayout());
        frame = new JFrame();
    }
    
    /**
     * Create new SES project taking input from user. Here, user inputs are project
     * name, root name and project location. <b>Thing</b> is default root name and
     * default location is jar file location. Check the project name and root name
     * and make sure that they are not same showing error while user type root name.
     * Also check the duplicate project name for the default location.
     */
    public void createNewProjectWindow() {
    	
    	projectNameLabel = new JLabel("Project Name:");
        projectNameLabel.setBounds(20, 30, 120, 30);
        
        newProjectNameField = new JTextField(30);
        newProjectNameField.setBounds(150, 30, 410, 30);

        RootLabel = new JLabel("Root:");
        RootLabel.setBounds(20, 70, 120, 30);
        
        newRootNameField = new JTextField();
        newRootNameField.setBounds(150, 70, 410, 30);
        newRootNameField.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        newRootNameField.setText("Thing");

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
        errorLabelField.setVisible(false);

        create = new JButton("Create");
        create.setBounds(350, 250, 100, 30);
        
        cancel = new JButton("Cancel");
        cancel.setBounds(460, 250, 100, 30);
        cancel.setToolTipText("Cancel");
        
        JPanel panelTop = new JPanel();
        JPanel panelCenter = new JPanel();
        panelCenter.setLayout(null);
        JPanel panelBottom = new JPanel();

        panelCenter.add(projectNameLabel);
        panelCenter.add(newProjectNameField);
        
        panelCenter.add(RootLabel);
        panelCenter.add(newRootNameField);

        panelCenter.add(defaultProjectLocationChecker);
        panelCenter.add(projectLocationField);
        panelCenter.add(selectProjectLocation);

        panelCenter.add(errorLabelField);
        panelCenter.add(create);
        panelCenter.add(cancel);

        panelTop.setBorder(new EtchedBorder());
        panelCenter.setBorder(new EtchedBorder());
        panelBottom.setBorder(new EtchedBorder());

        create.setToolTipText("Create Project");
        
        addFunctions();
        
        int width = 600;
        int height = 360;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screen.width - width) / 2;
        int y = (screen.height - height) / 2;

        frame.setBounds(x, y, width, height);
        frame.setTitle("New Project");
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
                	errorLabelField.setText("There is a file with the same name. It will be overwritten.");
                    errorLabelField.setVisible(true);
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
        
        selectProjectLocation.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
            	selectLocation();
            }
        });
//      -------------------------------------
        
        newRootNameField.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent arg0) {}

            @Override
            public void keyReleased(KeyEvent e) {

                if (newRootNameField.getText().trim().equals(newProjectNameField.getText().trim())) {
                    errorLabelField.setVisible(true);
                    errorLabelField.setText("Root name shold be different from project name.");
                } else {
                    errorLabelField.setText("");
                    errorLabelField.setVisible(false);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}
        });
//      -------------------------------------
        
        create.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	createFunc();
                frame.dispose();
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
    
    private void selectLocation() {
    	
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
    
    private void createFunc() {
    	
    	String newProjectName = newProjectNameField.getText();
        String newRootName = newRootNameField.getText();
        String oldProjectTreeProjectName = ODMEEditor.projName;

        ODMEEditor.projName = newProjectName;
        JtreeToGraphVariables.newFileName = newProjectName;
        JtreeToGraphVariables.projectFileNameGraph = newProjectName;
        JtreeToGraphVariables.nodeNumber = 1;

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

        JtreeToGraphDelete.deleteAllNodesFromGraphWindow(newRootName);
        JtreeToGraphAdd.addPageLengthNodes();

        DefaultMutableTreeNode rootNodeNew = new DefaultMutableTreeNode(newRootName);
        DynamicTree.treeModel.setRoot(rootNodeNew);
        DynamicTree.treeModel.reload();
        ODMEEditor.treePanel.tree.setModel(DynamicTree.treeModel);

        ODMEEditor.projectPanel
                .changeCurrentProjectFileName(newProjectName, oldProjectTreeProjectName);

        Variable.setNullToAllRows();
        Constraint.setNullToAllRows();
        
        ODMEEditor.newProjectFolderCreation();
        Main.createScenariosJson();
    }
}
