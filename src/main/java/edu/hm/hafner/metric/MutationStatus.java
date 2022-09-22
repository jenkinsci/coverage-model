package edu.hm.hafner.metric;

/**
 * Represents all possible outcomes for mutations.
 *
 * @author Melissa Bauer
 */
public enum MutationStatus {
    KILLED,
    SURVIVED,
    NO_COVERAGE,
    NON_VIABLE,
    TIMED_OUT,
    MEMORY_ERROR,
    RUN_ERROR
}
