package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.model.FileNode;
import edu.hm.hafner.model.MethodNode;
import edu.hm.hafner.model.Node;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link MethodNode}.
 *
 * @author Michael Gasser
 */
class MethodNodeTest {
    /**
     * Tests with a valid line number.
     */
    @Test
    void shouldGetValidLineNumber() {
        // Given
        int validLineNumber = 5;
        MethodNode node = new MethodNode("main", validLineNumber);
        int secondValidLineNumber = 1;
        MethodNode secondNode = new MethodNode("main", secondValidLineNumber);

        // When & Then
        assertThat(node)
                .hasValidLineNumber()
                .hasLineNumber(validLineNumber);
        assertThat(secondNode)
                .hasValidLineNumber()
                .hasLineNumber(secondValidLineNumber);
    }

    /**
     * Tests if an invalid line number is recognized.
     */
    @Test
    void shouldCheckInvalidLineNumber() {
        // Given
        MethodNode node = new MethodNode("main", -1);
        MethodNode secondNode = new MethodNode("main", 0);

        // When & Then
        assertThat(node).doesNotHaveValidLineNumber();
        assertThat(secondNode).doesNotHaveValidLineNumber();
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(MethodNode.class)
                .withPrefabValues(
                        Node.class,
                        new FileNode("main.c"),
                        new FileNode("test.c")
                )
                .withIgnoredFields("parent")
                .verify();
    }

    /**
     * Tests toString() method.
     */
    @Test
    void shouldTextuallyRepresent() {
        // Given
        MethodNode node = new MethodNode("main", 5);

        // When & Then
        assertThat(node).hasToString("[Method] main (5)");
    }
}