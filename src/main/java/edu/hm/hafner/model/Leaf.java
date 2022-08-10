package edu.hm.hafner.model;

import java.io.Serializable;
import java.util.Objects;

import edu.hm.hafner.util.Ensure;

/**
 * A leaf in the tree. A leaf is a non-divisible coverage metric like line, instruction or branch
 * coverage or mutation r complexity.
 *
 * @author Ullrich Hafner
 */
public class Leaf implements Serializable {
    private static final long serialVersionUID = -1062406664372222691L;

    private final Metric metric;

    /**
     * Creates a new leaf with the given coverage for the specified metric.
     *
     * @param metric
     *         the coverage metric
     */
    public Leaf(final Metric metric) {
        Ensure.that(metric.isLeaf()).isTrue("Metrics like '%s' are no leaf metrics", metric);

        this.metric = metric;
    }

    public Metric getMetric() {
        return metric;
    }

    @Override
    public String toString() {
        return String.format("%s", metric);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Leaf leaf = (Leaf) o;
        return metric.equals(leaf.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric);
    }
}
