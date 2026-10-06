package edu.hm.hafner.coverage;

/**
 * Represents all possible outcomes for mutations.
 *
 * @author Melissa Bauer
 */
public enum MutationStatus {
    KILLED(LineCoverage.COVERED),
    SURVIVED(LineCoverage.COVERED),
    NO_COVERAGE(LineCoverage.MISSED),
    NON_VIABLE(LineCoverage.UNKNOWN),
    TIMED_OUT(LineCoverage.COVERED),
    MEMORY_ERROR(LineCoverage.UNKNOWN),
    RUN_ERROR(LineCoverage.UNKNOWN);

    private final LineCoverage lineCoverage;

    MutationStatus(final LineCoverage lineCoverage) {
        this.lineCoverage = lineCoverage;
    }

    public boolean isDetected() {
        return this == KILLED;
    }

    public boolean isNotDetected() {
        return this == SURVIVED || this == NO_COVERAGE;
    }

    public boolean isCovered() {
        return lineCoverage == LineCoverage.COVERED;
    }

    public boolean isMissed() {
        return lineCoverage == LineCoverage.MISSED;
    }

    enum LineCoverage {
        COVERED,
        MISSED,
        UNKNOWN
    }
}
