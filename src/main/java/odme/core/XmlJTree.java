package odme.core;

import org.w3c.dom.Document;
import javax.swing.JOptionPane;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/**
 * <h1>XmlJTree</h1>
 * <p>
 * This class is used to transform XML file into JTree model. During opening
 * existing project an XML document is used and based on that document a JTree
 * model is set to the current tree model using this class.
 * </p>
 *
 * @author ---
 * @version ---
 */
public class XmlJTree extends JTree {

	private static final long serialVersionUID = 1L;
	public UndoableTreeModel dtModel;

    public XmlJTree(String filePath) {
    	dtModel = null;

        try {
            dtModel = readModel(filePath);
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        catch (SAXException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "An error occurred: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (dtModel != null) {
            this.setModel(dtModel);
        }
    }

    /** Builds a detached model without constructing or updating any Swing component. */
    public static UndoableTreeModel readModel(String filePath)
            throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(filePath);
        Node documentRoot = unwrapEditorRoot(document.getDocumentElement());
        if (documentRoot == null) {
            throw new IOException("Tree model has no root: " + filePath);
        }
        return new UndoableTreeModel(builtTreeNode(documentRoot));
    }

    private static Node unwrapEditorRoot(Node documentRoot) {
        if (documentRoot == null) {
            return null;
        }
        if (!"start".equals(documentRoot.getNodeName())) {
            return documentRoot;
        }

        NodeList nodeList = documentRoot.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node child = nodeList.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return child;
            }
        }
        return documentRoot;
    }

    private static DefaultMutableTreeNode builtTreeNode(Node root) {
        DefaultMutableTreeNode dmtNode;
        dmtNode = new DefaultMutableTreeNode(root.getNodeName());
        NodeList nodeList = root.getChildNodes();

        for (int count = 0; count < nodeList.getLength(); count++) {
            Node tempNode = nodeList.item(count);
            if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                if (tempNode.hasChildNodes()) {
                    dmtNode.add(builtTreeNode(tempNode));
                }
            }
        }
        return dmtNode;
    }
}
