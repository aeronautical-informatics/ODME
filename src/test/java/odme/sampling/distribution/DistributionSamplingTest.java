package odme.sampling.distribution;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link DistributionSampling}.
 */
class DistributionSamplingTest {

    // ────────────────────────────────────────────────────────────────────────
    // normalDistributionSample
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void normalDistributionSample_returnsNonNegativeValue() {
        double result = DistributionSampling.normalDistributionSample(10.0, 2.0, 1);
        assertTrue(result >= 0.0, "Normal sample must be non-negative");
    }

    @Test
    void normalDistributionSample_resultIsOneOfFiveTiers() {
        // The method produces exactly μ−σ, μ, μ+σ, μ+2σ, or μ+3σ
        double mean = 50.0;
        double sd = 10.0;
        double[] expected = {mean - sd, mean, mean + sd, mean + 2 * sd, mean + 3 * sd};

        for (int trial = 0; trial < 200; trial++) {
            double v = DistributionSampling.normalDistributionSample(mean, sd, 1);
            boolean matched = false;
            for (double e : expected) {
                if (Math.abs(v - e) < 1e-9) { matched = true; break; }
            }
            assertTrue(matched, "Value " + v + " is not one of the five expected tiers");
        }
    }

    @Test
    void normalDistributionSample_multipleIterationsRunWithoutError() {
        // numTests > 1 should not throw; it returns the last sampled value
        assertDoesNotThrow(() -> DistributionSampling.normalDistributionSample(5.0, 1.0, 100));
    }

    @Test
    void normalDistributionSample_meansWithLargeStdDev_nonNegative() {
        // With mean=1, sd=0.1, all tiers are positive → always ≥ 0
        for (int i = 0; i < 50; i++) {
            double v = DistributionSampling.normalDistributionSample(1.0, 0.1, 1);
            assertTrue(v >= 0.0);
        }
    }

    @Test
    void normalDistributionSample_zeroStdDev_returnsMean() {
        // When stdDev=0, all tiers collapse to mean (μ + k*0 = μ)
        double result = DistributionSampling.normalDistributionSample(42.0, 0.0, 1);
        assertEquals(42.0, result, 1e-9);
    }

    // ────────────────────────────────────────────────────────────────────────
    // uniformDistributionSample
    // ────────────────────────────────────────────────────────────────────────

    @RepeatedTest(50)
    void uniformDistributionSample_withinBounds() {
        double a = 5.0, b = 15.0;
        double v = DistributionSampling.uniformDistributionSample(a, b);
        assertTrue(v >= a && v <= b,
                "Uniform sample " + v + " must be in [" + a + ", " + b + "]");
    }

    @Test
    void uniformDistributionSample_zeroBandwidth_returnsLowerBound() {
        // a == b: the value must equal a
        double result = DistributionSampling.uniformDistributionSample(7.0, 7.0);
        assertEquals(7.0, result, 1e-9);
    }

    @Test
    void uniformDistributionSample_negativeBounds_withinRange() {
        double a = -100.0, b = -10.0;
        double v = DistributionSampling.uniformDistributionSample(a, b);
        assertTrue(v >= a && v <= b);
    }

    @Test
    void uniformDistributionSample_setsStaticValueField() {
        double a = 2.0, b = 8.0;
        double result = DistributionSampling.uniformDistributionSample(a, b);
        // The static field 'value' should match the returned value
        assertEquals(result, DistributionSampling.value, 1e-15);
    }
}
