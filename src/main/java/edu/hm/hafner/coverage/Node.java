package edu.hm.hafner.coverage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.Ensure;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A hierarchical decomposition of coverage results.
 *
 * @author Ullrich Hafner
 */
// TODO: Make sure that we do not have children with the same name in the same node
@SuppressWarnings({"PMD.GodClass", "PMD.ExcessivePublicCount", "PMD.CyclomaticComplexity"})
public abstract class Node implements Serializable {
    private static final long serialVersionUID = -6608885640271135273L;

    static final String EMPTY_NAME = "-";
    static final String ROOT = "^";

    private final Metric metric;

    private /* almost final */ String name;
    private final List<Node> children = new ArrayList<>();
    private final List<Value> values = new ArrayList<>();

    @CheckForNull
    private Node parent;

    /**
     * Creates a new node with the given name.
     *
     * @param metric
     *         the metric this node belongs to
     * @param name
     *         the human-readable name of the node
     */
    protected Node(final Metric metric, final String name) {
        Ensure.that(metric.isContainer()).isTrue("Cannot create a container node with a value metric");

        this.metric = metric;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    void setName(final String name) { // Might be used during the deserialization of old reports
        this.name = name;
    }

    /**
     * Returns the name of the parent element or {@link #ROOT} if there is no such element.
     *
     * @return the name of the parent element
     */
    public String getParentName() {
        if (parent == null) {
            return ROOT;
        }
        Metric type = parent.getMetric();

        List<String> parentsOfSameType = new ArrayList<>();
        for (Node node = parent; node != null && node.getMetric().equals(type); node = node.parent) {
            parentsOfSameType.add(0, node.getName());
        }
        return String.join(".", parentsOfSameType);
    }

    public Metric getMetric() {
        return metric;
    }

    /**
     * Returns the available metrics for the whole tree starting with this node.
     *
     * @return the elements in this tree
     */
    public NavigableSet<Metric> getMetrics() {
        NavigableSet<Metric> elements = children.stream()
                .map(Node::getMetrics)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        elements.add(getMetric());
        getMetricsOfValues().forEach(elements::add);
        if (elements.contains(Metric.LINE)) {
            // These metrics depend on the existence of other metrics
            elements.add(Metric.LOC);
            if (elements.contains(Metric.COMPLEXITY)) {
                elements.add(Metric.COMPLEXITY_DENSITY);
            }
        }
        if (elements.contains(Metric.COMPLEXITY)) {
            elements.add(Metric.COMPLEXITY_MAXIMUM);
        }
        return elements;
    }

    /**
     * Returns whether results for the specified metric are available within the tree spanned by this node.
     *
     * @param searchMetric
     *         the metric to look for
     *
     * @return {@code true} if results for the specified metric are available, {@code false} otherwise
     */
    public boolean containsMetric(final Metric searchMetric) {
        return getMetrics().contains(searchMetric);
    }

    /**
     * Returns a collection of source folders that contain the source code files of all {@link FileNode file nodes}.
     *
     * @return a collection of source folders
     */
    public Set<String> getSourceFolders() {
        return children.stream()
                .map(Node::getSourceFolders)
                .flatMap(Collection::parallelStream)
                .collect(Collectors.toSet());
    }

    /**
     * Returns whether this node has children or not.
     *
     * @return {@code true} if this node has children, {@code false} otherwise
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public List<Node> getChildren() {
        return new ArrayList<>(children);
    }

    /**
     * Appends the specified child element to the list of children.
     *
     * @param child
     *         the child to add
     */
    public void addChild(final Node child) {
        if (children.stream().anyMatch(node -> node.getName().equals(child.getName()))) {
            throw new IllegalArgumentException(
                    String.format("There is already a child %s with the name %s in %s", child, child.getName(), this));
        }

        children.add(child);
        child.setParent(this);
    }

    @SuppressWarnings("PMD.NullAssignment") // remove link to parent
    protected void removeChild(final Node child) {
        Ensure.that(children.contains(child)).isTrue("The node %s is not a child of this node %s", child, this);

        children.remove(child);
        child.parent = null;
    }

    /**
     * Adds alls given nodes as children to the current node.
     *
     * @param nodes
     *         nodes to add
     */
    public void addAllChildren(final Collection<? extends Node> nodes) {
        nodes.forEach(this::addChild);
    }


    /**
     * Returns the parent node.
     *
     * @return the parent, if existent
     * @throws NoSuchElementException
     *         if no parent exists
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "This class is about walking through a tree of nodes.")
    public Node getParent() {
        if (parent == null) {
            throw new NoSuchElementException("Parent is not set");
        }
        return parent;
    }

    /**
     * Returns whether this node is the root of the tree.
     *
     * @return {@code true} if this node is the root of the tree, {@code false} otherwise
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns whether this node has a parent node.
     *
     * @return {@code true} if this node has a parent node, {@code false} if it is the root of the hierarchy
     */
    public boolean hasParent() {
        return !isRoot();
    }

    private void setParent(final Node parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    public List<Value> getValues() {
        return List.copyOf(values);
    }

    /**
     * Appends the specified value to the list of values.
     *
     * @param value
     *         the value to add
     */
    public void addValue(final Value value) {
        if (getMetricsOfValues().anyMatch(value.getMetric()::equals)) {
            throw new IllegalArgumentException(
                    String.format("There is already a leaf %s with the metric %s", value, value.getMetric()));
        }
        replaceValue(value);
    }

    /**
     * Replaces an existing value of the specified metric with the specified value. If no value with the specified
     * metric exists, then the value is added.
     *
     * @param value
     *         the value to replace
     */
    public void replaceValue(final Value value) {
        values.stream()
                .filter(v -> v.getMetric().equals(value.getMetric()))
                .findAny()
                .ifPresent(values::remove);
        values.add(value);
    }

    protected void addAllValues(final Collection<? extends Value> additionalValues) {
        additionalValues.forEach(this::addValue);
    }

    /**
     * Returns the available metrics for the whole tree starting with this node.
     *
     * @return the elements in this tree
     */
    public NavigableSet<Metric> getValueMetrics() {
        NavigableSet<Metric> elements = children.stream()
                .map(Node::getValueMetrics)
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(TreeSet::new));

        getMetricsOfValues().forEach(elements::add);
        return elements;
    }

    private Stream<Metric> getMetricsOfValues() {
        return values.stream().map(Value::getMetric);
    }

    NavigableMap<Metric, Value> getMetricsDistribution() {
        return new TreeMap<>(aggregateValues().stream()
                .collect(Collectors.toMap(Value::getMetric, Function.identity())));
    }

    private Value getValueOf(final Metric searchMetric) {
        return getValue(searchMetric).orElseThrow(() ->
                new NoSuchElementException(String.format("Node %s has no metric %s", this, searchMetric)));
    }

    /**
     * Returns the value for the specified metric. The value is aggregated for the whole subtree this node is the root
     * of.
     *
     * @param searchMetric
     *         the metric to get the value for
     *
     * @return coverage ratio
     */
    public Optional<Value> getValue(final Metric searchMetric) {
        return searchMetric.getValueFor(this);
    }

    /**
     * Returns the value for the specified metric. The value is aggregated for the whole subtree this node is the root
     * of.
     *
     * @param searchMetric
     *         the metric to get the value for
     * @param defaultValue
     *         the default value to return if no value has been defined for the specified metric
     * @param <T>
     *         the concrete type of the value
     *
     * @return coverage ratio
     */
    public <T extends Value> T getTypedValue(final Metric searchMetric, final T defaultValue) {
        var possiblyValue = searchMetric.getValueFor(this);

        //noinspection unchecked
        return possiblyValue.map(value -> (T) defaultValue.getClass().cast(value)).orElse(defaultValue);
    }

    /**
     * Aggregates all values that are part of the subtree that is spanned by this node.
     *
     * @return aggregation of values below this tree
     */
    public List<Value> aggregateValues() {
        return getMetrics().stream().map(this::getValue).flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    /**
     * Computes the delta of all metrics between this node and the specified reference node as fractions. Each delta
     * value is computed by the value specific {@link Value#delta(Value)} method. If the reference node does not contain
     * a specific metric, then no delta is computed and the metric is omitted in the result map.
     *
     * @param reference
     *         the reference node
     *
     * @return the delta coverage for each available metric as fraction
     */
    public NavigableMap<Metric, Fraction> computeDelta(final Node reference) {
        NavigableMap<Metric, Fraction> deltaPercentages = new TreeMap<>();
        NavigableMap<Metric, Value> metricPercentages = getMetricsDistribution();
        NavigableMap<Metric, Value> referencePercentages = reference.getMetricsDistribution();

        for (Entry<Metric, Value> entry : metricPercentages.entrySet()) {
            Metric key = entry.getKey();
            if (referencePercentages.containsKey(key)) {
                deltaPercentages.put(key, entry.getValue().delta(referencePercentages.get(key)));
            }
        }
        return deltaPercentages;
    }

    /**
     * Returns recursively all nodes for the specified metric type.
     *
     * @param searchMetric
     *         the metric to look for
     *
     * @return all nodes for the given metric
     */
    public List<Node> getAll(final Metric searchMetric) {
        List<Node> childNodes = children.stream()
                .map(child -> child.getAll(searchMetric))
                .flatMap(List::stream).collect(Collectors.toList());
        if (metric.equals(searchMetric)) {
            childNodes.add(this);
        }
        return childNodes;
    }

    /**
     * Finds the metric with the given name starting from this node.
     *
     * @param searchMetric
     *         the metric to search for
     * @param searchName
     *         the name of the node
     *
     * @return the result if found
     */
    public Optional<Node> find(final Metric searchMetric, final String searchName) {
        if (matches(searchMetric, searchName)) {
            return Optional.of(this);
        }
        return children.stream()
                .map(child -> child.find(searchMetric, searchName))
                .flatMap(Optional::stream)
                .findAny();
    }

    /**
     * Searches for a package within this node that has the given name.
     *
     * @param searchName
     *         the name of the package
     *
     * @return the first matching package or an empty result, if no such package exists
     */
    public Optional<PackageNode> findPackage(final String searchName) {
        return find(Metric.PACKAGE, searchName).map(PackageNode.class::cast);
    }

    /**
     * Searches for a file within this node that has the given name.
     *
     * @param searchName
     *         the name of the file
     *
     * @return the first matching file or an empty result, if no such file exists
     */
    public Optional<FileNode> findFile(final String searchName) {
        return find(Metric.FILE, searchName).map(FileNode.class::cast);
    }

    /**
     * Searches for a class within this node that has the given name.
     *
     * @param searchName
     *         the name of the class
     *
     * @return the first matching class or an empty result, if no such class exists
     */
    public Optional<ClassNode> findClass(final String searchName) {
        return find(Metric.CLASS, searchName).map(ClassNode.class::cast);
    }

    /**
     * Searches for a method within this node that has the given name and signature.
     *
     * @param searchName
     *         the name of the method
     * @param searchSignature
     *         the signature of the method
     *
     * @return the first matching method or an empty result, if no such method exists
     */
    public Optional<MethodNode> findMethod(final String searchName, final String searchSignature) {
        return getAll(Metric.METHOD).stream()
                .map(MethodNode.class::cast)
                .filter(node -> node.getMethodName().equals(searchName)
                        && node.getSignature().equals(searchSignature))
                .findAny();
    }

    /**
     * Returns the file names that are contained within the subtree of this node.
     *
     * @return the file names
     */
    public Set<String> getFiles() {
        return children.stream().map(Node::getFiles).flatMap(Collection::stream).collect(Collectors.toSet());
    }

    public List<FileNode> getAllFileNodes() {
        return getAll(Metric.FILE).stream().map(FileNode.class::cast).collect(Collectors.toList());
    }

    public List<MethodNode> getAllMethodNodes() {
        return getAll(Metric.METHOD).stream().map(MethodNode.class::cast).collect(Collectors.toList());
    }

    /**
     * Finds the metric with the given hash code starting from this node.
     *
     * @param searchMetric
     *         the metric to search for
     * @param searchNameHashCode
     *         the hash code of the node name
     *
     * @return the result if found
     */
    public Optional<Node> findByHashCode(final Metric searchMetric, final int searchNameHashCode) {
        if (matches(searchMetric, searchNameHashCode)) {
            return Optional.of(this);
        }
        return children.stream()
                .map(child -> child.findByHashCode(searchMetric, searchNameHashCode))
                .flatMap(Optional::stream)
                .findAny();
    }

    /**
     * Returns whether this node matches the specified metric and name.
     *
     * @param searchMetric
     *         the metric to search for
     * @param searchName
     *         the name of the node
     *
     * @return the result if found
     */
    public boolean matches(final Metric searchMetric, final String searchName) {
        if (!metric.equals(searchMetric)) {
            return false;
        }
        return name.equals(searchName);
    }

    /**
     * Returns whether this node matches the specified metric and name.
     *
     * @param searchMetric
     *         the metric to search for
     * @param searchNameHashCode
     *         the hash code of the node name
     *
     * @return the result if found
     */
    public boolean matches(final Metric searchMetric, final int searchNameHashCode) {
        if (!metric.equals(searchMetric)) {
            return false;
        }
        return name.hashCode() == searchNameHashCode;
    }

    /**
     * Creates a deep copy of the tree with this as root node.
     *
     * @return the root node of the copied tree
     */
    public Node copyTree() {
        return copyTree(null);
    }

    /**
     * Creates a deep copy of the tree with the specified {@link Node} as root.
     *
     * @param copiedParent
     *         The root node
     *
     * @return the copied tree
     */
    public Node copyTree(@CheckForNull final Node copiedParent) {
        Node copy = copyNode();

        if (copiedParent != null) {
            copy.setParent(copiedParent);
        }
        getChildren().stream()
                .map(node -> node.copyTree(this))
                .forEach(copy::addChild);

        return copy;
    }

    /**
     * Creates a copy of this instance that has no children and no parent yet. This method will copy all stored values
     * of this node. This method delegates to the instance local {@link #copy()} method to copy all properties
     * introduced by subclasses.
     *
     * @return the copied node
     */
    public final Node copyNode() {
        Node copy = copy();
        getValues().forEach(copy::addValue);
        return copy;
    }

    /**
     * Creates a copy of this instance that has no children and no parent yet. Node properties from the parent class
     * {@link Node} must not be copied. All other immutable properties need to be copied one by one.
     *
     * @return the copied node
     */
    public abstract Node copy();

    private static boolean haveSameNameAndMetric(final List<? extends Node> nodes) {
        return nodes.stream().map(Node::getName).distinct().count() == 1
                && nodes.stream().map(Node::getMetric).distinct().count() == 1;
    }

    /**
     * Creates a new tree of merged {@link Node nodes} if all nodes have the same name and metric. If the nodes have
     * different names or metrics, then these nodes will be attached to a new {@link ContainerNode} node.
     *
     * @param nodes
     *         the nodes to merge
     *
     * @return a new tree with the merged {@link Node nodes}
     */
    public static Node merge(final List<? extends Node> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge an empty list of nodes");
        }
        if (nodes.size() == 1) {
            return nodes.get(0); // No merge required
        }

        if (haveSameNameAndMetric(nodes)) {
            return nodes.stream()
                    .map(t -> (Node) t)
                    .reduce(Node::merge)
                    .orElseThrow(() -> new NoSuchElementException("No node found"));
        }

        var container = new ContainerNode("Container");
        container.addAllChildren(nodes); // non-compatible nodes will be added to a new container node
        return container;
    }

    /**
     * Creates a new tree of {@link Node nodes} that will contain the merged nodes of the trees that are starting at
     * this and the specified {@link Node}. In order to merge these two trees, this node and the specified {@code other}
     * root node have to use the same {@link Metric} and name.
     *
     * @param other
     *         the other tree to merge (represented by the root node)
     *
     * @return a new tree with the merged {@link Node nodes}
     * @throws IllegalArgumentException
     *         if this root node is not compatible to the {@code other} root node
     */
    @SuppressWarnings({"ReferenceEquality", "PMD.CompareObjectsWithEquals"})
    public Node merge(final Node other) {
        if (other == this) {
            return this; // nothing to do
        }

        ensureSameMetric(other);

        if (getName().equals(other.getName())) {
            Node combinedReport = copyTree();
            combinedReport.mergeNode(other);
            return combinedReport;
        }
        else {
            throw new IllegalArgumentException(
                    String.format("Cannot merge nodes with different names: %s - %s", this, other));
        }
    }

    private void ensureSameMetric(final Node other) {
        if (getMetric() != other.getMetric()) {
            throw new IllegalArgumentException(
                    String.format("Cannot merge nodes of different metrics: %s - %s", this, other));
        }
    }

    private void mergeNode(final Node other) {
        ensureSameMetric(other);
        other.values.forEach(this::mergeValues);
        other.getChildren().forEach(otherChild -> {
            Optional<Node> existingChild = getChildren().stream()
                    .filter(c -> c.getName().equals(otherChild.getName())).findFirst();
            if (existingChild.isPresent()) {
                existingChild.get().mergeNode(otherChild);
            }
            else {
                addChild(otherChild.copyTree());
            }
        });
    }

    private void mergeValues(final Value otherValue) {
        if (getMetricsOfValues().anyMatch(v -> v.equals(otherValue.getMetric()))) {
            var old = getValueOf(otherValue.getMetric());
            try {
                var max = old.max(otherValue);

                replaceValue(max);
            }
            catch (AssertionError exception) {
                throw new IllegalStateException(String.format("Cannot merge coverage of %s because of: %s",
                        this, exception.getMessage()), exception);
            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(metric, node.metric) && Objects.equals(name, node.name)
                && Objects.equals(children, node.children) && Objects.equals(values, node.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, name, children, values);
    }

    @Override
    public String toString() {
        return getValue(Metric.LINE)
                .map(lineCoverage -> String.format("[%s] %s <%d, %s>",
                        getMetric(), getName(), getChildren().size(), lineCoverage))
                .orElse(String.format("[%s] %s <%d>", getMetric(), getName(), children.size()));
    }

    public boolean isEmpty() {
        return getChildren().isEmpty() && getValues().isEmpty();
    }

    /**
     * Checks whether code any changes have been detected no matter if the code coverage is affected or not.
     *
     * @return {@code true} whether code changes have been detected
     */
    public boolean hasModifiedLines() {
        return getChildren().stream().anyMatch(Node::hasModifiedLines);
    }

    /**
     * Creates a new coverage tree that represents the modified lines coverage. This new tree will contain only those
     * elements that contain modified lines.
     *
     * @return the filtered tree
     */
    public Node filterByModifiedLines() {
        return filterTreeByModifiedLines().orElse(copy());
    }

    protected Optional<Node> filterTreeByModifiedLines() {
        return filterTreeByMapping(Node::filterTreeByModifiedLines);
    }

    /**
     * Creates a new coverage tree that represents the modified files coverage. This new tree will contain only those
     * elements that have modified files. The difference against the modified line coverage is that the modified files
     * coverage tree represents the total coverage of all files with coverage relevant changes, not only the coverage
     * of the modified lines.
     *
     * @return the filtered tree
     */
    public Node filterByModifiedFiles() {
        return filterTreeByModifiedFiles().orElse(copy());
    }

    protected Optional<Node> filterTreeByModifiedFiles() {
        return filterTreeByMapping(Node::filterTreeByModifiedFiles);
    }

    /**
     * Creates a new coverage tree that shows indirect coverage changes. This new tree will contain only those elements
     * that have elements with a modified coverage but with no modified code lines.
     *
     * @return the filtered tree
     */
    public Node filterByIndirectChanges() {
        return filterTreeByIndirectChanges().orElse(copy());
    }

    protected Optional<Node> filterTreeByIndirectChanges() {
        return filterTreeByMapping(Node::filterTreeByIndirectChanges);
    }

    /**
     * Filters a coverage tree by the given mapping function.
     *
     * @param mappingFunction
     *         The mapping function to be used
     *
     * @return the root of the pruned coverage tree
     */
    private Optional<Node> filterTreeByMapping(final Function<Node, Optional<Node>> mappingFunction) {
        var prunedChildren = getChildren()
                .stream()
                .map(mappingFunction)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
        if (prunedChildren.isEmpty()) {
            return Optional.empty();
        }

        var copy = copy();
        copy.addAllChildren(prunedChildren);
        return Optional.of(copy);
    }
}
