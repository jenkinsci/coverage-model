package edu.hm.hafner.metric;

import org.apache.commons.lang3.StringUtils;

/**
 * A {@link Node} for a specific package. It converts a package structure to a corresponding path structure.
 *
 * @author Ullrich Hafner
 */
public final class PackageNode extends Node {
    private static final long serialVersionUID = 8236436628673022634L;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public PackageNode(final String name) {
        super(Metric.PACKAGE, StringUtils.defaultIfBlank(name, "-"));
    }

    @Override
    public String getPath() {
        return mergePath(getName().replaceAll("\\.", "/"));
    }

    static PackageNode appendPackage(final PackageNode localChild, final PackageNode localParent) {
        localParent.addChild(localChild);
        return localParent;
    }

    @Override
    public Node copyEmpty() {
        return new PackageNode(getName());
    }
}
