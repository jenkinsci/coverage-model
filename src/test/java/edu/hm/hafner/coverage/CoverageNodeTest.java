package edu.hm.hafner.coverage;

import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.CoverageMetric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
class CoverageNodeTest {
    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);

        root.add(new CoverageNode(PACKAGE, ""));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(0);

        root.add(new CoverageNode(PACKAGE, "."));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }
}
