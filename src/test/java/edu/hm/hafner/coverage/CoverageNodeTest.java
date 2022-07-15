package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class CoverageNodeTest {
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

    @Test
    void shouldNotBreakEquals() {
        // Given
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, ".");
        root.add(child);

        // When
        CoverageNode actualCopy = root.copyTree();

        // Then
        assertThat(actualCopy).isEqualTo(root);
    }
}
