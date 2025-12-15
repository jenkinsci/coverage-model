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
