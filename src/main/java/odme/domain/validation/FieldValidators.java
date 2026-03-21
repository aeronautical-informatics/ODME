package odme.domain.validation;

/**
 * Pure business-logic validators for SES variable fields.
 *
 * <p>Extracted from {@code odme.odmeeditor.Variable} so that validation can be
 * exercised in unit tests without any Swing dependency. Each method returns a
 * boolean indicating whether the combination of field values is valid.</p>
 */
public final class FieldValidators {

    /** Regex for valid variable/value names: starts with letter or underscore,
     *  followed by alphanumerics or underscores. */
    public static final String VARIABLE_FIELD_REGEX = "[a-zA-Z_][a-zA-Z0-9_]*";

    /** Regex for integer values (optionally negative). */
    private static final String INT_REGEX = "^-?[0-9]+";

    /** Regex for positive integer values. */
    private static final String POSITIVE_INT_REGEX = "^[0-9]+";

    /** Regex for floating-point values (e.g. "3.14"). */
    private static final String FLOAT_REGEX = "^\\d*\\.\\d+";

    /** Regex for comment fields: starts with letter or underscore, then alphanumerics and spaces. */
    private static final String COMMENT_REGEX = "^[a-zA-Z_][a-zA-Z0-9 ]*";

    private FieldValidators() {
        // utility class
    }

    /**
     * Validates the variable name field in context of the selected type and
     * related field values.
     *
     * @param selectedType the variable type (e.g. "string", "boolean", "int", "float", "double")
     * @param variableName the variable name to validate
     * @param value        the default value field
     * @param lowerBound   the lower bound field (may be null/empty for string/boolean)
     * @param upperBound   the upper bound field (may be null/empty for string/boolean)
     * @return {@code true} if the combination is valid
     */
    public static boolean validateVariableField(String selectedType,
                                                 String variableName,
                                                 String value,
                                                 String lowerBound,
                                                 String upperBound) {
        String name = trimSafe(variableName);
        String val = trimSafe(value);
        String lb = trimSafe(lowerBound);
        String ub = trimSafe(upperBound);

        if (!name.matches(VARIABLE_FIELD_REGEX)) {
            return false;
        }

        switch (selectedType) {
            case "string":
                return val.matches(VARIABLE_FIELD_REGEX);
            case "boolean":
                return val.equals("true") || val.equals("false");
            case "double":
                return val.matches(FLOAT_REGEX)
                        && lb.matches(FLOAT_REGEX)
                        && ub.matches(FLOAT_REGEX);
            default: // int, float, etc.
                return val.matches(POSITIVE_INT_REGEX)
                        && lb.matches(POSITIVE_INT_REGEX)
                        && ub.matches(POSITIVE_INT_REGEX);
        }
    }

    /**
     * Validates a comment field value.
     *
     * @param comment the comment text
     * @return {@code true} if the comment matches the expected pattern
     */
    public static boolean validateComment(String comment) {
        try {
            return trimSafe(comment).matches(COMMENT_REGEX);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validates the value field in the context of the selected type and bounds.
     *
     * @param selectedType the variable type
     * @param value        the default value
     * @param lowerBound   the lower bound (for numeric types)
     * @param upperBound   the upper bound (for numeric types)
     * @return {@code true} if the value is valid for the given type and within bounds
     */
    public static boolean validateValueField(String selectedType,
                                              String value,
                                              String lowerBound,
                                              String upperBound) {
        String val = trimSafe(value);
        String lb = trimSafe(lowerBound);
        String ub = trimSafe(upperBound);

        switch (selectedType) {
            case "boolean":
                return val.equals("true") || val.equals("false");

            case "int":
                try {
                    if (!val.matches(INT_REGEX)) return false;
                    int v = Integer.parseInt(val);
                    int lo = Integer.parseInt(lb);
                    int hi = Integer.parseInt(ub);
                    return v >= lo && v <= hi;
                } catch (Exception e) {
                    return false;
                }

            case "float":
                try {
                    if (!val.matches(FLOAT_REGEX)) return false;
                    float v = Float.parseFloat(val);
                    float lo = Float.parseFloat(lb);
                    float hi = Float.parseFloat(ub);
                    return v >= lo && v <= hi;
                } catch (Exception e) {
                    return false;
                }

            case "double":
                try {
                    if (!val.matches(FLOAT_REGEX)) return false;
                    double v = Double.parseDouble(val);
                    double lo = Double.parseDouble(lb);
                    double hi = Double.parseDouble(ub);
                    return v >= lo && v <= hi;
                } catch (Exception e) {
                    return false;
                }

            case "string":
                return val.matches(VARIABLE_FIELD_REGEX);

            default:
                return false;
        }
    }

    /**
     * Validates the lower bound field in context of the variable type
     * and related field values.
     *
     * @param selectedType the variable type
     * @param variableName the variable name
     * @param value        the default value
     * @param lowerBound   the lower bound to validate
     * @param upperBound   the upper bound
     * @return {@code true} if the lower bound is valid
     */
    public static boolean validateLowerBoundField(String selectedType,
                                                   String variableName,
                                                   String value,
                                                   String lowerBound,
                                                   String upperBound) {
        String name = trimSafe(variableName);
        String val = trimSafe(value);
        String lb = trimSafe(lowerBound);
        String ub = trimSafe(upperBound);

        switch (selectedType) {
            case "int":
                return name.matches(VARIABLE_FIELD_REGEX)
                        && val.matches(POSITIVE_INT_REGEX)
                        && lb.matches(POSITIVE_INT_REGEX)
                        && ub.matches(POSITIVE_INT_REGEX);
            case "float":
            case "double":
                return name.matches(VARIABLE_FIELD_REGEX)
                        && val.matches(FLOAT_REGEX)
                        && lb.matches(FLOAT_REGEX)
                        && ub.matches(FLOAT_REGEX);
            default:
                return true; // string and boolean do not use bounds
        }
    }

    /**
     * Validates the upper bound field in context of the variable type
     * and related field values.
     *
     * @param selectedType the variable type
     * @param variableName the variable name
     * @param value        the default value
     * @param lowerBound   the lower bound
     * @param upperBound   the upper bound to validate
     * @return {@code true} if the upper bound is valid
     */
    public static boolean validateUpperBoundField(String selectedType,
                                                   String variableName,
                                                   String value,
                                                   String lowerBound,
                                                   String upperBound) {
        String name = trimSafe(variableName);
        String val = trimSafe(value);
        String lb = trimSafe(lowerBound);
        String ub = trimSafe(upperBound);

        switch (selectedType) {
            case "float":
                return name.matches(VARIABLE_FIELD_REGEX)
                        && val.matches(POSITIVE_INT_REGEX)
                        && lb.matches(POSITIVE_INT_REGEX)
                        && ub.matches(POSITIVE_INT_REGEX);
            case "int":
                return name.matches(VARIABLE_FIELD_REGEX)
                        && val.matches(POSITIVE_INT_REGEX)
                        && lb.matches(POSITIVE_INT_REGEX)
                        && ub.matches(POSITIVE_INT_REGEX);
            case "double":
                return name.matches(VARIABLE_FIELD_REGEX)
                        && val.matches(FLOAT_REGEX)
                        && lb.matches(FLOAT_REGEX)
                        && ub.matches(FLOAT_REGEX);
            default:
                return true; // string and boolean do not use bounds
        }
    }

    private static String trimSafe(String s) {
        return s == null ? "" : s.trim();
    }
}
