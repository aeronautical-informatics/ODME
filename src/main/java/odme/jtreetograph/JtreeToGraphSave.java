package odme.jtreetograph;

import static odme.jtreetograph.JtreeToGraphVariables.*;
import static odeme.behaviour.BehaviourToTree.selectedScenario;

import java.io.File;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import behaviourtreetograph.JTreeToGraphBehaviour;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxXmlUtils;

import odme.odmeeditor.ODMEEditor;

public class JtreeToGraphSave {

	public static boolean saveToXMLFile(Document doc, String filePath) 
			throws TransformerException {
        if (doc != null) {
            try {
                javax.xml.transform.TransformerFactory tFactory =
                        javax.xml.transform.TransformerFactory.newInstance();
                javax.xml.transform.Transformer transformer = tFactory.newTransformer();
                javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(doc);
                javax.xml.transform.stream.StreamResult result =
                        new javax.xml.transform.stream.StreamResult(new File(filePath));
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "0");

                transformer.transform(source, result);
                return true;
            } 
            catch (TransformerFactoryConfigurationError ex) {
                return false;
            }
        } 
        else {
            return false;
        }
    }

    public static void saveBehaviourGraph() {
        try {
            // saving into xml
            mxCodec codec = new mxCodec();
            String xml = mxXmlUtils.getXml(codec.encode(JTreeToGraphBehaviour.benhaviourGraph.getModel()));

            File ssdBehaviourGraph = new File (String.format("%s/%s/%s/%sbehaviourGraph.xml",
                    ODMEEditor.fileLocation, ODMEEditor.projName,  selectedScenario, projectFileNameGraph));
            java.io.FileWriter fw = new java.io.FileWriter(ssdBehaviourGraph);
            fw.write(xml);
            fw.close();
        }
        catch (Exception e) {
            System.out.println("Error:" + e);
        }
    }
	public static void saveGraph() {
        try {
            // saving into xml
            mxCodec codec = new mxCodec();
            String xml = mxXmlUtils.getXml(codec.encode(graph.getModel()));
            java.io.FileWriter fw = new java.io.FileWriter(ssdFileGraph);
            fw.write(xml);
            fw.close();
        } 
        catch (Exception e) {
            System.out.println("Error:" + e);
        }

    }
	
	public static void saveModuleFromCurrentModel(Object obj) {
        mxCell cell = (mxCell) obj;
        Object[] outgoing = graph.getOutgoingEdges(cell);
        
        if (outgoing.length > 0) {
            Object targetCell = graph.getModel().getTerminal(outgoing[outgoing.length - 1], false);
            mxCell targetCell2 = (mxCell) targetCell;

            nodeNamesForGraphSync[treeSyncNodeCount] = (String) targetCell2.getValue();
            treeSyncNodeCount++;

            saveModuleFromCurrentModelSecondStep(targetCell);
        }
    }

    public static void saveModuleFromCurrentModelSecondStep(Object obj) {
        mxCell cell = (mxCell) obj;
        Object[] outgoing = graph.getOutgoingEdges(cell);

        if (outgoing.length > 0) {
            for (int i = 0; i < outgoing.length; i++) {
                Object targetCell = graph.getModel().getTerminal(outgoing[i], false);
                mxCell targetCell2 = (mxCell) targetCell;

                Object sourceCell = graph.getModel().getTerminal(outgoing[i], true);
                mxCell sourceCell2 = (mxCell) sourceCell;

                nodeNamesForGraphSync[treeSyncNodeCount] =
                        targetCell2.getValue() + "-" + sourceCell2.getValue() + "-" + "hasparent";
                treeSyncNodeCount++;

                saveModuleFromCurrentModelSecondStep(targetCell);
            }
        }
    }
    
    public static void saveModuleFromCurrentModelAsXML(Object obj, String fileName) {
        mxCell cell = (mxCell) obj;
        Document calendarDOMDoc = null;
        try {
            DOMImplementation domImpl =
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation();

            calendarDOMDoc = domImpl.createDocument(null, "start", null);

        } 
        catch (ParserConfigurationException e1) {
            e1.printStackTrace(System.err);
        } 
        catch (DOMException e2) {
            e2.printStackTrace(System.err);
        }

        calendarDOMDoc.getDocumentElement().appendChild(JtreeToGraphGeneral.childNodes(calendarDOMDoc, cell));

        try {
        	saveToXMLFile(calendarDOMDoc, fileName + ".xml");

        } 
        catch (TransformerException ex) {
            Logger.getLogger(ODMEEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static Element saveAllTreeNodes(Document thisDoc, TreeNode thisTreeNode) {
        Element thisElement = null;
        String nodeName = ((DefaultMutableTreeNode) thisTreeNode).getUserObject().toString();
        thisElement = thisDoc.createElement(nodeName);

        if (thisTreeNode.getChildCount() >= 0) {
            for (Enumeration<?> e = thisTreeNode.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                thisElement.appendChild(saveAllTreeNodes(thisDoc, n));
            }
        }
        return thisElement;
    }
}
