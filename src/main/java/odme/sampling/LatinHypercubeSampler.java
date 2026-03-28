package odme.sampling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Simple Latin Hypercube Sampler for the sampling package.
 * Generates normalized samples in [0,1] for each dimension.
 * Used by SamplingManager to produce space-filling parameter samples
 * before scaling them to their real-world ranges.
 */
public class LatinHypercubeSampler {

    private final Random random = new Random();

    /**
     * Generates normalized LHS samples in [0,1]^dimensions.
     *
     * @param dimensions number of parameters (columns)
     * @param numSamples number of samples (rows)
     * @return list of double[] arrays, each of length {@code dimensions}, values in [0,1]
     */
    public List<double[]> generateNormalizedSamples(int dimensions, int numSamples) {
        if (dimensions <= 0 || numSamples <= 0) {
            return new ArrayList<>();
        }

        double[][] matrix = new double[numSamples][dimensions];

        for (int dim = 0; dim < dimensions; dim++) {
            // Create a random permutation of strata [0, numSamples)
            int[] perm = new int[numSamples];
            for (int i = 0; i < numSamples; i++) perm[i] = i;
            for (int i = numSamples - 1; i > 0; i--) {
                int j = random.nextInt(i + 1);
                int tmp = perm[i]; perm[i] = perm[j]; perm[j] = tmp;
            }

            for (int i = 0; i < numSamples; i++) {
                double u = random.nextDouble();
                matrix[i][dim] = (perm[i] + u) / numSamples;
            }
        }

        List<double[]> result = new ArrayList<>(numSamples);
        for (double[] row : matrix) {
            result.add(row);
        }
        return result;
    }
}
