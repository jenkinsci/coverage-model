package edu.hm.hafner.coverage;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 * @author Michael Gasser
 */
class CoverageNodeTest {
    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, ""));
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, "."));
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
    }

    /**
     * Tests if the class can split packages with multiple dots and children.
     */
    @Test
    void shouldSplitPackagesWithMultipleDots() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        root.add(new CoverageNode(CoverageMetric.PACKAGE, ".ui.rating"));

        // When & Then
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(3);

        // When & Then
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, ".ui.home");
        child.add(new CoverageNode(CoverageMetric.PACKAGE, "view"));
        root.add(child);
        root.splitPackages();
        assertThat(child.getChildren()).hasSize(1);
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(5);
    }

    /**
     * Tests if a node keeps its child package after splitting.
     */
    @Test
    void shouldKeepChildAfterSplit() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, "ui");
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
        CoverageMetric leafMetric = CoverageMetric.LINE;
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");

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
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, parentName);
        String childName = ".";
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, childName);
        CoverageNode secondChild = new CoverageNode(CoverageMetric.PACKAGE, "ui");

        // When
        parent.add(child);
        child.add(secondChild);

        // Then
        assertThat(parent)
                .doesNotHaveParent()
                .isRoot()
                .hasParentName(CoverageNode.ROOT);

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
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");

        // When & Then
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent);
    }

    /**
     * Tests all merge functionality.
     */
    @Test
    void shouldMergePath() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, ".");
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
        CoverageMetric metric = CoverageMetric.MODULE;
        CoverageNode root = new CoverageNode(metric, name);

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
        CoverageMetric rootMetric = CoverageMetric.MODULE;
        CoverageNode root = new CoverageNode(rootMetric, "Root");
        CoverageMetric childMetric = CoverageMetric.PACKAGE;
        CoverageNode child = new CoverageNode(childMetric, ".");
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
    private static CoverageNode getNodeWithLineCoverage(final Coverage coverage) {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.LINE, coverage);
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
        CoverageNode root = getNodeWithLineCoverage(new Coverage(linesCovered, linesMissed));

        // When & Then
        assertThat(root.getMetricsDistribution()).containsExactly(
                entry(CoverageMetric.MODULE, new Coverage(1, 0)),
                entry(CoverageMetric.LINE, new Coverage(linesCovered, linesMissed))
        );
        assertThat(root.getMetricsPercentages()).containsExactly(
                entry(CoverageMetric.MODULE, Fraction.ONE),
                entry(CoverageMetric.LINE, coveredPercentage)
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
        CoverageNode root = getNodeWithLineCoverage(new Coverage(linesCovered, linesMissed));

        // When & Then
        assertThat(root.getCoverage(CoverageMetric.LINE))
                .hasCovered(linesCovered)
                .hasMissed(linesMissed)
                .hasCoveredPercentage(coveredPercentage);
        assertThat(root.printCoverageFor(CoverageMetric.LINE, Locale.GERMAN)).isEqualTo("25,00%");
        // Default Locale is set to English using beforeAll()
        assertThat(root.printCoverageFor(CoverageMetric.LINE)).isEqualTo("25.00%");
    }

    /**
     * Tests module coverage functionality.
     */
    @Test
    void shouldGetModuleCoverage() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        PackageCoverageNode pkg = new PackageCoverageNode("ui");
        root.add(pkg);

        // When & Then
        assertThat(root.getCoverage(CoverageMetric.MODULE)).hasCovered(0);
    }

    /**
     * Tests computeDelta() method.
     */
    @Test
    void shouldComputeDelta() {
        // Given
        CoverageNode fullCovered = getNodeWithLineCoverage(new Coverage(100, 0));
        CoverageNode halfCovered = getNodeWithLineCoverage(new Coverage(50, 50));

        // When & Then
        assertThat(fullCovered.computeDelta(halfCovered)).containsExactly(
                entry(CoverageMetric.MODULE, Fraction.ZERO),
                entry(CoverageMetric.LINE, Fraction.ONE_HALF)
        );
    }

    /**
     * Tests computeDelta() method with an occurring overflow.
     */
    @Test
    void shouldComputeDeltaWithOverflow() {
        // Given
        CoverageNode hugeMissed = getNodeWithLineCoverage(new Coverage(1, Integer.MAX_VALUE - 1));
        CoverageNode otherHugeMissed = getNodeWithLineCoverage(new Coverage(1, Integer.MAX_VALUE - 2));
        CoverageNode hugeCovered = getNodeWithLineCoverage(new Coverage(Integer.MAX_VALUE - 1, 1));
        CoverageNode halfCovered = getNodeWithLineCoverage(new Coverage(Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2));

        // When & Then
        assertThat(hugeMissed.computeDelta(otherHugeMissed)).containsExactly(
                entry(CoverageMetric.MODULE, Fraction.ZERO),
                entry(CoverageMetric.LINE, Fraction.ZERO)
        );

        assertThat(hugeCovered.computeDelta(halfCovered)).containsExactly(
                entry(CoverageMetric.MODULE, Fraction.ZERO),
                entry(CoverageMetric.LINE, Fraction.ONE_HALF)
        );
    }

    /**
     * Tests all find functionality.
     */
    @Test
    void shouldFindMetric() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageMetric childMetric = CoverageMetric.PACKAGE;
        String childName = ".";
        CoverageNode child = new CoverageNode(childMetric, childName);
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
        CoverageMetric metric = CoverageMetric.MODULE;
        String name = "Root";
        CoverageNode root = new CoverageNode(metric, name);

        // When & Then
        assertThat(root.matches(metric, name)).isTrue();
        assertThat(root.matches(metric, name.hashCode())).isTrue();

        assertThat(root.matches(CoverageMetric.LINE, name)).isFalse();
        assertThat(root.matches(metric, "wrongName")).isFalse();
    }

    /**
     * Tests add functionality for child and leaf.
     */
    @Test
    void shouldAddChildAndLeaf() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, ".");
        CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.FILE, Coverage.NO_COVERAGE);

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
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");

        // When & Then
        assertThat(root).hasToString("[Module] Root");
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(CoverageNode.class)
                .withPrefabValues(
                        CoverageNode.class,
                        new FileCoverageNode("main.c"),
                        new PackageCoverageNode("ui")
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
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode newRoot = new CoverageNode(CoverageMetric.MODULE, "New root");
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, ".");
        root.add(child);

        // When
        CoverageNode withoutRootCopy = root.copyTree();
        CoverageNode withRootCopy = root.copyTree(newRoot);

        // Then
        assertThat(withoutRootCopy.copyEmpty()).isEqualTo(new CoverageNode(CoverageMetric.MODULE, "Root"));
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
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageLeaf leaf = new CoverageLeaf(CoverageMetric.LINE, Coverage.NO_COVERAGE);
        root.add(leaf);

        // When & Then
        assertThat(root.copyTree())
                .isEqualTo(root)
                .hasOnlyLeaves(leaf);
    }
}
