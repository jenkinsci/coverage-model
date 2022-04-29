package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link FileCoverageNode}.
 *
 * @author Michael Gasser
 */
class FileCoverageNodeTest {
    /**
     * Test if the correct path is returned.
     */
    @Test
    void shouldGetPath() {
        // Given
        String fileName = "main.c";
        FileCoverageNode fileNode = new FileCoverageNode(fileName);

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
        FileCoverageNode fileNode = new FileCoverageNode(fileName);
        FileCoverageNode fileChild = new FileCoverageNode("file.c");
        fileNode.add(fileChild);

        // When
        CoverageNode actualEmptyCopy = fileNode.copyEmpty();

        // Then
        assertThat(actualEmptyCopy)
                .hasName(fileName)
                .hasNoChildren()
                .isEqualTo(new FileCoverageNode(fileName));
    }
}