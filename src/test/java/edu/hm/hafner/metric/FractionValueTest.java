package edu.hm.hafner.metric;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link FractionValue}.
 *
 * @author Ullrich Hafner
 */
class FractionValueTest {
    @Test
    void shouldCreateDelta() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var fiftyAgain = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.LINE, Fraction.getFraction(100, 1));

        assertThat(fifty.isBelowThreshold(50.1)).isTrue();
        assertThat(fifty.isBelowThreshold(50)).isFalse();

        assertThat(fifty.add(fifty)).isEqualTo(hundred);
        assertThat(fifty.max(hundred)).isEqualTo(hundred);
        assertThat(fifty.max(fiftyAgain)).isEqualTo(fifty);
        assertThat(hundred.max(fifty)).isEqualTo(hundred);
    }

    @Test
    void shouldGetFraction() {
        FractionValue testFractionValue = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        assertThat(testFractionValue.getFraction()).isEqualTo(Fraction.getFraction(50, 1));
    }

    @Test
    void shouldThrowExceptionAdd() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.FILE, Fraction.getFraction(100, 1));

        var fiftyLoc = new FractionValue(Metric.LOC, Fraction.getFraction(50, 1));
        var loc = new LinesOfCode(2);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.add(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fiftyLoc.add(loc));
    }


    @Test
    void shouldReturnDelta() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.LINE, Fraction.getFraction(100, 1));

        assertThat(fifty.isBelowThreshold(50.1)).isTrue();
        assertThat(fifty.isBelowThreshold(50)).isFalse();

        assertThat(hundred.delta(fifty)).isEqualTo(Fraction.getFraction(50, 1));
    }
    @Test
    void shouldThrowExceptionDelta() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.FILE, Fraction.getFraction(100, 1));

        var fiftyLoc = new FractionValue(Metric.LOC, Fraction.getFraction(50, 1));
        var loc = new LinesOfCode(2);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.delta(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fiftyLoc.delta(loc));
    }

    @Test
    void shouldThrowExceptionMax() {
        var fifty = new FractionValue(Metric.LINE, Fraction.getFraction(50, 1));
        var hundred = new FractionValue(Metric.FILE, Fraction.getFraction(100, 1));

        var fiftyLoc = new FractionValue(Metric.LOC, Fraction.getFraction(50, 1));
        var loc = new LinesOfCode(2);

        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fifty.max(hundred));
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> fiftyLoc.max(loc));
    }

    @Test
    void equalTest() {
        EqualsVerifier.forClass(FractionValue.class).withRedefinedSuperclass().verify();
    }


}
