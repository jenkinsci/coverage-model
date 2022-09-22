package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

class ModuleNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.MODULE;
    }

    @Override
    Node createNode(final String name) {
        return new ModuleNode(name);
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        ModuleNode root = new ModuleNode("Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.addChild(new FileNode("file.c"));
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        ModuleNode root = new ModuleNode("Root");
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
        ModuleNode root = new ModuleNode("Root");
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
    void shouldDetectExistingPackagesOnSplit() {
        ModuleNode root = new ModuleNode("Root");
        Node eduPackage = new PackageNode("edu");
        Node differentPackage = new PackageNode("org");

        root.addChild(differentPackage);
        root.addChild(eduPackage);

        assertThat(root.getAll(PACKAGE)).hasSize(2);

        root.addChild(new PackageNode("edu.hm.hafner"));
        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).hasSize(4);
    }

    @Test
    void shouldKeepNodesAfterSplitting() {
        ModuleNode root = new ModuleNode("Root");
        Node pkg = new PackageNode("edu.hm.hafner");
        Node file = new FileNode("HelloWorld.java");

        root.addChild(pkg);
        pkg.addChild(file);
        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).hasSize(3);
        assertThat(root.getAll(FILE)).hasSize(1);
    }
}
