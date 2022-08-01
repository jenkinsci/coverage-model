package edu.hm.hafner.model;

/**
 * A {@link Node} for a specific package. It converts a package structure to a corresponding path structure.
 *
 * @author Ullrich Hafner
 */
public class PackageNode extends Node {
    private static final long serialVersionUID = 8236436628673022634L;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public PackageNode(final String name) {
        super(Metric.PACKAGE, name);
    }

    @Override
    public String getPath() {
        return mergePath(getName().replaceAll("\\.", "/"));
    }

    @Override
    public Node copyEmpty() {
        return new PackageNode(getName());
    }
}
