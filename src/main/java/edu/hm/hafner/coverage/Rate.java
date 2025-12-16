package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;

import java.io.Serial;
import java.util.Locale;

/**
 * A leaf in the tree that represents a rate or percentage. While rates are technically represented by a value
 * between 0 and 1, users prefer a visualization in the range 0% - 100%.
 *
 * @author Ullrich Hafner
 */
public class Rate extends Value {
    @Serial
    private static final long serialVersionUID = 7814954290956727708L;

    /** Serialization prefix for delta values. */
    public static final String PERCENTAGE = "%";

    /**
     * Returns a {@code null} object that indicates that no value has been recorded.
     *
     * @param metric
     *         the coverage metric
     *
     * @return the {@code null} object
     */
    public static Rate nullObject(final Metric metric) {
        return new Rate(metric, 0);
    }

    /**
     * Creates a new {@link Rate} instance from the provided string representation. The string representation is
     * expected to start with the metric, written in all caps characters and followed by a colon.
     * Then the {@link Rate} specific serialization is following. Whitespace characters will be ignored.
     *
     * <p>Examples: TEST_SUCCESS_RATE: %10/100, COHESION: %0/5</p>
     *
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created difference
     * @throws IllegalArgumentException
     *         if the string is not a valid cov instance
     */
    public static Rate valueOf(final String stringRepresentation) {
        var value = Value.valueOf(stringRepresentation);

        if (value instanceof Rate delta) {
            return delta;
        }
        throw new IllegalArgumentException("Cannot convert '%s' to a valid Rate instance.".formatted(stringRepresentation));
    }

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value to store
     */
    public Rate(final Metric metric, final Fraction value) {
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
    public Rate(final Metric metric, final double value) {
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
    public Rate(final Metric metric, final int numerator, final int denominator) {
        super(metric, numerator, denominator);
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
     * @throws ArithmeticException if numerator or denominator cannot be represented as integer values
     */
    public Rate(final Metric metric, final long numerator, final long denominator) {
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
    public Rate(final Metric metric, final int value) {
        super(metric, value);
    }

    @Override
    protected Value createValue(final Fraction newFraction) {
        return new Rate(getMetric(), newFraction);
    }

    @Override
    public double asDouble() {
        return rawValue() * 100.0;
    }

    @Override
    public String asText(final Locale locale) {
        return getMetric().format(locale, rawValue());
    }

    @Override
    public String asInformativeText(final Locale locale) {
        return getMetric().format(locale, rawValue());
    }

    private double rawValue() {
        return super.asDouble();
    }

    @Override
    protected String serializeValue() {
        return PERCENTAGE + super.serializeValue();
    }
}
