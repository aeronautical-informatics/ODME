package odme.sampling.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents the entire scenario model parsed from a YAML file.
 * Holds all parameters defining the sampling space and any constraints.
 */
public class Scenario {
    private List<Parameter> parameters = new ArrayList<>();
    private List<String> constraint = new ArrayList<>();
    private Map<String, String> constraintSourceTexts = Map.of();

    public List<Parameter> getParameters() { return parameters; }
    public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }

    public List<String> getConstraint() { return constraint; }
    public void setConstraint(List<String> constraint) {
        setConstraint(constraint, Map.of());
    }

    public void setConstraint(List<String> constraint, Map<String, String> sourceTexts) {
        this.constraint = constraint;
        this.constraintSourceTexts = Map.copyOf(sourceTexts);
    }

    public Map<String, String> getConstraintSourceTexts() {
        return constraintSourceTexts;
    }

    public String getConstraintSourceText(String expression) {
        return constraintSourceTexts.getOrDefault(expression, expression);
    }
}
