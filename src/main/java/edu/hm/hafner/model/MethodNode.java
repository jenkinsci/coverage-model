package edu.hm.hafner.model;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A {@link Node} for a specific method.
 *
 * @author Florian Orendi
 */
public class MethodNode extends Node {
    private static final long serialVersionUID = -5765205034179396434L;

    /**
     * The line number where the code of method begins (not including the method head).
     */
    private final int lineNumber;

    /**
     * Creates a new item node with the given name.
     *
     * @param name
     *         The human-readable name of the node
     * @param lineNumber
     *         The line number where the method begins (not including the method head)
     */
    public MethodNode(final String name, final int lineNumber) {
        super(Metric.METHOD, name);

        this.lineNumber = lineNumber;
    }

    /**
     * Creates a new method node with the given name.
     *
     * @param name
     *         The human-readable name of the node
     */
    public MethodNode(final String name) {
        super(Metric.METHOD, name);
        this.lineNumber = 0;
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

    @Override
    public boolean equals(@CheckForNull final Object o) {
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
        return lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lineNumber);
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" (%d)", lineNumber);
    }
}
