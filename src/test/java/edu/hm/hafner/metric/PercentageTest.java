package edu.hm.hafner.metric;

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
    private static final double COVERAGE_PERCENTAGE = 50.0;
    private static final Locale LOCALE = Locale.GERMAN;

    @Test
    void shouldHandleOverflow() {
        Fraction fraction = Fraction.getFraction(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 1);
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(fraction);
        assertThat(Percentage.getDoubleValue()).isEqualTo(100);
    }

    @Test
    void shouldCreatePercentageFromFraction() {
        Fraction fraction = Fraction.getFraction(COVERAGE_FRACTION);
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(fraction);
        assertThat(Percentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldCreatePercentageFromDouble() {
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(Percentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldCreatePercentageFromNumeratorAndDenominator() {
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(50, 1);
        assertThat(Percentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldNotCreatePercentageFromNumeratorAndZeroDenominator() {
        assertThatThrownBy(() -> edu.hm.hafner.metric.Percentage.valueOf(50, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(Percentage.DENOMINATOR_ZERO_MESSAGE);
    }

    @Test
    void shouldNotCreatePercentageOfInvalidStringRepresentation() {
        assertThatThrownBy(() -> edu.hm.hafner.metric.Percentage.valueOf("99%"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> edu.hm.hafner.metric.Percentage.valueOf("0.99/1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldHaveWorkingGetters() {
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(Percentage.getNumerator()).isEqualTo(50);
        assertThat(Percentage.getDenominator()).isEqualTo(1);
    }

    @Test
    void shouldGetDoubleValue() {
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(Percentage.getDoubleValue()).isEqualTo(50.0);
    }

    @Test
    void shouldFormatPercentage() {
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(Percentage.formatPercentage(LOCALE)).isEqualTo("50,00%");
    }

    @Test
    void shouldFormatDeltaPercentage() {
        Percentage Percentage = edu.hm.hafner.metric.Percentage.valueOf(COVERAGE_PERCENTAGE);
        assertThat(Percentage.formatDeltaPercentage(LOCALE)).isEqualTo("+50,00%");
    }

    @Test
    void shouldObeyEqualsContract() {
        EqualsVerifier.forClass(Percentage.class).verify();
    }

    @Test
    void shouldSerializeInstance() {
        Percentage percentage = edu.hm.hafner.metric.Percentage.valueOf(49, 1);
        assertThat(percentage.serializeToString())
                .isEqualTo("49/1");
        assertThat(edu.hm.hafner.metric.Percentage.valueOf("49/1")).isEqualTo(percentage).hasToString("49.00%");

        assertThatIllegalArgumentException().isThrownBy(() -> Percentage.valueOf("1/0"));
    }
}
