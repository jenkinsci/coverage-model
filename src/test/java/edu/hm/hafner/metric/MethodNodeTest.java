package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.metric.assertions.Assertions.*;

class MethodNodeTest extends AbstractNodeTest {
    @Override
    Node createNode(final String name) {
        return new MethodNode(name, 1234);
    }

    @Override
    Metric getMetric() {
        return Metric.METHOD;
    }

    @Test
    void shouldCreateMethodCoverageNode() {
        assertThat(new MethodNode("shouldCreateMethodCoverageNode()", 16))
                .hasMetric(Metric.METHOD)
                .hasName("shouldCreateMethodCoverageNode()")
                .hasLineNumber(16)
                .hasValidLineNumber();
    }

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

    @Test
    void shouldCheckLineNumberZero() {
        // Given
        MethodNode node = new MethodNode("main");

        // When & Then
        assertThat(node).hasMetric(Metric.METHOD).hasLineNumber(0);
    }
}
