package edu.hm.hafner.coverage;

import java.util.Locale;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class DifferenceTest {
    @Test
    void shouldFormatPercentageWithSign() {
        var positive = new Difference(Metric.COHESION, 2, 3);

        assertThat(positive.asInteger()).isEqualTo(1);
        assertThat(positive.asDouble()).isEqualTo(2.0 / 3);
        assertThat(positive.asRounded()).isEqualTo(0.67);

        assertThat(positive.asText(Locale.ENGLISH)).isEqualTo("+66.67%");
        assertThat(positive.asInformativeText(Locale.ENGLISH)).isEqualTo("+66.67%");
        assertThat(positive.serialize()).isEqualTo("COHESION: Δ2:3");
        assertThat(positive).isEqualTo(Value.valueOf("COHESION: Δ2:3"));

        var negative = new Difference(Metric.COHESION, -2, 3);

        assertThat(negative.asInteger()).isEqualTo(-1);
        assertThat(negative.asDouble()).isEqualTo(-2.0 / 3);
        assertThat(negative.asRounded()).isEqualTo(-0.67);

        assertThat(negative.asText(Locale.ENGLISH)).isEqualTo("-66.67%");
        assertThat(negative.asInformativeText(Locale.ENGLISH)).isEqualTo("-66.67%");
        assertThat(negative.serialize()).isEqualTo("COHESION: Δ-2:3");
        assertThat(negative).isEqualTo(Value.valueOf("COHESION: Δ-2:3"));

        var zero = new Difference(Metric.COHESION, 0);

        assertThat(zero.asInteger()).isEqualTo(0);
        assertThat(zero.asDouble()).isEqualTo(0);
        assertThat(zero.asRounded()).isEqualTo(0);

        assertThat(zero.asText(Locale.ENGLISH)).isEqualTo("±0%");
        assertThat(zero.asInformativeText(Locale.ENGLISH)).isEqualTo("±0%");
        assertThat(zero.serialize()).isEqualTo("COHESION: Δ0");
        assertThat(zero).isEqualTo(Value.valueOf("COHESION: Δ0"));
    }

    @Test
    void shouldFormatCoverageWithSign() {
        var positive = new Difference(Metric.LINE, 200, 3);

        assertThat(positive.asInteger()).isEqualTo(67);
        assertThat(positive.asDouble()).isEqualTo(200.0 / 3);
        assertThat(positive.asRounded()).isEqualTo(66.67);

        assertThat(positive.asText(Locale.ENGLISH)).isEqualTo("+66.67%");
        assertThat(positive.asInformativeText(Locale.ENGLISH)).isEqualTo("+66.67%");
        assertThat(positive.serialize()).isEqualTo("LINE: Δ200:3");
        assertThat(positive).isEqualTo(Value.valueOf("LINE: Δ200:3"));

        var negative = new Difference(Metric.LINE, -200, 3);

        assertThat(negative.asInteger()).isEqualTo(-67);
        assertThat(negative.asDouble()).isEqualTo(-200.0 / 3);
        assertThat(negative.asRounded()).isEqualTo(-66.67);

        assertThat(negative.asText(Locale.ENGLISH)).isEqualTo("-66.67%");
        assertThat(negative.asInformativeText(Locale.ENGLISH)).isEqualTo("-66.67%");
        assertThat(negative.serialize()).isEqualTo("LINE: Δ-200:3");
        assertThat(negative).isEqualTo(Value.valueOf("LINE: Δ-200:3"));

        var zero = new Difference(Metric.LINE, 0);

        assertThat(zero.asInteger()).isEqualTo(0);
        assertThat(zero.asDouble()).isEqualTo(0);
        assertThat(zero.asRounded()).isEqualTo(0);

        assertThat(zero.asText(Locale.ENGLISH)).isEqualTo("±0%");
        assertThat(zero.asInformativeText(Locale.ENGLISH)).isEqualTo("±0%");
        assertThat(zero.serialize()).isEqualTo("LINE: Δ0");
        assertThat(zero).isEqualTo(Value.valueOf("LINE: Δ0"));
    }

    @Test
    void shouldFormatIntegerWithSign() {
        var positive = new Difference(Metric.LOC, 2);

        assertThat(positive.asInteger()).isEqualTo(2);
        assertThat(positive.asDouble()).isEqualTo(2.0);
        assertThat(positive.asRounded()).isEqualTo(2);

        assertThat(positive.asText(Locale.ENGLISH)).isEqualTo("+2");
        assertThat(positive.asInformativeText(Locale.ENGLISH)).isEqualTo("+2");
        assertThat(positive.serialize()).isEqualTo("LOC: Δ2");
        assertThat(positive).isEqualTo(Value.valueOf("LOC: Δ2"));

        var negative = new Difference(Metric.LOC, -2);

        assertThat(negative.asInteger()).isEqualTo(-2);
        assertThat(negative.asDouble()).isEqualTo(-2.0);
        assertThat(negative.asRounded()).isEqualTo(-2);

        assertThat(negative.asText(Locale.ENGLISH)).isEqualTo("-2");
        assertThat(negative.asInformativeText(Locale.ENGLISH)).isEqualTo("-2");
        assertThat(negative.serialize()).isEqualTo("LOC: Δ-2");
        assertThat(negative).isEqualTo(Value.valueOf("LOC: Δ-2"));

        var zero = new Difference(Metric.LOC, 0);

        assertThat(zero.asInteger()).isEqualTo(0);
        assertThat(zero.asDouble()).isEqualTo(0);
        assertThat(zero.asRounded()).isEqualTo(0);

        assertThat(zero.asText(Locale.ENGLISH)).isEqualTo("±0");
        assertThat(zero.asInformativeText(Locale.ENGLISH)).isEqualTo("±0");
        assertThat(zero.serialize()).isEqualTo("LOC: Δ0");
        assertThat(zero).isEqualTo(Value.valueOf("LOC: Δ0"));
    }
}
