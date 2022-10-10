package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.metric.assertions.Assertions.*;

class FileNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.FILE;
    }

    @Override
    Node createNode(final String name) {
        return new FileNode(name);
    }

    @Test
    void shouldGetFilePath() {
        ModuleNode module = new ModuleNode("top-level"); // just for testing
        PackageNode folder = new PackageNode("folder"); // just for testing
        FileNode file = new FileNode("Coverage.java");

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
