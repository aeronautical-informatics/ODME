package ses;

public class EntityStructure {
    private String name;
    private final Node root;

    public EntityStructure(String name, Node root) {
        this.name = name;
        this.root = root;
    }

    public String getName()          { return name; }
    public void setName(String name) { this.name = name; }
    public Node getRoot()            { return root; }
}
