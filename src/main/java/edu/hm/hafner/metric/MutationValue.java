package edu.hm.hafner.metric;

import java.util.Objects;

/**
 * Leaf which represents a mutation.
 *
 * @author Melissa Bauer
 */
// FIXME: can we make this class immutable?
public final class MutationValue extends Value {
    private static final long serialVersionUID = -7725185756332899065L;

    /** Null object that indicates that the code coverage has not been measured. FIXME: Constructor? */
    public static final MutationValue NO_MUTATIONS = new MutationValue(false, MutationStatus.NO_COVERAGE);

    private final boolean isDetected;
    private final MutationStatus status;
    private int lineNumber;
    private Mutator mutator;
    private String killingTest;

    /**
     * Creates a new MutationLeaf.
     *
     * @param isDetected
     *         if mutation was detected
     * @param status
     *         of the mutation
     */
    public MutationValue(final boolean isDetected, final MutationStatus status) {
        super(Metric.MUTATION);

        this.isDetected = isDetected;
        this.status = status;
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

    /**
     * Creates and returns a new {@link MutationResult} .
     *
     * @return the result as {@link MutationResult}
     */
    public MutationResult getResult() {
        if (isKilled()) {
            return new MutationResult(1, 0);
        }
        else {
            return new MutationResult(0, 1);
        }
    }


    // TODO: wie soll die Information repräsentiert werden?
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
    public MutationValue add(final Value additional) {
        return this; // FIXME: implement method
    }

    @Override
    public MutationValue max(final Value other) {
        return this; // FIXME: implement method
    }

    @Override
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
        MutationValue that = (MutationValue) o;
        return isDetected == that.isDetected && lineNumber == that.lineNumber && status == that.status
                && mutator == that.mutator && Objects.equals(killingTest, that.killingTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isDetected, status, lineNumber, mutator, killingTest);
    }
}
