package odme.odmeeditor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import odme.core.FlagVariables;
import odme.core.UndoableTreeModel;
import odme.core.XmlJTree;
import org.w3c.dom.Document;

import javax.swing.tree.TreePath;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** All disk work and decoding for an open operation, independent of the live editor. */
public record ProjectLoadData(Path directory, String name, mxGraphModel graphModel,
                              UndoableTreeModel treeModel,
                              Multimap<TreePath, String> variables,
                              Multimap<TreePath, String> constraints,
                              Multimap<TreePath, String> behaviours,
                              FlagVariables flags) {
    public static ProjectLoadData load(Path directory, String name) throws Exception {
        Path graphFile = directory.resolve(name + "Graph.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(graphFile.toFile());
        mxGraphModel graphModel = new mxGraphModel();
        new mxCodec(document).decode(document.getDocumentElement(), graphModel);
        if (graphModel.getChildCount(graphModel.getRoot()) == 0) {
            throw new IOException("Graph model has no layers: " + graphFile);
        }
        // Old projects contain distant canvas markers; remove them before attaching the model.
        List<Object> markers = new ArrayList<>();
        for (Object cell : graphModel.getCells().values()) {
            if (cell instanceof mxCell vertex && vertex.isVertex()
                    && ("hideV".equals(vertex.getId()) || "hideH".equals(vertex.getId()))) {
                markers.add(cell);
            }
        }
        markers.forEach(graphModel::remove);
        UndoableTreeModel treeModel = XmlJTree.readModel(directory.resolve(name + ".xml").toString());
        Multimap<TreePath, String> constraints = readMap(directory.resolve(name + ".ssdcon"));
        if (!Files.exists(directory.resolve(name + ".ssdcon"))) {
            constraints.putAll(readMap(directory.resolve(name + ".ssdintercon")));
            constraints.putAll(readMap(directory.resolve(name + ".ssdintracons")));
        }
        Path flagsFile = directory.resolve(name + ".ssdflag");
        FlagVariables flags = Files.exists(flagsFile) ? (FlagVariables) readObject(flagsFile) : null;
        return new ProjectLoadData(directory, name, graphModel, treeModel,
                readMap(directory.resolve(name + ".ssdvar")), constraints,
                readMap(directory.resolve(name + ".ssdbeh")), flags);
    }

    @SuppressWarnings("unchecked")
    private static Multimap<TreePath, String> readMap(Path file) throws IOException, ClassNotFoundException {
        return Files.exists(file) ? (Multimap<TreePath, String>) readObject(file) : ArrayListMultimap.create();
    }

    private static Object readObject(Path file) throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(file))) {
            return input.readObject();
        }
    }
}
