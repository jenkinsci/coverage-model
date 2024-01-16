package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.MutationStatus.LineCoverage.*;

/**
 * Represents all possible outcomes for mutations.
 *
 * @author Melissa Bauer
 */
public enum MutationStatus {
    KILLED(COVERED),
    SURVIVED(COVERED),
    NO_COVERAGE(MISSED),
    NON_VIABLE(UNKNOWN),
    TIMED_OUT(COVERED),
    MEMORY_ERROR(UNKNOWN),
    RUN_ERROR(UNKNOWN);

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
        return lineCoverage == COVERED;
    }

    public boolean isMissed() {
        return lineCoverage == MISSED;
    }

    enum LineCoverage {
        COVERED,
        MISSED,
        UNKNOWN
    }
}
