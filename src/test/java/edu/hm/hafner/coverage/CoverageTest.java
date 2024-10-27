package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * TestCount the class {@link Coverage}.
 *
 * @author Ullrich Hafner
 * @author Jannik Treichel
 */
@DefaultLocale("en")
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Exception is thrown anyway")
class CoverageTest {
    private static final Coverage NO_COVERAGE = new CoverageBuilder()
            .withMetric(Metric.LINE)
            .withCovered(0)
            .withMissed(0)
            .build();

    @Test
    void shouldComputeDelta() {
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage worse = builder.withCovered(0).withMissed(2).build();
        Coverage ok = builder.withCovered(1).withMissed(1).build();
        Coverage better = builder.withCovered(2).withMissed(0).build();

        assertThat(worse.delta(better).doubleValue()).isEqualTo(getDelta("-1/1"));
        assertThat(better.delta(worse).doubleValue()).isEqualTo(getDelta("1/1"));
        assertThat(worse.delta(ok).doubleValue()).isEqualTo(getDelta("-1/2"));
        assertThat(ok.delta(worse).doubleValue()).isEqualTo(getDelta("1/2"));
    }

    @Test
    void shouldCompareWithThreshold() {
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage zero = builder.withCovered(0).withMissed(2).build();
        Coverage fifty = builder.withCovered(2).withMissed(2).build();
        Coverage hundred = builder.withCovered(2).withMissed(0).build();

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
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage worse = builder.withCovered(0).withMissed(2).build();
        Coverage coverage = builder.withCovered(1).withMissed(1).build();
        Coverage better = builder.withCovered(2).withMissed(0).build();

        assertThat(coverage.max(coverage)).isSameAs(coverage);
        assertThat(coverage.max(better)).isSameAs(better);
        assertThat(coverage.max(worse)).isSameAs(coverage);
    }

    @ParameterizedTest(name = "{index} => Detection of invalid covered items: {0}")
    @ValueSource(ints = {0, 1, 2, 4, 5})
    @DisplayName("Ensure that exception is thrown if totals do not match")
    void shouldThrowExceptionWhenMaximumIsInvalid(final int covered) {
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage coverage = builder.withCovered(2).withMissed(1).build();

        assertThatExceptionOfType(AssertionError.class).isThrownBy(
                () -> coverage.max(
                        builder.withCovered(covered).withMissed(0).build()));
    }

