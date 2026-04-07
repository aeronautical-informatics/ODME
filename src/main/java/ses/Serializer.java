package ses;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Serializer {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    // -------------------------------------------------------------------------
    // Serialisation: EntityStructure to JSON string
    // -------------------------------------------------------------------------

    public static String toJson(EntityStructure es) throws JsonProcessingException {
        ObjectNode root = mapper.createObjectNode();
        root.put("name", es.getName());
        root.set("root", nodeToJson(es.getRoot()));
        return mapper.writeValueAsString(root);
    }

    private static ObjectNode nodeToJson(Node node) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("name", node.getName());
        obj.put("type", node.getType().name());

        ArrayNode variables = obj.putArray("variables");
        for (Variable v : node.getVariables()) {
            variables.add(variableToJson(v));
        }

        ArrayNode constraints = obj.putArray("constraints");
        for (String c : node.getConstraints()) {
            constraints.add(c);
        }

        ArrayNode children = obj.putArray("children");
        for (Node child : node.getChildren()) {
            children.add(nodeToJson(child));
        }

        return obj;
    }

    private static ObjectNode variableToJson(Variable v) {
        ObjectNode obj = mapper.createObjectNode();
        obj.put("name", v.getName());
        obj.put("type", v.getType().name());
        if (v.getDefaultValue() != null) obj.put("defaultValue", v.getDefaultValue());
        if (v.getLowerBound() != null)   obj.put("lowerBound", v.getLowerBound());
        if (v.getUpperBound() != null)   obj.put("upperBound", v.getUpperBound());
        return obj;
    }

    // -------------------------------------------------------------------------
    // Deserialisation: JSON string to EntityStructure
    // -------------------------------------------------------------------------

    public static EntityStructure fromJson(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        String name = root.get("name").asText();
        Node rootNode = jsonToNode(root.get("root"));
        return new EntityStructure(name, rootNode);
    }

    private static Node jsonToNode(JsonNode json) {
        String name = json.get("name").asText();
        NodeType type = NodeType.valueOf(json.get("type").asText());
        Node node = new Node(name, type);

        for (JsonNode v : json.get("variables")) {
            node.addVariable(jsonToVariable(v));
        }

        for (JsonNode c : json.get("constraints")) {
            node.addConstraint(c.asText());
        }

        for (JsonNode child : json.get("children")) {
            node.addChild(jsonToNode(child));
        }

        return node;
    }

    private static Variable jsonToVariable(JsonNode json) {
        String name = json.get("name").asText();
        VariableType type = VariableType.valueOf(json.get("type").asText());
        Variable v = new Variable(name, type);
        if (json.has("defaultValue")) v.setDefaultValue(json.get("defaultValue").asText());
        if (json.has("lowerBound"))   v.setLowerBound(json.get("lowerBound").asDouble());
        if (json.has("upperBound"))   v.setUpperBound(json.get("upperBound").asDouble());
        return v;
    }
}
