package edu.hm.hafner.metric;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

/**
 * A {@link Node} for a specific package. It converts a package structure to a corresponding path structure.
 *
 * @author Ullrich Hafner
 */
public final class PackageNode extends Node {
    private static final long serialVersionUID = 8236436628673022634L;

    /**
     * Creates a new coverage item node with the given name. / and \ in the name are replaced with a .
     *
     * @param name
     *         the human-readable name of the node
     */
    public PackageNode(final String name) {
        super(Metric.PACKAGE, StringUtils.defaultIfBlank(name, "-"));
    }

    /**
     * Creates a new coverage item node with the given name. / and \ in the name are replaced with a .
     *
     * @param name
     *         the human-readable name of the node
     * @param values
     *         the values to add
     *
     * @see #addValue(Value)
     * @see #addAllValues(Collection)
     */
    public PackageNode(final String name, final Collection<Value> values) {
        this(name);

        addAllValues(values);
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
    public PackageNode copy() {
        return new PackageNode(getName());
    }

    /**
     * Replace all / and \ with a .
     *
     * @param name
     *         the package name to normalize
     *
     * @return the normalized name
     */
    public static String normalizePackageName(final String name) {
        if (name != null && !name.isBlank()) {
            if (name.contains("/")) {
                return StringUtils.replace(name, "/", ".");
            }
            else if (name.contains("\\")) {
                return StringUtils.replace(name, "\\", ".");
            }
            else {
                return name;
            }
        }
        else {
            return "-";
        }
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) <%d>", getMetric(), getName(), getPath(), getChildren().size());
    }
}
