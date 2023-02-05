package edu.hm.hafner.metric;

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
        Fraction fraction = Fraction.getFraction(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        Percentage percentage = Percentage.valueOf(fraction);
        assertThat(percentage.toDouble()).isEqualTo(100);
    }

    @Test
    void shouldCreatePercentageFromFraction() {
        Fraction fraction = Fraction.getFraction(COVERAGE_FRACTION);
        Percentage percentage = Percentage.valueOf(fraction);
        assertThat(percentage.toDouble()).isEqualTo(50.0);
    }

    @Test
    void shouldCreatePercentageFromNumeratorAndDenominator() {
        Percentage percentage = Percentage.valueOf(50, 100);
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
        Percentage percentage = Percentage.valueOf(49, 100);
        assertThat(percentage.serializeToString()).isEqualTo("49/100");
        assertThat(Percentage.valueOf("49/100")).isEqualTo(percentage).hasToString("49.00%");

        assertThatIllegalArgumentException().isThrownBy(() -> Percentage.valueOf("1/0"));
        assertThatIllegalArgumentException().isThrownBy(() -> Percentage.valueOf("2/1"));
    }
}
