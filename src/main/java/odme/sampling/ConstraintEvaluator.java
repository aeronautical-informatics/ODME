package odme.sampling;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.License;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates ODME constraint expressions against a given sample of parameter values.
 *
 * <p>Supported forms include plain boolean expressions and nested
 * {@code if(condition) then (result) else fallback} expressions.</p>
 */
public class ConstraintEvaluator {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("@([A-Za-z0-9_@\\-]+)");

    public void validate(String constraint, Set<String> variableNames) {
        validate(constraint, variableNames, constraint);
    }

    /** Checks a constraint whose variable references may have been resolved from model metadata. */
    public void validate(String constraint, Set<String> variableNames, String originalConstraint) {
        Map<String, Double> placeholderValues = new LinkedHashMap<>();
        for (String variableName : variableNames) {
            placeholderValues.put(variableName, 0.0);
        }
        Expression expression = prepareExpression(constraint, originalConstraint, placeholderValues);
        if (!expression.checkSyntax()) {
            throw invalidConstraint(originalConstraint, expression.getErrorMessage().strip());
        }
    }

    public boolean evaluate(String rawConstraint, Map<String, Double> sampleValues) {
        License.iConfirmNonCommercialUse("ODME Project - Clausthal University");

        Expression expression;
        try {
            expression = prepareExpression(rawConstraint, rawConstraint, sampleValues);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            return false;
        }
        return expression.calculate() == 1.0;
    }

    private IllegalArgumentException invalidConstraint(String originalConstraint, String reason) {
        return new IllegalArgumentException("Invalid constraint:\n" + originalConstraint + "\n\n" + reason);
    }

    private Expression prepareExpression(String constraint, String originalConstraint,
                                         Map<String, Double> sampleValues) {
        String formattedConstraint = formatExpression(constraint);
        if (formattedConstraint == null) {
            throw invalidConstraint(originalConstraint, "Expected if(condition) then (result) else fallback.");
        }

        Matcher matcher = TOKEN_PATTERN.matcher(formattedConstraint);
        StringBuffer rewritten = new StringBuffer();
        Map<String, Argument> arguments = new LinkedHashMap<>();
        Map<String, String> aliasesByResolvedKey = new LinkedHashMap<>();
        int index = 0;

        while (matcher.find()) {
            String tokenBody = matcher.group(1);
            String resolvedKey = resolveSampleKey(tokenBody, sampleValues);
            if (resolvedKey == null) {
                throw invalidConstraint(originalConstraint, "Unknown or ambiguous variable '@" + tokenBody + "'.");
            }

            String alias = aliasesByResolvedKey.get(resolvedKey);
            if (alias == null) {
                alias = "v" + index++;
                aliasesByResolvedKey.put(resolvedKey, alias);
            }
            arguments.putIfAbsent(alias, new Argument(alias, sampleValues.get(resolvedKey)));
            matcher.appendReplacement(rewritten, Matcher.quoteReplacement(alias));
        }
        matcher.appendTail(rewritten);

        return new Expression(rewritten.toString(), arguments.values().toArray(Argument[]::new));
    }

    private String resolveSampleKey(String tokenBody, Map<String, Double> sampleValues) {
        if (sampleValues.containsKey(tokenBody)) {
            return tokenBody;
        }

        String normalizedToken = normalize(tokenBody);

        List<String> normalizedMatches = sampleValues.keySet().stream()
                .filter(key -> normalize(key).equals(normalizedToken))
                .toList();
        if (normalizedMatches.size() == 1) {
            return normalizedMatches.get(0);
        }
        if (normalizedMatches.size() > 1) {
            return null;
        }

        List<String> suffixMatches = sampleValues.keySet().stream()
                .filter(key -> normalize(lastSegment(key)).equals(normalizedToken))
                .toList();
        if (suffixMatches.size() == 1) {
            return suffixMatches.get(0);
        }

        return null;
    }

    private String formatExpression(String rawExpression) {
        if (rawExpression == null || rawExpression.trim().isEmpty()) {
            return "1";
        }
        return convertConditionals(rawExpression.trim());
    }

    private String convertConditionals(String expression) {
        String trimmed = unwrapOuterParentheses(expression.trim());
        if (!startsWithIfKeyword(trimmed)) {
            return normalizeLiteralBooleans(trimmed);
        }

        int conditionStart = trimmed.indexOf('(');
        if (conditionStart < 0) {
            return null;
        }

        int conditionEnd = findMatchingParenthesis(trimmed, conditionStart);
        if (conditionEnd < 0) {
            return null;
        }

        int thenIndex = indexOfKeywordOutsideParentheses(trimmed, "then", conditionEnd + 1);
        if (thenIndex < 0) {
            return null;
        }

        String condition = trimmed.substring(conditionStart + 1, conditionEnd).trim();
        String afterThen = trimmed.substring(thenIndex + 4).trim();
        int elseIndex = indexOfKeywordOutsideParentheses(afterThen, "else", 0);
        if (elseIndex < 0) {
            return null;
        }

        String thenPart = afterThen.substring(0, elseIndex).trim();
        String elsePart = afterThen.substring(elseIndex + 4).trim();

        String convertedCondition = convertConditionals(condition);
        String convertedThen = convertConditionals(unwrapOuterParentheses(thenPart));
        String convertedElse = convertConditionals(unwrapOuterParentheses(elsePart));
        if (convertedCondition == null || convertedThen == null || convertedElse == null) {
            return null;
        }

        return "if(" + convertedCondition + ", " + convertedThen + ", " + convertedElse + ")";
    }

    private boolean startsWithIfKeyword(String expression) {
        if (!expression.regionMatches(true, 0, "if", 0, 2)) {
            return false;
        }
        return expression.length() == 2 || Character.isWhitespace(expression.charAt(2)) || expression.charAt(2) == '(';
    }

    private int indexOfKeywordOutsideParentheses(String expression, String keyword, int startIndex) {
        int depth = 0;
        String lower = expression.toLowerCase(Locale.ROOT);
        String target = keyword.toLowerCase(Locale.ROOT);

        for (int i = Math.max(0, startIndex); i <= lower.length() - target.length(); i++) {
            char current = lower.charAt(i);
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')') {
                depth = Math.max(0, depth - 1);
                continue;
            }
            if (depth == 0 && lower.startsWith(target, i)) {
                boolean leftBoundary = i == 0 || Character.isWhitespace(lower.charAt(i - 1)) || lower.charAt(i - 1) == ')';
                int rightIndex = i + target.length();
                boolean rightBoundary = rightIndex >= lower.length()
                        || Character.isWhitespace(lower.charAt(rightIndex))
                        || lower.charAt(rightIndex) == '(';
                if (leftBoundary && rightBoundary) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findMatchingParenthesis(String expression, int openingIndex) {
        int depth = 0;
        for (int i = openingIndex; i < expression.length(); i++) {
            char current = expression.charAt(i);
            if (current == '(') {
                depth++;
            } else if (current == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String unwrapOuterParentheses(String expression) {
        String trimmed = expression.trim();
        while (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            int matching = findMatchingParenthesis(trimmed, 0);
            if (matching != trimmed.length() - 1) {
                break;
            }
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private String normalizeLiteralBooleans(String expression) {
        return expression
                .replaceAll("(?i)\\btrue\\b", "1")
                .replaceAll("(?i)\\bfalse\\b", "0");
    }

    private String normalize(String value) {
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String lastSegment(String key) {
        String[] segments = key.split("[_@]");
        return segments.length == 0 ? key : segments[segments.length - 1];
    }
}
