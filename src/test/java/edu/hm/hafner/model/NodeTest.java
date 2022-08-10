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
import static edu.hm.hafner.model.Metric.FILE;
import static edu.hm.hafner.model.Metric.*;

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
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.add(new Node(FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.add(new Node(PACKAGE, ""));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.add(new Node(PACKAGE, "."));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }

    /**
     * Tests if the class can split packages with multiple dots and children.
     */
    @Test
    void shouldSplitPackagesWithMultipleDots() {
        // Given
        Node root = new Node(MODULE, "Root");
        root.add(new Node(PACKAGE, ".ui.rating"));

        // When & Then
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(3);

        // When & Then
        Node child = new Node(PACKAGE, ".ui.home");
        child.add(new Node(PACKAGE, "view"));
        root.add(child);
        root.splitPackages();
        assertThat(child.getChildren()).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(5);
    }

    /**
     * Tests if a node keeps its child package after splitting.
     */
    @Test
    void shouldKeepChildAfterSplit() {
        // Given
        Node root = new Node(MODULE, "Root");
        Node child = new Node(PACKAGE, "ui");
        root.add(child);

        // When
        root.splitPackages();

        // Then
        assertThat(root).hasOnlyChildren(child);
    }

    /**
     * Checks for exception if getAll() method is called with a LEAF.
     */
    @Test
    void shouldThrowExceptionWithLeafMetric() {
        // Given
        Metric leafMetric = LINE;
        Node root = new Node(MODULE, "Root");

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
        Node parent = new Node(MODULE, parentName);
        String childName = ".";
        Node child = new Node(PACKAGE, childName);
        Node secondChild = new Node(PACKAGE, "ui");

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
        Node root = new Node(MODULE, "Root");

        // When & Then
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent);
    }

    /**
     * Tests all merge functionality.
     */
    @Test
    void shouldMergePath() {
        // Given
        Node root = new Node(MODULE, "Root");
        Node child = new Node(PACKAGE, ".");
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
        Metric metric = MODULE;
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
        Metric rootMetric = MODULE;
        Node root = new Node(rootMetric, "Root");
        Metric childMetric = PACKAGE;
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
        Node root = new Node(MODULE, "Root");
        CoverageLeaf leaf = new CoverageLeaf(LINE, coverage);
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
        assertThat(root.getCoverageMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(1, 0)),
                entry(LINE, new Coverage(linesCovered, linesMissed))
        );
        assertThat(root.getCoverageMetricsPercentages()).containsExactly(
                entry(MODULE, Fraction.ONE),
                entry(LINE, coveredPercentage)
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
        assertThat(root.getCoverage(LINE))
                .hasCovered(linesCovered)
                .hasMissed(linesMissed)
                .hasCoveredPercentage(coveredPercentage);
        assertThat(root.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("25,00%");
        // Default Locale is set to English using beforeAll()
        assertThat(root.printCoverageFor(LINE)).isEqualTo("25.00%");
    }

    /**
     * Tests module coverage functionality.
     */
    @Test
    void shouldGetModuleCoverage() {
        // Given
        Node root = new Node(MODULE, "Root");
        PackageNode pkg = new PackageNode("ui");
        root.add(pkg);

        // When & Then
        assertThat(root.getCoverage(MODULE)).hasCovered(0);
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
                entry(MODULE, Fraction.ZERO),
                entry(LINE, Fraction.ONE_HALF)
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
                entry(MODULE, Fraction.ZERO),
                entry(LINE, Fraction.ZERO)
        );

        assertThat(hugeCovered.computeDelta(halfCovered)).containsExactly(
                entry(MODULE, Fraction.ZERO),
                entry(LINE, Fraction.ONE_HALF)
        );
    }

    /**
     * Tests all find functionality.
     */
    @Test
    void shouldFindMetric() {
        // Given
        Node root = new Node(MODULE, "Root");
        Metric childMetric = PACKAGE;
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
        Metric metric = MODULE;
        String name = "Root";
        Node root = new Node(metric, name);

        // When & Then
        assertThat(root.matches(metric, name)).isTrue();
        assertThat(root.matches(metric, name.hashCode())).isTrue();

        assertThat(root.matches(LINE, name)).isFalse();
        assertThat(root.matches(metric, "wrongName")).isFalse();
    }

    /**
     * Tests add functionality for child and leaf.
     */
    @Test
    void shouldAddChildAndLeaf() {
        // Given
        Node root = new Node(MODULE, "Root");
        Node child = new Node(PACKAGE, ".");
        CoverageLeaf leaf = new CoverageLeaf(LINE, Coverage.NO_COVERAGE);

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
        Node root = new Node(MODULE, "Root");

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
        Node root = new Node(MODULE, "Root");
        Node newRoot = new Node(MODULE, "New root");
        Node child = new Node(PACKAGE, ".");
        root.add(child);

        // When
        Node withoutRootCopy = root.copyTree();
        Node withRootCopy = root.copyTree(newRoot);

        // Then
        assertThat(withoutRootCopy.copyEmpty()).isEqualTo(new Node(MODULE, "Root"));
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
        Node root = new Node(MODULE, "Root");
        CoverageLeaf leaf = new CoverageLeaf(LINE, Coverage.NO_COVERAGE);
        root.add(leaf);

        // When & Then
        assertThat(root.copyTree())
                .isEqualTo(root)
                .hasOnlyLeaves(leaf);
    }
}
