package odme.sampling;

/** Maps equal portions of a normalized sample range to the integers within the bounds. */
public final class IntegerMapping {
    private IntegerMapping() {
    }

    public static long toInteger(double u, double min, double max) {
        if (!(u >= 0.0 && u <= 1.0)) {
            throw new IllegalArgumentException("Design value must be in [0, 1]: " + u);
        }

        double lo = Math.ceil(min);
        double hi = Math.floor(max);
        if (!(lo <= hi) || lo < Long.MIN_VALUE || hi >= 0x1.0p63) {
            throw new IllegalArgumentException("Invalid integer bounds [" + min + ", " + max + "]");
        }
        if (u == 1.0) {
            return (long) hi;
        }

        double count = hi - lo + 1.0;
        double index = Math.min(count - 1.0, Math.floor(u * count));
        return (long) Math.min(hi, lo + index);
    }
}
