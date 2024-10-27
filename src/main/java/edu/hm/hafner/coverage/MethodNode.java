package edu.hm.hafner.coverage;

import java.io.Serial;
import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A {@link Node} for a specific method.
 *
 * @author Florian Orendi
 */
public final class MethodNode extends Node {
    @Serial
    private static final long serialVersionUID = -5765205034179396434L;

    private final String signature;
    private /* almost final */ String methodName; // @since 0.25.0
    /** The line number where the code of method begins (not including the method head). */
    private final int lineNumber;

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
        super(Metric.METHOD, name + signature);

        this.signature = signature;
        this.methodName = name;
        this.lineNumber = lineNumber;
    }

    @SuppressFBWarnings(value = "RCN", justification = "Value might be null in old serializations")
    private Object readResolve() {
        if (methodName == null) { // serialization of old versions
            methodName = getName();
            setName(methodName + signature);
        }

        return this;
    }

    @Override
    public Node copy() {
        return new MethodNode(getMethodName(), getSignature(), getLineNumber());
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

    public String getMethodName() {
        return methodName;
    }

    @Override
    public boolean isAggregation() {
        return false;
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
        var that = (MethodNode) o;
        return lineNumber == that.lineNumber
                && Objects.equals(signature, that.signature)
                && Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), signature, methodName, lineNumber);
    }

    @Override
    public String toString() {
        return "[%s] %s <%s>".formatted(getMetric(), getName(), getLineNumber());
    }
}
