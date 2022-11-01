package edu.hm.hafner.metric;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.math.Fraction;

import edu.umd.cs.findbugs.annotations.CheckReturnValue;

/**
 * A leaf in the tree. A leaf is a non-divisible coverage metric like line, instruction or branch coverage or mutation
 * or complexity.
 *
 * @author Ullrich Hafner
 */
public abstract class Value implements Serializable {
    private static final long serialVersionUID = -1062406664372222691L;
    private static final Fraction HUNDRED = Fraction.getFraction(100, 1);

    private final Metric metric;

    /**
     * Creates a new leaf with the given coverage for the specified metric.
     *
     * @param metric
     *         the coverage metric
     */
    protected Value(final Metric metric) {
        this.metric = metric;
    }

    public final Metric getMetric() {
        return metric;
    }

    /**
     * Add the coverage from the specified instance to the coverage of this instance.
     *
     * @param other
     *         the additional coverage details
     *
     * @return the sum of this and the additional coverage
     */
    @CheckReturnValue
    public abstract Value add(Value other);

    /**
     * Computes the delta of this value with the specified value.
     *
     * @param other
     *         the value to compare with
     *
     * @return the delta of this and the additional value
     */
    @CheckReturnValue
    public abstract Fraction delta(Value other);

    /**
     * Merge this coverage with the specified coverage.
     *
     * @param other
     *         the other coverage
     *
     * @return the merged coverage
     * @throws IllegalArgumentException
     *         if the totals
     */
    @CheckReturnValue
    public abstract Value max(Value other);

    /**
     * Returns whether this value has the same metric as the specified value.
     *
     * @param other
     *         the other value to compare with
     *
     * @return {@code true} if this value  has the same metric as the specified value, {@code false} otherwise
     */
    protected boolean hasSameMetric(final Value other) {
        return other.getMetric().equals(getMetric());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Value value = (Value) o;
        return Objects.equals(metric, value.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric);
    }

    /**
     * Returns a string representation of a {@link Fraction} in the interval [0, 1] as a percentage.
     *
     * @param percentage
     *         the percentage to print
     *
     * @return the percentage formatted as a String
     */
    protected String printPercentage(final Fraction percentage) {
        return String.format("%.2f%%", percentage.multiplyBy(HUNDRED).doubleValue());
    }
}
