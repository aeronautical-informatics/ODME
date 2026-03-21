package odme.sampling.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the entire scenario model parsed from a YAML file.
 * Holds all parameters defining the sampling space and any constraints.
 */
public class Scenario {
    private List<Parameter> parameters = new ArrayList<>();
    private List<String> constraint = new ArrayList<>();

    public List<Parameter> getParameters() { return parameters; }
    public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }

    public List<String> getConstraint() { return constraint; }
    public void setConstraint(List<String> constraint) { this.constraint = constraint; }
}
