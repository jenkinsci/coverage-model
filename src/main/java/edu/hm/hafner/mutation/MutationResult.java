package edu.hm.hafner.mutation;

import java.util.Objects;

/**
 * Represents the summary of the mutation tree.
 */
public class MutationResult {

    private final int killed;
    private final int survived;

    /**
     * Creates a new object with 0 killed and 0 survived mutations.
     */
    public MutationResult() {
        this.killed = 0;
        this.survived = 0;
    }

    /**
     * Creates a new object with given killed and survived mutations.
     *
     * @param killed
     *         amount of killed mutations
     * @param survived
     *         amount of survived mutations
     */
    public MutationResult(final int killed, final int survived) {
        this.killed = killed;
        this.survived = survived;
    }

    public int getKilled() {
        return killed;
    }

    public int getSurvived() {
        return survived;
    }

    /**
     * Adds another {@link MutationResult} to the current and returns a new object.
     *
     * @param mutationResult
     *         another mutationResult
     *
     * @return a new object with summed up values
     */
    public MutationResult add(final MutationResult mutationResult) {
        return new MutationResult(killed + mutationResult.getKilled(),
                survived + mutationResult.survived);
    }

    @Override
    public String toString() {
        return "MutationResult{"
                + "killed=" + killed
                + ", survived=" + survived
                + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MutationResult that = (MutationResult) o;
        return killed == that.killed && survived == that.survived;
    }

    @Override
    public int hashCode() {
        return Objects.hash(killed, survived);
    }
}
