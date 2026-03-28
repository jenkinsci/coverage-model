package edu.hm.hafner.coverage;

/**
 * Used to simplify FileNode.
 *
 * @author Tim Schneider
 */
class CoverageMetricsValues {
    /* Three values to manage */
    private int covered;
    private int missed;
    private int total;

    /**
     * Creates a new {@link CoverageMetricsValues} instance from the values for covered and missing. These values
     * represent the covered and missed metrics.
     *
     * @param covered
     *         the coverage count
     * @param missed
     *         the missed count
     */
    CoverageMetricsValues(final int covered, final int missed) {
        this.covered = covered;
        this.missed = missed;
        this.total = covered + missed;
    }

    /**
     * Returns the total count.
     *
     * @return the current total count
     */
    int getTotal() {
        return this.total;
    }

    /**
     * Returns the covered count.
     *
     * @return the current covered count
     */
    int getCovered() {
        return this.covered;
    }

    /**
     * Returns the missed count.
     *
     * @return the current missed count
     */
    int getMissed() {
        return this.missed;
    }

    /**
     * Clears the current missed count.
     */
    void clearMissed() {
        this.missed = 0;
    }

    /**
     * Sets the covered count from the max of the totals from this instance and other.
     *
     * @param other
     *         another CoverageMetricsValues to compare against
     */
    void setCoveredFromMax(final CoverageMetricsValues other) {
        this.covered = Math.max(this.total, other.getTotal());
    }

    /**
     * Sets the total from the current covered count.
     */
    void setTotalFromCovered() {
        this.total = this.covered;
    }

    /**
     * Sets the counters from a best-guess normalization if two reports provide different totals.
     * We keep the maximum number of covered branches and align the total to the maximum reported value.
     *
     * @param other
     *         another CoverageMetricsValues to compare against
     */
    void setBestGuessFromMaxCoveredAndTotal(final CoverageMetricsValues other) {
        this.covered = Math.max(this.covered, other.getCovered());
        this.total = Math.max(this.total, other.getTotal());
        this.missed = this.total - this.covered;
    }

    /**
     * Check to see if there is any total for this instance.
     *
     * @return - boolean of if total > 1
     *
     */
    boolean hasAnyInfo() {
        return total > 1;
    }

    /**
     * Checks to see if the instance total is not equal to the input total.
     *
     * @param other
     *         another CoverageMetricsValues to compare against
     *
     * @return - boolean if the compared totals are not equal
     */
    boolean totalsNotEqual(final CoverageMetricsValues other) {
        return total != other.total;
    }

    /**
     * Checks to see if the instance total is equal to instances covered.
     *
     * @return - boolean total count equals covered count
     */
    boolean noMissing() {
        return total == covered;
    }

    /**
     * Calculates the maximum of this instance vs. input covered count.
     *
     * @param other
     *         another CoverageMetricsValues to compare against
     *
     * @return - integer maximum between the two covered counts
     */
    int getMaxCovered(final CoverageMetricsValues other) {
        return Math.max(this.covered, other.getCovered());
    }

    /**
     * Checks to see if the instance total is not equal to the input total.
     *
     * @param other
     *         another CoverageMetricsValues to compare against
     *
     * @return - integer minimum between the two missed counts
     */
    int getMinMissed(final CoverageMetricsValues other) {
        return Math.min(this.missed, other.getMissed());
    }
}
