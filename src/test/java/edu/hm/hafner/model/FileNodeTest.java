package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class FileNodeTest {

    @Test
    void shouldCreateFileCoverageNode() {
        assertThat(new FileNode("MyFileName"))
                .hasMetric(Metric.FILE)
                .hasName("MyFileName");
    }

    @Test
    void shouldGetFilePath() {
        FileNode folderCoverageNode = new FileNode("folder"); // just for testing
        FileNode fileCoverageNode = new FileNode("Coverage.java");

        folderCoverageNode.add(fileCoverageNode);

        assertThat(fileCoverageNode.getPath()).isEqualTo("folder/Coverage.java");
    }

    @Test
    void shouldPerformEmptyCopyWithoutChildren() {
        FileNode folderCoverageNode = new FileNode("folder");
        FileNode fileCoverageNode = new FileNode("Coverage.java");

        folderCoverageNode.add(fileCoverageNode);

        assertThat(folderCoverageNode).hasChildren(fileCoverageNode);
        assertThat(folderCoverageNode.copyEmpty())
                .hasNoChildren()
                .isNotSameAs(folderCoverageNode);

    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(FileNode.class)
                .withPrefabValues(
                        Node.class,
                        new PackageNode("src"),
                        new PackageNode("test")
                )
                .withIgnoredFields("parent")
                .withNonnullFields("metric", "name", "lineNumberToBranchCoverage", "lineNumberToInstructionCoverage")
                .verify();
    }
}
