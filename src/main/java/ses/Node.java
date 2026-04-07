package ses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Node {
    private String name;
    private final NodeType type;
    private final List<Node> children = new ArrayList<>();
    private final List<Variable> variables = new ArrayList<>();
    private final List<String> constraints = new ArrayList<>();

    private final List<Node> childrenView = Collections.unmodifiableList(children);
    private final List<Variable> variablesView = Collections.unmodifiableList(variables);
    private final List<String> constraintsView = Collections.unmodifiableList(constraints);

    public Node(String name, NodeType type) {
        this.name = name;
        this.type = type;
    }

    public String getName()                      { return name; }
    public void setName(String name)             { this.name = name; }
    public NodeType getType()                    { return type; }

    public List<Node> getChildren()              { return childrenView; }
    public List<Variable> getVariables()         { return variablesView; }
    public List<String> getConstraints()         { return constraintsView; }

    public void addChild(Node child)             { children.add(child); }
    public void removeChild(Node child)          { children.remove(child); }
    public void addVariable(Variable variable)   { variables.add(variable); }
    public void addConstraint(String constraint) { constraints.add(constraint); }

    /** Fluent child addition for building trees inline. */
    public Node withChild(Node child)            { children.add(child); return this; }
}
