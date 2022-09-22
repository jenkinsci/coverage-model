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
        FileNode folderCoverageNode = new FileNode("folder"); // just for testing
        FileNode fileCoverageNode = new FileNode("Coverage.java");

        folderCoverageNode.addChild(fileCoverageNode);

        assertThat(fileCoverageNode.getPath()).isEqualTo("folder/Coverage.java");
    }
}
