package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;

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