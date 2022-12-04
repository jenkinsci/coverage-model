package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link CyclomaticComplexity}.
 *
 * @author Melissa Bauer
 */
class CyclomaticComplexityTest {
    private static final Coverage COVERAGE = new CoverageBuilder().setMetric(Metric.LINE)
            .setCovered(1)
            .setMissed(1)
            .build();

    @Test
    void shouldCreateComplexityLeaf() {
        assertThat(new CyclomaticComplexity(125)).hasValue(125);
        assertThat(new CyclomaticComplexity(0)).hasValue(0);
    }

    @Test
    void shouldCompareWithThreshold() {
        assertThat(new CyclomaticComplexity(125).isBelowThreshold(0)).isFalse();
        assertThat(new CyclomaticComplexity(125).isBelowThreshold(125)).isFalse();
        assertThat(new CyclomaticComplexity(125).isBelowThreshold(125.1)).isTrue();
    }

    @Test
    void shouldOperateWithComplexities() {
        assertThat(new CyclomaticComplexity(25).add(new CyclomaticComplexity(100))).hasValue(125);
        assertThat(new CyclomaticComplexity(25).max(new CyclomaticComplexity(100))).hasValue(100);
        assertThat(new CyclomaticComplexity(100).max(new CyclomaticComplexity(99))).hasValue(100);
    }

    @Test
    void shouldFailAddForInvalidTypes() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CyclomaticComplexity(25).add(new LinesOfCode(100)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CyclomaticComplexity(25).add(COVERAGE));
    }

    @Test
    void shouldFailMaxForInvalidTypes() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CyclomaticComplexity(25).max(new LinesOfCode(100)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new CyclomaticComplexity(25).max(COVERAGE));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CyclomaticComplexity.class).withRedefinedSuperclass().verify();
    }
}
