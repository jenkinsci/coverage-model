package edu.hm.hafner.coverage;

/**
 * A {@link Node} which represents the top level node in the tree. Typically, such a node aggregates all
 * {@link ModuleNode modules} of a software project.
 *
 * @author Ullrich Hafner
 */
public final class ContainerNode extends Node {
    private static final long serialVersionUID = 6304208788771158650L;

    /**
     * Creates a new {@link ContainerNode} with the given name.
     *
     * @param name
     *         the name of the node
     */
    public ContainerNode(final String name) {
        super(Metric.CONTAINER, name);
    }

    @Override
    public ContainerNode copy() {
        return new ContainerNode(getName());
    }

    @Override
    public boolean isAggregation() {
        return true;
    }
}
