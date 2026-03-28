package odme.sampling.model;

import java.util.List;

/**
 * Represents a single parameter from the YAML scenario file that needs to be sampled.
 * Can be numerical (int/double/float/distribution) or categorical.
 */
public class Parameter {
    private String name;
    private String type; // "int", "double", "float", "categorical", "distribution"
    private String defaultValue;

    // For distribution parameters
    private String distributionName;
    private String distributionDetails;

    // For numerical parameters
    private Double min;
    private Double max;

    // For categorical parameters
    private List<String> options;

    // For constraints associated with a parameter
    private String constraint;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDefaultValue() { return defaultValue; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    public String getDistributionName() { return distributionName; }
    public void setDistributionName(String distributionName) { this.distributionName = distributionName; }

    public String getDistributionDetails() { return distributionDetails; }
    public void setDistributionDetails(String distributionDetails) { this.distributionDetails = distributionDetails; }

    public Double getMin() { return min != null ? min : 0.0; }
    public void setMin(Double min) { this.min = min; }

    public Double getMax() { return max != null ? max : 1.0; }
    public void setMax(Double max) { this.max = max; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }

    public String getConstraint() { return constraint; }
    public void setConstraint(String constraint) { this.constraint = constraint; }

    @Override
    public String toString() {
        return "Parameter{name='" + name + "', type='" + type + "', min=" + min + ", max=" + max + "}";
    }
}
