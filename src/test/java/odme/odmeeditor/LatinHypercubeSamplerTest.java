package odme.odmeeditor;

import odme.odmeeditor.LatinHypercubeSampler.Parameter;
import odme.odmeeditor.LatinHypercubeSampler.TestCase;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LatinHypercubeSamplerTest {

    @Test
    void sample_returnsCorrectNumberOfTestCases() {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler(42);
        List<Parameter> params = List.of(
                new Parameter("visibility_m", "FairWeather", "double", 5000.0, 15000.0),
                new Parameter("distance_m", "DS10", "double", 10.0, 12.0)
        );
        List<TestCase> results = sampler.sample(params, 10);
        assertEquals(10, results.size());
    }

    @Test
    void sample_allValuesWithinBounds() {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler(123);
        Parameter p1 = new Parameter("elevation_m", "EL10", "double", 1.0, 1.3);
        Parameter p2 = new Parameter("offset_m", "LO00", "double", 0.0, 0.7);
        List<Parameter> params = List.of(p1, p2);

        List<TestCase> results = sampler.sample(params, 50);
        for (TestCase tc : results) {
            double v1 = tc.values.get(p1);
            double v2 = tc.values.get(p2);
            assertTrue(v1 >= 1.0 && v1 <= 1.3, "elevation out of bounds: " + v1);
            assertTrue(v2 >= 0.0 && v2 <= 0.7, "offset out of bounds: " + v2);
        }
    }

    @Test
    void sample_strataCoverage() {
        // Each stratum should be hit exactly once per parameter
        LatinHypercubeSampler sampler = new LatinHypercubeSampler(7);
        int n = 20;
        Parameter p = new Parameter("dist", "Node", "double", 0.0, 100.0);
        List<TestCase> results = sampler.sample(List.of(p), n);

        // Map each value to its stratum index
        Set<Integer> strata = new HashSet<>();
        for (TestCase tc : results) {
            double val = tc.values.get(p);
            int stratum = (int) (val / (100.0 / n));
            if (stratum == n) stratum = n - 1; // edge case for max value
            strata.add(stratum);
        }
        // All n strata should be covered
        assertEquals(n, strata.size(), "Not all strata covered");
    }

    @Test
    void sample_integerParametersRounded() {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler(99);
        Parameter p = new Parameter("resolution", "DNN", "int", 600, 1200);
        List<TestCase> results = sampler.sample(List.of(p), 10);

        for (TestCase tc : results) {
            double val = tc.values.get(p);
            assertEquals(val, Math.round(val), 0.001, "int parameter not rounded");
            assertTrue(val >= 600 && val <= 1200);
        }
    }

    @Test
    void sample_emptyParametersReturnsEmpty() {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler();
        List<TestCase> results = sampler.sample(Collections.emptyList(), 10);
        assertTrue(results.isEmpty());
    }

    @Test
    void sample_zeroSamplesReturnsEmpty() {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler();
        Parameter p = new Parameter("x", "N", "double", 0, 1);
        List<TestCase> results = sampler.sample(List.of(p), 0);
        assertTrue(results.isEmpty());
    }

    @Test
    void toCsv_producesValidFormat() {
        LatinHypercubeSampler sampler = new LatinHypercubeSampler(1);
        Parameter p1 = new Parameter("vis", "Weather", "double", 1.0, 10.0);
        Parameter p2 = new Parameter("res", "DNN", "int", 200, 800);
        List<Parameter> params = List.of(p1, p2);
        List<TestCase> results = sampler.sample(params, 3);

        String csv = LatinHypercubeSampler.toCsv(params, results);
        String[] lines = csv.split("\n");
        assertEquals(4, lines.length); // header + 3 rows
        assertEquals("TestCase_ID,Weather.vis,DNN.res", lines[0]);

        // Each data row should have 3 columns
        for (int i = 1; i < lines.length; i++) {
            String[] cols = lines[i].split(",");
            assertEquals(3, cols.length, "Row " + i + " has wrong column count");
        }
    }

    @Test
    void sample_reproducibleWithSameSeed() {
        List<Parameter> params = List.of(
                new Parameter("a", "N", "double", 0, 100),
                new Parameter("b", "N", "double", -10, 10)
        );

        LatinHypercubeSampler s1 = new LatinHypercubeSampler(42);
        LatinHypercubeSampler s2 = new LatinHypercubeSampler(42);

        List<TestCase> r1 = s1.sample(params, 5);
        List<TestCase> r2 = s2.sample(params, 5);

        for (int i = 0; i < 5; i++) {
            for (Parameter p : params) {
                assertEquals(r1.get(i).values.get(p), r2.get(i).values.get(p), 1e-15,
                        "Same seed should produce identical results");
            }
        }
    }
}
