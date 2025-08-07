package odme.module.importFromCameo;

import odme.odmeeditor.ODMEEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import static odme.module.importFromCameo.ImportFromCameo.addValueToArray;

/**
 * This function creates a view to select the
 * Cameo files from which the import will
 * take place.
 */
public class FileImporter {

    private static final String[] requiredFileNames = {"DomainRelations", "OperationLinking", "InstanceRelations","Enums"};
    private static String defaultProjectLocation = ODMEEditor.fileLocation;

    public void showImportDialog(Frame parentFrame) {
        JDialog dialog = new JDialog(parentFrame, "Import Files", true);
        dialog.setSize(600, 300);
        dialog.setLayout(new GridLayout(7, 1));

        // Create a panel for project name
        JPanel projectPanel = new JPanel();
        JTextField projectNameField = new JTextField(20);
        projectPanel.add(new JLabel("Project Name:"));
        projectPanel.add(projectNameField);
        dialog.add(projectPanel);

        // Create a panel for setting default project location
        JPanel locationPanel = new JPanel();
        JTextField locationField = new JTextField(20);
        locationField.setText(defaultProjectLocation);
        locationField.setEditable(false);
        JButton locationButton = new JButton("Choose Project Location");
        locationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fileChooser.setAcceptAllFileFilterUsed(false);
                fileChooser.setCurrentDirectory(new File(ODMEEditor.repFslas));
                int result = fileChooser.showOpenDialog(dialog);
                if (result == JFileChooser.APPROVE_OPTION) {
                    defaultProjectLocation = fileChooser.getSelectedFile().getAbsolutePath();
                    locationField.setText(defaultProjectLocation);
                }
            }
        });
        locationPanel.add(new JLabel("Project Location:"));
        locationPanel.add(locationField);
        locationPanel.add(locationButton);
        dialog.add(locationPanel);

        // Create file chooser buttons and text fields
        JPanel[] filePanels = new JPanel[requiredFileNames.length];
        JTextField[] fileTextFields = new JTextField[requiredFileNames.length];
        JButton[] fileButtons = new JButton[requiredFileNames.length];

        for (int i = 0; i < requiredFileNames.length; i++) {
            filePanels[i] = new JPanel();
            fileTextFields[i] = new JTextField(20);
            fileTextFields[i].setEditable(false);
            fileButtons[i] = createFileButton(dialog, fileTextFields[i], requiredFileNames[i]);
            filePanels[i].add(new JLabel(requiredFileNames[i] + ".csv:"));
            filePanels[i].add(fileTextFields[i]);
            filePanels[i].add(fileButtons[i]);
            dialog.add(filePanels[i]);
        }

        // Add confirm and cancel buttons
        JPanel buttonPanel = new JPanel();
        JButton confirmButton = new JButton("Import");
        JButton cancelButton = new JButton("Cancel");

        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String projectNameString = projectNameField.getText();
                File projectDirectory = new File(defaultProjectLocation + File.separator + projectNameString);
                if (projectDirectory.exists()) {
                    JOptionPane.showMessageDialog(dialog, "Project name already exists in the selected location. Please enter another project name.");
                    return;
                }

                for (int i = 0; i < requiredFileNames.length; i++) {
                    if (fileTextFields[i].getText().isEmpty() || !new File(fileTextFields[i].getText()).getName().equals(requiredFileNames[i] + ".csv")) {
                        JOptionPane.showMessageDialog(dialog, "File name is not correct or file not selected: " + requiredFileNames[i] + ".csv");
                        return;
                    }
                }

                String[] allPaths = null;
                for (JTextField textField : fileTextFields) {
                    allPaths = addValueToArray(allPaths,textField.getText());
                }

                allPaths = addValueToArray(allPaths,projectNameField.getText());
                String domainRelationsValue = allPaths[0].replace("\\", "/");
                String operationLinkingValue = allPaths[1].replace("\\", "/");
                String instanceRelationsValue = allPaths[2].replace("\\", "/");
                String enumsValue = allPaths[3].replace("\\", "/");
                String projectName = allPaths[4];

                ImportFromCameo.importFromAllCSV(domainRelationsValue,operationLinkingValue,projectName,instanceRelationsValue,enumsValue,defaultProjectLocation.replace("\\", "/"));

//                System.out.printf("%-10s \n %-10s \n %-10s", projectName,domainRelationsValue,operationLinkingValue);

                dialog.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel);

        dialog.setVisible(true);
    }

    private static JButton createFileButton(JDialog dialog, JTextField textField, String expectedName) {
        JButton button = new JButton("Select");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                // Set the initial directory to the specific location
                fileChooser.setCurrentDirectory(new File(ODMEEditor.fileLocation));
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    public boolean accept(File f) {
                        return f.isDirectory() || f.getName().toLowerCase().endsWith(".csv");
                    }

                    public String getDescription() {
                        return "CSV Files (*.csv)";
                    }
                });

                int result = fileChooser.showOpenDialog(dialog);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getName().equals(expectedName + ".csv")) {
                        JOptionPane.showMessageDialog(dialog, "Selected file name must be " + expectedName + ".csv");
                        return;
                    }
                    textField.setText(selectedFile.getAbsolutePath());
                }
            }
        });
        return button;
    }
}
