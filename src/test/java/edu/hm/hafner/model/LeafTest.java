package edu.hm.hafner.model;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;
import static edu.hm.hafner.model.Metric.*;

/**
 * Tests the class {@link Node}.
 *
 * @author Melissa Bauer
 */
@DefaultLocale("en")
class LeafTest {

    @Test
    void shouldCreateLeaf() {
        Leaf leaf = new Leaf(Metric.LINE);

        assertThat(leaf)
                .hasMetric(Metric.LINE)
                .hasToString("Line");
    }

    @Test
    void shouldThrowExcpetionWithNoLeafMetric() {
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> new Leaf(MODULE))
                .withMessageContaining(MODULE.getName());
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(Leaf.class).withNonnullFields("metric").verify();
    }
}