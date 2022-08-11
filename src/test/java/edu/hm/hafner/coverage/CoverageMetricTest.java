package edu.hm.hafner.coverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageMetric}.
 *
 * @author Ullrich Hafner
 */
class CoverageMetricTest {

    @Test
    void shouldSortMetrics() {
        List<CoverageMetric> all = new ArrayList<>();
        all.add(CoverageMetric.MODULE);
        all.add(CoverageMetric.PACKAGE);
        all.add(CoverageMetric.FILE);
        all.add(CoverageMetric.CLASS);
        all.add(CoverageMetric.METHOD);
        all.add(CoverageMetric.LINE);
        all.add(CoverageMetric.BRANCH);
        all.add(CoverageMetric.INSTRUCTION);

        Collections.sort(all);
        verifyOrder(all);

        Collections.reverse(all);
        assertThat(all).containsExactly(
                CoverageMetric.BRANCH,
                CoverageMetric.INSTRUCTION,
                CoverageMetric.LINE,
                CoverageMetric.METHOD,
                CoverageMetric.CLASS,
                CoverageMetric.FILE,
                CoverageMetric.PACKAGE,
                CoverageMetric.MODULE);

        Collections.sort(all);
        verifyOrder(all);
    }

    @Test
    void shouldGetAvailableMetrics() {
        assertThat(CoverageMetric.getAvailableCoverageMetrics()).containsExactlyInAnyOrder(
                CoverageMetric.BRANCH,
                CoverageMetric.INSTRUCTION,
                CoverageMetric.LINE,
                CoverageMetric.METHOD,
                CoverageMetric.CLASS,
                CoverageMetric.FILE,
                CoverageMetric.PACKAGE,
                CoverageMetric.MODULE
        );
    }

    @Test
    void shouldGetCorrespondingValueByName() {
        CoverageMetric.getAvailableCoverageMetrics().forEach(coverageMetric ->
                assertThat(CoverageMetric.valueOf(coverageMetric.getName())).isEqualTo(coverageMetric));

        assertThat(CoverageMetric.valueOf("CUSTOM")).hasName("CUSTOM").isNotLeaf();
    }

    @Test
    void shouldGetCorrespondingValueByNameForSpecialMetrics() {
        assertThat(CoverageMetric.valueOf("cOnDITional")).isEqualTo(CoverageMetric.BRANCH);
        assertThat(CoverageMetric.valueOf("RePoRT")).isEqualTo(CoverageMetric.MODULE);

    }

    @Test
    void shouldDetermineIfMetricIsLeaf() {
        assertThat(CoverageMetric.FILE).isNotLeaf();
        assertThat(CoverageMetric.BRANCH).isLeaf();
    }

    private void verifyOrder(final List<CoverageMetric> all) {
        assertThat(all).containsExactly(
                CoverageMetric.MODULE,
                CoverageMetric.PACKAGE,
                CoverageMetric.FILE,
                CoverageMetric.CLASS,
                CoverageMetric.METHOD,
                CoverageMetric.LINE,
                CoverageMetric.INSTRUCTION,
                CoverageMetric.BRANCH
        );
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CoverageMetric.class).withIgnoredFields("order", "leaf").verify();
    }
}
