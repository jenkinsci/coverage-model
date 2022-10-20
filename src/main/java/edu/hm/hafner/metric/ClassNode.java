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
     * Finds the metric with the given name starting from this node.
     *
     * @param searchName
     *         the metric to search for
     * @param searchSignature
     *         the name of the node
     *
     * @return the result if found
     */
    public Optional<MethodNode> findMethodNode(final String searchName, final String searchSignature) {
        return getChildren().stream()
                .map(MethodNode.class::cast)
                .filter(node -> node.getName().equals(searchName) && node.getSignature().equals(searchSignature))
                .findAny();
    }

    @Override
    public ClassNode copy() {
        return new ClassNode(getName());
    }
}
