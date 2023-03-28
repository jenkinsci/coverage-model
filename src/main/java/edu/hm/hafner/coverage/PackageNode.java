package edu.hm.hafner.coverage;

import java.util.Collection;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A {@link Node} for a specific package. It converts a package structure to a corresponding path structure.
 *
 * @author Ullrich Hafner
 */
public final class PackageNode extends Node {
    private static final long serialVersionUID = 8236436628673022634L;

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("[a-z]+[.\\w]*");

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

    @SuppressFBWarnings("MODIFICATION_AFTER_VALIDATION")
    @Override
    public String getPath() {
        String localPath;
        if (PACKAGE_PATTERN.matcher(getName()).matches()) {
            localPath = getName().replaceAll("\\.", "/");
        }
        else {
            localPath = getName();
        }
        return mergePath(localPath);
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
     * Create a new file node with the given name and add it to the list of children.
     *
     * @param fileName
     *         the file name
     *
     * @return the created and linked file node
     */
    public FileNode createFileNode(final String fileName) {
        var fileNode = new FileNode(fileName);
        addChild(fileNode);
        return fileNode;
    }

    /**
     * Searches for the specified file node. If the file node is not found then a new file node will be created
     * and linked to this package node.
     *
     * @param fileName
     *         the file name
     *
     * @return the existing or created file node
     * @see #createFileNode(String)
     */
    public FileNode findOrCreateFileNode(final String fileName) {
        return findFile(fileName).orElseGet(() -> createFileNode(fileName));
    }

    /**
     * Searches for the specified class node. If the class node is not found then a new class node will be created and
     * linked to this file node.
     *
     * @param className
     *         the class name
     *
     * @return the created and linked class node
     * @see #createClassNode(String)
     */
    public ClassNode findOrCreateClassNode(final String className) {
        return findClass(className).orElseGet(() -> createClassNode(className));
    }

    /**
     * Create a new class node with the given name and add it to the list of children.
     *
     * @param className
     *         the class name
     *
     * @return the created and linked class node
     */
    public ClassNode createClassNode(final String className) {
        var classNode = new ClassNode(className);
        addChild(classNode);
        return classNode;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) <%d>", getMetric(), getName(), getPath(), getChildren().size());
    }
}
