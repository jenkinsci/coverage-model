package edu.hm.hafner.metric;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link Node} which represents a module of a project.
 *
 * @author Melissa Bauer
 */
public final class ModuleNode extends Node {
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

    @Override
    public ModuleNode copyEmpty() {
        return new ModuleNode(getName());
    }

    /**
     * Splits flat packages into a package hierarchy. Changes the internal tree structure of package nodes in place.
     * <p>
     * Examples:
     * </p>
     * <ul>
     *     <li>
     *         A package name {@code "edu"} will produce a single node with the name {@code "edu"}.
     *     </li>
     *     <li>
     *         A package name {@code "edu.hm.hafner"} will produce three package nodes, that are linked together,
     *         starting with the {@code "edu"} package ({@code "edu" -> "hm" -> "hafner"}).
     *     </li>
     * </ul>
     */
    public void splitPackages() {
        List<Node> allPackages = getChildren().stream()
                .filter(child -> Metric.PACKAGE.equals(child.getMetric()))
                .collect(Collectors.toList());
        allPackages.forEach(this::removeChild);
        for (Node packageNode : allPackages) {
            String[] packageParts = StringUtils.split(packageNode.getName(), "./\\");
            ArrayUtils.reverse(packageParts);
            Optional<PackageNode> splitPackages = Arrays.stream(packageParts)
                    .map(PackageNode::new)
                    .reduce(PackageNode::appendPackage);
            if (splitPackages.isPresent()) {
                PackageNode localRoot = splitPackages.get();
                Node localTail = localRoot;
                while (localTail.hasChildren()) {
                    localTail = localTail.getChildren().get(0);
                }
                localTail.addAllChildren(packageNode.getChildren());
                localTail.addAllValues(packageNode.getValues());
                mergeSinglePackage(localRoot);
            }
        }
    }

    private void mergeSinglePackage(final Node packageNode) {
        for (Node existing : getChildren()) {
            if (isEqual(packageNode, existing)) {
                // replace existing with merged two nodes
                removeChild(existing);
                Node merged = existing.combineWith(packageNode);
                addChild(merged);

                return;
            }
        }

        addChild(packageNode); // fallback: if the package does not yet exist add it as new package node
    }

    private static boolean isEqual(final Node packageNode, final Node existing) {
        return existing.getMetric().equals(packageNode.getMetric())
                && existing.getName().equals(packageNode.getName());
    }
}
