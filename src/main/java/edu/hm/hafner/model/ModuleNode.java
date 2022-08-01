package edu.hm.hafner.model;

/**
 * A {@link Node} which represents the first node in the data tree.
 *
 * @author Melissa Bauer
 */
public class ModuleNode extends Node {
    private static final long serialVersionUID = 2393265115219226404L;

    /**
     * Creates a new module node with the given name.
     *
     * @param name
     *         the name of the module
     */
    public ModuleNode(final String name) {
        super(Metric.MODULE, name);
    }
}
