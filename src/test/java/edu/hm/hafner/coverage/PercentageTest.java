package edu.hm.hafner.coverage;

import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static org.assertj.core.api.Assertions.*;

/**
 * Test class for {@link Percentage}.
 *
 * @author Florian Orendi
 */
class PercentageTest {
    private static final double COVERAGE_FRACTION = 0.5;

    @Test
    void shouldHandleOverflow() {
        var fraction = Fraction.getFraction(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        var percentage = Percentage.valueOf(fraction);
        assertThat(percentage.toDouble()).isEqualTo(100);
        assertThat(percentage.toInt()).isEqualTo(100);
        assertThat(percentage.formatDeltaPercentage(Locale.ENGLISH)).isEqualTo("+100.00%");
    }

    @Test
    void shouldCreatePercentageFromFraction() {
        var fraction = Fraction.getFraction(COVERAGE_FRACTION);
        var percentage = Percentage.valueOf(fraction);
        assertThat(percentage.toDouble()).isEqualTo(50.0);
        assertThat(percentage.formatDeltaPercentage(Locale.ENGLISH)).isEqualTo("+50.00%");
    }

    @Test
    void shouldCreatePercentageFromNumeratorAndDenominator() {
        var percentage = Percentage.valueOf(50, 100);
        assertThat(percentage.toDouble()).isEqualTo(50.0);
    }

    @Test
    void shouldNotCreatePercentageFromNumeratorAndZeroDenominator() {
        assertThatThrownBy(() -> Percentage.valueOf(50, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(Percentage.TOTALS_ZERO_MESSAGE);
    }

    @Test
    void shouldNotCreatePercentageOfInvalidStringRepresentation() {
        assertThatThrownBy(() -> Percentage.valueOf("99%"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Percentage.valueOf("0.99/1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(Percentage.class).verify();
    }

    @Test
    void shouldSerializeInstance() {
        var percentage = Percentage.valueOf(49, 100);
        assertThat(percentage.serializeToString()).isEqualTo("49/100");
        assertThat(Percentage.valueOf("49/100")).isEqualTo(percentage).hasToString("49.00%");

        assertThatIllegalArgumentException().isThrownBy(() -> Percentage.valueOf("1/0"));
        assertThatIllegalArgumentException().isThrownBy(() -> Percentage.valueOf("2/1"));
    }

    @Test
    void shouldRoundCorrectly() {
        var oneThird = Percentage.valueOf(1, 3);

        assertThat(oneThird.serializeToString()).isEqualTo("1/3");
        assertThat(oneThird.formatPercentage(Locale.GERMAN)).isEqualTo("33,33%");
        assertThat(oneThird.formatPercentage()).isEqualTo("33.33%");
        assertThat(oneThird.toInt()).isEqualTo(33);
        assertThat(oneThird.toRounded()).isEqualTo(33.33);

        var twoThirds = Percentage.valueOf(2, 3);

        assertThat(twoThirds.serializeToString()).isEqualTo("2/3");
        assertThat(twoThirds.formatPercentage(Locale.GERMAN)).isEqualTo("66,67%");
        assertThat(twoThirds.formatPercentage()).isEqualTo("66.67%");
        assertThat(twoThirds.toInt()).isEqualTo(67);
        assertThat(twoThirds.toRounded()).isEqualTo(66.67);
    }

    @Test
    void shouldHandle100PercentCorrectly() {
        var oneMissing = Percentage.valueOf(1_000_000 - 1, 1_000_000);

        assertThat(oneMissing.formatPercentage(Locale.GERMAN)).isEqualTo("99,99%");
        assertThat(oneMissing.formatPercentage()).isEqualTo("99.99%");
        assertThat(oneMissing.toInt()).isEqualTo(99);
        assertThat(oneMissing.toDouble()).isEqualTo(99.9999);
        assertThat(oneMissing.toRounded()).isEqualTo(99.99);
    }
}
