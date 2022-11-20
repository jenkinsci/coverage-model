package edu.hm.hafner.metric;

import java.util.Objects;

import org.apache.commons.lang3.math.Fraction;

/**
 * A delta represents the difference between two different value instances (a {@link Fraction} is used to represent that
 * difference).
 *
 * @author Ullrich Hafner
 */
public class Delta extends Value {
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
    public Delta(final Metric metric, final Fraction fraction) {
        super(metric);

        this.fraction = fraction;
    }

    @Override
    public Value add(final Value other) {
        if (hasSameMetric(other) && other instanceof Delta) {
            return new Delta(getMetric(), new SafeFraction(fraction).add(((Delta) other).fraction));
        }
        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public Fraction delta(final Value other) {
        if (hasSameMetric(other) && other instanceof Delta) {
            return new SafeFraction(fraction).subtract(((Delta) other).fraction);
        }
        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public Value max(final Value other) {
        if (hasSameMetric(other) && other instanceof Delta) {
            if (fraction.doubleValue() < ((Delta) other).fraction.doubleValue()) {
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

    @Override
    public String toString() {
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
        Delta delta = (Delta) o;
        return Objects.equals(fraction, delta.fraction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), fraction);
    }
}
