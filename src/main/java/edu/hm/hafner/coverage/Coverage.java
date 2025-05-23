package edu.hm.hafner.coverage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.Generated;
import edu.hm.hafner.util.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.CheckForNull;

import java.io.Serial;
import java.util.Locale;
import java.util.Objects;

/**
 * Value of a code coverage metric. The code coverage is measured using the number of covered and missed items. The type
 * of items (line, instruction, branch, mutation, file, etc.) is provided by the companion class {@link Metric}.
 *
 * @author Ullrich Hafner
 */
public final class Coverage extends Value {
    @Serial
    private static final long serialVersionUID = -3802318446471137305L;
    private static final String FRACTION_SEPARATOR = "/";
    private static final String N_A = "n/a";

    /**
     * Creates a new {@link Coverage} instance from the provided string representation. The string representation is
     * expected to contain the number of covered items and the total number of items - separated by a slash, e.g.
     * "100/345", or "0/0". Whitespace characters will be ignored.
     *
     * @param metric
     *         the coverage metric of this instance
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created coverage
     * @throws IllegalArgumentException
     *         if the string is not a valid Coverage instance
     */
    public static Coverage valueOf(final Metric metric, final String stringRepresentation) {
        var errorMessage = "Cannot convert %s to a valid Coverage instance.".formatted(stringRepresentation);
        try {
            var cleanedFormat = StringUtils.deleteWhitespace(stringRepresentation);
            if (N_A.equals(cleanedFormat)) {
                return nullObject(metric);
            }
            if (StringUtils.contains(cleanedFormat, FRACTION_SEPARATOR)) {
                var extractedCovered = StringUtils.substringBefore(cleanedFormat, FRACTION_SEPARATOR);
                var extractedTotal = StringUtils.substringAfter(cleanedFormat, FRACTION_SEPARATOR);

                int covered = Integer.parseInt(extractedCovered);
                int total = Integer.parseInt(extractedTotal);
                if (total >= covered) {
                    return new CoverageBuilder().withMetric(metric)
                            .withCovered(covered)
                            .withMissed(total - covered)
                            .build();
                }
            }
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
        throw new IllegalArgumentException(errorMessage);
    }

    /**
     * Returns a {@code null} object that indicates that no coverage has been recorded.
     *
     * @param metric
     *         the coverage metric
     *
     * @return the {@code null} object
     */
    public static Coverage nullObject(final Metric metric) {
        return new CoverageBuilder().withMetric(metric)
                .withCovered(0)
                .withMissed(0)
                .build();
    }

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
    private Coverage(final Metric metric, final int covered, final int missed) {
        super(metric, Fraction.getFraction(covered)); // Only the covered items are stored in the parent

        this.missed = missed;
    }

    /**
     * Returns the number of covered items.
     *
     * @return the number of covered items
     */
    public int getCovered() {
        return getFraction().getNumerator();
    }

    /**
     * Returns the percentage of covered items. If this coverage is undefined (i.e., {@code total} is zero) then zero
     * will be returned.
     *
     * @return the covered percentage
     */
    public Percentage getCoveredPercentage() {
        if (getTotal() == 0) {
            return Percentage.ZERO;
        }
        return Percentage.valueOf(getCovered(), getTotal());
    }

    /**
     * Returns the number of missed items.
     *
     * @return the number of missed items
     */
    public int getMissed() {
        return missed;
    }

    @Override
    public Coverage add(final Value other) {
        var otherCoverage = castValue(other);

        return new CoverageBuilder().withMetric(getMetric())
                .withCovered(getCovered() + otherCoverage.getCovered())
                .withMissed(getMissed() + otherCoverage.getMissed())
                .build();
    }

    @Override
    public Difference subtract(final Value other) {
        ensureSameMetricAndType(other);

        return new Difference(getMetric(), asDouble() - other.asDouble());
    }

    @Override
    public Coverage max(final Value other) {
        var otherCoverage = castValue(other);
        Ensure.that(getTotal() == otherCoverage.getTotal())
                .isTrue("Cannot compute maximum of coverages %s and %s since total differs",
                        this, other);

        if (getCovered() >= otherCoverage.getCovered()) {
            return this;
        }
        return otherCoverage;
    }

    private Coverage castValue(final Value other) {
        ensureSameMetricAndType(other);

        return (Coverage) other; // the type is checked in ensureSameMetric
    }

    /**
     * Returns whether this coverage percentage is below the given threshold. The threshold must be a percentage in the
     * range of [0, 100].
     *
     * @param threshold
     *         the threshold in the range of [0, 100]
     *
     * @return {@code true}, if this value is below the specified threshold
     */
    @Override
    public boolean isOutOfValidRange(final double threshold) {
        return getCoveredPercentage().toDouble() < threshold;
    }

    public int getTotal() {
        return getCovered() + getMissed();
    }

    public boolean isSet() {
        return getTotal() > 0;
    }

    @Override
    public String asText(final Locale locale) {
        if (isSet()) {
            return String.format(locale, "%.2f%%", asRounded());
        }
        return N_A;
    }

    @Override
    public String asInformativeText(final Locale locale) {
        if (isSet()) {
            return String.format(locale, "%.2f%% (%d/%d)", asRounded(), getCovered(), getTotal());
        }
        return N_A;
    }

    @Override
    public int asInteger() {
        return getCoveredPercentage().toInt();
    }

    @Override
    public double asDouble() {
        return getCoveredPercentage().toDouble();
    }

    @Override
    protected String serializeValue() {
        if (isSet()) {
            return String.format(Locale.ENGLISH, "%d/%d", getCovered(), getTotal());
        }
        return N_A;
    }

    @Override
    @Generated
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
        var coverage = (Coverage) o;
        return missed == coverage.missed;
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), missed);
    }

    @Override
    public String toString() {
        if (isSet()) {
            return String.format(Locale.ENGLISH, "%s: %s (%d/%d)",
                    getMetric(), getCoveredPercentage(), getCovered(), getTotal());
        }
        return String.format(Locale.ENGLISH, "%s: n/a", getMetric());
    }

    @Override
    public double asRounded() {
        return getCoveredPercentage().toRounded();
    }

    /**
     * Builder to create cached {@link Coverage} instances.
     */
    @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static final class CoverageBuilder {
        @VisibleForTesting
        static final int CACHE_SIZE = 16;
        private static final Coverage[] LINE_CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];
        private static final Coverage[] BRANCH_CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];
        private static final Coverage[] INSTRUCTION_CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];
        private static final Coverage[] MUTATION_CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];
        private static final Coverage[] MCDC_PAIR_CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];
        private static final Coverage[] FUNCTION_CALL_CACHE = new Coverage[CACHE_SIZE * CACHE_SIZE];

        static {
            for (int covered = 0; covered < CACHE_SIZE; covered++) {
                for (int missed = 0; missed < CACHE_SIZE; missed++) {
                    LINE_CACHE[getCacheIndex(covered, missed)] = new Coverage(Metric.LINE, covered, missed);
                    BRANCH_CACHE[getCacheIndex(covered, missed)] = new Coverage(Metric.BRANCH, covered, missed);
                    INSTRUCTION_CACHE[getCacheIndex(covered, missed)] = new Coverage(Metric.INSTRUCTION, covered, missed);
                    MUTATION_CACHE[getCacheIndex(covered, missed)] = new Coverage(Metric.MUTATION, covered, missed);
                    MCDC_PAIR_CACHE[getCacheIndex(covered, missed)] = new Coverage(Metric.MCDC_PAIR, covered, missed);
                    FUNCTION_CALL_CACHE[getCacheIndex(covered, missed)] = new Coverage(Metric.FUNCTION_CALL, covered, missed);
                }
            }
        }

        private static int getCacheIndex(final int covered, final int missed) {
            return covered * CACHE_SIZE + missed;
        }

        @CheckForNull
        private Metric metric;
        private int covered;
        private boolean isCoveredSet;
        private int missed;
        private boolean isMissedSet;
        private int total;
        private boolean isTotalSet;

        /**
         * Creates a new {@link CoverageBuilder} with all properties unset.
         */
        public CoverageBuilder() {
        }

        /**
         * Creates a new {@link CoverageBuilder} with the specified metric. All other properties are unset.
         *
         * @param metric
         *         the metric to set
         */
        public CoverageBuilder(@CheckForNull final Metric metric) {
            this.metric = metric;
        }

        /**
         * Creates a new {@link CoverageBuilder} with all properties set to the value of the provided existing
         * instance.
         *
         * @param existing
         *         the existing coverage to copy all properties from
         */
        public CoverageBuilder(final Coverage existing) {
            withMetric(existing.getMetric());
            withCovered(existing.getCovered());
            withMissed(existing.getMissed());
        }

        /**
         * Sets the metric of the coverage.
         *
         * @param metric
         *         the metric of the coverage
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withMetric(final Metric metric) {
            this.metric = metric;
            return this;
        }

        /**
         * Sets the metric of the coverage.
         *
         * @param metric
         *         the metric of the coverage
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withMetric(final String metric) {
            return withMetric(Metric.valueOf(metric));
        }

        /**
         * Sets the number of total items.
         *
         * @param total
         *         the number of total items
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withTotal(final int total) {
            this.total = total;
            isTotalSet = true;
            return this;
        }

        /**
         * Sets the number of total items.
         *
         * @param total
         *         the number of total items
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withTotal(final String total) {
            return withTotal(CoverageParser.parseInteger(total));
        }

        /**
         * Sets the number of covered items.
         *
         * @param covered
         *         the number of covered items
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withCovered(final int covered) {
            Ensure.that(covered >= 0).isTrue("No negative values allowed for covered items: %s", covered);

            this.covered = covered;
            isCoveredSet = true;
            return this;
        }

        /**
         * Sets the number of covered items.
         *
         * @param covered
         *         the number of covered items
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withCovered(final String covered) {
            return withCovered(CoverageParser.parseInteger(covered));
        }

        /**
         * Sets the number of missed items.
         *
         * @param missed
         *         the number of missed items
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withMissed(final int missed) {
            Ensure.that(missed >= 0).isTrue("No negative values allowed for missed items: %s", missed);

            this.missed = missed;
            isMissedSet = true;
            return this;
        }

        /**
         * Sets the number of missed items.
         *
         * @param missed
         *         the number of missed items
         *
         * @return this
         */
        @CanIgnoreReturnValue
        public CoverageBuilder withMissed(final String missed) {
            return withMissed(CoverageParser.parseInteger(missed));
        }

        /**
         * Creates the new {@link Coverage} instance.
         *
         * @return the new instance
         */
        @SuppressWarnings("PMD.CyclomaticComplexity")
        public Coverage build() {
            if (isCoveredSet && isMissedSet && isTotalSet) {
                throw new IllegalArgumentException(
                        "Setting all three values covered, missed, and total is not allowed, just select two of them.");
            }
            if (isTotalSet) {
                if (isCoveredSet) {
                    return createOrGetCoverage(covered, total - covered);
                }
                else if (isMissedSet) {
                    return createOrGetCoverage(total - missed, missed);
                }
            }
            else {
                if (isCoveredSet && isMissedSet) {
                    return createOrGetCoverage(covered, missed);
                }
            }
            throw new IllegalArgumentException("Exactly two properties have to be set.");
        }

        @SuppressWarnings({"checkstyle:HiddenField", "ParameterHidesMemberVariable", "PMD.CyclomaticComplexity"})
        private Coverage createOrGetCoverage(final int covered, final int missed) {
            if (metric == null) {
                throw new IllegalArgumentException("No metric defined.");
            }
            if (covered < CACHE_SIZE && missed < CACHE_SIZE) {
                switch (metric) {
                    case LINE -> {
                        return LINE_CACHE[getCacheIndex(covered, missed)];
                    }
                    case BRANCH -> {
                        return BRANCH_CACHE[getCacheIndex(covered, missed)];
                    }
                    case INSTRUCTION -> {
                        return INSTRUCTION_CACHE[getCacheIndex(covered, missed)];
                    }
                    case MUTATION -> {
                        return MUTATION_CACHE[getCacheIndex(covered, missed)];
                    }
                    case MCDC_PAIR -> {
                        return MCDC_PAIR_CACHE[getCacheIndex(covered, missed)];
                    }
                    case FUNCTION_CALL -> {
                        return FUNCTION_CALL_CACHE[getCacheIndex(covered, missed)];
                    }
                    default -> {
                        // use constructor to create an instance
                    }
                }
            }
            return new Coverage(metric, covered, missed);
        }

        /**
         * Increments the number of covered items by 1.
         */
        public void incrementCovered() {
            incrementCovered(1);
        }

        /**
         * Increments the number of covered items by the specified amount.
         *
         * @param amount
         *         the amount to increment
         */
        public void incrementCovered(final int amount) {
            withCovered(covered + amount);
        }

        /**
         * Increments the number of missed items by 1.
         */
        public void incrementMissed() {
            incrementMissed(1);
        }

        /**
         * Increments the number of missed items by the specified amount.
         *
         * @param amount
         *         the amount to increment
         */
        public void incrementMissed(final int amount) {
            withMissed(missed + amount);
        }
    }
}
