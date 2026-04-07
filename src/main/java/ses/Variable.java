package ses;

public class Variable {
    private String name;
    private final VariableType type;
    private String defaultValue;  // null if not set
    private Double lowerBound;    // null if not set; only meaningful for INT and FLOAT
    private Double upperBound;    // null if not set; only meaningful for INT and FLOAT

    public Variable(String name, VariableType type) {
        this.name = name;
        this.type = type;
    }

    public String getName()             { return name; }
    public void setName(String name)    { this.name = name; }
    public VariableType getType()       { return type; }

    public String getDefaultValue()              { return defaultValue; }
    public void setDefaultValue(String value)    { this.defaultValue = value; }

    public Double getLowerBound()                { return lowerBound; }
    public void setLowerBound(Double lowerBound) { this.lowerBound = lowerBound; }

    public Double getUpperBound()                { return upperBound; }
    public void setUpperBound(Double upperBound) { this.upperBound = upperBound; }
}
