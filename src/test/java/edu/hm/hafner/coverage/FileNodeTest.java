package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class FileNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.FILE;
    }

    @Override
    FileNode createNode(final String name) {
        var fileNode = new FileNode(name);
        fileNode.addCounters(10, 1, 0);
        fileNode.addCounters(11, 2, 2);
        fileNode.addModifiedLines(10);
        fileNode.addIndirectCoverageChange(15, 123);
        var empty = new FileNode("empty");
        fileNode.computeDelta(empty);
        return fileNode;
    }

    @Test
    void shouldGetFilePath() {
        var module = new ModuleNode("top-level"); // just for testing
        var folder = new PackageNode("folder"); // just for testing
        var file = new FileNode("Coverage.java");

        folder.addChild(file);
        module.addChild(folder);

        var absolutePath = "folder/Coverage.java";

        assertThat(file.getPath()).isEqualTo(absolutePath);
        assertThat(file.getFiles()).containsExactly(absolutePath);
        assertThat(folder.getFiles()).containsExactly(absolutePath);
        assertThat(module.getFiles()).containsExactly(absolutePath);

        assertThat(module.getAll(Metric.FILE)).containsExactly(file);
        assertThat(folder.getAll(Metric.FILE)).containsExactly(file);
        assertThat(file.getAll(Metric.FILE)).containsExactly(file);
    }
}
