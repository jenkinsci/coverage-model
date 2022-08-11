package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class FileCoverageNodeTest {

    @Test
    void shouldCreateFileCoverageNode() {
        assertThat(new FileCoverageNode("MyFileName"))
                .hasMetric(CoverageMetric.FILE)
                .hasName("MyFileName");
    }

    @Test
    void shouldGetFilePath() {
        FileCoverageNode folderCoverageNode = new FileCoverageNode("folder"); // just for testing
        FileCoverageNode fileCoverageNode = new FileCoverageNode("Coverage.java");

        folderCoverageNode.add(fileCoverageNode);

        assertThat(fileCoverageNode.getPath()).isEqualTo("folder/Coverage.java");
    }

    @Test
    void shouldPerformEmptyCopyWithoutChildren() {
        FileCoverageNode folderCoverageNode = new FileCoverageNode("folder");
        FileCoverageNode fileCoverageNode = new FileCoverageNode("Coverage.java");

        folderCoverageNode.add(fileCoverageNode);

        assertThat(folderCoverageNode).hasChildren(fileCoverageNode);
        assertThat(folderCoverageNode.copyEmpty())
                .hasNoChildren()
                .isNotSameAs(folderCoverageNode);

    }

}