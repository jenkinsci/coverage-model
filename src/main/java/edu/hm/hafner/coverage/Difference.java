package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;

import java.io.Serial;
import java.util.Locale;

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
    /** Serialization prefix for delta values. */
    public static final String DELTA = "Δ";

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
     * Creates a new {@link Difference} instance from the provided string representation. The string representation is
     * expected to start with the metric, written in all caps characters and followed by a colon.
     * Then the {@link Difference} specific serialization is following. Whitespace characters will be ignored.
     *
     * <p>Examples: LINE: Δ10/100, BRANCH: Δ0/5, LOC: Δ160</p>
     *
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created difference
     * @throws IllegalArgumentException
     *         if the string is not a valid cov instance
     */
    @SuppressWarnings("PMD.CyclomaticComplexity") // this is a factory method that selects the correct metric
    public static Difference valueOf(final String stringRepresentation) {
        var value = Value.valueOf(stringRepresentation);

        if (value instanceof Difference delta) {
            return delta;
        }
        throw new IllegalArgumentException("Cannot convert '%s' to a valid Difference instance.".formatted(stringRepresentation));
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

    @Override
    protected String serializeValue() {
        return DELTA + super.serializeValue();
    }
}
