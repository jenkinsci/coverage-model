package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.model.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageLeaf}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class CoverageLeafTest {
    private static final Coverage COVERED = new Coverage(1, 0);

    @Test
    void shouldCreateLeaf() {
        CoverageLeaf coverageLeaf = new CoverageLeaf(Metric.LINE, COVERED);

        assertThat(coverageLeaf)
                .hasMetric(Metric.LINE)
                .hasCoverage(COVERED)
                .hasToString("[Line]: 100.00% (1/1)");
        assertThat(coverageLeaf.getCoverage(Metric.LINE)).isEqualTo(COVERED);
        assertThat(coverageLeaf.getCoverage(Metric.MODULE)).isEqualTo(Coverage.NO_COVERAGE);
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(CoverageLeaf.class).withNonnullFields("metric").verify();
    }
}
