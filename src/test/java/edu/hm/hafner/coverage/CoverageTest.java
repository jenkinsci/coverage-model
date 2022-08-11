package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link Coverage}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class CoverageTest {
    @Test
    void shouldProvideNullObject() {
        assertThat(Coverage.NO_COVERAGE).isNotSet()
                .hasCovered(0)
                .hasCoveredPercentage(Fraction.ZERO)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(0)
                .hasToString(Coverage.NO_COVERAGE_AVAILABLE);
        assertThat(Coverage.NO_COVERAGE.formatCoveredPercentage()).isEqualTo(Coverage.NO_COVERAGE_AVAILABLE);
        assertThat(Coverage.NO_COVERAGE.formatMissedPercentage()).isEqualTo(Coverage.NO_COVERAGE_AVAILABLE);
        assertThat(Coverage.NO_COVERAGE.add(Coverage.NO_COVERAGE)).isEqualTo(Coverage.NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        Coverage coverage = new Coverage(6, 4);
        assertThat(coverage).isSet()
                .hasCovered(6)
                .hasCoveredPercentage(Fraction.getFraction(6, 10))
                .hasMissed(4)
                .hasMissedPercentage(Fraction.getFraction(4, 10))
                .hasTotal(10)
                .hasToString("60.00% (6/10)");

        assertThat(coverage.formatCoveredPercentage()).isEqualTo("60.00%");
        assertThat(coverage.formatMissedPercentage()).isEqualTo("40.00%");

        assertThat(coverage.add(Coverage.NO_COVERAGE)).isEqualTo(coverage);

        Coverage sum = coverage.add(new Coverage(10, 0));
        assertThat(sum).isEqualTo(new Coverage(16, 4));
        assertThat(sum.formatCoveredPercentage()).isEqualTo("80.00%");
        assertThat(sum.formatMissedPercentage()).isEqualTo("20.00%");
    }

    @ParameterizedTest(name = "Test {index}: Covered items ''{0}''")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("Coverage creation")
    void shouldCreateCoverage(final int covered) {
        Coverage coverage = new Coverage(covered, 5 - covered);

        assertThat(coverage).hasCovered(covered).hasTotal(5);
        assertThat(coverage.toString()).contains(covered + "/");
    }

    @ParameterizedTest(name = "Test {index}: Covered ''{0}'', Missed ''{1}'', toString ''({2})'''")
    @CsvSource({
            "0, 1, 0/1",
            "1, 0, 1/1",
            "0, 2, 0/2",
            "1, 1, 1/2",
            "2, 0, 2/2"
    })
    @DisplayName("Coverage creation")
    void shouldCreateCoverage(final int covered, final int missed, final String toString) {
        Coverage coverage = new Coverage(covered, missed);

        assertThat(coverage).hasCovered(covered).hasMissed(missed);
        assertThat(coverage.toString()).endsWith(String.format("(%s)", toString));
    }

    @Test
    void shouldCorrectlyCalculateEqualityAndHashCode() {
        EqualsVerifier.forClass(Coverage.class).verify();
    }

}
