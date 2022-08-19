package edu.hm.hafner.model;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.complexity.ComplexityLeaf;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;
import edu.hm.hafner.mutation.MutationLeaf;
import edu.hm.hafner.mutation.MutationResult;
import edu.hm.hafner.util.Ensure;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A hierarchical decomposition of coverage results.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
public class Node implements Serializable {
    private static final long serialVersionUID = -6608885640271135273L;

    private static final Coverage COVERED_NODE = new Coverage(1, 0);
    private static final Coverage MISSED_NODE = new Coverage(0, 1);

    static final String ROOT = "^";

    private final Metric metric;
    private final String name;
    private final List<String> sources = new ArrayList<>();
    private final List<Node> children = new ArrayList<>();
    private final List<Leaf> leaves = new ArrayList<>();
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
    public Node(final Metric metric, final String name) {
        this.metric = metric;
        this.name = name;
    }

    /**
     * Gets the parent node.
     *
     * @return the parent, if existent
     * @throws NoSuchElementException
     *         if no parent exists
     */
    public Node getParent() {
        if (parent == null) {
            throw new NoSuchElementException("Parent is not set");
        }
        return parent;
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
        leaves.stream().map(Leaf::getMetric).forEach(elements::add);

        return elements;
    }

    /**
     * Returns a mapping of metric to coverage. The root of the tree will be skipped.
     *
     * @return a mapping of metric to coverage.
     */
    public NavigableMap<Metric, Coverage> getMetricsDistribution() {
        return getMetrics().stream()
                .collect(Collectors.toMap(Function.identity(), this::getCoverage, (o1, o2) -> o1, TreeMap::new));
    }

    public NavigableMap<Metric, Fraction> getMetricsPercentages() {
        return getMetrics().stream().collect(Collectors.toMap(
                Function.identity(),
                searchMetric -> getCoverage(searchMetric).getCoveredPercentage(),
                (o1, o2) -> o1, // is never reached because stream input is already a set
                TreeMap::new));
    }

    public String getName() {
        return name;
    }

    public List<String> getSources() {
        return sources;
    }

    public List<Node> getChildren() {
        return children;
    }

    public List<Leaf> getLeaves() {
        return leaves;
    }

    private void addAll(final List<Node> nodes) {
        nodes.forEach(this::add);
    }

    /**
     * Appends the specified child element to the list of children.
     *
     * @param child
     *         the child to add
     */
    public void add(final Node child) {
        children.add(child);
        child.setParent(this);
    }

    /**
     * Appends the specified leaf element to the list of leaves.
     *
     * @param leaf
     *         the leaf to add
     */
    public void add(final Leaf leaf) {
        leaves.add(leaf);
    }

