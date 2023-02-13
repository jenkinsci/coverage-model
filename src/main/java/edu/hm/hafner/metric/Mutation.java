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

    private final boolean detected;
    private final MutationStatus status;
    private int lineNumber;
    private Mutator mutator;
    private String killingTest;
    private final String mutatedClass;
    private final String method;
    private final String signature;
    private final String description;

    public Mutation(final boolean detected, final MutationStatus status, final int lineNumber, final Mutator mutator,
            final String killingTest, final String mutatedClass,
            final String method, final String signature, final String description) {
        this.detected = detected;
        this.status = status;
        this.lineNumber = lineNumber;
        this.mutator = mutator;
        this.killingTest = killingTest;
        this.mutatedClass = mutatedClass;
        this.method = method;
        this.signature = signature;
        this.description = description;
    }

    public String getMutatedClass() {
        return mutatedClass;
    }

    public String getMethod() {
        return method;
    }

    public String getSignature() {
        return signature;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDetected() {
        return detected;
    }

    public MutationStatus getStatus() {
        return status;
    }

    public boolean isValid() {
        return isCovered() || isMissed();
    }

    public boolean isCovered() {
        return status.isCovered();
    }

    public boolean isMissed() {
        return status.isMissed();
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
     * Returns if the mutation has survived.
     *
     * @return if the mutation has survived
     */
    public boolean hasSurvived() {
        return status.equals(MutationStatus.SURVIVED);
    }

    @Override
    public String toString() {
        return "[Mutation]:"
                + " isDetected=" + detected
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
        return detected == mutation.detected && lineNumber == mutation.lineNumber && status == mutation.status
                && mutator == mutation.mutator && Objects.equals(killingTest, mutation.killingTest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detected, status, lineNumber, mutator, killingTest);
    }
}
