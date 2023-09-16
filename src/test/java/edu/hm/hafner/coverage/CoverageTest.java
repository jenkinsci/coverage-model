package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.Coverage.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

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

    @Test
    void shouldCompareWithThreshold() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage zero = builder.setCovered(0).setMissed(2).build();
        Coverage fifty = builder.setCovered(2).setMissed(2).build();
        Coverage hundred = builder.setCovered(2).setMissed(0).build();

        assertThat(zero.isOutOfValidRange(0)).isFalse();
        assertThat(zero.isOutOfValidRange(0.1)).isTrue();
        assertThat(fifty.isOutOfValidRange(50)).isFalse();
        assertThat(fifty.isOutOfValidRange(50.1)).isTrue();
        assertThat(hundred.isOutOfValidRange(100)).isFalse();
        assertThat(hundred.isOutOfValidRange(100.1)).isTrue();
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
        Coverage wrongMetric = builder.setMetric(Metric.LOC).build();
        var loc = new LinesOfCode(1);

        assertThatIllegalArgumentException().isThrownBy(() -> coverage.add(loc));
        assertThatIllegalArgumentException().isThrownBy(() -> coverage.max(loc));
        assertThatIllegalArgumentException().isThrownBy(() -> coverage.delta(loc));
        assertThatIllegalArgumentException().isThrownBy(() -> coverage.add(wrongMetric));
        assertThatIllegalArgumentException().isThrownBy(() -> coverage.max(wrongMetric));
        assertThatIllegalArgumentException().isThrownBy(() -> coverage.delta(wrongMetric));
        assertThatIllegalArgumentException().isThrownBy(() -> wrongMetric.add(loc));
        assertThatIllegalArgumentException().isThrownBy(() -> wrongMetric.max(loc));
        assertThatIllegalArgumentException().isThrownBy(() -> wrongMetric.delta(loc));
    }

    @Test
    void shouldProvideNullObject() {
        assertThat(NO_COVERAGE)
                .hasCovered(0)
                .hasMissed(0)
                .hasTotal(0)
                .hasCoveredPercentage(Percentage.ZERO)
                .hasToString("LINE: n/a");
        assertThat(NO_COVERAGE.add(NO_COVERAGE)).isEqualTo(NO_COVERAGE);
    }

    @Test
    void shouldCreatePercentages() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder.setCovered(6).setMissed(4).build();
        assertThat(coverage)
                .hasCovered(6)
                .hasMissed(4)
                .hasTotal(10)
                .hasCoveredPercentage(Percentage.valueOf(6, 10))
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

    @Test
    void shouldSerializeToString() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        Coverage coverage = builder.setCovered(10).setMissed(10).build();

        assertThat(coverage.serialize()).isEqualTo("LINE: 10/20");
    }

    @Test
    void shouldSetMetricCoveredMissedByString() {
        var builder = new CoverageBuilder().setMetric("LINE");

        Coverage coverage = builder.setCovered("10").setMissed("16").build();

        assertThat(coverage)
                .hasMetric(Metric.LINE)
                .hasCovered(10)
                .hasMissed(16);
    }

    @Test
    void shouldCreateCoverageBasedOnStringRepresentation() {
        Coverage coverage = valueOf(Metric.LINE, "16/20");

        assertThat(coverage)
                .hasMetric(Metric.LINE)
                .hasCovered(16)
                .hasMissed(4)
                .hasTotal(20);
    }

    @Test
    void shouldThrowExceptionOnBadStringRepresentation() {
        String invalidSeparator = "10-20";
        String totalSmallerThanCovered = "20/10";
        String noNumber = "NO/NUMBER";

        assertThatIllegalArgumentException()
                .isThrownBy(() -> valueOf(Metric.LINE, invalidSeparator))
                .withMessageContaining(invalidSeparator);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> valueOf(Metric.LINE, totalSmallerThanCovered))
                .withMessageContaining(totalSmallerThanCovered);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> valueOf(Metric.LINE, noNumber))
                .withMessageContaining(noNumber);
    }

    @Test
    void shouldCreateCoverageBasedOnFullStringRepresentation() {
        Value value = Value.valueOf("LINE:10/20");
        assertThat(value).isInstanceOf(Coverage.class);

        Coverage coverage = (Coverage) value;
        assertThat(coverage)
                .hasMetric(Metric.LINE)
                .hasCovered(10)
                .hasMissed(10)
                .hasTotal(20);
    }

    @Test
    void shouldCalculateThirdValueOnBuilder() {
        Coverage coveredTotal = new CoverageBuilder().setMetric(Metric.LINE).setCovered(15).setTotal(40).build();
        Coverage coveredMissed = new CoverageBuilder().setMetric(Metric.LINE).setCovered(16).setMissed(16).build();
        Coverage totalMissed = new CoverageBuilder().setMetric(Metric.LINE).setTotal(40).setMissed(15).build();

        assertThat(coveredTotal).hasTotal(40).hasMissed(25).hasCovered(15);
        assertThat(coveredMissed).hasTotal(32).hasMissed(16).hasCovered(16);
        assertThat(totalMissed).hasTotal(40).hasMissed(15).hasCovered(25);
    }

    @Test
    void shouldThrowExceptionWhenSettingNoneOnBuilder() {
        var coverageBuilder = new CoverageBuilder();

        assertThatIllegalArgumentException()
                .isThrownBy(coverageBuilder::build)
                .withMessageContaining("Exactly two properties have to be set.");
    }

    @Test
    void shouldThrowExceptionWhenSettingOneOnBuilder() {
        CoverageBuilder onlyTotal = new CoverageBuilder().setTotal(20);
        CoverageBuilder onlyMissed = new CoverageBuilder().setMissed(20);
        CoverageBuilder onlyCovered = new CoverageBuilder().setCovered(20);

        assertThatIllegalArgumentException()
                .isThrownBy(onlyTotal::build)
                .withMessageContaining("Exactly two properties have to be set.");
        assertThatIllegalArgumentException()
                .isThrownBy(onlyMissed::build)
                .withMessageContaining("Exactly two properties have to be set.");
        assertThatIllegalArgumentException()
                .isThrownBy(onlyCovered::build)
                .withMessageContaining("Exactly two properties have to be set.");
    }

    @Test
    void shouldThrowExceptionWhenSettingThreeOnBuilder() {
        var coverageBuilder = new CoverageBuilder().setCovered(10).setMissed(10).setTotal(20);

        assertThatIllegalArgumentException()
                .isThrownBy(coverageBuilder::build)
                .withMessageContaining("Setting all three values covered, missed, and total is not allowed");
    }

    @Test
    void shouldThrowExceptionIfNoMetricDefinedOnBuilder() {
        var coverageBuilder = new CoverageBuilder().setMissed(10).setTotal(20);

        assertThatIllegalArgumentException()
                .isThrownBy(coverageBuilder::build)
                .withMessageContaining("No metric defined.");
    }
}
