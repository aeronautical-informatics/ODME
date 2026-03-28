package odme.odmeeditor;
import odme.core.EditorContext;
import javax.swing.JOptionPane;
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
      
      File scenarioFile = new File(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json");
      if(!scenarioFile.exists()){ 
    	  createScenariosJson();
      }
      
      //-------------------------------------
    	
    	// look and feel (Modern FlatLaf UI)
        setLookAndFeel();
        
        // show splash screen
        splash = new SplashScreen(1);
        splash.runningPBar();

        // Create and set up the main window.
        frame = new JFrame("Operational Design Domain Modelling Environment");
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
       // Multi-resolution icons for taskbar, title bar, dock, and Alt-Tab
       java.util.List<Image> icons = new java.util.ArrayList<>();
       for (String size : new String[]{"16x16", "32x32", "48x48", "64x64", "128x128", "256x256", "512x512"}) {
           java.net.URL url = ODMEEditor.class.getClassLoader().getResource("logo/odme_icon_" + size + ".png");
           if (url != null) {
               icons.add(new ImageIcon(url).getImage());
           }
       }
       if (!icons.isEmpty()) {
           frame.setIconImages(icons);
           // Set macOS dock icon (overrides default Java coffee cup)
           try {
               Taskbar taskbar = Taskbar.getTaskbar();
               taskbar.setIconImage(icons.get(icons.size() - 1)); // largest icon
           } catch (UnsupportedOperationException | SecurityException ignored) {
               // Not macOS or not supported — frame icons are enough
           }
       }
       frame.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
       frame.setVisible(true);
//      -------------------------------------
      //printMemeoryUsage();
    }
    
    private static void setLookAndFeel() {
        try {
            // Deploy the macOS native Flat theme integration natively out-of-the-box
            com.formdev.flatlaf.themes.FlatMacLightLaf.setup();
            
            // Apply professional UX refinements
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 8);
            UIManager.put("ProgressBar.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 10);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabSeparatorsFullHeight", true);
            UIManager.put("SplitPane.dividerSize", 5);
            UIManager.put("defaultFont", new Font("Helvetica Neue", Font.PLAIN, 13));
        } 
        catch (Exception e) {
            // Fallback gracefully to default if unsupported
            System.err.println("Failed to initialize FlatLaf");
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
	         FileWriter file = new FileWriter(EditorContext.getInstance().getFileLocation() + "/" + EditorContext.getInstance().getProjName() + "/scenarios.json");
	         file.write(data.toJSONString());
	         file.close();
	    } 
    	catch (IOException e) {
	         e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
	    }
    }
}
