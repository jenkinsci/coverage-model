package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

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
        CoverageLeaf coverageLeaf = new CoverageLeaf(CoverageMetric.LINE, COVERED);

        assertThat(coverageLeaf)
                .hasMetric(CoverageMetric.LINE)
                .hasCoverage(COVERED)
                .hasToString("[Line]: 100.00% (1/1)");
        assertThat(coverageLeaf.getCoverage(CoverageMetric.LINE)).isEqualTo(COVERED);
        assertThat(coverageLeaf.getCoverage(CoverageMetric.MODULE)).isEqualTo(Coverage.NO_COVERAGE);
    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CoverageLeaf.class).verify();
    }
}
