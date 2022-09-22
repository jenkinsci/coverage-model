package edu.hm.hafner.complexity;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.model.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.assertions.Assertions.*;

/**
 * Tests the class {@link ComplexityLeaf}.
 *
 * @author Melissa Bauer
 */
class ComplexityLeafTest {

    @Test
    void shouldCreateComplexityLeaf() {
        ComplexityLeaf complexityLeaf = new ComplexityLeaf(125);

        assertThat(complexityLeaf)
                .isSet()
                .hasComplexity(125)
                .hasMetric(Metric.COMPLEXITY)
                .hasToString("[Complexity]: 125");
    }

    @Test
    void shouldCreateEmptyComplexityLeaf() {
        ComplexityLeaf complexityLeaf = new ComplexityLeaf(0);

        assertThat(complexityLeaf)
                .isNotSet()
                .hasComplexity(0)
                .hasMetric(Metric.COMPLEXITY)
                .hasToString("[Complexity]: 0");
    }

    @Test
    void shouldAddComplexityLeaf() {
        ComplexityLeaf complexityLeaf = new ComplexityLeaf(25);
        complexityLeaf = complexityLeaf.add(new ComplexityLeaf(100));

        assertThat(complexityLeaf)
                .isSet()
                .hasComplexity(125)
                .hasMetric(Metric.COMPLEXITY);
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(ComplexityLeaf.class).withNonnullFields("metric", "complexity").verify();
    }
}