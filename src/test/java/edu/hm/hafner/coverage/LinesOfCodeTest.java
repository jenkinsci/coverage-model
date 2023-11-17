package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * TestCount the class {@link CyclomaticComplexity}.
 *
 * @author Melissa Bauer
 */
class LinesOfCodeTest {
    private static final Coverage COVERAGE = new CoverageBuilder().withMetric(Metric.LINE)
            .withCovered(1)
            .withMissed(1)
            .build();

    @Test
    void shouldCreateComplexityLeaf() {
        assertThat(new LinesOfCode(125)).hasValue(125);
        assertThat(new LinesOfCode(0)).hasValue(0);
    }

    @Test
    void shouldCompareWithThreshold() {
        assertThat(new LinesOfCode(125).isOutOfValidRange(200)).isFalse();
        assertThat(new LinesOfCode(125).isOutOfValidRange(125)).isFalse();
        assertThat(new LinesOfCode(125).isOutOfValidRange(124.9)).isTrue();
    }

    @Test
    void shouldOperateWithLoc() {
        assertThat(new LinesOfCode(25).add(new LinesOfCode(100))).hasValue(125);
        assertThat(new LinesOfCode(25).max(new LinesOfCode(100))).hasValue(100);
        assertThat(new LinesOfCode(100).max(new LinesOfCode(99))).hasValue(100);
    }

    @Test
    void shouldComputeDelta() {
        var large = new LinesOfCode(1000);
        var medium = new LinesOfCode(100);
        var small = new LinesOfCode(10);

        assertThat(large.delta(medium)).isEqualTo(getDelta(900));
        assertThat(large.delta(small)).isEqualTo(getDelta(990));
        assertThat(medium.delta(small)).isEqualTo(getDelta(90));
        assertThat(medium.delta(large)).isEqualTo(getDelta(-900));
        assertThat(small.delta(large)).isEqualTo(getDelta(-990));
        assertThat(small.delta(medium)).isEqualTo(getDelta(-90));
    }

    private static Fraction getDelta(final int value) {
        return Fraction.getFraction(value, 1);
    }

    @Test
    void shouldFailAddForInvalidTypes() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).add(new CyclomaticComplexity(100)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).add(COVERAGE));
    }

    @Test
    void shouldFailMaxForInvalidTypes() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).max(new CyclomaticComplexity(100)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).max(COVERAGE));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CyclomaticComplexity.class).withRedefinedSuperclass().verify();
    }
}
