package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import static edu.hm.hafner.coverage.CoverageMetric.*;
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
        CoverageLeaf coverageLeaf = new CoverageLeaf(LINE, COVERED);

        assertThat(coverageLeaf)
                .hasMetric(LINE)
                .hasCoverage(COVERED)
                .hasToString("[Line]: 100.00% (1/1)");
        assertThat(coverageLeaf.getCoverage(LINE)).isEqualTo(COVERED);
        assertThat(coverageLeaf.getCoverage(CoverageMetric.MODULE)).isEqualTo(Coverage.NO_COVERAGE);
    }
}
