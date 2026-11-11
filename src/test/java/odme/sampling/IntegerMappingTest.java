package odme.sampling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegerMappingTest {
    @ParameterizedTest
    @CsvSource({
            "0.0, 0", "0.1, 0",
            "0.25, 1", "0.4, 1",
            "0.5, 2", "0.6, 2",
            "0.75, 3", "0.9, 3", "1.0, 3"
    })
    void toInteger_dividesDesignRangeIntoEqualIntervals(double u, long expected) {
        assertEquals(expected, IntegerMapping.toInteger(u, 0, 3));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.5, 1.0})
    void toInteger_singletonAlwaysReturnsItsOnlyValue(double u) {
        assertEquals(2, IntegerMapping.toInteger(u, 2, 2));
    }

    @ParameterizedTest
    @CsvSource({"0.0, 1", "0.25, 1", "0.5, 2", "0.75, 3", "1.0, 3"})
    void toInteger_onlyUsesIntegersWithinFractionalBounds(double u, long expected) {
        assertEquals(expected, IntegerMapping.toInteger(u, 0.5, 3.7));
    }

    @Test
    void toInteger_rejectsBoundsWithoutAnInteger() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> IntegerMapping.toInteger(0.5, 0.2, 0.8));

        assertTrue(exception.getMessage().contains("[0.2, 0.8]"));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.1, 1.1, Double.NaN})
    void toInteger_rejectsInvalidDesignValues(double u) {
        assertThrows(IllegalArgumentException.class, () -> IntegerMapping.toInteger(u, 0, 3));
    }
}
