package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class PackageNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return PACKAGE;
    }

    @Override
    Node createNode(final String name) {
        return new PackageNode(name);
    }

    /**
     * TestCount the copy functionality with a child.
     */
    @Test
    void shouldCopyEmpty() {
        var parentName = ".ui.home.model";
        var parent = new PackageNode(parentName);
        var child = new PackageNode("data");
        parent.addChild(child);

        var actualEmptyCopy = parent.copy();

        assertThat(actualEmptyCopy)
                .hasName(parentName)
                .hasNoChildren()
                .isEqualTo(new PackageNode(parentName))
                .isAggregation();
    }

    /**
     * TestCount the match functionality using a path hashcode.
     */
    @Test
    void shouldMatchPath() {
        var pkgName = "ui.home.model";
        var pkg = new PackageNode(pkgName);

        assertThat(pkg.matches(PACKAGE, "ui.home.model".hashCode())).isTrue();
        assertThat(pkg.matches(PACKAGE, "test.path".hashCode())).isFalse();
    }

    @Test
    void shouldSplitPackages() {
        var root = new ModuleNode("root");

        root.addChild(new PackageNode("left"));
        root.addChild(new PackageNode("left.right"));

        assertThat(root.getAll(PACKAGE)).extracting(Node::getName)
                .containsExactlyInAnyOrder("left", "left.right");

        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).extracting(Node::getName)
                .containsExactlyInAnyOrder("left", "right");
    }

    @Test
    void shouldSplitReversePackages() {
        var root = new ModuleNode("root");

        root.addChild(new PackageNode("left.right"));
        root.addChild(new PackageNode("left"));

        assertThat(root.getAll(PACKAGE)).extracting(Node::getName)
                .containsExactlyInAnyOrder("left", "left.right");

        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).extracting(Node::getName)
                .containsExactlyInAnyOrder("left", "right");
    }

    @Test
    void shouldNormalizePackageNameCorrectly() {
        var normalizedName = "edu.hm.hafner";

        assertThat(PackageNode.normalizePackageName("edu/hm/hafner")).isEqualTo(normalizedName);
        assertThat(PackageNode.normalizePackageName("edu\\hm\\hafner")).isEqualTo(normalizedName);
    }
}
