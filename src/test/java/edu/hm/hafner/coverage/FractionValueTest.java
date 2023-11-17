package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * TestCount the class {@link FractionValue}.
 *
 * @author Ullrich Hafner
 */
class FractionValueTest {
    @Test
    void shouldCreateDelta() {
        var fraction = Fraction.getFraction(50, 1);
        var fifty = new FractionValue(Metric.LINE, fraction);
        var fiftyAgain = new FractionValue(Metric.LINE, 50, 1);
        var hundred = new FractionValue(Metric.LINE, Fraction.getFraction(100, 1));

        assertThat(fifty.isOutOfValidRange(50.1)).isTrue();
        assertThat(fifty.isOutOfValidRange(50)).isFalse();

        assertThat(fifty.add(fifty)).isEqualTo(hundred);
        assertThat(fifty.max(hundred)).isEqualTo(hundred);
        assertThat(fifty.max(fiftyAgain)).isEqualTo(fifty);
        assertThat(hundred.max(fifty)).isEqualTo(hundred);

        assertThat(fifty).hasFraction(fraction);
        assertThat(fifty.serialize()).isEqualTo("LINE: 50/1");
    }

    @Test
    void shouldVerifyContract() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.FILE, Fraction.getFraction(100, 1));

        var fiftyLoc = new FractionValue(Metric.LOC, Fraction.getFraction(50, 1));
        var loc = new LinesOfCode(2);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.add(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fiftyLoc.add(loc));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.delta(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fiftyLoc.delta(loc));

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.max(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fiftyLoc.max(loc));
    }

    @Test
    void shouldReturnDelta() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.LINE, Fraction.getFraction(100, 1));

        assertThat(fifty.isOutOfValidRange(50.1)).isTrue();
        assertThat(fifty.isOutOfValidRange(50)).isFalse();

        assertThat(hundred.delta(fifty)).isEqualTo(Fraction.getFraction(50, 1));
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(FractionValue.class).withRedefinedSuperclass().verify();
    }
}
