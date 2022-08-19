package edu.hm.hafner.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

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

    @Test
    void shouldGetCorrespondingValueByName() {
        Metric.getAvailableCoverageMetrics().forEach(coverageMetric ->
                assertThat(Metric.valueOf(coverageMetric.getName())).isEqualTo(coverageMetric));

        assertThat(Metric.valueOf("CUSTOM")).hasName("CUSTOM").isNotLeaf();
    }

    @Test
    void shouldGetCorrespondingValueByNameForSpecialMetrics() {
        assertThat(Metric.valueOf("cOnDITional")).isEqualTo(Metric.BRANCH);
        assertThat(Metric.valueOf("RePoRT")).isEqualTo(Metric.MODULE);

    }

    @Test
    void shouldDetermineIfMetricIsLeaf() {
        assertThat(Metric.FILE).isNotLeaf();
        assertThat(Metric.BRANCH).isLeaf();
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Metric.class)
                .withIgnoredFields("order", "leaf")
                .withNonnullFields("name").verify();
    }
}
