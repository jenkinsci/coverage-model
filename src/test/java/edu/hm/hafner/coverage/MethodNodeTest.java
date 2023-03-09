package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class MethodNodeTest extends AbstractNodeTest {
    @Override
    Node createNode(final String name) {
        return new MethodNode(name, "(Ljava/util/Map;)V", 1234);
    }

    @Override
    Metric getMetric() {
        return Metric.METHOD;
    }

    @Test
    void shouldCreateMethodCoverageNode() {
        assertThat(new MethodNode("shouldCreateMethodCoverageNode()", "(Ljava/util/Map;)V", 16))
                .hasMetric(Metric.METHOD)
                .hasName("shouldCreateMethodCoverageNode()")
                .hasSignature("(Ljava/util/Map;)V")
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
        var node = new MethodNode("main", "(Ljava/util/Map;)V", validLineNumber);
        int secondValidLineNumber = 1;
        var secondNode = new MethodNode("main", "(Ljava/util/Map;)V",  secondValidLineNumber);

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
        var node = new MethodNode("main", "(Ljava/util/Map;)V", -1);
        var secondNode = new MethodNode("main", "(Ljava/util/Map;)V", 0);

        // When & Then
        assertThat(node).doesNotHaveValidLineNumber();
        assertThat(secondNode).doesNotHaveValidLineNumber();
    }

    @Test
    void shouldCheckLineNumberZero() {
        // Given
        var node = new MethodNode("main", "(Ljava/util/Map;)V");

        // When & Then
        assertThat(node).hasMetric(Metric.METHOD).hasLineNumber(0);
    }
}
