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

    /**
     * Tests for creating predefined metrics with their names.
     */
    @Test
    void shouldCreatePredefinedMetrics() {
        // When & Then
        assertThat(CoverageMetric.valueOf("MODULE")).isEqualTo(CoverageMetric.MODULE).isNotLeaf();
        assertThat(CoverageMetric.valueOf("REPORT")).isEqualTo(CoverageMetric.MODULE).isNotLeaf();
        assertThat(CoverageMetric.valueOf("PACKAGE")).isEqualTo(CoverageMetric.PACKAGE).isNotLeaf();
        assertThat(CoverageMetric.valueOf("FILE")).isEqualTo(CoverageMetric.FILE).isNotLeaf();
        assertThat(CoverageMetric.valueOf("CLASS")).isEqualTo(CoverageMetric.CLASS).isNotLeaf();
        assertThat(CoverageMetric.valueOf("METHOD")).isEqualTo(CoverageMetric.METHOD).isNotLeaf();
        assertThat(CoverageMetric.valueOf("INSTRUCTION")).isEqualTo(CoverageMetric.INSTRUCTION).isLeaf();
        assertThat(CoverageMetric.valueOf("LINE")).isEqualTo(CoverageMetric.LINE).isLeaf();
        assertThat(CoverageMetric.valueOf("BRANCH")).isEqualTo(CoverageMetric.BRANCH).isLeaf();
        assertThat(CoverageMetric.valueOf("CONDITIONAL")).isEqualTo(CoverageMetric.BRANCH).isLeaf();
    }

    /**
     * Test for creating a new metric with name.
     */
    @Test
    void shouldCreateNewMetric() {
        // Given
        String newMetricName = "Subpackage";

        // When
        CoverageMetric actualMetric = CoverageMetric.valueOf(newMetricName);

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
        EqualsVerifier.forClass(CoverageMetric.class).withIgnoredFields("order", "leaf").verify();
    }
}
