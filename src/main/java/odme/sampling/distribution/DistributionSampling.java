package odme.sampling.distribution;

import java.util.Random;

/**
 * Provides sampling utilities for statistical distributions.
 * Supports normal (via SD-threshold discretisation) and uniform distributions.
 */
public class DistributionSampling {

    public static double value;

    /**
     * Samples from a normal distribution using a 5-tier SD-threshold approach.
     * Generates {@code numTests} samples and returns the last value.
     *
     * @param mean    distribution mean (μ)
     * @param stdDev  standard deviation (σ)
     * @param numTests number of samples to generate
     * @return the last sampled value (≥ 0)
     */
    public static double normalDistributionSample(double mean, double stdDev, int numTests) {
        Random random = new Random();
        for (int i = 1; i <= numTests; i++) {
            value = -1;
            int sdLevel = -1;
            while (value < 0) {
                sdLevel = random.nextInt(5);
                switch (sdLevel) {
                    case 0: value = mean - stdDev; break;      // μ − σ
                    case 1: value = mean; break;               // μ
                    case 2: value = mean + stdDev; break;      // μ + σ
                    case 3: value = mean + 2 * stdDev; break;  // μ + 2σ
                    case 4: value = mean + 3 * stdDev; break;  // μ + 3σ
                }
            }
        }
        return value;
    }

    /**
     * Samples uniformly from [a, b].
     */
    public static double uniformDistributionSample(double a, double b) {
        Random random = new Random();
        double u = random.nextDouble();
        value = a + (b - a) * u;
        return value;
    }
}
