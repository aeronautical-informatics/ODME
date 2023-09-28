package behaviourtreetograph;

import odme.odmeeditor.ODMEEditor;
import java.io.*;
import java.util.Scanner;
import static odeme.behaviour.BehaviourToTree.selectedScenario;
import static odme.odmeeditor.ODMEEditor.fileLocation;


public class JtreeToGraphModify {
    /**
     * This method will change the generated XML file according to requirement.
     */
    public static void modifiyXMLFile(){
        PrintWriter f0 = null;
        try {
            String path = new String();

            path = fileLocation + "/" + ODMEEditor.projName +  "/" +selectedScenario+ "/OutputBehaviourxml.xml";

            f0 = new PrintWriter(new FileWriter(path));
        }

        catch (IOException e1) {
            e1.printStackTrace();
        }

        Scanner in = null;

        try{

            String path = new String();

            path = fileLocation +"/" + ODMEEditor.projName +  "/" +selectedScenario+"/behaviourxml.xml";
            File file = new File (path);
            if (!file.exists()) {
                file.createNewFile();
            }

            in = new Scanner(file);
            while (in.hasNext()){

                String line = in.nextLine();

                if (line.equals("<start>")) {
                    String result = "root main_tree_to_execute='MainTree'";
                    String line1 = "<" + result +">";

                    String behaviourID= "BehaviourTree ID= 'MainTree'";
                    String line2 = "<" + behaviourID + ">";
                    f0.print(line1);
                    f0.print(line2);
                }
                else if (line.equals("</start>"))
                {
                    String result = "root";
                    String behaviourId = "BehaviourTree";

                    result = result.replaceAll("\\s","");
                    String line1 = "</" + result +">";
                    String line2 = "</" + behaviourId + ">";

                    f0.print(line2);
                    f0.print(line1);
                }
                else if (line.equals("<Events>")){
                    String event = "RootNode name='Events' ";
                    String appendLine = "<" + event + ">";
                    f0.print(appendLine);
                }
                else if(line.equals("</Events>")) {
                    String event = "RootNode";
                    String appendLine = "</" + event + ">";
                    f0.print(appendLine);
                }
                else if (line.endsWith("/>")) {
                    line = line.replaceAll("<","");
                    line = line.replaceAll("/>","");
                    String formated = String.format("<Action name = '%s'  >",line);
                    String line2 = "</" +"Action" + ">";
                    f0.println(formated);
                    f0.println(line2);
                }
                else {
                    f0.println(line);
                }
            }

            in.close();
            f0.close();
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
