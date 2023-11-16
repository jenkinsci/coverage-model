package edu.hm.hafner.coverage;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class MethodNodeTest extends AbstractNodeTest {
    @Override
    Node createNode(final String name) {
        return new MethodNode(name, StringUtils.EMPTY, 1234);
    }

    @Override
    Metric getMetric() {
        return Metric.METHOD;
    }

    @Test
    void shouldCreateMethodCoverageNode() {
        assertThat(new MethodNode("shouldCreateMethodCoverageNode()", "(Ljava/util/Map;)V", 16))
                .hasMetric(Metric.METHOD)
                .hasName("shouldCreateMethodCoverageNode()(Ljava/util/Map;)V")
                .hasMethodName("shouldCreateMethodCoverageNode()")
                .hasSignature("(Ljava/util/Map;)V")
                .hasLineNumber(16)
                .hasValidLineNumber()
                .isNotAggregation();
    }

    @Test
    void shouldGetValidLineNumber() {
        int validLineNumber = 5;
        var node = new MethodNode("main", "(Ljava/util/Map;)V", validLineNumber);

        assertThat(node)
                .hasValidLineNumber()
                .hasLineNumber(validLineNumber);

        int secondValidLineNumber = 1;
        var secondNode = new MethodNode("main", "(Ljava/util/Map;)V",  secondValidLineNumber);
        assertThat(secondNode)
                .hasValidLineNumber()
                .hasLineNumber(secondValidLineNumber);
    }

    @ParameterizedTest(name = "[{index}] Compute method coverage based on {0} metric")
    @EnumSource(value = Metric.class, names = {"LINE", "BRANCH", "INSTRUCTION"})
    void shouldComputeMethodCoverage(final Metric targetMetric) {
        var node = new MethodNode("method", "signature");

        var builder = new CoverageBuilder().withMetric(Metric.METHOD);
        var notCovered = builder.withCovered(0).withMissed(1).build();
        var covered = builder.withCovered(1).withMissed(0).build();

        assertThat(node.getValue(Metric.METHOD)).isPresent().contains(notCovered);

        node.addValue(builder.withMetric(targetMetric).withCovered(1).withMissed(0).build());
        assertThat(node.getValue(Metric.METHOD)).isPresent().contains(covered);
    }

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
