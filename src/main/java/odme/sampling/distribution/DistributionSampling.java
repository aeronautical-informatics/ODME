package odme.sampling.distribution;

import java.util.Random;

/**
 * Provides sampling utilities for statistical distributions.
 * Supports normal (via SD-threshold discretisation) and uniform distributions.
 */
public class DistributionSampling {

    private static final Random RANDOM = new Random();

    static double value;

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
        for (int i = 1; i <= numTests; i++) {
            value = -1;
            while (value < 0) {
                int sdLevel = RANDOM.nextInt(5);
                switch (sdLevel) {
                    case 0: value = mean - stdDev; break;      // μ − σ
                    case 1: value = mean; break;               // μ
                    case 2: value = mean + stdDev; break;      // μ + σ
                    case 3: value = mean + 2 * stdDev; break;  // μ + 2σ
                    case 4: value = mean + 3 * stdDev; break;  // μ + 3σ
                    default: break;
                }
            }
        }
        return value;
    }

    /**
     * Samples uniformly from [a, b].
     */
    public static double uniformDistributionSample(double a, double b) {
        value = a + (b - a) * RANDOM.nextDouble();
        return value;
    }
}
