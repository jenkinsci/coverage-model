package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.complexity.ComplexityLeaf;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link FileNode}.
 *
 * @author Michael Gasser
 */
class FileNodeTest {
    /**
     * Test if the correct path is returned.
     */
    @Test
    void shouldGetPath() {
        // Given
        String fileName = "main.c";
        FileNode fileNode = new FileNode(fileName);

        // When & Then
        assertThat(fileNode.getPath()).isEqualTo(fileName);
    }

    /**
     * Tests the copy functionality with a child.
     */
    @Test
    void shouldCopyEmpty() {
        // Given
        String fileName = "main.c";
        FileNode fileNode = new FileNode(fileName);
        FileNode fileChild = new FileNode("file.c");
        fileNode.add(fileChild);

        // When
        Node actualEmptyCopy = fileNode.copyEmpty();

        // Then
        assertThat(actualEmptyCopy)
                .hasName(fileName)
                .hasNoChildren()
                .isEqualTo(new FileNode(fileName));
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