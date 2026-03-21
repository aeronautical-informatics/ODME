package odme.sampling;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link odme.sampling.LatinHypercubeSampler}.
 * Verifies that generated samples are space-filling, within bounds,
 * and behave correctly for edge cases.
 */
class LatinHypercubeSamplerTest {

    private final LatinHypercubeSampler sampler = new LatinHypercubeSampler();

    @Test
    void generateNormalizedSamples_returnsCorrectCount() {
        List<double[]> samples = sampler.generateNormalizedSamples(3, 10);
        assertEquals(10, samples.size());
    }

    @Test
    void generateNormalizedSamples_correctDimensions() {
        List<double[]> samples = sampler.generateNormalizedSamples(5, 8);
        for (double[] sample : samples) {
            assertEquals(5, sample.length, "Each sample must have 5 dimensions");
        }
    }

    @Test
    void generateNormalizedSamples_allValuesInUnitInterval() {
        List<double[]> samples = sampler.generateNormalizedSamples(4, 20);
        for (double[] sample : samples) {
            for (double v : sample) {
                assertTrue(v >= 0.0 && v <= 1.0,
                        "Value " + v + " is outside [0,1]");
            }
        }
    }

    @Test
    void generateNormalizedSamples_strataCoverage() {
        // With n samples over 1 dimension, each stratum [k/n, (k+1)/n) must be hit exactly once
        int n = 20;
        List<double[]> samples = sampler.generateNormalizedSamples(1, n);
        Set<Integer> strata = new HashSet<>();
        for (double[] s : samples) {
            int stratum = (int) (s[0] * n);
            if (stratum == n) stratum = n - 1;
            strata.add(stratum);
        }
        assertEquals(n, strata.size(), "LHS must cover all strata exactly once");
    }

    @Test
    void generateNormalizedSamples_zeroDimensions_returnsEmpty() {
        List<double[]> samples = sampler.generateNormalizedSamples(0, 10);
        assertTrue(samples.isEmpty());
    }

    @Test
    void generateNormalizedSamples_zeroSamples_returnsEmpty() {
        List<double[]> samples = sampler.generateNormalizedSamples(3, 0);
        assertTrue(samples.isEmpty());
    }

    @Test
    void generateNormalizedSamples_negativeDimensions_returnsEmpty() {
        List<double[]> samples = sampler.generateNormalizedSamples(-1, 5);
        assertTrue(samples.isEmpty());
    }

    @Test
    void generateNormalizedSamples_oneSample_returnsOneSample() {
        List<double[]> samples = sampler.generateNormalizedSamples(3, 1);
        assertEquals(1, samples.size());
        assertEquals(3, samples.get(0).length);
    }

    @Test
    void generateNormalizedSamples_multiDimensional_independentStrata() {
        // Verify that strata coverage holds independently for each dimension
        int n = 15;
        int dims = 3;
        List<double[]> samples = sampler.generateNormalizedSamples(dims, n);

        for (int d = 0; d < dims; d++) {
            Set<Integer> strata = new HashSet<>();
            for (double[] s : samples) {
                int stratum = (int) (s[d] * n);
                if (stratum == n) stratum = n - 1;
                strata.add(stratum);
            }
            final int dim = d;
            assertEquals(n, strata.size(),
                    "Dimension " + dim + " must cover all strata exactly once");
        }
    }
}
