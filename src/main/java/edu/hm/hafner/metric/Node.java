package edu.hm.hafner.metric;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
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
@SuppressWarnings("PMD.GodClass")
public abstract class Node implements Serializable {
    private static final long serialVersionUID = -6608885640271135273L;

    static final String ROOT = "^";

    /**
     * Creates a new tree of merged {@link Node nodes} if all nodes have the same name and metric. If the nodes have
     * different names or metrics, then these nodes will be attached to a new {@link ContainerNode} node.
     *
     * @param nodes
     *         the nodes to merge
     *
     * @return a new tree with the merged {@link Node nodes}
     */
    public static Node merge(final List<Node> nodes) {
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("Cannot merge an empty list of nodes");
        }
        if (nodes.size() == 1) {
            return nodes.get(0); // No merge required
        }

        if (nodes.stream().map(Node::getName).distinct().count() == 1
                && nodes.stream().map(Node::getMetric).distinct().count() == 1) {
            return nodes.stream()
                    .reduce(Node::combineWith)
                    .orElseThrow(() -> new NoSuchElementException("No node found"));
        }

        var container = new ContainerNode("Container");
        container.addAllChildren(nodes); // non-compatible nodes will be added to a new container node
        return container;
    }

    private final Metric metric;
    private final String name;
    private final List<Node> children = new ArrayList<>();
    private final Map<Metric, Value> values = new EnumMap<>(Metric.class);

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
        Ensure.that(Metric.isNodeMetric(metric))
                .isTrue("Cannot create a node with a value metric");
        this.metric = metric;
        this.name = name;
    }

    /**
     * Returns the source code path of this node.
     *
     * @return the element type
     */
    public String getPath() {
        return StringUtils.EMPTY;
    }

    protected String mergePath(final String localPath) {
        // default packages are named '-' at the moment
        if ("-".equals(localPath)) {
            return StringUtils.EMPTY;
        }

        if (hasParent()) {
            String parentPath = getParent().getPath();

            if (StringUtils.isBlank(parentPath)) {
                return localPath;
            }
            if (StringUtils.isBlank(localPath)) {
                return parentPath;
            }
            return parentPath + "/" + localPath;
        }

        return localPath;
    }

    /**
     * Returns the type of the metric for this node.
     *
     * @return the element type
     */
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
        elements.addAll(values.keySet());
        if (elements.contains(Metric.LINE)) {
            elements.add(Metric.LOC); // TODO: would it make sense to make that not hard-coded?
        }
        return elements;
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

        elements.addAll(values.keySet());
        return elements;
    }

    public List<Value> condenseValues() {
        return getValueMetrics().stream()
                .map(this::getValue)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    /**
     * Returns a mapping of metric to coverage. The root of the tree will be skipped.
     *
     * @return a mapping of metric to coverage.
     */
    // FIXME: wouldn't it be sufficient to return the values only, since the metric is contained?
    public NavigableMap<Metric, Value> getMetricsDistribution() {
        return new TreeMap<>(getMetrics().stream()
                .filter(m -> getValue(m).isPresent())
                .collect(Collectors.toMap(Function.identity(), this::getValueOf)));
    }

    private Value getValueOf(final Metric searchMetric) {
        return getValue(searchMetric).orElseThrow(() ->
                new NoSuchElementException(String.format("Node %s has no metric %s", this, searchMetric)));
    }

    /**
     * Computes the coverage delta between this node and the specified reference node as fractions between 0 and 1.
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
     * Checks whether code any changes have been detected no matter if the code coverage is affected or not.
     *
     * @return {@code true} whether code changes have been detected
     */
    public boolean hasCodeChanges() {
        return getChildren().stream().anyMatch(Node::hasCodeChanges);
    }

    public String getName() {
        return name;
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
     * Clear the children and values of this node.
     */
    // TODO: check if this method needs to be exposed as API
    public void clear() {
        clearChildren();
        values.clear();
    }

    // TODO: check if this method needs to be exposed as API
    public void clearChildren() {
        children.forEach(c -> c.parent = null);
        children.clear();
        values.clear();
    }

    /**
     * Appends the specified child element to the list of children.
     *
     * @param child
     *         the child to add
     */
    public void addChild(final Node child) {
        children.add(child);
        child.setParent(this);
    }

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

    public List<Value> getValues() {
        return new ArrayList<>(values.values());
    }

    /**
     * Appends the specified value to the list of values.
     *
     * @param value
     *         the value to add
     */
    public void addValue(final Value value) {
        if (values.containsKey(value.getMetric())) {
            throw new IllegalArgumentException(
                    String.format("There is already a leaf %s with the metric %s", value, value.getMetric()));
        }
        values.put(value.getMetric(), value);
    }

    // FIXME: why is this for mutation coverages only?

    /**
     * Replaces the specified value to the list of values to guarantee immutability.
     *
     * @param value
     *         the value to replace
     */
    public void replaceMutationValue(final Value value) {
        values.put(value.getMetric(), value);
    }

    protected void addAllValues(final Collection<? extends Value> additionalValues) {
        additionalValues.forEach(this::addValue);
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
        return name.equals(searchName) || getPath().equals(searchName);
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
        return name.hashCode() == searchNameHashCode || getPath().hashCode() == searchNameHashCode;
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
     * Creates a deep copy of the tree with this as root node.
     *
     * @return the root node of the copied tree
     */
    public Optional<Node> prune(final Predicate<Node> predicate, final Consumer<Node> consumer) {
        var copy = copy();
        var prunedChildren = children.stream().map(c -> c.prune(predicate, consumer)).flatMap(Optional::stream).collect(Collectors.toList());
        copy.addAllChildren(prunedChildren);
        if (predicate.test(this)) {
            consumer.accept(this);
            return Optional.of(copy);
        }
        else if (!prunedChildren.isEmpty()) {
            return Optional.of(copy);
        }
        return Optional.empty();
    }

    /**
     * Recursively copies the tree with the passed {@link Node} as root.
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
    public Node combineWith(final Node other) {
        if (other == this) {
            return this; // nothing to do
        }

        if (!other.getMetric().equals(getMetric())) {
            throw new IllegalArgumentException(
                    String.format("Cannot merge nodes of different metrics: %s - %s", this, other));
        }

        if (getName().equals(other.getName())) {
            Node combinedReport = copyTree();
            combinedReport.safelyCombineChildren(other);
            return combinedReport;
        }
        else {
            throw new IllegalArgumentException(
                    String.format("Cannot merge nodes with different names: %s - %s", this, other));
        }
    }

    private void safelyCombineChildren(final Node other) {
        BiFunction<Value, Value, Value> mergeOperation = hasChildren() ? Value::add : Value::max;
        other.values.forEach((k, v) -> values.merge(k, v, mergeOperation));

        other.getChildren().forEach(otherChild -> {
            Optional<Node> existingChild = getChildren().stream()
                    .filter(c -> c.getName().equals(otherChild.getName())).findFirst();
            if (existingChild.isPresent()) {
                existingChild.get().safelyCombineChildren(otherChild);
            }
            else {
                addChild(otherChild.copyTree());
            }
        });
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
        return String.format("[%s] %s <%d>", getMetric(), getName(), children.size());
    }

    /**
     * Returns the file names that are contained within the subtree of this node.
     *
     * @return the file names
     */
    public List<String> getFiles() {
        return children.stream().map(Node::getFiles).flatMap(List::stream).collect(Collectors.toList());
    }

    public List<FileNode> getAllFileNodes() {
        return getAll(Metric.FILE).stream().map(t -> (FileNode) t).collect(Collectors.toList());
    }
}
