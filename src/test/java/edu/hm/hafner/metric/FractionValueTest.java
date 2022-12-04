package edu.hm.hafner.metric;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

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
        var hundred = new FractionValue(Metric.LINE, Fraction.getFraction(100, 1));

        assertThat(fifty.isBelowThreshold(50.1)).isTrue();
        assertThat(fifty.isBelowThreshold(50)).isFalse();

        assertThat(fifty.add(fifty)).isEqualTo(hundred);
        assertThat(fifty.max(hundred)).isEqualTo(hundred);
    }
}
