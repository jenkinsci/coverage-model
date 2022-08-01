package edu.hm.hafner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageLeaf;
import edu.hm.hafner.model.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link Metric}.
 *
 * @author Ullrich Hafner
 */
class MetricTest {
    @Test
    void shouldSortMetrics() {
        List<Metric> all = new ArrayList<>();
        all.add(Metric.MODULE);
        all.add(Metric.PACKAGE);
        all.add(Metric.FILE);
        all.add(Metric.CLASS);
        all.add(Metric.METHOD);
        all.add(Metric.LINE);
        all.add(Metric.BRANCH);
        all.add(Metric.INSTRUCTION);
        all.add(Metric.COMPLEXITY);
        all.add(Metric.MUTATION);

        Collections.sort(all);
        verifyOrder(all);

        Collections.reverse(all);
        assertThat(all).containsExactly(
                Metric.MUTATION,
                Metric.COMPLEXITY,
                Metric.BRANCH,
                Metric.INSTRUCTION,
                Metric.LINE,
                Metric.METHOD,
                Metric.CLASS,
                Metric.FILE,
                Metric.PACKAGE,
                Metric.MODULE);

        Collections.sort(all);
        verifyOrder(all);
    }

    @Test
    void shouldGetAvailableMetrics() {
        assertThat(Metric.getAvailableCoverageMetrics()).containsExactlyInAnyOrder(
                Metric.MUTATION,
                Metric.COMPLEXITY,
                Metric.BRANCH,
                Metric.INSTRUCTION,
                Metric.LINE,
                Metric.METHOD,
                Metric.CLASS,
                Metric.FILE,
                Metric.PACKAGE,
                Metric.MODULE
        );
    }

    private void verifyOrder(final List<Metric> all) {
        assertThat(all).containsExactly(
                Metric.MODULE,
                Metric.PACKAGE,
                Metric.FILE,
                Metric.CLASS,
                Metric.METHOD,
                Metric.LINE,
                Metric.INSTRUCTION,
                Metric.BRANCH,
                Metric.COMPLEXITY,
                Metric.MUTATION
        );
    }

    /**
     * Tests for creating predefined metrics with their names.
     */
    @Test
    void shouldCreatePredefinedMetrics() {
        // When & Then
        assertThat(Metric.valueOf("MODULE")).isEqualTo(Metric.MODULE).isNotLeaf();
        assertThat(Metric.valueOf("REPORT")).isEqualTo(Metric.MODULE).isNotLeaf();
        assertThat(Metric.valueOf("PACKAGE")).isEqualTo(Metric.PACKAGE).isNotLeaf();
        assertThat(Metric.valueOf("FILE")).isEqualTo(Metric.FILE).isNotLeaf();
        assertThat(Metric.valueOf("CLASS")).isEqualTo(Metric.CLASS).isNotLeaf();
        assertThat(Metric.valueOf("METHOD")).isEqualTo(Metric.METHOD).isNotLeaf();
        assertThat(Metric.valueOf("INSTRUCTION")).isEqualTo(Metric.INSTRUCTION).isLeaf();
        assertThat(Metric.valueOf("LINE")).isEqualTo(Metric.LINE).isLeaf();
        assertThat(Metric.valueOf("BRANCH")).isEqualTo(Metric.BRANCH).isLeaf();
        assertThat(Metric.valueOf("CONDITIONAL")).isEqualTo(Metric.BRANCH).isLeaf();
        assertThat(Metric.valueOf("COMPLEXITY")).isEqualTo(Metric.COMPLEXITY).isLeaf();
        assertThat(Metric.valueOf("MUTATION")).isEqualTo(Metric.MUTATION).isLeaf();
    }

    /**
     * Test for creating a new metric with name.
     */
    @Test
    void shouldCreateNewMetric() {
        // Given
        String newMetricName = "Subpackage";

        // When
        Metric actualMetric = Metric.valueOf(newMetricName);

        // Then
        assertThat(actualMetric)
                .hasName(newMetricName)
                .hasToString(newMetricName)
                .isNotLeaf();
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Metric.class)
                .withNonnullFields("name")
                .withIgnoredFields("order", "leaf").verify();
    }
}
