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

    /**
     * Returns whether a mutation with this status has been detected by the tests. Besides killed mutations, mutations
     * that caused a timeout are detected as well, since the tests would never complete.
     *
     * @return {@code true} if a mutation with this status has been detected, {@code false} otherwise
     */
    public boolean isDetected() {
        return this == KILLED || this == TIMED_OUT;
    }

    /**
     * Returns whether a mutation with this status has not been detected by the tests, either because the mutation
     * survived or because the mutated code is not covered by any test.
     *
     * @return {@code true} if a mutation with this status has not been detected, {@code false} otherwise
     */
    public boolean isNotDetected() {
        return this == SURVIVED || this == NO_COVERAGE;
    }

    /**
     * Returns whether a mutation with this status is valid, i.e., whether it has been either detected or not detected.
     * Invalid mutations (non-viable mutations, memory errors, or run errors) could not be evaluated by the tests. They
     * are not part of the mutation coverage and the test strength.
     *
     * @return {@code true} if a mutation with this status is valid, {@code false} otherwise
     */
    public boolean isValid() {
        return isDetected() || isNotDetected();
    }

    /**
     * Returns whether a mutation with this status is part of the test strength, i.e., whether the mutation is valid and
     * has been covered by at least one test.
     *
     * @return {@code true} if a mutation with this status is part of the test strength, {@code false} otherwise
     */
    public boolean isRelevantForTestStrength() {
        return isValid() && this != NO_COVERAGE;
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
