package edu.hm.hafner.coverage;

import java.io.Serial;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;

/**
 * A leaf in the tree that represents a delta of two {@link Value} instances. Such values are used to show the
 * delta (i.e., the difference) of two other values. The delta uses a slightly different textual representation than the
 * plain value: positive values are prefixed with a plus sign, zero is also handled differently.
 *
 * @author Ullrich Hafner
 */
public class Difference extends Value {
    @Serial
    private static final long serialVersionUID = -1115727256219835389L;

    /**
     * Returns a {@code null} object that indicates that no value has been recorded.
     *
     * @param metric
     *         the coverage metric
     *
     * @return the {@code null} object
     */
    public static Difference nullObject(final Metric metric) {
        return new Difference(metric, 0);
    }

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value to store
     */
    public Difference(final Metric metric, final Fraction value) {
        super(metric, value);
    }

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value to store
     */
    public Difference(final Metric metric, final double value) {
        super(metric, value);
    }

    /**
     * Creates a new leaf with the given value (a fraction) for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param numerator
     *         the numerator, i.e., the three in 'three sevenths'
     * @param denominator
     *         the denominator, i.ee, the seven in 'three sevenths'
     */
    public Difference(final Metric metric, final int numerator, final int denominator) {
        super(metric, numerator, denominator);
    }

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value
     */
    public Difference(final Metric metric, final int value) {
        super(metric, value);
    }

    @Override
    public String asText(final Locale locale) {
        return getMetric().formatDelta(locale, asDouble());
    }

    @Override
    public String asInformativeText(final Locale locale) {
        return getMetric().formatDelta(locale, asDouble());
    }
}
