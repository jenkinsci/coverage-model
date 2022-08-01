package edu.hm.hafner.mutation;

import java.util.Objects;

import edu.hm.hafner.model.Metric;
import edu.hm.hafner.model.Leaf;

/**
 * Leaf which represents a mutation.
 *
 * @author Melissa Bauer
 */
public class MutationLeaf extends Leaf {
    private static final long serialVersionUID = -7725185756332899065L;

    private final boolean isDetected;
    private MutationStatus status;
    private int lineNumber;
    private Mutator mutator;
    private String killingTest;

    public boolean isDetected() {
        return isDetected;
    }

    public MutationStatus getStatus() {
        return status;
    }

    public void setStatus(final MutationStatus status) {
        this.status = status;
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
     * Creates a new MutationLeaf.
     *
     * @param isDetected
     *         if mutation was detected
     * @param status
     *         of the mutation
     */
    public MutationLeaf(final boolean isDetected, final MutationStatus status) {
        super(Metric.MUTATION);
        this.isDetected = isDetected;
        this.status = status;
    }

    /**
     * Returns if the mutation was killed.
     *
     * @return if the mutation was killed
     */
    public boolean isKilled() {
        return status.equals(MutationStatus.KILLED);
    }

    @Override // TODO: wie soll die Information repräsentiert werden?
    public String toString() {
        return "[Mutation]:" +
                "isDetected=" + isDetected +
                ", status=" + status +
                ", lineNumber=" + lineNumber +
                ", mutator=" + mutator +
                ", killingTest='" + killingTest + '\'' +
                '}';
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
        MutationLeaf that = (MutationLeaf) o;
        return isDetected == that.isDetected && lineNumber == that.lineNumber && status == that.status
                && mutator == that.mutator && killingTest.equals(that.killingTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), isDetected, status, lineNumber, mutator, killingTest);
    }
}
