package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link FractionValue}.
 *
 * @author Ullrich Hafner
 */
@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Exception is thrown anyway")
class FractionValueTest {
    @Test
    void shouldCreateDelta() {
        var fraction = Fraction.getFraction(50, 1);
        var fifty = new Value(Metric.TESTS, fraction);
        var fiftyAgain = new Value(Metric.TESTS, 50, 1);
        var hundred = new Value(Metric.TESTS, Fraction.getFraction(100, 1));

        assertThat(fifty.isOutOfValidRange(50.1)).isTrue();
        assertThat(fifty.isOutOfValidRange(50)).isFalse();

        assertThat(fifty.add(fifty)).isEqualTo(hundred);
        assertThat(fifty.max(hundred)).isEqualTo(hundred);
        assertThat(fifty.max(fiftyAgain)).isEqualTo(fifty);
        assertThat(hundred.max(fifty)).isEqualTo(hundred);

        assertThat(fifty).hasFraction(fraction);
        assertThat(fifty.serialize()).isEqualTo("TESTS: 50");
    }

    @Test
    void shouldVerifyContract() {
        var fifty = new Value(Metric.TESTS, Fraction.getFraction(50, 1));
        var hundred = new Value(Metric.CYCLOMATIC_COMPLEXITY, Fraction.getFraction(100, 1));
        var loc = new Value(Metric.LOC, 2);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.add(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.add(loc));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> hundred.delta(fifty));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> hundred.delta(loc));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> loc.max(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> loc.max(fifty));
    }

    @Test
    void shouldReturnDelta() {
        var fifty = new Value(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new Value(Metric.LINE, Fraction.getFraction(100, 1));

        assertThat(fifty.isOutOfValidRange(50.1)).isTrue();
        assertThat(fifty.isOutOfValidRange(50)).isFalse();

        assertThat(hundred.delta(fifty)).isEqualTo(Fraction.getFraction(50, 1));
    }
}
