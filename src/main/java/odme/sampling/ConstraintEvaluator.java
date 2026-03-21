package odme.sampling;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates ODME constraint expressions against a given sample of parameter values.
 * <p>
 * Constraint syntax: {@code if(@param > value) then (@other < limit) else true}
 * <p>
 * Uses the mXparser library to safely evaluate the resulting mathematical expression.
 */
public class ConstraintEvaluator {

    /**
     * Returns {@code true} if the given sample satisfies the constraint expression.
     *
     * @param rawConstraint  e.g. "if(@rain_intensity > 5) then (@luminosity < 1000) else true"
     * @param sampleValues   map of parameter name → sampled value
     */
    public boolean evaluate(String rawConstraint, Map<String, Double> sampleValues) {
        // Confirm non-commercial use as required by mXparser 5.x
        License.iConfirmNonCommercialUse("ODME Project - Clausthal University");

        List<Argument> arguments = new ArrayList<>();
        for (Map.Entry<String, Double> entry : sampleValues.entrySet()) {
            // Strip special chars that mXparser cannot handle in variable names
            String cleanName = entry.getKey().replaceAll("[@_()\\-]", "");
            arguments.add(new Argument(cleanName, entry.getValue()));
        }

        String formattedConstraint = formatExpression(rawConstraint);
        if (formattedConstraint == null) {
            System.err.println("Could not parse constraint structure: " + rawConstraint);
            return false;
        }

        Expression expression = new Expression(formattedConstraint, arguments.toArray(new Argument[0]));
        double result = expression.calculate();

        if (Double.isNaN(result)) {
            System.err.println("Syntax error in constraint: " + expression.getErrorMessage());
            System.err.println("Formatted expression: " + formattedConstraint);
            return false;
        }

        return result == 1.0;
    }

    /**
     * Converts ODME syntax "if(condition) then (result) else fallback"
     * into mXparser format "if(condition, result, fallback)".
     */
    private String formatExpression(String rawExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return "1"; // empty constraint is always satisfied
        }

        Pattern pattern = Pattern.compile(
                "if\\s*\\((.*)\\)\\s*then\\s*\\((.*)\\)\\s*else\\s*(.*)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(rawExpression);

        if (matcher.find()) {
            return String.format("if(%s, %s, %s)",
                    cleanPart(matcher.group(1)),
                    cleanPart(matcher.group(2)),
                    cleanPart(matcher.group(3)));
        }
        return null;
    }

    private String cleanPart(String part) {
        return part.trim()
                .replaceAll("[@_()\\-]", "")
                .replaceAll("true", "1")
                .replaceAll("false", "0");
    }
}
