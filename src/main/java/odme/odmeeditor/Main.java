package odme.odmeeditor;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/*
 * run configuration for eclipse:
 * mainclass=> odme.odmeeditor.Main
 * */


public class Main {
	public static JFrame frame;
	
	private static SplashScreen splash;
	private static ODMEEditor newContentPane;
	private static MenuBar menuBar;
	private static ToolBar toolBar;

    public static void main(String[] args) {
      // Create the folder of the Main project if it doesn't exist
      File f = new File("Main");
      f.mkdirs(); 
      
      File scenarioFile = new File(ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/scenarios.json");
      if(!scenarioFile.exists()){ 
    	  createScenariosJson();
      }
      
      //-------------------------------------
    	
    	// look and feel
        setLookAndFeel();
        
        // show splash screen
        splash = new SplashScreen(1);
        splash.runningPBar();

        // Create and set up the main window.
        frame = new JFrame("Operation Domain Modeling Environment");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        newContentPane = new ODMEEditor();
        frame.setContentPane(newContentPane);
      
        // add menubar
        menuBar = new MenuBar(frame);
     	menuBar.show();
     	
     	// add toolbar
     	toolBar = new ToolBar(frame);
     	toolBar.show();
     	
     	ODMEEditor.addStatuBar(frame);

     	frame.add(ODMEEditor.splitPane, BorderLayout.CENTER);
      
//      -------------------------------------
       frame.pack();
       ImageIcon windowIcon =
              new ImageIcon(ODMEEditor.class.getClassLoader().getResource("images/tu_clausthal_icon.jpg"));
       frame.setIconImage(windowIcon.getImage());
       frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
       frame.setVisible(true);
//      -------------------------------------
      //printMemeoryUsage();
    }
    
    private static void setLookAndFeel() {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } 
        catch (Exception e) {
            // If Nimbus is not available, then have to set another look and feel.
        }
    }
    
    @SuppressWarnings("unchecked")
	public static void createScenariosJson() {
    	JSONArray data = new JSONArray();
    	JSONObject jo = new JSONObject();
    	jo.put("name", "InitScenario");
    	jo.put("risk", "");
    	jo.put("remarks", "");
			
		JSONObject jom = new JSONObject();
		jom.put("scenario", jo);			
		data.add(jom);
    	
    	try {
	         FileWriter file = new FileWriter(ODMEEditor.fileLocation + "/" + ODMEEditor.projName + "/scenarios.json");
	         file.write(data.toJSONString());
	         file.close();
	    } 
    	catch (IOException e) {
	         e.printStackTrace();
	    }
    }
}
