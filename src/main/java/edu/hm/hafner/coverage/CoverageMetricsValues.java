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
     * Creates a new {@link CoverageMetricsValues} instance from the values for covered/missing. 
     * These values represent the coveraged and missed metrics
     *
     * @param covered  the coverage count
     * @param missed   the missed count
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
    public int getTotal() {
        return this.total;
    }
    
    /**
     * Returns the covered count.
     *
     * @return the current covered count 
     */
    public int getCovered() {
        return this.covered;
    }
    
    /**
     * Returns the missed count.
     *
     * @return the current missed count 
     */
    public int getMissed() {
        return this.missed;
    }
    
    /**
     * Sets the total count.
     *
     * @param total
     *          the total count to be set
     */
    public void setTotal(final int total) {
        this.total = total;
    }
    
    /**
     * Sets the covered count.
     *
     * @param covered
     *          the covered count to be set
     */
    public void setCovered(final int covered) {
        this.covered = covered;
    }
    
    /**
     * Clears the current missed count.
     */
    public void clearMissed() {
        this.missed = 0;
    }   
    
    /**
     * Sets the covered count from the max of the totals from this instance and other.
     *
     * @param other
     *          another CoverageMetricsValues to compare against
     */
    public void setCoveredFromMax(final CoverageMetricsValues other) {
        this.covered = Math.max(this.total, other.getTotal());
    }
    
    /**
     * Sets the total from the current covered count.
     */    
    public void setTotalFromCovered() {
        this.total = this.covered;
    }
    
    /**
     * Check to see if there is any total for this instance.
     *
     * @return - boolean of if total > 1
     *          
     */    
    public boolean hasAnyInfo() {
        return total > 1;
    }
    
    /**
     * Checks to see if instance total is not equal to the input total.
     *
     * @param other
     *          another CoverageMetricsValues to compare against
     *
     * @return - boolean if the compated totals are not equal
     */
    public boolean totalsNotEqual(final CoverageMetricsValues other) {
        return total != other.total;
    }

    /**
     * Checks to see if instance total is equal to instances covered.
     *
     * @return - boolean total count equals covered count
     */
    public boolean noMissing() {
        return total == covered;
    }

    /**
     * Calculates the maximum of this instance vs input covered count.
     *
     * @param other
     *          another CoverageMetricsValues to compare against
     *
     * @return - integer maximum between the two covered counts
     */
    public int getMaxCovered(final CoverageMetricsValues other) {
        return Math.max(this.covered, other.getCovered());
    }

    /**
     * Checks to see if instance total is not equal to the input total.
     *
     * @param other
     *          another CoverageMetricsValues to compare against
     *
     * @return - integer minumum between the two missed counts
     */
    public int getMinMissed(final CoverageMetricsValues other) {
        return Math.min(this.missed, other.getMissed());
    }
}
