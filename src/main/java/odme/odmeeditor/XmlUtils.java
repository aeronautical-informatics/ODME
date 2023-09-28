package odme.odmeeditor;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

import org.json.JSONObject;
import org.json.XML;
import org.json.JSONException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

import org.xml.sax.Locator;

public class XmlUtils {
	
    public static XMLViewer ontologyview, sesview, schemaview;
    
    /**
     * @author Roy
     * #ROY - adding new funcitonality
     * DEPRECATED
     * */
    public static String xmlToJson(String xml) {
    	JSONObject xmlJsonObj=null;
    	try {xmlJsonObj = XML.toJSONObject(xml);} 
    	catch (JSONException je) {System.out.println(je.toString());}
    	if(xmlJsonObj==null)return null;
    	return xmlJsonObj.toString(4);
    }
    
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
    
    /**
     * @author Roy
     *  converts xsd to yaml, but the newly converted text can be converted back to xsd
     *  DEPRECATED
     * */
    public static String xsdToYaml(String fileLocation, String projName, String fileName) {
    	
    	// make the path string
    	String path = new String();
    	if (ODMEEditor.toolMode == "ses")
    		path = fileLocation + "/" + projName + "/"+fileName;
    	else 
    		path = fileLocation + "/" + ODMEEditor.currentScenario + "/"+fileName; 
    	
        ObjectMapper xmlMapper = new XmlMapper();
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        // Add JAXB annotations support to handle XSD elements
        JaxbAnnotationModule module = new JaxbAnnotationModule();
        xmlMapper.registerModule(module);
        yamlMapper.registerModule(module);

        File xsdFile = new File(path);
        Object xsdObject;
        String yamlString=null;
		try {
			
			xsdObject = xmlMapper.readValue(xsdFile, Object.class); // read xsd
	        yamlString = yamlMapper.writeValueAsString(xsdObject); // convert to yaml
	        // System.out.println(yamlString);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		return yamlString;
    }
   
    /**
     * @author Roy
     * #ROY - making re-usable (to be used in MenuBar::exportHumanReadable)
     * */
    public static String readFile(String fileLocation, String projName, String fileName) {
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
        StringBuilder xmlcontent = new StringBuilder(); // lighter more efficient to use StringBuilder for stream of text
        while (in.hasNext()) {
            String line = in.nextLine();
            xmlcontent.append(line+"\n");
        }
        in.close();
        return xmlcontent.toString();
    }
    
    public static void showViewer(String fileLocation, String projName, String fileName, XMLViewer view) {
        view.textArea.setText(
    		readFile(fileLocation,projName,fileName) // #ROY - call the re-usable function
		);
    }
}
