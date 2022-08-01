package edu.hm.hafner.model;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link Node}.
 *
 * @author Ullrich Hafner
 * @author Michael Gasser
 */
class NodeTest {
    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        Node root = new Node(Metric.MODULE, "Root");
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);

        root.add(new Node(Metric.FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        Node root = new Node(Metric.MODULE, "Root");
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);

        root.add(new Node(Metric.PACKAGE, ""));
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        Node root = new Node(Metric.MODULE, "Root");
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(0);

        root.add(new Node(Metric.PACKAGE, "."));
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(1);
    }

    /**
     * Tests if the class can split packages with multiple dots and children.
     */
    @Test
    void shouldSplitPackagesWithMultipleDots() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        root.add(new Node(Metric.PACKAGE, ".ui.rating"));

        // When & Then
        root.splitPackages();
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(3);

        // When & Then
        Node child = new Node(Metric.PACKAGE, ".ui.home");
        child.add(new Node(Metric.PACKAGE, "view"));
        root.add(child);
        root.splitPackages();
        assertThat(child.getChildren()).hasSize(1);
        assertThat(root.getAll(Metric.PACKAGE)).hasSize(5);
    }

    /**
     * Tests if a node keeps its child package after splitting.
     */
    @Test
    void shouldKeepChildAfterSplit() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        Node child = new Node(Metric.PACKAGE, "ui");
        root.add(child);

        // When
        root.splitPackages();

        // Then
        assertThat(root).hasOnlyChildren(child);
    }

    /**
     * Checks for exception if getAll() method is called with a LEAF metric.
     */
    @Test
    void shouldThrowExceptionWithLeafMetric() {
        // Given
        Metric leafMetric = Metric.LINE;
        Node root = new Node(Metric.MODULE, "Root");

        // When & Then
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> root.getAll(leafMetric))
                .withMessageContaining(leafMetric.getName());
    }

    /**
     * Tests all add and parent functionality.
     */
    @Test
    void shouldAddChildren() {
        // Given
        String parentName = "Root";
        Node parent = new Node(Metric.MODULE, parentName);
        String childName = ".";
        Node child = new Node(Metric.PACKAGE, childName);
        Node secondChild = new Node(Metric.PACKAGE, "ui");

        // When
        parent.add(child);
        child.add(secondChild);

        // Then
        assertThat(parent)
                .doesNotHaveParent()
                .isRoot()
                .hasParentName(Node.ROOT);

        assertThat(child)
                .hasParent()
                .hasParent(parent)
                .hasParentName(parentName);

        assertThat(secondChild)
                .hasParent()
                .hasParent(child)
                .hasParentName(childName);
    }

    /**
     * Checks for exception if no parent was set.
     */
    @Test
    void shouldThrowExceptionWithoutParent() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");

        // When & Then
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent);
    }

    /**
     * Tests all merge functionality.
     */
    @Test
    void shouldMergePath() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        Node child = new Node(Metric.PACKAGE, ".");
        child.setParent(root);

        // When & Then
        assertThat(root).hasPath("");
        assertThat(root.mergePath("-")).isEqualTo("");
        assertThat(root.mergePath("test/path")).isEqualTo("test/path");
        assertThat(child.mergePath("test/path")).isEqualTo("test/path");
    }

    /**
     * Tests the constructor and relevant getters.
     */
    @Test
    void shouldInitializeNode() {
        // Given
        String name = "Root";
        Metric metric = Metric.MODULE;
        Node root = new Node(metric, name);

        // When & Then
        assertThat(root)
                .hasName(name)
                .hasMetric(metric);
    }

    /**
     * Tests getMetrics() method.
     */
    @Test
    void shouldGetMetrics() {
        // Given
        Metric rootMetric = Metric.MODULE;
        Node root = new Node(rootMetric, "Root");
        Metric childMetric = Metric.PACKAGE;
        Node child = new Node(childMetric, ".");
        root.add(child);

        // When & Then
        assertThat(root).hasOnlyMetrics(rootMetric, childMetric);
        assertThat(child).hasOnlyMetrics(childMetric);
    }

    /**
     * Helper method to construct a new MODULE node with a LINE leaf of the given coverage.
     *
     * @param coverage
     *         the line coverage to set
     *
     * @return the new node
     */
    private static Node getNodeWithLineCoverage(final Coverage coverage) {
        Node root = new Node(Metric.MODULE, "Root");
        CoverageLeaf leaf = new CoverageLeaf(Metric.LINE, coverage);
        root.add(leaf);
        return root;
    }

    /**
     * Tests the calculation of getMetricsDistribution() and getMetricsPercentages() methods.
     */
    @Test
    void shouldGetMetricsDistributionAndPercentages() {
        // Given
        int linesCovered = 25;
        int linesMissed = 75;
        Fraction coveredPercentage = Fraction.getFraction(linesCovered, linesCovered + linesMissed);
        Node root = getNodeWithLineCoverage(new Coverage(linesCovered, linesMissed));

        // When & Then
        assertThat(root.getMetricsDistribution()).containsExactly(
                entry(Metric.MODULE, new Coverage(1, 0)),
                entry(Metric.LINE, new Coverage(linesCovered, linesMissed))
        );
        assertThat(root.getMetricsPercentages()).containsExactly(
                entry(Metric.MODULE, Fraction.ONE),
                entry(Metric.LINE, coveredPercentage)
        );
    }

    /**
     * Tests line coverage functionality.
     */
    @Test
    void shouldGetAndPrintLineCoverage() {
        // Given
        int linesCovered = 25;
        int linesMissed = 75;
        Fraction coveredPercentage = Fraction.getFraction(linesCovered, linesCovered + linesMissed);
        Node root = getNodeWithLineCoverage(new Coverage(linesCovered, linesMissed));

        // When & Then
        assertThat(root.getCoverage(Metric.LINE))
                .hasCovered(linesCovered)
                .hasMissed(linesMissed)
                .hasCoveredPercentage(coveredPercentage);
        assertThat(root.printCoverageFor(Metric.LINE, Locale.GERMAN)).isEqualTo("25,00%");
        // Default Locale is set to English using beforeAll()
        assertThat(root.printCoverageFor(Metric.LINE)).isEqualTo("25.00%");
    }

    /**
     * Tests module coverage functionality.
     */
    @Test
    void shouldGetModuleCoverage() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        PackageNode pkg = new PackageNode("ui");
        root.add(pkg);

        // When & Then
        assertThat(root.getCoverage(Metric.MODULE)).hasCovered(0);
    }

    /**
     * Tests computeDelta() method.
     */
    @Test
    void shouldComputeDelta() {
        // Given
        Node fullCovered = getNodeWithLineCoverage(new Coverage(100, 0));
        Node halfCovered = getNodeWithLineCoverage(new Coverage(50, 50));

        // When & Then
        assertThat(fullCovered.computeDelta(halfCovered)).containsExactly(
                entry(Metric.MODULE, Fraction.ZERO),
                entry(Metric.LINE, Fraction.ONE_HALF)
        );
    }

    /**
     * Tests computeDelta() method with an occurring overflow.
     */
    @Test
    void shouldComputeDeltaWithOverflow() {
        // Given
        Node hugeMissed = getNodeWithLineCoverage(new Coverage(1, Integer.MAX_VALUE - 1));
        Node otherHugeMissed = getNodeWithLineCoverage(new Coverage(1, Integer.MAX_VALUE - 2));
        Node hugeCovered = getNodeWithLineCoverage(new Coverage(Integer.MAX_VALUE - 1, 1));
        Node halfCovered = getNodeWithLineCoverage(new Coverage(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2));

        // When & Then
        assertThat(hugeMissed.computeDelta(otherHugeMissed)).containsExactly(
                entry(Metric.MODULE, Fraction.ZERO),
                entry(Metric.LINE, Fraction.ZERO)
        );

        assertThat(hugeCovered.computeDelta(halfCovered)).containsExactly(
                entry(Metric.MODULE, Fraction.ZERO),
                entry(Metric.LINE, Fraction.ONE_HALF)
        );
    }

    /**
     * Tests all find functionality.
     */
    @Test
    void shouldFindMetric() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        Metric childMetric = Metric.PACKAGE;
        String childName = ".";
        Node child = new Node(childMetric, childName);
        root.add(child);

        // When & Then
        assertThat(root.find(childMetric, childName)).hasValue(child);
        assertThat(root.findByHashCode(childMetric, childName.hashCode())).hasValue(child);
    }

    /**
     * Tests all match functionality.
     */
    @Test
    void shouldMatchMetricAndName() {
        // Given
        Metric metric = Metric.MODULE;
        String name = "Root";
        Node root = new Node(metric, name);

        // When & Then
        assertThat(root.matches(metric, name)).isTrue();
        assertThat(root.matches(metric, name.hashCode())).isTrue();

        assertThat(root.matches(Metric.LINE, name)).isFalse();
        assertThat(root.matches(metric, "wrongName")).isFalse();
    }

    /**
     * Tests add functionality for child and leaf.
     */
    @Test
    void shouldAddChildAndLeaf() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        Node child = new Node(Metric.PACKAGE, ".");
        CoverageLeaf leaf = new CoverageLeaf(Metric.FILE, Coverage.NO_COVERAGE);

        // When
        root.add(child);
        root.add(leaf);

        // Then
        assertThat(root)
                .hasOnlyChildren(child)
                .hasOnlyLeaves(leaf);
    }

    /**
     * Tests the toString() method.
     */
    @Test
    void shouldTextuallyRepresent() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");

        // When & Then
        assertThat(root).hasToString("[Module] Root");
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(Node.class)
                .withPrefabValues(
                        Node.class,
                        new FileNode("main.c"),
                        new PackageNode("ui")
                )
                .withIgnoredFields("parent")
                .verify();
    }

    /**
     * Tests copy functionality with a child.
     */
    @Test
    void shouldCopyWithChild() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        Node newRoot = new Node(Metric.MODULE, "New root");
        Node child = new Node(Metric.PACKAGE, ".");
        root.add(child);

        // When
        Node withoutRootCopy = (Node) root.copyTree();
        Node withRootCopy = (Node) root.copyTree(newRoot);

        // Then
        assertThat(withoutRootCopy.copyEmpty()).isEqualTo(new Node(Metric.MODULE, "Root"));
        assertThat(withoutRootCopy)
                .hasOnlyChildren(child)
                .isEqualTo(root);

        assertThat(withRootCopy)
                .hasParent(newRoot)
                .hasOnlyChildren(child);
    }

    /**
     * Tests copy functionality with a leaf.
     */
    @Test
    void shouldCopyLeaf() {
        // Given
        Node root = new Node(Metric.MODULE, "Root");
        CoverageLeaf leaf = new CoverageLeaf(Metric.LINE, Coverage.NO_COVERAGE);
        root.add(leaf);

        // When & Then
        assertThat(root.copyTree())
                .isEqualTo(root)
                .hasOnlyLeaves(leaf);
    }
}
