package edu.hm.hafner.metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A {@link Node} for a specific method.
 *
 * @author Florian Orendi
 */
public final class MethodNode extends Node {
    private static final long serialVersionUID = -5765205034179396434L;

    private final String signature;
    /** The line number where the code of method begins (not including the method head). */
    private final int lineNumber;
    // FIXME: check where these elements are best placed
    private final List<Mutation> mutations = new ArrayList<>();

    /**
     * Creates a new method node with the given name. The line number will be set to 0.
     *
     * @param name
     *         The human-readable name of the node
     * @param signature
     *         The signature of the method
     */
    public MethodNode(final String name, final String signature) {
        this(name, signature, 0);
    }

    /**
     * Creates a new item node with the given name.
     *
     * @param name
     *         The human-readable name of the node
     * @param signature
     *         The signature of the method
     * @param lineNumber
     *         The line number where the method begins (not including the method head)
     */
    public MethodNode(final String name, final String signature, final int lineNumber) {
        super(Metric.METHOD, name);

        this.signature = signature;
        this.lineNumber = lineNumber;
    }

    /**
     * Adds a mutation to the method.
     *
     * @param mutation the mutation to add
     */
    public void addMutation(final Mutation mutation) {
        mutations.add(mutation);
    }

    public List<Mutation> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    @Override
    public Node copy() {
        return new MethodNode(getName(), getSignature(), getLineNumber());
    }

    /**
     * Checks whether the line number is valid.
     *
     * @return {@code true} if the line number is valid, else {@code false}
     */
    public boolean hasValidLineNumber() {
        return lineNumber > 0;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getSignature() {
        return signature;
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
        MethodNode that = (MethodNode) o;
        return lineNumber == that.lineNumber
                && Objects.equals(signature, that.signature)
                && Objects.equals(mutations, that.mutations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), signature, lineNumber, mutations);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s(%d) <%s>", getMetric(), getName(), getLineNumber(), getSignature());
    }
}
