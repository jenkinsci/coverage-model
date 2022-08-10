package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.complexity.ComplexityLeaf;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class ClassNodeTest {

    @Test
    void shouldCreateClassNode() {
        ClassNode classNode = new ClassNode("TestClass");

        assertThat(classNode)
                .hasMetrics(Metric.CLASS)
                .hasName("TestClass")
                .hasToString("[Class] TestClass");
    }
}