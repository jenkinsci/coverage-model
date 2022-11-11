package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

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
        var moduleNode = new ModuleNode(name);
        moduleNode.addSource("/path/to/sources");
        return moduleNode;
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

        eduPackage.addChild(new FileNode("edu/File.c"));

        var builder = new CoverageBuilder().setMetric(LINE);
        eduPackage.addValue(builder.setCovered(10).setMissed(0).build());

        assertThat(root.getAll(PACKAGE)).hasSize(2);
        assertThat(root.getValue(LINE)).contains(builder.build());

        var subPackage = new PackageNode("edu.hm.hafner");
        root.addChild(subPackage);
        subPackage.addValue(builder.setMissed(10).build());
        subPackage.addChild(new FileNode("edu.hm.hafner/OtherFile.c"));
        assertThat(root.getValue(LINE)).contains(builder.setCovered(20).setMissed(10).build());

        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(4);

        assertThat(root.getChildren()).hasSize(2).satisfiesExactlyInAnyOrder(
                org -> assertThat(org.getName()).isEqualTo("org"),
                edu -> assertThat(edu.getName()).isEqualTo("edu"));

        assertThat(root.getValue(LINE)).contains(builder.setCovered(20).setMissed(10).build());

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
