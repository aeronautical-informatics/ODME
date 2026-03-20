package odme.odmeeditor;

import java.util.*;

/**
 * Latin Hypercube Sampling (LHS) for generating space-filling test cases
 * from ODD parameter ranges.
 *
 * LHS divides each parameter's range into N equal strata, then samples
 * exactly once from each stratum per parameter, ensuring uniform coverage
 * of the parameter space with far fewer samples than full factorial design.
 */
public class LatinHypercubeSampler {

    private final Random random;

    public LatinHypercubeSampler() {
        this.random = new Random();
    }

    public LatinHypercubeSampler(long seed) {
        this.random = new Random(seed);
    }

    /**
     * A single ODD parameter with name, type, and bounds.
     */
    public static class Parameter {
        public final String name;
        public final String parentNode;
        public final String dataType;
        public final double min;
        public final double max;

        public Parameter(String name, String parentNode, String dataType, double min, double max) {
            this.name = name;
            this.parentNode = parentNode;
            this.dataType = dataType;
            this.min = min;
            this.max = max;
        }

        @Override
        public String toString() {
            return parentNode + "." + name + " [" + min + ", " + max + "]";
        }
    }

    /**
     * A generated test case: one sampled value per parameter.
     */
    public static class TestCase {
        public final int id;
        public final Map<Parameter, Double> values;

        public TestCase(int id, Map<Parameter, Double> values) {
            this.id = id;
            this.values = values;
        }
    }

    /**
     * Generate N test cases using Latin Hypercube Sampling.
     *
     * For each parameter, the range [min, max] is divided into N equal
     * strata. One sample is drawn uniformly from each stratum, then the
     * strata are randomly permuted across test cases. This ensures:
     * - Each stratum is sampled exactly once per parameter
     * - No two test cases share the same stratum for any parameter
     * - The design is space-filling in all dimensions
     *
     * @param parameters list of ODD parameters with bounds
     * @param n          number of samples (test cases) to generate
     * @return list of n TestCase objects
     */
    public List<TestCase> sample(List<Parameter> parameters, int n) {
        if (parameters.isEmpty() || n <= 0) {
            return Collections.emptyList();
        }

        int d = parameters.size();

        // For each parameter, create a permutation of strata [0, n)
        // and sample one value within each stratum
        double[][] samples = new double[d][n];
        for (int dim = 0; dim < d; dim++) {
            Parameter p = parameters.get(dim);
            double range = p.max - p.min;

            // Create permutation of strata indices
            int[] perm = randomPermutation(n);

            for (int i = 0; i < n; i++) {
                int stratum = perm[i];
                // Sample uniformly within this stratum
                double u = random.nextDouble();
                double stratumValue = (stratum + u) / n;
                double value = p.min + stratumValue * range;

                // For integer types, round to nearest int within bounds
                if (p.dataType.equalsIgnoreCase("int") || p.dataType.equalsIgnoreCase("integer")) {
                    value = Math.round(value);
                    value = Math.max(p.min, Math.min(p.max, value));
                }

                samples[dim][i] = value;
            }
        }

        // Build TestCase objects
        List<TestCase> testCases = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Map<Parameter, Double> values = new LinkedHashMap<>();
            for (int dim = 0; dim < d; dim++) {
                values.put(parameters.get(dim), samples[dim][i]);
            }
            testCases.add(new TestCase(i + 1, values));
        }

        return testCases;
    }

    /**
     * Generate a random permutation of [0, n).
     */
    private int[] randomPermutation(int n) {
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = arr[i];
            arr[i] = arr[j];
            arr[j] = tmp;
        }
        return arr;
    }

    /**
     * Export test cases as CSV string.
     */
    public static String toCsv(List<Parameter> parameters, List<TestCase> testCases) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("TestCase_ID");
        for (Parameter p : parameters) {
            sb.append(",").append(p.parentNode).append(".").append(p.name);
        }
        sb.append("\n");

        // Rows
        for (TestCase tc : testCases) {
            sb.append(tc.id);
            for (Parameter p : parameters) {
                Double val = tc.values.get(p);
                if (p.dataType.equalsIgnoreCase("int") || p.dataType.equalsIgnoreCase("integer")) {
                    sb.append(",").append(val.intValue());
                } else {
                    sb.append(",").append(String.format(Locale.US, "%.6f", val));
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }
}
