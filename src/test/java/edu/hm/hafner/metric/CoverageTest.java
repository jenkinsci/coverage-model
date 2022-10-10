package edu.hm.hafner.metric;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.Coverage.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link Coverage}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class CoverageTest {
    private static final Coverage NO_COVERAGE = new Coverage(Metric.LINE, 0, 0);

    @Test
    void shouldComputeDelta() {
        Coverage worse = new Coverage(Metric.LINE, 0, 2);
        Coverage ok = new Coverage(Metric.LINE, 1, 1);
        Coverage better = new Coverage(Metric.LINE, 2, 0);

        assertThat(worse.delta(better).doubleValue()).isEqualTo(getDelta("-1/1"));
        assertThat(better.delta(worse).doubleValue()).isEqualTo(getDelta("1/1"));
        assertThat(worse.delta(ok).doubleValue()).isEqualTo(getDelta("-1/2"));
        assertThat(ok.delta(worse).doubleValue()).isEqualTo(getDelta("1/2"));
    }

    private static double getDelta(final String value) {
        return Fraction.getFraction(value).doubleValue();
    }

    @Test
    void shouldComputeMaximum() {
        Coverage worse = new Coverage(Metric.LINE, 0, 2);
        Coverage coverage = new Coverage(Metric.LINE, 1, 1);
        Coverage better = new Coverage(Metric.LINE, 2, 0);

        assertThat(coverage.max(coverage)).isSameAs(coverage);
        assertThat(coverage.max(better)).isSameAs(better);
        assertThat(coverage.max(worse)).isSameAs(coverage);
    }

    @ParameterizedTest(name = "{index} => Detection of invalid covered items: {0}")
    @ValueSource(ints = {0, 1, 2, 4, 5})
    @DisplayName("Ensure that exception is thrown if totals do not match")
    void shouldThrowExceptionWhenMaximumIsInvalid(final int covered) {
        Coverage coverage = new Coverage(Metric.LINE, 2, 1);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(
                () -> coverage.max(new Coverage(Metric.LINE, covered, 0)));
    }

    @Test
    @DisplayName("Ensure that exception is thrown if constructor is invoked with invalid values")
    void shouldThrowExceptionWhenInitializationIsInvalid() {
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> new Coverage(Metric.LINE, -1, 0));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> new Coverage(Metric.LINE, 0, -1));
    }

    @Test
    @DisplayName("Ensure that exception is thrown if Value instances are not compatible")
    void shouldThrowExceptionWithIncompatibleValue() {
        Coverage coverage = new Coverage(Metric.LINE, 1, 2);
        LinesOfCode loc = new LinesOfCode(1);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> coverage.add(loc));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> coverage.max(loc));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> coverage.delta(loc));
    }

    @Test
    void shouldProvideNullObject() {
        assertThat(NO_COVERAGE)
                .hasCovered(0)
                .hasCoveredPercentage(Fraction.ZERO)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(0)
                .hasToString(NO_COVERAGE_AVAILABLE);
        assertThat(NO_COVERAGE.add(NO_COVERAGE)).isEqualTo(NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        Coverage coverage = new Coverage(Metric.LINE, 6, 4);
        assertThat(coverage)
                .hasCovered(6)
                .hasCoveredPercentage(Fraction.getFraction(6, 10))
                .hasMissed(4)
                .hasMissedPercentage(Fraction.getFraction(4, 10))
                .hasTotal(10)
                .hasToString("60.00% (6/10)");

        assertThat(coverage.add(NO_COVERAGE)).isEqualTo(coverage);

        Coverage sum = coverage.add(new Coverage(Metric.LINE, 10, 0));
        assertThat(sum).isEqualTo(new Coverage(Metric.LINE, 16, 4));
    }

    @ParameterizedTest(name = "Test {index}: Covered items ''{0}''")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("Coverage creation")
    void shouldCreateCoverage(final int covered) {
        Coverage coverage = new Coverage(Metric.LINE, covered, 5 - covered);

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
        Coverage coverage = new Coverage(Metric.LINE, covered, missed);

        assertThat(coverage).hasCovered(covered).hasMissed(missed);
        assertThat(coverage.toString()).endsWith(String.format("(%s)", toString));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Coverage.class).withRedefinedSuperclass().verify();
    }
}
