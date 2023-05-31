package odme.odmeeditor;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class XmlUtils {
	
    public static XMLViewer ontologyview, sesview, schemaview;
    
    public static XMLViewer initView(String title) {
    	XMLViewer view = new XMLViewer();
    	view.setPreferredSize(new Dimension(200, 200));
    	ODMEEditor.removeTopLeftIcon(view);
    	view.pack();
    	view.setVisible(true);
    	view.setTitle(title);
    	view.textArea.setEditable(false);
    	
    	return view;
    }
    
    public static void showViewer(String fileLocation, String projName, String fileName, XMLViewer view) {
        Scanner in = null;
        try {
        	String path = new String();
        	if (ODMEEditor.toolMode == "ses")
        		path = fileLocation + "/" + projName + "/"+fileName;
        	else 
        		path = fileLocation + "/" + ODMEEditor.currentScenario + "/"+fileName; 
        	
            in = new Scanner(new File(path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder xmlcontent = new StringBuilder();
        while (in.hasNext()) {
            String line = in.nextLine();
            xmlcontent.append(line+"\n");
        }

        view.textArea.setText(xmlcontent.toString());
        in.close();
    }
}
