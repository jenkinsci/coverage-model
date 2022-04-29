package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link MethodCoverageNode}.
 *
 * @author Michael Gasser
 */
class MethodCoverageNodeTest {
    /**
     * Tests with a valid line number.
     */
    @Test
    void shouldGetValidLineNumber() {
        // Given
        int validLineNumber = 5;
        MethodCoverageNode node = new MethodCoverageNode("main", validLineNumber);
        int secondValidLineNumber = 1;
        MethodCoverageNode secondNode = new MethodCoverageNode("main", secondValidLineNumber);

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
        MethodCoverageNode node = new MethodCoverageNode("main", -1);
        MethodCoverageNode secondNode = new MethodCoverageNode("main", 0);

        // When & Then
        assertThat(node).doesNotHaveValidLineNumber();
        assertThat(secondNode).doesNotHaveValidLineNumber();
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(MethodCoverageNode.class)
                .withPrefabValues(
                        CoverageNode.class,
                        new FileCoverageNode("main.c"),
                        new FileCoverageNode("test.c")
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
        MethodCoverageNode node = new MethodCoverageNode("main", 5);

        // When & Then
        assertThat(node).hasToString("[Method] main (5)");
    }
}