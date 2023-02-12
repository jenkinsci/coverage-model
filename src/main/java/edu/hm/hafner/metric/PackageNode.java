package edu.hm.hafner.metric;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A {@link Node} for a specific package. It converts a package structure to a corresponding path structure.
 *
 * @author Ullrich Hafner
 */
public final class PackageNode extends Node {
    private static final long serialVersionUID = 8236436628673022634L;

    /**
     * Replace slashes and backslashes with a dot so that package names use the typical format of packages or
     * namespaces.
     *
     * @param name
     *         the package name to normalize
     *
     * @return the normalized name or "-" if the name is empty or {@code null}
     */
    public static String normalizePackageName(@CheckForNull final String name) {
        if (StringUtils.isNotBlank(name)) {
            return StringUtils.replaceEach(name, new String[] {"/", "\\"}, new String[] {".", "."});
        }
        else {
            return Node.EMPTY_NAME;
        }
    }

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param name
     *         the human-readable name of the node, see {@link #normalizePackageName(String)}
     */
    public PackageNode(@CheckForNull final String name) {
        super(Metric.PACKAGE, normalizePackageName(name));
    }

    /**
     * Creates a new coverage item node with the given name. / and \ in the name are replaced with a .
     *
     * @param name
     *         the human-readable name of the node, see {@link #normalizePackageName(String)}
     * @param values
     *         the values to add
     *
     * @see #addValue(Value)
     * @see #addAllValues(Collection)
     */
    public PackageNode(@CheckForNull final String name, final Collection<Value> values) {
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

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) <%d>", getMetric(), getName(), getPath(), getChildren().size());
    }

    public FileNode createFileNode(final String fileName) {
        var fileNode = new FileNode(fileName);
        addChild(fileNode);
        return fileNode;
    }

    public FileNode findOrCreateFileNode(final String fileName) {
        return findFile(fileName).orElseGet(() -> createFileNode(fileName));
    }
}
