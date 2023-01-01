package edu.hm.hafner.metric;

import java.util.Objects;

import org.apache.commons.lang3.math.Fraction;

/**
 * Represents the value of a rational number based metric. Internally the rational number is stored using a
 * {@link Fraction} instance.
 *
 * @author Ullrich Hafner
 */
public final class FractionValue extends Value {
    private static final long serialVersionUID = -7019903979028578410L;

    private final Fraction fraction;

    /**
     * Creates a new leaf with the delta value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param fraction
     *         the value to store
     */
    public FractionValue(final Metric metric, final Fraction fraction) {
        super(metric);

        this.fraction = fraction;
    }

    /**
     * Creates a new leaf with the delta value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param numerator
     *         the numerator of the rational number
     * @param denominator
     *         the denominator of the rational number
     */
    public FractionValue(final Metric metric, final int numerator, final int denominator) {
        this(metric, Fraction.getFraction(numerator, denominator));
    }

    public Fraction getFraction() {
        return fraction;
    }

    @Override
    public Value add(final Value other) {
        if (hasSameMetric(other) && other instanceof FractionValue) {
            return new FractionValue(getMetric(), asSafeFraction().add(((FractionValue) other).fraction));
        }
        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public Fraction delta(final Value other) {
        if (hasSameMetric(other) && other instanceof FractionValue) {
            return asSafeFraction().subtract(((FractionValue) other).fraction);
        }
        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public Value max(final Value other) {
        if (hasSameMetric(other) && other instanceof FractionValue) {
            if (fraction.doubleValue() < ((FractionValue) other).fraction.doubleValue()) {
                return other;
            }
            return this;
        }
        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public boolean isBelowThreshold(final double threshold) {
        return fraction.doubleValue() < threshold;
    }

    private SafeFraction asSafeFraction() {
        return new SafeFraction(fraction);
    }

    @Override
    public String toString() {
        return serialize();
    }

    @Override
    public String serialize() {
        return String.format("%s: %s", getMetric(), fraction);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        FractionValue fractionValue = (FractionValue) o;
        return Objects.equals(fraction, fractionValue.fraction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fraction);
    }
}
