package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link PackageNode}.
 *
 * @author Michael Gasser
 */
class PackageNodeTest {
    /**
     * Tests if correct package path is returned.
     */
    @Test
    void shouldGetPath() {
        // Given
        String pkgName = ".ui.home.model";
        PackageNode pkg = new PackageNode(pkgName);

        // When & Then
        assertThat(pkg)
                .hasName(pkgName)
                .hasMetric(Metric.PACKAGE);
        assertThat(pkg.getPath()).isEqualTo("/ui/home/model");
    }

    /**
     * Tests the path merge functionality with a child.
     */
    @Test
    void shouldMergePath() {
        // Given
        String parentName = "ui";
        PackageNode parent = new PackageNode(parentName);
        PackageNode child = new PackageNode("model");
        child.setParent(parent);

        // When & Then
        assertThat(child.mergePath("Update.java")).isEqualTo(parentName + "/Update.java");
        assertThat(child.mergePath("")).isEqualTo(parentName);
    }

    /**
     * Tests the copy functionality with a child.
     */
    @Test
    void shouldCopyEmpty() {
        // Given
        String parentName = ".ui.home.model";
        PackageNode parent = new PackageNode(parentName);
        PackageNode child = new PackageNode("data");
        parent.add(child);

        // When
        Node actualEmptyCopy = parent.copyEmpty();

        // Then
        assertThat(actualEmptyCopy)
                .hasName(parentName)
                .hasNoChildren()
                .isEqualTo(new PackageNode(parentName));
    }

    /**
     * Tests the match functionality using a path hashcode.
     */
    @Test
    void shouldMatchPath() {
        // Given
        String pkgName = ".ui.home.model";
        PackageNode pkg = new PackageNode(pkgName);

        // When & Then
        assertThat(pkg.matches(Metric.PACKAGE, "/ui/home/model".hashCode())).isTrue();
        assertThat(pkg.matches(Metric.PACKAGE, "/test/path".hashCode())).isFalse();
    }

    /**
     * Verifies that a standalone package is not split.
     */
    @Test
    void shouldNotSplitPackage() {
        // Given
        PackageNode pkg = new PackageNode(".ui.home.model");

        // When
        pkg.splitPackages();

        // Then
        assertThat(pkg).hasNoChildren();
    }
}