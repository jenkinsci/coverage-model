package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class IntegerValueTest {
    @Test
    void shouldThrowExceptionOnInvalidDeltaParameter() {
        var linesOfCode = new LinesOfCode(20);

        var cyclomaticComplexity = new CyclomaticComplexity(10);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> linesOfCode.delta(cyclomaticComplexity))
                .withMessageContaining("Cannot cast incompatible types");

        var fractionValue = new FractionValue(Metric.LOC, Fraction.getFraction(1, 1));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> linesOfCode.delta(fractionValue))
                .withMessageContaining("Cannot cast incompatible types");
    }

    @Test
    void shouldSerialize() {
        var linesOfCode = new LinesOfCode(20);
        var cyclomaticComplexity = new CyclomaticComplexity(10);

        assertThat(linesOfCode.serialize())
                .isEqualTo("LOC: 20");
        assertThat(cyclomaticComplexity.serialize())
                .isEqualTo("COMPLEXITY: 10");
    }
}
