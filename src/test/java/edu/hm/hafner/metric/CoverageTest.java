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
    private static final Coverage NO_COVERAGE = new CoverageBuilder()
            .setMetric(Metric.LINE)
            .setCovered(0)
            .setMissed(0)
            .build();

    @Test
    void shouldComputeDelta() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage worse = builder.setCovered(0).setMissed(2).build();
        Coverage ok = builder.setCovered(1).setMissed(1).build();
        Coverage better = builder.setCovered(2).setMissed(0).build();

        assertThat(worse.delta(better).doubleValue()).isEqualTo(getDelta("-1/1"));
        assertThat(better.delta(worse).doubleValue()).isEqualTo(getDelta("1/1"));
        assertThat(worse.delta(ok).doubleValue()).isEqualTo(getDelta("-1/2"));
        assertThat(ok.delta(worse).doubleValue()).isEqualTo(getDelta("1/2"));
    }

    private double getDelta(final String value) {
        return Fraction.getFraction(value).doubleValue();
    }

    @Test
    void shouldComputeMaximum() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage worse = builder.setCovered(0).setMissed(2).build();
        Coverage coverage = builder.setCovered(1).setMissed(1).build();
        Coverage better = builder.setCovered(2).setMissed(0).build();

        assertThat(coverage.max(coverage)).isSameAs(coverage);
        assertThat(coverage.max(better)).isSameAs(better);
        assertThat(coverage.max(worse)).isSameAs(coverage);
    }

    @ParameterizedTest(name = "{index} => Detection of invalid covered items: {0}")
    @ValueSource(ints = {0, 1, 2, 4, 5})
    @DisplayName("Ensure that exception is thrown if totals do not match")
    void shouldThrowExceptionWhenMaximumIsInvalid(final int covered) {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder.setCovered(2).setMissed(1).build();

        assertThatExceptionOfType(AssertionError.class).isThrownBy(
                () -> coverage.max(
                        builder.setCovered(covered).setMissed(0).build()));
    }

    @Test
    @DisplayName("Ensure that exception is thrown if constructor is invoked with invalid values")
    void shouldThrowExceptionWhenInitializationIsInvalid() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> builder.setCovered(-1).setMissed(0).build());
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> builder.setCovered(0).setMissed(-1).build());
    }

    @Test
    @DisplayName("Ensure that exception is thrown if Value instances are not compatible")
    void shouldThrowExceptionWithIncompatibleValue() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder.setCovered(1).setMissed(2).build();
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
                .hasToString("LINE: n/a");
        assertThat(NO_COVERAGE.add(NO_COVERAGE)).isEqualTo(NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder.setCovered(6).setMissed(4).build();
        assertThat(coverage)
                .hasCovered(6)
                .hasCoveredPercentage(Fraction.getFraction(6, 10))
                .hasMissed(4)
                .hasMissedPercentage(Fraction.getFraction(4, 10))
                .hasTotal(10)
                .hasToString("LINE: 60.00% (6/10)");

        assertThat(coverage.add(NO_COVERAGE)).isEqualTo(coverage);

        Coverage sum = coverage.add(builder.setCovered(10).setMissed(0).build());
        assertThat(sum).isEqualTo(builder.setCovered(16).setMissed(4).build());
    }

    @ParameterizedTest(name = "Test {index}: Covered items ''{0}''")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("Coverage creation")
    void shouldCreateCoverage(final int covered) {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder
                .setCovered(covered)
                .setMissed(5 - covered)
                .build();

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
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder.setCovered(covered).setMissed(missed).build();

        assertThat(coverage).hasCovered(covered).hasMissed(missed);
        assertThat(coverage.toString()).endsWith(String.format("(%s)", toString));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Coverage.class).withRedefinedSuperclass().verify();
    }
}
