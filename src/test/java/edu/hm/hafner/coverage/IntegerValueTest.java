package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

@SuppressFBWarnings(value = "RV_RETURN_VALUE_IGNORED", justification = "Exception is thrown anyway")
abstract class IntegerValueTest {
    private static Fraction getDelta(final int value) {
        return Fraction.getFraction(value, 1);
    }

    abstract IntegerValue createValue(int value);

    @Test
    void shouldThrowExceptionOnInvalidDeltaParameter() {
        var value = createValue(20);

        var coverage = Coverage.nullObject(Metric.LINE);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> value.delta(coverage))
                .withMessageContaining("Cannot cast incompatible types");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> value.add(coverage))
                .withMessageContaining("Cannot cast incompatible types");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> value.max(coverage))
                .withMessageContaining("Cannot cast incompatible types");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> value.delta(coverage))
                .withMessageContaining("Cannot cast incompatible types");
    }

    @Test
    void shouldCreateValue() {
        assertThat(createValue(100)).hasValue(100);
        assertThat(createValue(-100)).hasValue(-100);
        assertThat(createValue(0)).hasValue(0);

        var value = createValue(123);
        assertThat(value.serialize()).startsWith(value.getMetric().name()).endsWith(": 123");

        assertThat(value.create(100, Metric.COMPLEXITY)).hasValue(100);
        assertThat(value.create(-100, Metric.COMPLEXITY)).hasValue(-100);
        assertThat(value.create(0, Metric.COMPLEXITY)).hasValue(0);
    }

    @Test
    void shouldCompareWithThreshold() {
        assertThat(createValue(125).isOutOfValidRange(200)).isFalse();
        assertThat(createValue(125).isOutOfValidRange(125)).isFalse();
        assertThat(createValue(125).isOutOfValidRange(124.9)).isTrue();
    }

    @Test
    void shouldAddValues() {
        assertThat(createValue(25).add(createValue(100))).hasValue(125);
        assertThat(createValue(-25).add(createValue(100))).hasValue(75);
    }

    @Test
    void shouldFindMaximum() {
        assertThat(createValue(25).max(createValue(26))).hasValue(26);
        assertThat(createValue(26).max(createValue(25))).hasValue(26);
    }

    @Test
    void shouldComputeDelta() {
        var large = createValue(1000);
        var medium = createValue(100);
        var small = createValue(10);

        assertThat(large.delta(medium)).isEqualTo(getDelta(900));
        assertThat(large.delta(small)).isEqualTo(getDelta(990));
        assertThat(medium.delta(small)).isEqualTo(getDelta(90));
        assertThat(medium.delta(large)).isEqualTo(getDelta(-900));
        assertThat(small.delta(large)).isEqualTo(getDelta(-990));
        assertThat(small.delta(medium)).isEqualTo(getDelta(-90));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(createValue(0).getClass()).withRedefinedSuperclass().verify();
    }
}
