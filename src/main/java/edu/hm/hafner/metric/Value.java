package edu.hm.hafner.metric;

import java.io.Serializable;
import java.util.Objects;

/**
 * A leaf in the tree. A leaf is a non-divisible coverage metric like line, instruction or branch
 * coverage or mutation r complexity.
 *
 * @author Ullrich Hafner
 */
public abstract class Value implements Serializable {
    private static final long serialVersionUID = -1062406664372222691L;

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
    public abstract Value add(Value other);

    /**
     * Merge this coverage with the specified coverage.
     *
     * @param other the other coverage
     * @return the merged coverage
     * @throws IllegalArgumentException if the totals
     */
    public abstract Value max(Value other);

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
}
