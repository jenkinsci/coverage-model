package edu.hm.hafner.metric;

import java.util.Locale;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.VisibleForTesting;

/**
 * Value of a code coverage metric. The code coverage is measured using the number of covered and missed items. The type
 * of items (line, instruction, branch, file, etc.) is provided by the companion class {@link Metric}.
 *
 * @author Ullrich Hafner
 */
public final class Coverage extends Value {
    private static final long serialVersionUID = -3802318446471137305L;

    private static final Fraction HUNDRED = Fraction.getFraction(100, 1);

    @VisibleForTesting
    static final String NO_COVERAGE_AVAILABLE = "-";

    private final int covered;
    private final int missed;

    /**
     * Creates a new code coverage with the specified values.
     *
     * @param metric
     *         the metric for this coverage
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     */
    public Coverage(final Metric metric, final int covered, final int missed) {
        super(metric);

        Ensure.that(covered >= 0).isTrue("No negative values allowed for covered items: %s", covered);
        Ensure.that(missed >= 0).isTrue("No negative values allowed for missed items: %s", missed);

        this.covered = covered;
        this.missed = missed;
    }

    /**
     * Returns the number of covered items.
     *
     * @return the number of covered items
     */
    public int getCovered() {
        return covered;
    }

    /**
     * Returns the covered percentage as a {@link Fraction} in the range of {@code [0, 1]}.
     *
     * @return the covered percentage
     */
    public Fraction getCoveredPercentage() {
        if (getTotal() == 0) {
            return Fraction.ZERO;
        }
        return Fraction.getFraction(covered, getTotal());
    }

   /**
     * Returns the number of missed items.
     *
     * @return the number of missed items
     */
    public int getMissed() {
        return missed;
    }

    /**
     * Returns the missed percentage as a {@link Fraction} in the range of {@code [0, 1]}.
     *
     * @return the missed percentage
     */
    public Fraction getMissedPercentage() {
        if (getTotal() == 0) {
            return Fraction.ZERO;
        }
        return Fraction.ONE.subtract(getCoveredPercentage());
    }

    private String printPercentage(final Locale locale, final Fraction percentage) {
        return String.format(locale, "%.2f%%", percentage.multiplyBy(HUNDRED).doubleValue());
    }

    @Override
    public Coverage add(final Value other) {
        return castAndMap(other, o -> new Coverage(getMetric(), covered + o.getCovered(), missed + o.getMissed()));
    }

    @Override
    public Coverage max(final Value other) {
        return castAndMap(other, this::computeMax);
    }

    private Coverage computeMax(final Coverage otherCoverage) {
        Ensure.that(getTotal() == otherCoverage.getTotal())
                .isTrue("Cannot compute maximum of coverages %s and %s since total differs", this, otherCoverage);
        if (getCovered() >= otherCoverage.getCovered()) {
            return this;
        }
        return otherCoverage;
    }

    private Coverage castAndMap(final Value other, final UnaryOperator<Coverage> mapper) {
        if (hasSameMetric(other) && other instanceof Coverage) {
            return mapper.apply((Coverage) other);
        }

        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    public int getTotal() {
        return missed + covered;
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
        Coverage coverage = (Coverage) o;
        return covered == coverage.covered && missed == coverage.missed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), covered, missed);
    }

    @Override
    public String toString() {
        int total = getTotal();
        if (total > 0) {
            return String.format("%s (%s)", printPercentage(Locale.getDefault(), getCoveredPercentage()), getCoveredPercentage());
        }
        return NO_COVERAGE_AVAILABLE;
    }
}
