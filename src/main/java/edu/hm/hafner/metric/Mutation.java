package edu.hm.hafner.metric;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class which represents a mutation of the PIT Mutation Testing tool.
 *
 * @author Melissa Bauer
 */
public final class Mutation implements Serializable {
    private static final long serialVersionUID = -7725185756332899065L;

    private final boolean isDetected;
    private final MutationStatus status;
    private int lineNumber;
    private Mutator mutator;
    private String killingTest;

    /**
     * Creates a new {@link Mutation}.
     *
     * @param isDetected
     *         if the mutation was detected
     * @param status
     *         of the mutation
     */
    public Mutation(final boolean isDetected, final MutationStatus status) {
        this.isDetected = isDetected;
        this.status = status;
    }

    /**
     * Creates a new {@link Mutation} with all fields set at the beginning.
     *
     * @param isDetected if the mutation was detected
     * @param status of the mutation
     * @param lineNumber of the mutation
     * @param mutator which mutator was used
     * @param killingTest test which killed the mutation or null
     */
    public Mutation(final boolean isDetected, final MutationStatus status, final int lineNumber, final Mutator mutator,
            final String killingTest) {
        this.isDetected = isDetected;
        this.status = status;
        this.lineNumber = lineNumber;
        this.mutator = mutator;
        this.killingTest = killingTest;
    }

    public boolean isDetected() {
        return isDetected;
    }

    public MutationStatus getStatus() {
        return status;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public Mutator getMutator() {
        return mutator;
    }

    public void setMutator(final Mutator mutator) {
        this.mutator = mutator;
    }

    public void setKillingTest(final String killingTest) {
        this.killingTest = killingTest;
    }

    public String getKillingTest() {
        return killingTest;
    }

    /**
     * Returns if the mutation was killed.
     *
     * @return if the mutation was killed
     */
    public boolean isKilled() {
        return status.equals(MutationStatus.KILLED);
    }

    @Override
    public String toString() {
        return "[Mutation]:"
                + " isDetected=" + isDetected
                + ", status=" + status
                + ", lineNumber=" + lineNumber
                + ", mutator=" + mutator
                + ", killingTest='" + killingTest + "'";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Mutation mutation = (Mutation) o;
        return isDetected == mutation.isDetected && lineNumber == mutation.lineNumber && status == mutation.status
                && mutator == mutation.mutator && Objects.equals(killingTest, mutation.killingTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isDetected, status, lineNumber, mutator, killingTest);
    }
}
