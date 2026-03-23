package odme.sampling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConstraintEvaluator}.
 * Exercises the ODME constraint syntax: "if(@param > val) then (@other < limit) else true"
 */
class ConstraintEvaluatorTest {

    private ConstraintEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new ConstraintEvaluator();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Basic satisfied / violated cases
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void evaluate_satisfiedConstraint_returnsTrue() {
        // if rain_intensity > 5, then luminosity must be < 1000
        // rain=10 (condition true), luminosity=500 (consequence satisfied) → true
        Map<String, Double> sample = Map.of("rain_intensity", 10.0, "luminosity", 500.0);
        boolean result = evaluator.evaluate(
                "if(@rain_intensity > 5) then (@luminosity < 1000) else true",
                sample);
        assertTrue(result);
    }

    @Test
    void evaluate_violatedConstraint_returnsFalse() {
        // rain=10 (condition true), luminosity=1500 (consequence VIOLATED) → false
        Map<String, Double> sample = Map.of("rain_intensity", 10.0, "luminosity", 1500.0);
        boolean result = evaluator.evaluate(
                "if(@rain_intensity > 5) then (@luminosity < 1000) else true",
                sample);
        assertFalse(result);
    }

    @Test
    void evaluate_conditionFalse_elseTrue_returnsTrue() {
        // rain=2 (condition false) → else branch = true → satisfied
        Map<String, Double> sample = Map.of("rain_intensity", 2.0, "luminosity", 2000.0);
        boolean result = evaluator.evaluate(
                "if(@rain_intensity > 5) then (@luminosity < 1000) else true",
                sample);
        assertTrue(result);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Edge values (boundary)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void evaluate_conditionExactlyAtBoundary_notExceeding() {
        // rain == 5  (condition is > 5, so false) → else true
        Map<String, Double> sample = Map.of("rain_intensity", 5.0, "luminosity", 2000.0);
        boolean result = evaluator.evaluate(
                "if(@rain_intensity > 5) then (@luminosity < 1000) else true",
                sample);
        assertTrue(result, "Condition rain == 5 is not > 5, so else=true should hold");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Null / empty / malformed inputs
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void evaluate_nullConstraint_returnsTrue() {
        // null / empty constraint → empty string → always satisfied (returns 1)
        boolean result = evaluator.evaluate(null, Map.of("x", 1.0));
        // formatExpression returns "1" for null/empty → result == 1.0 → true
        assertTrue(result, "null constraint should be treated as always satisfied");
    }

    @Test
    void evaluate_emptyConstraint_returnsTrue() {
        boolean result = evaluator.evaluate("  ", Map.of("x", 1.0));
        assertTrue(result, "blank constraint should be treated as always satisfied");
    }

    @Test
    void evaluate_plainBooleanExpression_returnsTrue() {
        boolean result = evaluator.evaluate("x > 5 and y < 10", Map.of("x", 6.0, "y", 8.0));
        assertTrue(result, "Plain boolean expressions should be evaluated directly");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Variables with special characters in names
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void evaluate_variableWithUnderscoreInName_handledCorrectly() {
        // Names like "EgoAC_speed" have underscores stripped → "EgoACspeed"
        // The sample map must use the same original name for lookup
        Map<String, Double> sample = Map.of("EgoAC_speed", 60.0, "EgoAC_altitude", 100.0);
        boolean result = evaluator.evaluate(
                "if(@EgoAC_speed > 50) then (@EgoAC_altitude < 200) else true",
                sample);
        assertTrue(result);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Multiple parameters
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void evaluate_extraParametersInMap_noError() {
        // Having more keys in the map than referenced by the constraint should be fine
        Map<String, Double> sample = Map.of(
                "rain_intensity", 10.0,
                "luminosity", 500.0,
                "temperature", 25.0,
                "wind_speed", 30.0);
        boolean result = evaluator.evaluate(
                "if(@rain_intensity > 5) then (@luminosity < 1000) else true",
                sample);
        assertTrue(result);
    }

    @Test
    void evaluate_emptyValueMap_noException() {
        // No matching variable → expression arguments are empty, NaN expected → false
        boolean result = evaluator.evaluate(
                "if(@rain_intensity > 5) then (@luminosity < 1000) else true",
                Map.of());
        assertFalse(result, "Unresolved variables should cause NaN → false");
    }
    @Test
    void evaluate_compactReference_resolvesCanonicalSampleKey() {
        Map<String, Double> sample = Map.of("EgoAC_Speed", 60.0, "EgoAC_Altitude", 100.0);
        boolean result = evaluator.evaluate(
                "if(@EgoACSpeed > 50) then (@EgoACAltitude < 200) else true",
                sample);
        assertTrue(result);
    }

    @Test
    void evaluate_uniqueSuffixReference_resolvesBareVariableName() {
        Map<String, Double> sample = Map.of("EgoAC_Speed", 60.0, "EgoAC_Altitude", 100.0);
        boolean result = evaluator.evaluate(
                "if(@Speed > 50) then (@Altitude < 200) else true",
                sample);
        assertTrue(result);
    }
}
