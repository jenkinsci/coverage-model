package edu.hm.hafner.coverage;

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
            return EMPTY_NAME;
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

    @SuppressWarnings("CanIgnoreReturnValueSuggester")
    static PackageNode appendPackage(final PackageNode localChild, final PackageNode localParent) {
        localParent.addChild(localChild);
        return localParent;
    }

    @Override
    public PackageNode copy() {
        return new PackageNode(getName());
    }

    @Override
    public boolean isAggregation() {
        return true;
    }
}
