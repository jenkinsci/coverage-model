package edu.hm.hafner.coverage;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.VisibleForTesting;

/**
 * Value of a code coverage item. The code coverage is measured using the number of covered and missed items. The type
 * of items (line, instruction, branch, file, etc.) is provided by the companion class {@link CoverageMetric}.
 *
 * @author Ullrich Hafner
 */
public final class Coverage implements Serializable {
    private static final long serialVersionUID = -3802318446471137305L;

    /** Null object that indicates that the code coverage has not been measured. */
    public static final Coverage NO_COVERAGE = new Coverage(0, 0);

    private static final Fraction HUNDRED = Fraction.getFraction(100, 1);

    @VisibleForTesting
    static final String NO_COVERAGE_AVAILABLE = "-";

    private final int covered;
    private final int missed;

    /**
     * Creates a new code coverage with the specified values.
     *
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     */
    public Coverage(final int covered, final int missed) {
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
     * Formats the covered percentage as String (with a precision of two digits after the comma). Uses {@code
     * Locale.getDefault()} to format the percentage.
     *
     * @return the covered percentage
     * @see #formatCoveredPercentage(Locale)
     */
    public String formatCoveredPercentage() {
        return formatCoveredPercentage(Locale.getDefault());
    }

    /**
     * Formats the covered percentage as String (with a precision of two digits after the comma).
     *
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return the covered percentage
     */
    public String formatCoveredPercentage(final Locale locale) {
        return printPercentage(locale, getCoveredPercentage());
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

    /**
     * Formats the missed percentage as formatted String (with a precision of two digits after the comma). Uses {@code
     * Locale.getDefault()} to format the percentage.
     *
     * @return the missed percentage
     */
    public String formatMissedPercentage() {
        return formatMissedPercentage(Locale.getDefault());
    }

    /**
     * Formats the missed percentage as formatted String (with a precision of two digits after the comma).
     *
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return the missed percentage
     */
    public String formatMissedPercentage(final Locale locale) {
        return printPercentage(locale, getMissedPercentage());
    }

    private String printPercentage(final Locale locale, final Fraction percentage) {
        if (isSet()) {
            return String.format(locale, "%.2f%%", percentage.multiplyBy(HUNDRED).doubleValue());
        }
        return NO_COVERAGE_AVAILABLE;
    }

    /**
     * Add the coverage from the specified instance to the coverage of this instance.
     *
     * @param additional
     *         the additional coverage details
     *
     * @return the sum of this and the additional coverage
     */
    public Coverage add(final Coverage additional) {
        return new Coverage(covered + additional.getCovered(),
                missed + additional.getMissed());
    }

    @Override
    public String toString() {
        int total = getTotal();
        if (total > 0) {
            return String.format("%s (%s)", formatCoveredPercentage(), getCoveredPercentage());
        }
        return NO_COVERAGE_AVAILABLE;
    }

    public int getTotal() {
        return missed + covered;
    }

    public boolean isSet() {
        return getTotal() > 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Coverage coverage = (Coverage) o;
        return covered == coverage.covered && missed == coverage.missed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(covered, missed);
    }
}
