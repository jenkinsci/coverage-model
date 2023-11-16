package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class ModuleNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return MODULE;
    }

    @Override
    Node createNode(final String name) {
        var moduleNode = new ModuleNode(name);
        moduleNode.addSource("/path/to/sources");
        assertThat(moduleNode).hasSourceFolders("/path/to/sources").isAggregation();
        return moduleNode;
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        var root = new ModuleNode("Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.addChild(new FileNode("file.c", "path"));
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        var root = new ModuleNode("Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        Node packageNode = new PackageNode("");
        root.addChild(packageNode);
        assertThat(root.getAll(PACKAGE)).hasSize(1);

        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        assertThat(root).hasOnlyChildren(packageNode);
    }

    @Test
    void shouldSplitPackagesIntoHierarchy() {
        var root = new ModuleNode("Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.addChild(new PackageNode("edu.hm.hafner"));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(3).satisfiesExactly(
                s -> assertThat(s).hasName("hafner"),
                s -> assertThat(s).hasName("hm"),
                s -> assertThat(s).hasName("edu")
        );
    }

    @Test
    void shouldKeepNodesAfterSplitting() {
        var root = new ModuleNode("Root");
        Node pkg = new PackageNode("edu.hm.hafner");
        Node file = new FileNode("HelloWorld.java", "path");

        root.addChild(pkg);
        pkg.addChild(file);
        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).hasSize(3);
        assertThat(root.getAll(FILE)).hasSize(1);
    }

    @Test
    void shouldNotMergeWhenDifferentMetric() {
        var root = new ModuleNode("Root");
        Node pkg = new PackageNode("edu.hm.hafner");
        Node file = new FileNode("Helicopter.java", "path");

        root.addChild(pkg);
        root.addChild(file);
        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).hasSize(3);
        assertThat(root.getAll(FILE)).hasSize(1);
    }
}
