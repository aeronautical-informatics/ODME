package odme.core;

import org.w3c.dom.Document;
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
    private Node root;

    public XmlJTree(String filePath) {
    	dtModel = null;
    	root = null;

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc = null;

        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(filePath);
            root = doc.getDocumentElement();

        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        catch (SAXException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (root != null) {
            dtModel = new UndoableTreeModel(builtTreeNode(root));
            this.setModel(dtModel);
        }
    }

    private DefaultMutableTreeNode builtTreeNode(Node root) {
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
