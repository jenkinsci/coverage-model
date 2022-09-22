package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link CyclomaticComplexity}.
 *
 * @author Melissa Bauer
 */
class LinesOfCodeTest {
    @Test
    void shouldCreateComplexityLeaf() {
        assertThat(new LinesOfCode(125)).hasValue(125);
        assertThat(new LinesOfCode(0)).hasValue(0);
    }

    @Test
    void shouldOperateWithLoc() {
        assertThat(new LinesOfCode(25).add(new LinesOfCode(100))).hasValue(125);
        assertThat(new LinesOfCode(25).max(new LinesOfCode(100))).hasValue(100);
        assertThat(new LinesOfCode(100).max(new LinesOfCode(99))).hasValue(100);
    }

    @Test
    void shouldFailAddForInvalidTypes() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).add(new CyclomaticComplexity(100)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).add(new Coverage(Metric.LINE, 1, 1)));
    }

    @Test
    void shouldFailMaxForInvalidTypes() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).max(new CyclomaticComplexity(100)));
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LinesOfCode(25).max(new Coverage(Metric.LINE, 1, 1)));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(CyclomaticComplexity.class).withRedefinedSuperclass().verify();
    }
}