    /**
     * Appends the specified source to the list of sources.
     *
     * @param source
     *         the source to add
     */
    public void addSource(final String source) {
        sources.add(source);
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

    void setParent(final Node parent) {
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
     * @throws AssertionError
     *         if the coverage metric is a LEAF metric
     */
    public List<Node> getAll(final Metric searchMetric) {
        Ensure.that(searchMetric.isLeaf())
                .isFalse("Leaves like '%s' are not stored as inner nodes of the tree", searchMetric);

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
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
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
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
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
        return metric.equals(searchMetric) && name.equals(searchName);
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
     * Splits flat packages into a package hierarchy. Changes the internal tree structure in place.
     */
    public void splitPackages() {
        if (Metric.MODULE.equals(metric)) {
            List<Node> allPackages = children.stream()
                    .filter(child -> Metric.PACKAGE.equals(child.getMetric()))
                    .collect(Collectors.toList());
            if (!allPackages.isEmpty()) {
                children.clear();
                for (Node packageNode : allPackages) {
                    String[] packageParts = packageNode.getName().split("\\.");
                    if (packageParts.length > 1) {
                        Deque<String> packageLevels = new ArrayDeque<>(Arrays.asList(packageParts));
                        insertPackage(packageNode, packageLevels);
                    }
                    else {
                        add(packageNode);
                    }
                }
            }
        }
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
     * Recursively copies the tree with the passed {@link Node} as root.
     *
     * @param copiedParent
     *         The root node
     *
     * @return the copied tree
     */
    public Node copyTree(@CheckForNull final Node copiedParent) {
        Node copy = copyEmpty();
        if (copiedParent != null) {
            copy.setParent(copiedParent);
        }

        getChildren().stream()
                .map(node -> node.copyTree(this))
                .forEach(copy::add);
        getLeaves().forEach(copy::add);

        return copy;
    }

    /**
     * Creates a copied instance of this node that has no children, leaves, and parent yet.
     *
     * @return the new and empty node
     */
    public Node copyEmpty() {
        return new Node(metric, name);
    }

    private void insertPackage(final Node aPackage, final Deque<String> packageLevels) {
        String nextLevelName = packageLevels.pop();
        Node subPackage = createChild(nextLevelName);
        if (packageLevels.isEmpty()) {
            subPackage.addAll(aPackage.children);
        }
        else {
            subPackage.insertPackage(aPackage, packageLevels);
        }
    }

    private Node createChild(final String childName) {
        for (Node child : children) {
            if (child.getName().equals(childName)) {
                return child;
            }

        }
        Node newNode = new PackageNode(childName);
        add(newNode);
        return newNode;
    }

    @Override
    public boolean equals(@CheckForNull final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Node node = (Node) o;
        return Objects.equals(metric, node.metric) && Objects.equals(name, node.name) && Objects.equals(sources, node.sources)
                && Objects.equals(children, node.children) && Objects.equals(leaves, node.leaves);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metric, name, sources, children, leaves);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", getMetric(), getName());
    }

    /**
     * Combines two related or unrelated coverage-reports.
     * @param other module to combine with
     * @return combined report
     */
    public Node combineWith(final Node other) {

        if (!other.getMetric().equals(Metric.MODULE)) {
            throw new IllegalArgumentException("Provided Node is not of MetricType MODULE");
        }
        if (!this.getMetric().equals(Metric.MODULE)) {
            throw new IllegalStateException("Cannot perform combineWith on a non-module Node");
        }

        final Node combinedReport;
        if (this.getName().equals(other.getName())) {
            combinedReport = this.copyTree();
            combinedReport.safelyCombineChildren(other);
        }
        else {
            combinedReport = new Node(Metric.GROUP, "Combined Report");
            combinedReport.add(this.copyTree());
            combinedReport.add(other.copyTree());
        }

        return combinedReport;
    }

    private void safelyCombineChildren(final Node other) {
        if (!this.leaves.isEmpty()) {
            if (other.getChildren().isEmpty()) {
                mergeLeaves(this.getMetricsDistribution(), other.getMetricsDistribution());
                return;
            }
            this.leaves.clear();
        }

        other.getChildren().forEach(otherChild -> {
            Optional<Node> existingChild = this.getChildren().stream()
                    .filter(c -> c.getName().equals(otherChild.getName())).findFirst();
            if (existingChild.isPresent()) {
                existingChild.get().safelyCombineChildren(otherChild);
            }
            else {
                this.add(otherChild.copyTree());
            }
        });
    }

    private void mergeLeaves(final SortedMap<Metric, Coverage> metricsDistribution, final SortedMap<Metric, Coverage> metricsDistributionOther) {
        if (!metricsDistribution.keySet().equals(metricsDistributionOther.keySet())) {
            throw new IllegalStateException(
                    String.format("Reports to combine have a mismatch of leaves in %s %s", this.getMetric(), this.getName()));
        }

        leaves.clear();
        metricsDistribution.keySet().forEach(key -> {
            if (metricsDistribution.get(key).getTotal() != metricsDistributionOther.get(key).getTotal()) {
                throw new IllegalStateException(
                        String.format("Reports to combine have a mismatch of total %s coverage in %s %s",
                                key.getName(), this.getMetric(), this.getName()));
            }
            Coverage maxCoverage = Stream.of(metricsDistribution.get(key), metricsDistributionOther.get(key))
                    .max(Comparator.comparing(Coverage::getCovered)).get();
            leaves.add(new CoverageLeaf(key, maxCoverage));
        });
    }

    //---------------------------- Coverage methods ---------------------------------------------

    /**
     * Returns a mapping of metric to coverage. The root of the tree will be skipped.
     *
     * @return a mapping of metric to coverage.
     */
    public NavigableMap<Metric, Coverage> getCoverageMetricsDistribution() {
        return getMetrics().stream().filter(tmpMetric -> !tmpMetric.equals(Metric.COMPLEXITY)).collect(Collectors.toMap(
                Function.identity(),
                this::getCoverage,
                (o1, o2) -> o1,
                TreeMap::new));
    }

    public NavigableMap<Metric, Fraction> getCoverageMetricsPercentages() {
        return getMetrics().stream().filter(tmpMetric -> !tmpMetric.equals(Metric.COMPLEXITY)).collect(Collectors.toMap(
                Function.identity(),
                searchMetric -> getCoverage(searchMetric).getCoveredPercentage(),
                (o1, o2) -> o1,
                TreeMap::new));
    }

    /**
     * Prints the coverage for the specified element. Uses {@code Locale.getDefault()} to format the percentage.
     *
     * @param searchMetric
     *         the element to print the coverage for
     *
     * @return coverage ratio in a human-readable format
     * @throws AssertionError
     *         if coverage of complexity is wanted
     * @see #printCoverageFor(Metric, Locale)
     */
    public String printCoverageFor(final Metric searchMetric) {
        if ("Complexity".equals(searchMetric.getName())) {
            throw new AssertionError("Complexity is no coverage metric.");
        }
        return printCoverageFor(searchMetric, Locale.getDefault());
    }

    /**
     * Prints the coverage for the specified element.
     *
     * @param searchMetric
     *         the element to print the coverage for
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return coverage ratio in a human-readable format
     * @throws AssertionError
     *         if coverage of complexity is wanted
     */
    public String printCoverageFor(final Metric searchMetric, final Locale locale) {
        if ("Complexity".equals(searchMetric.getName())) {
            throw new AssertionError("Complexity is no coverage metric.");
        }

        return getCoverage(searchMetric).formatCoveredPercentage(locale);
    }

    /**
     * Returns the coverage for the specified metric.
     *
     * @param searchMetric
     *         the element to get the coverage for
     *
     * @return coverage ratio
     */
    public Coverage getCoverage(final Metric searchMetric) {
        Coverage childrenCoverage = aggregateChildren(searchMetric);

        if (searchMetric.isLeaf()) {
            return leaves.stream()
                    .filter(leaf -> !leaf.getMetric().equals(Metric.COMPLEXITY)) // all except complexity leaves
                    .map(CoverageLeaf.class::cast)
                    .map(node -> node.getCoverage(searchMetric))
                    .reduce(childrenCoverage, Coverage::add);
        }
        else {
            if (metric.equals(searchMetric)) {
                if (getCoverage(Metric.LINE).getCovered() > 0) {
                    return childrenCoverage.add(COVERED_NODE);
                }
                else {
                    return childrenCoverage.add(MISSED_NODE);
                }
            }
            return childrenCoverage;
        }
    }

    private Coverage aggregateChildren(final Metric searchMetric) {
        return children.stream()
                .map(node -> node.getCoverage(searchMetric))
                .reduce(Coverage.NO_COVERAGE, Coverage::add);
    }

    /**
     * Computes the coverage delta between this node and the specified reference node.
     *
     * @param reference
     *         the reference node
     *
     * @return the delta coverage for each available metric
     */
    public SortedMap<Metric, Fraction> computeDelta(final Node reference) {
        SortedMap<Metric, Fraction> deltaPercentages = new TreeMap<>();
        SortedMap<Metric, Fraction> metricPercentages = getCoverageMetricsPercentages();
        SortedMap<Metric, Fraction> referencePercentages = reference.getCoverageMetricsPercentages();
        metricPercentages.forEach((key, value) ->
                deltaPercentages.put(key,
                        saveSubtractFraction(value, referencePercentages.getOrDefault(key, Fraction.ZERO))));
        return deltaPercentages;
    }

    /**
     * Calculates the difference between two fraction. Since there might be an arithmetic exception due to an overflow,
     * the method handles it and calculates the difference based on the double values of the fractions.
     *
     * @param minuend
     *         The minuend as a fraction
     * @param subtrahend
     *         The subtrahend as a fraction
     *
     * @return the difference as a fraction
     */
    private Fraction saveSubtractFraction(final Fraction minuend, final Fraction subtrahend) {
        try {
            return minuend.subtract(subtrahend);
        }
        catch (ArithmeticException e) {
            double diff = minuend.doubleValue() - subtrahend.doubleValue();
            return Fraction.getFraction(diff);
        }
    }

    //---------------------------- Complexity methods ---------------------------------------------

    /**
     * Sums up the complexity of all methods and returns it. Complexity of the complete module is equal to the
     * complexity of all methods.
     *
     * @return the complexity
     */
    public int getComplexity() {
        int childrenComplexity = children.stream()
                .map(Node::getComplexity)
                .reduce(0, Integer::sum);

        return leaves.stream()
                .filter(leaf -> leaf.getMetric().equals(Metric.COMPLEXITY)) // only complexity leaves
                .map(ComplexityLeaf.class::cast)
                .map(ComplexityLeaf::getComplexity)
                .reduce(childrenComplexity, Integer::sum);
    }

    //---------------------------- Mutation methods ---------------------------------------------

    /**
     * Sums up all killed/survived mutations.
     *
     * @return The amount of killed/survived mutations.
     */
    public MutationResult getMutationResult() {
        MutationResult mutationResult = children.stream()
                .map(Node::getMutationResult)
                .reduce(new MutationResult(), MutationResult::add);

        return leaves.stream()
                .filter(leaf -> leaf.getMetric().equals(Metric.MUTATION)) // only mutation leaves
                .map(MutationLeaf.class::cast)
                .map(MutationLeaf::getResult)
                .reduce(mutationResult, MutationResult::add);
    }

    /**
     * Combines two related or unrelated coverage-reports.
     * @param other module to combine with
     * @return combined report
     */
    public Node combineWith(final Node other) {

        if (!other.getMetric().equals(Metric.MODULE)) {
            throw new IllegalArgumentException("Provided Node is not of MetricType MODULE");
        }
        if (!this.getMetric().equals(Metric.MODULE)) {
            throw new IllegalStateException("Cannot perform combineWith on a non-module Node");
        }

        final Node combinedReport;
        if (this.getName().equals(other.getName())) {
            combinedReport = this.copyTree();
            combinedReport.safelyCombineChildren(other);
        }
        else {
            combinedReport = new Node(Metric.GROUP, "Combined Report");
            combinedReport.add(this.copyTree());
            combinedReport.add(other.copyTree());
        }

        return combinedReport;
    }

    private void safelyCombineChildren(final Node other) {
        if (!this.leaves.isEmpty()) {
            if (other.getChildren().isEmpty()) {
                mergeLeaves(this.getMetricsDistribution(), other.getMetricsDistribution());
                return;
            }
            this.leaves.clear();
        }

        other.getChildren().forEach(otherChild -> {
            Optional<Node> existingChild = this.getChildren().stream()
                    .filter(c -> c.getName().equals(otherChild.getName())).findFirst();
            if (existingChild.isPresent()) {
                existingChild.get().safelyCombineChildren(otherChild);
            }
            else {
                this.add(otherChild.copyTree());
            }
        });
    }

    private void mergeLeaves(final SortedMap<Metric, Coverage> metricsDistribution, final SortedMap<Metric, Coverage> metricsDistributionOther) {
        if (!metricsDistribution.keySet().equals(metricsDistributionOther.keySet())) {
            throw new IllegalStateException(
                    String.format("Reports to combine have a mismatch of leaves in %s %s", this.getMetric(), this.getName()));
        }

        leaves.clear();
        metricsDistribution.keySet().forEach(key -> {
            if (metricsDistribution.get(key).getTotal() != metricsDistributionOther.get(key).getTotal()) {
                throw new IllegalStateException(
                        String.format("Reports to combine have a mismatch of total %s coverage in %s %s",
                                key.getName(), this.getMetric(), this.getName()));
            }
            Coverage maxCoverage = Stream.of(metricsDistribution.get(key), metricsDistributionOther.get(key))
                    .max(Comparator.comparing(Coverage::getCovered)).get();
            leaves.add(new CoverageLeaf(key, maxCoverage));
        });
    }

    //---------------------------- Coverage methods ---------------------------------------------

    /**
     * Returns a mapping of metric to coverage. The root of the tree will be skipped.
     *
     * @return a mapping of metric to coverage.
     */
    public NavigableMap<Metric, Coverage> getCoverageMetricsDistribution() {
        return getMetrics().stream().filter(tmpMetric -> !tmpMetric.equals(Metric.COMPLEXITY)).collect(Collectors.toMap(
                Function.identity(),
                this::getCoverage,
                (o1, o2) -> o1,
                TreeMap::new));
    }

    public NavigableMap<Metric, Fraction> getCoverageMetricsPercentages() {
        return getMetrics().stream().filter(tmpMetric -> !tmpMetric.equals(Metric.COMPLEXITY)).collect(Collectors.toMap(
                Function.identity(),
                searchMetric -> getCoverage(searchMetric).getCoveredPercentage(),
                (o1, o2) -> o1,
                TreeMap::new));
    }

    /**
     * Prints the coverage for the specified element. Uses {@code Locale.getDefault()} to format the percentage.
     *
     * @param searchMetric
     *         the element to print the coverage for
     *
     * @return coverage ratio in a human-readable format
     * @throws AssertionError
     *         if coverage of complexity is wanted
     * @see #printCoverageFor(Metric, Locale)
     */
    public String printCoverageFor(final Metric searchMetric) {
        if ("Complexity".equals(searchMetric.getName())) {
            throw new AssertionError("Complexity is no coverage metric.");
        }
        return printCoverageFor(searchMetric, Locale.getDefault());
    }

    /**
     * Prints the coverage for the specified element.
     *
     * @param searchMetric
     *         the element to print the coverage for
     * @param locale
     *         the locale to use when formatting the percentage
     *
     * @return coverage ratio in a human-readable format
     * @throws AssertionError
     *         if coverage of complexity is wanted
     */
    public String printCoverageFor(final Metric searchMetric, final Locale locale) {
        if ("Complexity".equals(searchMetric.getName())) {
            throw new AssertionError("Complexity is no coverage metric.");
        }

        return getCoverage(searchMetric).formatCoveredPercentage(locale);
    }

    /**
     * Returns the coverage for the specified metric.
     *
     * @param searchMetric
     *         the element to get the coverage for
     *
     * @return coverage ratio
     */
    public Coverage getCoverage(final Metric searchMetric) {
        Coverage childrenCoverage = aggregateChildren(searchMetric);

        if (searchMetric.isLeaf()) {
            return leaves.stream()
                    .filter(leaf -> !leaf.getMetric().equals(Metric.COMPLEXITY)) // all except complexity leaves
                    .map(CoverageLeaf.class::cast)
                    .map(node -> node.getCoverage(searchMetric))
                    .reduce(childrenCoverage, Coverage::add);
        }
        else {
            if (metric.equals(searchMetric)) {
                if (getCoverage(Metric.LINE).getCovered() > 0) {
                    return childrenCoverage.add(COVERED_NODE);
                }
                else {
                    return childrenCoverage.add(MISSED_NODE);
                }
            }
            return childrenCoverage;
        }
    }

    private Coverage aggregateChildren(final Metric searchMetric) {
        return children.stream()
                .map(node -> node.getCoverage(searchMetric))
                .reduce(Coverage.NO_COVERAGE, Coverage::add);
    }

    /**
     * Computes the coverage delta between this node and the specified reference node.
     *
     * @param reference
     *         the reference node
     *
     * @return the delta coverage for each available metric
     */
    public SortedMap<Metric, Fraction> computeDelta(final Node reference) {
        SortedMap<Metric, Fraction> deltaPercentages = new TreeMap<>();
        SortedMap<Metric, Fraction> metricPercentages = getCoverageMetricsPercentages();
        SortedMap<Metric, Fraction> referencePercentages = reference.getCoverageMetricsPercentages();
        metricPercentages.forEach((key, value) ->
                deltaPercentages.put(key,
                        saveSubtractFraction(value, referencePercentages.getOrDefault(key, Fraction.ZERO))));
        return deltaPercentages;
    }

    /**
     * Calculates the difference between two fraction. Since there might be an arithmetic exception due to an overflow,
     * the method handles it and calculates the difference based on the double values of the fractions.
     *
     * @param minuend
     *         The minuend as a fraction
     * @param subtrahend
     *         The subtrahend as a fraction
     *
     * @return the difference as a fraction
     */
    private Fraction saveSubtractFraction(final Fraction minuend, final Fraction subtrahend) {
        try {
            return minuend.subtract(subtrahend);
        }
        catch (ArithmeticException e) {
            double diff = minuend.doubleValue() - subtrahend.doubleValue();
            return Fraction.getFraction(diff);
        }
    }

    //---------------------------- Complexity methods ---------------------------------------------

    /**
     * Sums up the complexity of all methods and returns it. Complexity of the complete module is equal to the
     * complexity of all methods.
     *
     * @return the complexity
     */
    public int getComplexity() {
        int childrenComplexity = children.stream()
                .map(Node::getComplexity)
                .reduce(0, Integer::sum);

        return leaves.stream()
                .filter(leaf -> leaf.getMetric().equals(Metric.COMPLEXITY)) // only complexity leaves
                .map(ComplexityLeaf.class::cast)
                .map(ComplexityLeaf::getComplexity)
                .reduce(childrenComplexity, Integer::sum);
    }

    //---------------------------- Mutation methods ---------------------------------------------

    /**
     * Sums up all killed/survived mutations.
     *
     * @return The amount of killed/survived mutations.
     */
    public MutationResult getMutationResult() {
        MutationResult mutationResult = children.stream()
                .map(Node::getMutationResult)
                .reduce(new MutationResult(), MutationResult::add);

        return leaves.stream()
                .filter(leaf -> leaf.getMetric().equals(Metric.MUTATION)) // only mutation leaves
                .map(MutationLeaf.class::cast)
                .map(MutationLeaf::getResult)
                .reduce(mutationResult, MutationResult::add);
    }
}
