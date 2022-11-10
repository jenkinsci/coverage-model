package edu.hm.hafner.metric;

import java.util.Optional;

/**
 * A {@link Node} for a specific class.
 */
public final class ClassNode extends Node {
    private static final long serialVersionUID = 1621410859864978552L;

    /**
     * Creates a new {@link ClassNode} with the given name.
     *
     * @param name
     *         the name of the class
     */
    public ClassNode(final String name) {
        super(Metric.CLASS, name);
    }

    /**
     * Searches for a method within this class (or within a nested class) with the given name and signature.
     *
     * @param searchName
     *         the name of the method
     * @param searchSignature
     *         the signature of the method
     *
     * @return the first matching method or an empty result, if no such method exists
     */
    public Optional<MethodNode> findMethodNode(final String searchName, final String searchSignature) {
        return getAll(Metric.METHOD).stream()
                .map(MethodNode.class::cast)
                .filter(node -> node.getName().equals(searchName)
                        && node.getSignature().equals(searchSignature))
                .findAny();
    }

    @Override
    public ClassNode copy() {
        return new ClassNode(getName());
    }
}
