package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.NavigableSet;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link Metric}.
 *
 * @author Ullrich Hafner
 */
class MetricTest {
    @EnumSource(Metric.class)
    @ParameterizedTest(name = "{0} should be converted to a tag name and then back to a metric")
    void shouldConvertToTags(final Metric metric) {
        var tag = metric.toTagName();
        assertThat(tag).matches("^[a-z-]*$");

        var converted = Metric.fromTag(tag);
        assertThat(converted).isSameAs(metric);
    }

    @Test
    void shouldGetCoverageMetrics() {
        NavigableSet<Metric> metrics = Metric.getCoverageMetrics();

        assertThat(metrics).containsExactly(
                Metric.CONTAINER,
                Metric.MODULE,
                Metric.PACKAGE,
                Metric.FILE,
                Metric.CLASS,
                Metric.METHOD,
                Metric.LINE,
                Metric.BRANCH,
                Metric.INSTRUCTION);
    }

    /**
     * Tests if the object in the evaluator-attribute of class {@link Metric}
     * correctly overrides its isAggregatingChildren-method.
     */
    @Test
    void shouldCorrectlyImplementIsContainer() {
        assertThat(Metric.MODULE.isContainer()).isTrue();
        assertThat(Metric.LINE.isContainer()).isFalse();
        assertThat(Metric.COMPLEXITY_DENSITY.isContainer()).isFalse();
        assertThat(Metric.LOC.isContainer()).isFalse();
    }

    @Test
    void shouldCorrectlyComputeDensityEvaluator() {
        var node = new PackageNode("package");
        node.addValue(new Coverage.CoverageBuilder().setMetric(Metric.LINE).setCovered(5).setMissed(5).build());
        node.addValue(new CyclomaticComplexity(10));

        Value complexityDensity = Metric.COMPLEXITY_DENSITY.getValueFor(node).get();

        assertThat(complexityDensity)
                .isInstanceOf(FractionValue.class);
        assertThat((FractionValue) complexityDensity)
                .hasFraction(Fraction.getFraction(10, 10));
    }

    @Test
    void shouldReturnEmptyOptionalOnComputeDensityEvaluator() {
        Coverage zeroLines = new Coverage.CoverageBuilder().setMetric(Metric.LINE).setCovered(0).setMissed(0).build();
        Coverage tenLines = new Coverage.CoverageBuilder().setMetric(Metric.LINE).setCovered(5).setMissed(5).build();
        CyclomaticComplexity cyclomaticComplexity = new CyclomaticComplexity(10);

        var onlyLinesOfCode = new PackageNode("package");
        onlyLinesOfCode.addValue(tenLines);
        var onlyCyclomaticComplexity = new PackageNode("package");
        onlyCyclomaticComplexity.addValue(cyclomaticComplexity);
        var zeroLinesOfCodeWithComplexity = new PackageNode("package");
        zeroLinesOfCodeWithComplexity.addValue(zeroLines);
        zeroLinesOfCodeWithComplexity.addValue(cyclomaticComplexity);

        assertThat(Metric.COMPLEXITY_DENSITY.getValueFor(onlyLinesOfCode)).isEmpty();
        assertThat(Metric.COMPLEXITY_DENSITY.getValueFor(onlyCyclomaticComplexity)).isEmpty();
        assertThat(Metric.COMPLEXITY_DENSITY.getValueFor(zeroLinesOfCodeWithComplexity)).isEmpty();
    }
}
