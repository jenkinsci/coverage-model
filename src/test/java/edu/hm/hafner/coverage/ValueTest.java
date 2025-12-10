package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class ValueTest {
    @Test
    void shouldProvideNullObject() {
        var zero = Value.nullObject(Metric.LOC);

        assertThat(zero)
                .hasMetric(Metric.LOC)
                .hasFraction(Fraction.ZERO);

        assertThat(zero.add(zero)).isEqualTo(zero);
        assertThat(zero.subtract(zero)).isEqualTo(Difference.nullObject(Metric.LOC));
        assertThat(zero.max(zero)).isEqualTo(zero);

        assertThat(zero.asInteger()).isZero();
        assertThat(zero.asDouble()).isEqualTo(0);
        assertThat(zero.asRounded()).isEqualTo(0);

        assertThat(zero.asText(Locale.ENGLISH)).isEqualTo("0");
        assertThat(zero.getSummary(Locale.ENGLISH)).isEqualTo("Lines of Code: 0");
        assertThat(zero.asInformativeText(Locale.ENGLISH)).isEqualTo("0");
        assertThat(zero.getDetails(Locale.ENGLISH)).isEqualTo("Lines of Code: 0");
        assertThat(zero.serialize()).isEqualTo("LOC: 0");

        assertThatInstanceIsCorrectlySerializedAndDeserialized(zero);
    }

    private void assertThatInstanceIsCorrectlySerializedAndDeserialized(final Value value) {
        assertThat(Value.valueOf(value.serialize())).isEqualTo(value);
    }

    @Test
    void shouldHandlePercentageRounding() {
        var oneThird = new Value(Metric.COHESION, 1, 3);

        assertThat(oneThird.asInteger()).isZero();
        assertThat(oneThird.asDouble()).isEqualTo(1.0 / 3);
        assertThat(oneThird.asRounded()).isEqualTo(0.33);
        assertThat(oneThird.asRoundedText(Locale.ENGLISH)).isEqualTo("0.33");

        var twoThirds = new Value(Metric.COHESION, 2, 3);

        assertThat(twoThirds.asInteger()).isEqualTo(1);
        assertThat(twoThirds.asDouble()).isEqualTo(2.0 / 3);
        assertThat(twoThirds.asRounded()).isEqualTo(0.67);

        assertThat(twoThirds.asText(Locale.ENGLISH)).isEqualTo("66.67%");
        assertThat(twoThirds.asInformativeText(Locale.ENGLISH)).isEqualTo("66.67%");
        assertThat(twoThirds.serialize()).isEqualTo("COHESION: 2:3");

        assertThat(oneThird.max(twoThirds)).isEqualTo(twoThirds);
        assertThat(twoThirds.max(oneThird)).isEqualTo(twoThirds);

        assertThatInstanceIsCorrectlySerializedAndDeserialized(oneThird);
        assertThatInstanceIsCorrectlySerializedAndDeserialized(twoThirds);
    }

    @Test
    void shouldHandlePercentages() {
        var oneThird = new Rate(Metric.COHESION, 1, 3);

        assertThat(oneThird.asInteger()).isEqualTo(33);
        assertThat(oneThird.asDouble()).isCloseTo(100 / 3.0, Percentage.withPercentage(0.01));
        assertThat(oneThird.asRounded()).isEqualTo(33.33);

        var twoThirds = new Rate(Metric.COHESION, 2, 3);

        assertThat(twoThirds.asInteger()).isEqualTo(67);
        assertThat(twoThirds.asDouble()).isCloseTo(200 / 3.0, Percentage.withPercentage(0.01));
        assertThat(twoThirds.asRounded()).isEqualTo(66.67);

        assertThat(twoThirds.asText(Locale.ENGLISH)).isEqualTo("66.67%");
        assertThat(twoThirds.asInformativeText(Locale.ENGLISH)).isEqualTo("66.67%");
        assertThat(twoThirds.serialize()).isEqualTo("COHESION: %2:3");

        assertThat(oneThird.max(twoThirds)).isEqualTo(twoThirds);
        assertThat(twoThirds.max(oneThird)).isEqualTo(twoThirds);

        assertThatInstanceIsCorrectlySerializedAndDeserialized(oneThird);
        assertThatInstanceIsCorrectlySerializedAndDeserialized(twoThirds);
    }

    @ParameterizedTest(name = "digits={0}")
    @ValueSource(ints = {5, 6, 7, 8})
    @DisplayName("Percentage values should be exactly 100% for almost perfect rates")
    void shouldHandle100Percent(final int digits) {
        var value = BigInteger.TEN.pow(digits).intValue();
        var percentage = new Value(Metric.PERCENTAGE, value - 1, value);

        assertThat(percentage.asText(Locale.ENGLISH)).isEqualTo("100.00%");
        assertThat(percentage).hasToString("100.00%");
    }

    @ParameterizedTest(name = "digits={0}")
    @ValueSource(ints = {5, 6, 7, 8})
    @DisplayName("Not perfect rate values should be less than 100%")
    void shouldHandle100Rate(final int digits) {
        var value = BigInteger.TEN.pow(digits).intValue();
        var rate = new Value(Metric.RATE, value - 1, value);

        assertThat(rate.asText(Locale.ENGLISH)).isEqualTo("99.99%");
        assertThat(rate).hasToString("99.99%");
    }

    @Test
    void shouldHandleDoubleValues() {
        var fraction = new Value(Metric.COHESION, 1, 3);
        var value = new Value(Metric.COHESION, 1.0 / 3.0);

        assertThat(value.asInteger()).isZero();
        assertThat(value.asDouble()).isEqualTo(1.0 / 3);
        assertThat(value.asRounded()).isEqualTo(0.33);
        assertThat(value.asDouble()).isEqualTo(fraction.asDouble());

        assertThatInstanceIsCorrectlySerializedAndDeserialized(value);
    }

    @Test
    void shouldReturnCorrectValueOfCoverage() {
        var container = Value.valueOf("CONTAINER: 1/1");

        assertThat(container)
                .isInstanceOfSatisfying(Coverage.class, coverage -> {
                    assertThat(coverage.getMetric()).isEqualTo(Metric.CONTAINER);
                    assertThat(coverage.getCovered()).isOne();
                    assertThat(coverage.getMissed()).isZero();
                });

        assertThat(Value.valueOf("MODULE: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("PACKAGE: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("FILE: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("CLASS: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("METHOD: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("LINE: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("INSTRUCTION: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("BRANCH: 1/1"))
                .isInstanceOf(Coverage.class);
        assertThat(Value.valueOf("MUTATION: 1/1"))
                .isInstanceOf(Coverage.class);
    }

    @Test
    void shouldReturnCorrectValueOfFractionValue() {
        var complexity = Value.valueOf("COMPLEXITY: 1");

        assertThat(complexity)
                .isInstanceOf(Value.class)
                .hasMetric(Metric.CYCLOMATIC_COMPLEXITY)
                .hasFraction(Fraction.getFraction(1, 1));

        assertThat(complexity.asText(Locale.ENGLISH)).isEqualTo("1");
        assertThat(complexity.getSummary(Locale.ENGLISH)).isEqualTo("Cyclomatic Complexity: 1");
        assertThat(complexity.asInformativeText(Locale.ENGLISH)).isEqualTo("1");
        assertThat(complexity.getDetails(Locale.ENGLISH)).isEqualTo("Cyclomatic Complexity: 1");
        assertThat(complexity.serialize()).isEqualTo("CYCLOMATIC_COMPLEXITY: 1");

        assertThatInstanceIsCorrectlySerializedAndDeserialized(complexity);
    }

    @Test
    void shouldReturnCorrectValueOfLinesOfCode() {
        var linesOfCode = Value.valueOf("LOC: 1");

        assertThat(linesOfCode).isInstanceOf(Value.class).hasMetric(Metric.LOC);
        assertThat(linesOfCode.asInteger()).isEqualTo(1);
        assertThat(linesOfCode.asText(Locale.ENGLISH)).isEqualTo("1");

        assertThat(linesOfCode.asText(Locale.ENGLISH)).isEqualTo("1");
        assertThat(linesOfCode.getSummary(Locale.ENGLISH)).isEqualTo("Lines of Code: 1");
        assertThat(linesOfCode.asInformativeText(Locale.ENGLISH)).isEqualTo("1");
        assertThat(linesOfCode.getDetails(Locale.ENGLISH)).isEqualTo("Lines of Code: 1");
        assertThat(linesOfCode.serialize()).isEqualTo("LOC: 1");

        assertThatInstanceIsCorrectlySerializedAndDeserialized(linesOfCode);
    }

    @Test
    void shouldThrowExceptionOnInvalidStringRepresentation() {
        var badRepresentation = "Bad representation";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Value.valueOf(badRepresentation))
                .withMessageContaining("Cannot convert '%s' to a valid Value instance.".formatted(badRepresentation));

        var badNumber = "COMPLEXITY: BadNumber";
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Value.valueOf(badNumber))
                .withMessageContaining("Cannot convert '%s' to a valid Value instance.".formatted(badNumber));
    }

    @Test
    void shouldThrowExceptionWhenUsingDifferentType() {
        var linesOfCode = new Value(Metric.LOC, 10);
        var complexity = new Value(Metric.CYCLOMATIC_COMPLEXITY, 10);
        var coverage = Coverage.nullObject(Metric.LOC);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> assertThat(linesOfCode.add(complexity)).isNotNull()) // assertion required for SpotBugs
                .withMessageContaining("Cannot calculate with different metrics");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> assertThat(linesOfCode.add(coverage)).isNotNull()) // assertion required for SpotBugs
                .withMessageContaining("Cannot calculate with different types");
    }

    @Test
    void shouldFindValueInCollection() {
        var linesOfCode = new Value(Metric.LOC, 10);
        var cyclomaticComplexity = new Value(Metric.CYCLOMATIC_COMPLEXITY, 20);
        var ncss = new Value(Metric.NCSS, 30);
        var npathComplexity = new Value(Metric.NPATH_COMPLEXITY, 40);
        var cognitiveComplexity = new Value(Metric.COGNITIVE_COMPLEXITY, 50);

        var values = List.of(linesOfCode, cyclomaticComplexity, ncss, npathComplexity, cognitiveComplexity);

        assertThat(Value.getValue(Metric.LOC, values))
                .isEqualTo(linesOfCode);
        assertThat(Value.findValue(Metric.LOC, values))
                .contains(linesOfCode);
        assertThat(Value.getValue(Metric.CYCLOMATIC_COMPLEXITY, values))
                .isEqualTo(cyclomaticComplexity);
        assertThat(Value.findValue(Metric.CYCLOMATIC_COMPLEXITY, values))
                .contains(cyclomaticComplexity);
        assertThat(Value.getValue(Metric.NCSS, values))
                .isEqualTo(ncss);
        assertThat(Value.findValue(Metric.NCSS, values))
                .contains(ncss);
        assertThat(Value.getValue(Metric.NPATH_COMPLEXITY, values))
                .isEqualTo(npathComplexity);
        assertThat(Value.findValue(Metric.NPATH_COMPLEXITY, values))
                .contains(npathComplexity);
        assertThat(Value.getValue(Metric.COGNITIVE_COMPLEXITY, values))
                .isEqualTo(cognitiveComplexity);
        assertThat(Value.findValue(Metric.COGNITIVE_COMPLEXITY, values))
                .contains(cognitiveComplexity);

        assertThat(Value.findValue(Metric.LINE, values))
                .isEmpty();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> Value.getValue(Metric.LINE, values))
                .withMessageContaining("No value for metric");
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(Value.class).verify();
    }

    @Test
    @SuppressWarnings({"EqualsWithItself", "SelfComparison"})
    void shouldCompareValues() {
        var one = new Value(Metric.LOC, 1);
        var two = new Value(Metric.LOC, 2);

        assertThat(one.compareTo(one)).isZero();

        assertThat(one.compareTo(two)).isNegative();
        assertThat(two.compareTo(one)).isPositive();

        var ncss = new Value(Metric.NCSS, 1);
        assertThat(one.compareTo(ncss)).isNegative();
        assertThat(ncss.compareTo(one)).isPositive();

        var line = new Value(Metric.LINE, 0);
        assertThat(one.compareTo(line)).isPositive();
        assertThat(line.compareTo(one)).isNegative();
    }
}
