package edu.hm.hafner.coverage;

import java.util.Objects;

import edu.hm.hafner.model.Leaf;
import edu.hm.hafner.model.Metric;

/**
 * A leaf in the coverage hierarchy. A leaf is a non-divisible coverage metric like line, instruction or branch
 * coverage.
 *
 * @author Ullrich Hafner
 */
public final class CoverageLeaf extends Leaf {
    private static final long serialVersionUID = -1062406664372222691L;

    private final Coverage coverage;

    /**
     * Creates a new leaf with the given coverage for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param coverage
     *         the coverage of the element
     */
    public CoverageLeaf(final Metric metric, final Coverage coverage) {
        super(metric);
        this.coverage = coverage;
    }

    public Coverage getCoverage() {
        return coverage;
    }

    /**
     * Returns the coverage for the specified metric.
     *
     * @param searchMetric
     *         the metric to get the coverage for
     *
     * @return coverage ratio
     */
    public Coverage getCoverage(final Metric searchMetric) {
        if (super.getMetric().equals(searchMetric)) {
            return coverage;
        }
        return Coverage.NO_COVERAGE;
    }

    @Override
    public String toString() {
        return String.format("[%s]: %s", super.getMetric(), coverage);
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
        CoverageLeaf that = (CoverageLeaf) o;
        return coverage.equals(that.coverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coverage);
    }
}