    @Test
    @DisplayName("Ensure that exception is thrown if constructor is invoked with invalid values")
    void shouldThrowExceptionWhenInitializationIsInvalid() {
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> builder.withCovered(-1).withMissed(0).build());
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> builder.withCovered(0).withMissed(-1).build());
    }

    @Test
    @DisplayName("Ensure that exception is thrown if Value instances are not compatible")
    void shouldThrowExceptionWithIncompatibleValue() {
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage coverage = builder.withCovered(1).withMissed(2).build();
        Coverage wrongMetric = builder.withMetric(Metric.LOC).build();
        var loc = new Value(Metric.LOC, 1);

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
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage coverage = builder.withCovered(6).withMissed(4).build();
        assertThat(coverage)
                .hasCovered(6)
                .hasMissed(4)
                .hasTotal(10)
                .hasCoveredPercentage(Percentage.valueOf(6, 10))
                .hasToString("LINE: 60.00% (6/10)");

        assertThat(coverage.add(NO_COVERAGE)).isEqualTo(coverage);

        Coverage sum = coverage.add(builder.withCovered(10).withMissed(0).build());
        assertThat(sum).isEqualTo(builder.withCovered(16).withMissed(4).build());
    }

    @ParameterizedTest(name = "Test {index}: Covered items ''{0}''")
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    @DisplayName("Coverage creation")
    void shouldCreateCoverage(final int covered) {
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage coverage = builder
                .withCovered(covered)
                .withMissed(5 - covered)
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
        var builder = new CoverageBuilder().withMetric(Metric.LINE);

        Coverage coverage = builder.withCovered(covered).withMissed(missed).build();

        assertThat(coverage).hasCovered(covered).hasMissed(missed);
        assertThat(coverage.toString()).endsWith(String.format("(%s)", toString));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Coverage.class).withRedefinedSuperclass().verify();
    }

    @Test
    void shouldSetMetricCoveredMissedByString() {
        var builder = new CoverageBuilder().withMetric("LINE");

        Coverage coverage = builder.withCovered("10").withMissed("16").build();

        assertThat(coverage)
                .hasMetric(Metric.LINE)
                .hasCovered(10)
                .hasMissed(16);
        assertThat(coverage.serialize()).isEqualTo("LINE: 10/26");
    }

    @Test
    void shouldCreateCoverageBasedOnStringRepresentation() {
        Coverage coverage = Coverage.valueOf(Metric.LINE, "16/20");

        assertThat(coverage)
                .hasMetric(Metric.LINE)
                .hasCovered(16)
                .hasMissed(4)
                .hasTotal(20);
    }

    @Test
    void shouldThrowExceptionOnBadStringRepresentation() {
        String invalidSeparator = "10-20";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coverage.valueOf(Metric.LINE, invalidSeparator))
                .withMessageContaining(invalidSeparator);

        String totalSmallerThanCovered = "20/10";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coverage.valueOf(Metric.LINE, totalSmallerThanCovered))
                .withMessageContaining(totalSmallerThanCovered);

        String noNumber = "NO/NUMBER";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Coverage.valueOf(Metric.LINE, noNumber))
                .withMessageContaining(noNumber);
    }

    @Test
    void shouldCreateCoverageBasedOnFullStringRepresentation() {
        Value value = Value.valueOf("LINE:10/20");
        assertThat(value).isInstanceOf(Coverage.class);

        var coverage = (Coverage) value;
        assertThat(coverage)
                .hasMetric(Metric.LINE)
                .hasCovered(10)
                .hasMissed(10)
                .hasTotal(20);
    }

    @Test
    void shouldCalculateThirdValueOnBuilder() {
        Coverage coveredTotal = new CoverageBuilder().withMetric(Metric.LINE).withCovered(15).withTotal(40).build();
        assertThat(coveredTotal).hasTotal(40).hasMissed(25).hasCovered(15);

        Coverage coveredMissed = new CoverageBuilder().withMetric(Metric.LINE).withCovered(16).withMissed(16).build();
        assertThat(coveredMissed).hasTotal(32).hasMissed(16).hasCovered(16);

        Coverage totalMissed = new CoverageBuilder().withMetric(Metric.LINE).withTotal(40).withMissed(15).build();
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
        CoverageBuilder onlyTotal = new CoverageBuilder().withTotal(20);
        assertThatIllegalArgumentException()
                .isThrownBy(onlyTotal::build)
                .withMessageContaining("Exactly two properties have to be set.");

        CoverageBuilder onlyMissed = new CoverageBuilder().withMissed(20);
        assertThatIllegalArgumentException()
                .isThrownBy(onlyMissed::build)
                .withMessageContaining("Exactly two properties have to be set.");

        CoverageBuilder onlyCovered = new CoverageBuilder().withCovered(20);
        assertThatIllegalArgumentException()
                .isThrownBy(onlyCovered::build)
                .withMessageContaining("Exactly two properties have to be set.");
    }

    @Test
    void shouldThrowExceptionWhenSettingThreeOnBuilder() {
        var coverageBuilder = new CoverageBuilder().withCovered(10).withMissed(10).withTotal(20);

        assertThatIllegalArgumentException()
                .isThrownBy(coverageBuilder::build)
                .withMessageContaining("Setting all three values covered, missed, and total is not allowed");
    }

    @Test
    void shouldThrowExceptionIfNoMetricDefinedOnBuilder() {
        var coverageBuilder = new CoverageBuilder().withMissed(10).withTotal(20);

        assertThatIllegalArgumentException()
                .isThrownBy(coverageBuilder::build)
                .withMessageContaining("No metric defined.");
    }
}
