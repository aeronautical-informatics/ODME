package structuretest;

import com.google.common.collect.Multimap;
import odme.odmeeditor.ODMEEditor;

import javax.swing.tree.TreePath;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class BehaviourCoverageTest {


    private int totalBehaviours;
    private int matchedBehaviours;

    private Multimap<TreePath, String> behavioursList;
    private List<String> behaviourValuesList;

    public BehaviourCoverageTest() {
        behaviourValuesList = new ArrayList<>();
    }

    public void checkCodeCoverageForBehaviours(List<String[]> scenariosList) {

        // Now get list of all behaviours saved in project.ssdbeh file.
        File ssdFileBeh = new File(ODMEEditor.fileLocation + "/" +ODMEEditor.projName + ".ssdbeh");
        System.out.println("Behaviour file path = " + ssdFileBeh);

        if (ssdFileBeh.exists()) {
            try {
                ObjectInputStream oisbeh = new ObjectInputStream(new FileInputStream(ssdFileBeh));
                behavioursList = (Multimap<TreePath, String>) oisbeh.readObject();
                oisbeh.close();

                behaviourValuesList = new ArrayList<>(behavioursList.values());
                System.out.println("behaviourList values  = " + behaviourValuesList);

            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Behaviour file does not exist ");
        }

        // Now check each scenario's behaviour XML file to verify if behaviours are used or not
        int totalValues = behaviourValuesList.size();
        int matchedSpecialNodes = 0;

        // Checking each scenario's behaviourxml file to check if behaviours are used or not
        for (String[] scenario : scenariosList) {
            try {
                String path = ODMEEditor.fileLocation + "\\" + scenario[0] + "\\"+"behaviourxml.xml";
                File file = new File(path);

                if (!file.exists()) {
                    System.out.println("File does not exist: " + path);
                } else {
                    Scanner in = new Scanner(file);
                    while (in.hasNext()) {
                        String line = in.nextLine();
                        if (line.endsWith("/>")) {
                            line = line.replaceAll("<", "");
                            line = line.replaceAll("/>", "");
                        }
                        if (behaviourValuesList.contains(line)) {
                            System.out.println("Behaviour Match found: " + line);
                            matchedSpecialNodes++; // Increment matched nodes
                        }
                    }
                    in.close();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        double percentageMatched = (totalValues > 0) ? ((double) matchedSpecialNodes / totalValues) * 100 : 0;

        totalBehaviours = totalValues;
        matchedBehaviours = matchedSpecialNodes;

        // Display result in a dialog
//        String message = "Match Percentage: " + String.format("%.2f", percentageMatched) + "%";
//        JOptionPane.showMessageDialog(null, message, "Matched Behaviours Results", JOptionPane.INFORMATION_MESSAGE);
    }

    public int getTotalBehaviours() {
        return totalBehaviours;
    }

    public int getMatchedBehaviours() {
        return matchedBehaviours;
    }
}
