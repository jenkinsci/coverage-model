package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class MethodCoverageNodeTest {

    @Test
    void shouldCreateMethodCoverageNode() {
        assertThat(new MethodCoverageNode("shouldCreateMethodCoverageNode()", 16))
                .hasMetric(CoverageMetric.METHOD)
                .hasName("shouldCreateMethodCoverageNode()")
                .hasLineNumber(16)
                .hasValidLineNumber()
                .hasToString("[Method] shouldCreateMethodCoverageNode() (16)");

    }

    @Test
    void shouldDetectInvalidLineNumber() {
        MethodCoverageNode methodCoverageNode = new MethodCoverageNode("shouldCreateMethodCoverageNode()", -1);
        MethodCoverageNode methodCoverageNodeOnLineZero = new MethodCoverageNode("shouldCreateMethodCoverageNode()", 0);
        MethodCoverageNode methodCoverageNodeOnLineOne = new MethodCoverageNode("shouldCreateMethodCoverageNode()", 1);

        assertThat(methodCoverageNode).doesNotHaveValidLineNumber();
        assertThat(methodCoverageNodeOnLineZero).doesNotHaveValidLineNumber();
        assertThat(methodCoverageNodeOnLineOne).hasValidLineNumber();
    }

    @Test
    void shouldCorrectlyImplementEquality() {
        EqualsVerifier.simple().forClass(MethodCoverageNode.class)
                .withIgnoredFields("parent")
                .withPrefabValues(CoverageNode.class, new CoverageNode(CoverageMetric.LINE, "line"), new CoverageNode(CoverageMetric.LINE, "line2"))
                .verify();
    }

}
