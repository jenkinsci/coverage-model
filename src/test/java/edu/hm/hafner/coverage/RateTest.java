package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class RateTest {
    @Test
    void shouldCreateRateInstances() {
        var rate = Rate.valueOf("COHESION: %3:4");

        assertThat(rate.asDouble()).isCloseTo(75, withinPercentage(0.0001));
        assertThat(rate.asText(Locale.ENGLISH)).isEqualTo("75.00%");
        assertThat(rate.asInformativeText(Locale.ENGLISH)).isEqualTo("75.00%");
        assertThat(rate.asRoundedText(Locale.ENGLISH)).isEqualTo("75.00");
        assertThat(rate.serialize()).isEqualTo("COHESION: %3:4");

        assertThat(rate.add(rate).asDouble()).isCloseTo(75, withinPercentage(0.0001));
    }

    @Test
    void shouldCorrectlyAdd() {
        var zero = Rate.valueOf("COHESION: %0:6");
        var hundred = Rate.valueOf("COHESION: %6:6");

        var fifty = zero.add(hundred);
        assertThat(fifty.asDouble()).isCloseTo(50, withinPercentage(0.0001));
        assertThat(fifty.serialize()).isEqualTo("COHESION: %6:12");
    }

    @Test
    void shouldThrowExceptionOnWrongSerialization() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> Rate.valueOf("COHESION: 75")
        );
        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(
                () -> new Rate(Metric.COHESION, Long.MAX_VALUE, 1)
        );
    }
}
