package edu.hm.hafner.coverage;

import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class ValueTest {
    @Test
    void shouldReturnCorrectValueOfCoverage() {
        var container = Value.valueOf("CONTAINER: 1/1");

        assertThat(container)
                .isInstanceOf(Coverage.class);
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

        assertThat((Coverage) container)
                .hasCovered(1)
                .hasMissed(0);
    }

    @Test
    void shouldReturnCorrectValueOfFractionValue() {
        var fractionValue = Value.valueOf("COMPLEXITY_DENSITY: 1/1");

        assertThat(fractionValue)
                .isInstanceOf(Value.class).hasMetric(Metric.CYCLOMATIC_COMPLEXITY_DENSITY);
        assertThat(fractionValue)
                .hasFraction(Fraction.getFraction(1, 1));
    }

    @Test
    void shouldReturnCorrectValueOfLinesOfCode() {
        var linesOfCode = Value.valueOf("LOC: 1");

        assertThat(linesOfCode).isInstanceOf(Value.class).hasMetric(Metric.LOC);
        assertThat(linesOfCode.asInteger()).isEqualTo(1);
        assertThat(linesOfCode.asText()).isEqualTo("1");
    }

    @Test
    void shouldThrowExceptionOnInvalidStringRepresentation() {
        var badRepresentation = "Bad representation";
        var badNumber = "COMPLEXITY: BadNumber";

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Value.valueOf(badRepresentation))
                .withMessageContaining("Cannot convert '%s' to a valid Value instance.".formatted(badRepresentation));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Value.valueOf(badNumber))
                .withMessageContaining("Cannot convert '%s' to a valid Value instance.".formatted(badNumber));
    }

    @Test
    @SuppressWarnings("Varifier")
    void shouldGetValue() {
        var linesOfCode = new Value(Metric.LOC, 10);
        var cyclomaticComplexity = new Value(Metric.CYCLOMATIC_COMPLEXITY, 20);
        var ncss = new Value(Metric.NCSS, 30);
        var npathComplexity = new Value(Metric.NPATH_COMPLEXITY, 40);
        var coginitiveComplexity = new Value(Metric.COGNITIVE_COMPLEXITY, 50);

        List<Value> values = List.of(linesOfCode, cyclomaticComplexity, ncss, npathComplexity, coginitiveComplexity);

        assertThat(Value.getValue(Metric.LOC, values))
                .isEqualTo(linesOfCode);
        assertThat(Value.getValue(Metric.CYCLOMATIC_COMPLEXITY, values))
                .isEqualTo(cyclomaticComplexity);
        assertThat(Value.getValue(Metric.NCSS, values))
                .isEqualTo(ncss);
        assertThat(Value.getValue(Metric.NPATH_COMPLEXITY, values))
                .isEqualTo(npathComplexity);
        assertThat(Value.getValue(Metric.COGNITIVE_COMPLEXITY, values))
                .isEqualTo(coginitiveComplexity);
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> Value.getValue(Metric.LINE, values))
                .withMessageContaining("No value for metric");
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(Value.class).verify();
    }
}
