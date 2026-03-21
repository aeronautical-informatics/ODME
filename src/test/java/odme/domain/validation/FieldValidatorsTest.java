package odme.domain.validation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive tests for {@link FieldValidators}.
 * Targets 90%+ line/branch coverage by exercising every public method
 * with valid inputs, invalid inputs, boundary conditions, and regex edge cases.
 */
class FieldValidatorsTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // validateVariableField
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ValidateVariableField {

        // ── string type ──────────────────────────────────────────────────────

        @Test
        void string_validNameAndValue_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "myVar", "hello", "", "")).isTrue();
        }

        @Test
        void string_valueWithUnderscore_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "_name", "_val_123", "", "")).isTrue();
        }

        @Test
        void string_valueWithDigitsOnly_returnsFalse() {
            // VARIABLE_FIELD_REGEX requires starting with letter or underscore
            assertThat(FieldValidators.validateVariableField(
                    "string", "myVar", "123", "", "")).isFalse();
        }

        @Test
        void string_emptyValue_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "myVar", "", "", "")).isFalse();
        }

        @Test
        void string_valueWithSpaces_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "myVar", "has space", "", "")).isFalse();
        }

        @Test
        void string_valueWithSpecialChars_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "myVar", "bad@val", "", "")).isFalse();
        }

        // ── boolean type ─────────────────────────────────────────────────────

        @Test
        void boolean_trueValue_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "boolean", "flag", "true", "", "")).isTrue();
        }

        @Test
        void boolean_falseValue_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "boolean", "flag", "false", "", "")).isTrue();
        }

        @Test
        void boolean_capitalizedTrue_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "boolean", "flag", "True", "", "")).isFalse();
        }

        @Test
        void boolean_randomString_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "boolean", "flag", "yes", "", "")).isFalse();
        }

        // ── double type ──────────────────────────────────────────────────────

        @Test
        void double_allFloats_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "double", "x", "1.5", "0.0", "10.0")).isTrue();
        }

        @Test
        void double_integerValue_returnsFalse() {
            // FLOAT_REGEX requires a decimal point
            assertThat(FieldValidators.validateVariableField(
                    "double", "x", "5", "0.0", "10.0")).isFalse();
        }

        @Test
        void double_invalidLowerBound_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "double", "x", "1.5", "abc", "10.0")).isFalse();
        }

        @Test
        void double_invalidUpperBound_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "double", "x", "1.5", "0.0", "abc")).isFalse();
        }

        @Test
        void double_leadingDotValue_returnsTrue() {
            // FLOAT_REGEX is "^\d*\.\d+" which matches ".5"
            assertThat(FieldValidators.validateVariableField(
                    "double", "x", ".5", ".0", ".9")).isTrue();
        }

        // ── int / default type ───────────────────────────────────────────────

        @Test
        void int_allPositiveIntegers_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "int", "count", "5", "0", "100")).isTrue();
        }

        @Test
        void int_negativeValue_returnsFalse() {
            // POSITIVE_INT_REGEX does not allow negative
            assertThat(FieldValidators.validateVariableField(
                    "int", "count", "-1", "0", "100")).isFalse();
        }

        @Test
        void int_floatValue_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "int", "count", "1.5", "0", "100")).isFalse();
        }

        @Test
        void float_allPositiveIntegers_returnsTrue() {
            // "float" falls through to default, which uses POSITIVE_INT_REGEX
            assertThat(FieldValidators.validateVariableField(
                    "float", "x", "5", "0", "10")).isTrue();
        }

        @Test
        void unknownType_validPositiveInts_returnsTrue() {
            assertThat(FieldValidators.validateVariableField(
                    "customType", "x", "5", "0", "10")).isTrue();
        }

        // ── variable name validation ─────────────────────────────────────────

        @Test
        void invalidName_startingWithDigit_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "1bad", "val", "", "")).isFalse();
        }

        @Test
        void invalidName_withSpecialChars_returnsFalse() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "bad-name", "val", "", "")).isFalse();
        }

        @ParameterizedTest
        @NullAndEmptySource
        void nullOrEmptyName_returnsFalse(String name) {
            assertThat(FieldValidators.validateVariableField(
                    "string", name, "val", "", "")).isFalse();
        }

        @Test
        void nameWithLeadingTrailingSpaces_isTrimmed() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "  myVar  ", "val", "", "")).isTrue();
        }

        @Test
        void nullBoundsForString_stillWorks() {
            assertThat(FieldValidators.validateVariableField(
                    "string", "x", "val", null, null)).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // validateComment
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ValidateComment {

        @Test
        void validComment_lettersOnly_returnsTrue() {
            assertThat(FieldValidators.validateComment("this is a comment")).isTrue();
        }

        @Test
        void validComment_startsWithUnderscore_returnsTrue() {
            assertThat(FieldValidators.validateComment("_my comment 1")).isTrue();
        }

        @Test
        void validComment_alphanumericWithSpaces_returnsTrue() {
            assertThat(FieldValidators.validateComment("test 123 abc")).isTrue();
        }

        @Test
        void invalidComment_startsWithDigit_returnsFalse() {
            assertThat(FieldValidators.validateComment("1bad")).isFalse();
        }

        @Test
        void invalidComment_startsWithSpace_returnsFalse() {
            // after trim, an empty-leading string won't match
            assertThat(FieldValidators.validateComment("   ")).isFalse();
        }

        @Test
        void invalidComment_specialChars_returnsFalse() {
            assertThat(FieldValidators.validateComment("bad@comment")).isFalse();
        }

        @Test
        void nullComment_returnsFalse() {
            assertThat(FieldValidators.validateComment(null)).isFalse();
        }

        @Test
        void emptyComment_returnsFalse() {
            assertThat(FieldValidators.validateComment("")).isFalse();
        }

        @Test
        void commentWithLeadingSpacesTrimmed_returnsTrue() {
            assertThat(FieldValidators.validateComment("  validComment  ")).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // validateValueField
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ValidateValueField {

        // ── boolean ──────────────────────────────────────────────────────────

        @Test
        void boolean_true_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "boolean", "true", "", "")).isTrue();
        }

        @Test
        void boolean_false_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "boolean", "false", "", "")).isTrue();
        }

        @Test
        void boolean_invalid_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "boolean", "maybe", "", "")).isFalse();
        }

        // ── int ──────────────────────────────────────────────────────────────

        @Test
        void int_withinBounds_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "int", "5", "0", "10")).isTrue();
        }

        @Test
        void int_atLowerBound_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "int", "0", "0", "10")).isTrue();
        }

        @Test
        void int_atUpperBound_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "int", "10", "0", "10")).isTrue();
        }

        @Test
        void int_belowLowerBound_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "int", "-1", "0", "10")).isFalse();
        }

        @Test
        void int_aboveUpperBound_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "int", "11", "0", "10")).isFalse();
        }

        @Test
        void int_nonNumericValue_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "int", "abc", "0", "10")).isFalse();
        }

        @Test
        void int_nonNumericBounds_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "int", "5", "abc", "10")).isFalse();
        }

        @Test
        void int_negativeValue_matchesIntRegex_butBoundsCheck() {
            // INT_REGEX allows negative, but parseInt may fail if bounds are bad
            assertThat(FieldValidators.validateValueField(
                    "int", "-5", "-10", "10")).isTrue();
        }

        // ── float ────────────────────────────────────────────────────────────

        @Test
        void float_withinBounds_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "float", "1.5", "0.0", "10.0")).isTrue();
        }

        @Test
        void float_atBounds_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "float", "0.0", "0.0", "10.0")).isTrue();
        }

        @Test
        void float_outOfBounds_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "float", "11.0", "0.0", "10.0")).isFalse();
        }

        @Test
        void float_nonFloat_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "float", "abc", "0.0", "10.0")).isFalse();
        }

        @Test
        void float_integerValue_failsFloatRegex_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "float", "5", "0.0", "10.0")).isFalse();
        }

        @Test
        void float_invalidBounds_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "float", "1.5", "x", "10.0")).isFalse();
        }

        // ── double ───────────────────────────────────────────────────────────

        @Test
        void double_withinBounds_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "double", "3.14", "0.0", "100.0")).isTrue();
        }

        @Test
        void double_outOfBounds_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "double", "200.0", "0.0", "100.0")).isFalse();
        }

        @Test
        void double_nonFloat_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "double", "abc", "0.0", "100.0")).isFalse();
        }

        @Test
        void double_invalidBounds_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "double", "1.0", "bad", "100.0")).isFalse();
        }

        // ── string ───────────────────────────────────────────────────────────

        @Test
        void string_validValue_returnsTrue() {
            assertThat(FieldValidators.validateValueField(
                    "string", "hello_world", "", "")).isTrue();
        }

        @Test
        void string_invalidValue_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "string", "bad value!", "", "")).isFalse();
        }

        // ── unknown type ─────────────────────────────────────────────────────

        @Test
        void unknownType_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "unknownType", "val", "0", "10")).isFalse();
        }

        // ── null / empty handling ────────────────────────────────────────────

        @Test
        void int_nullValue_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "int", null, "0", "10")).isFalse();
        }

        @Test
        void int_emptyBounds_returnsFalse() {
            assertThat(FieldValidators.validateValueField(
                    "int", "5", "", "")).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // validateLowerBoundField
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ValidateLowerBoundField {

        @Test
        void int_allValid_returnsTrue() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "int", "count", "5", "0", "10")).isTrue();
        }

        @Test
        void int_invalidName_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "int", "1bad", "5", "0", "10")).isFalse();
        }

        @Test
        void int_nonNumericValue_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "int", "count", "abc", "0", "10")).isFalse();
        }

        @Test
        void int_nonNumericLowerBound_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "int", "count", "5", "abc", "10")).isFalse();
        }

        @Test
        void int_nonNumericUpperBound_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "int", "count", "5", "0", "abc")).isFalse();
        }

        @Test
        void float_allValidFloats_returnsTrue() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "float", "x", "1.5", "0.0", "10.0")).isTrue();
        }

        @Test
        void float_intValue_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "float", "x", "5", "0.0", "10.0")).isFalse();
        }

        @Test
        void double_allValidFloats_returnsTrue() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "double", "x", "1.5", "0.0", "10.0")).isTrue();
        }

        @Test
        void double_invalidLowerBound_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "double", "x", "1.5", "bad", "10.0")).isFalse();
        }

        @Test
        void string_returnsTrue() {
            // string type does not use bounds
            assertThat(FieldValidators.validateLowerBoundField(
                    "string", "x", "val", "anything", "anything")).isTrue();
        }

        @Test
        void boolean_returnsTrue() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "boolean", "x", "true", "", "")).isTrue();
        }

        @Test
        void unknownType_returnsTrue() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "custom", "x", "val", "lb", "ub")).isTrue();
        }

        @Test
        void int_nullValues_returnsFalse() {
            assertThat(FieldValidators.validateLowerBoundField(
                    "int", null, null, null, null)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // validateUpperBoundField
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class ValidateUpperBoundField {

        @Test
        void int_allValid_returnsTrue() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "int", "count", "5", "0", "10")).isTrue();
        }

        @Test
        void int_invalidName_returnsFalse() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "int", "1bad", "5", "0", "10")).isFalse();
        }

        @Test
        void int_nonNumericValue_returnsFalse() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "int", "count", "abc", "0", "10")).isFalse();
        }

        @Test
        void int_nonNumericUpperBound_returnsFalse() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "int", "count", "5", "0", "abc")).isFalse();
        }

        @Test
        void float_allValid_returnsTrue() {
            // Note: float case in validateUpperBoundField uses POSITIVE_INT_REGEX
            assertThat(FieldValidators.validateUpperBoundField(
                    "float", "x", "5", "0", "10")).isTrue();
        }

        @Test
        void float_floatValues_returnsFalse() {
            // float case in validateUpperBoundField uses POSITIVE_INT_REGEX, not FLOAT_REGEX
            assertThat(FieldValidators.validateUpperBoundField(
                    "float", "x", "1.5", "0.0", "10.0")).isFalse();
        }

        @Test
        void double_allValidFloats_returnsTrue() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "double", "x", "1.5", "0.0", "10.0")).isTrue();
        }

        @Test
        void double_invalidValues_returnsFalse() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "double", "x", "bad", "0.0", "10.0")).isFalse();
        }

        @Test
        void string_returnsTrue() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "string", "x", "val", "anything", "anything")).isTrue();
        }

        @Test
        void boolean_returnsTrue() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "boolean", "x", "true", "", "")).isTrue();
        }

        @Test
        void unknownType_returnsTrue() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "custom", "x", "val", "lb", "ub")).isTrue();
        }

        @Test
        void int_nullValues_returnsFalse() {
            assertThat(FieldValidators.validateUpperBoundField(
                    "int", null, null, null, null)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VARIABLE_FIELD_REGEX pattern tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Nested
    class RegexPatterns {

        @ParameterizedTest
        @ValueSource(strings = {"a", "_", "myVar", "_var123", "ABC", "a1b2c3"})
        void validVariableNames_matchRegex(String name) {
            assertThat(name.matches(FieldValidators.VARIABLE_FIELD_REGEX)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"1abc", "", " ", "a b", "a-b", "a.b", "@x"})
        void invalidVariableNames_doNotMatchRegex(String name) {
            assertThat(name.matches(FieldValidators.VARIABLE_FIELD_REGEX)).isFalse();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Constructor is private (utility class)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void utilityClass_cannotBeInstantiated() throws Exception {
        var ctor = FieldValidators.class.getDeclaredConstructor();
        assertThat(java.lang.reflect.Modifier.isPrivate(ctor.getModifiers())).isTrue();
    }
}
