package edu.hm.hafner.coverage;

import java.util.NavigableSet;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * TestCount the class {@link Metric}.
 *
 * @author Ullrich Hafner
 */
class MetricTest {
    @ValueSource(strings = {"COMPLEXITY", "CyclomaticComplexity",
            "cyclomatic-complexity", "cyclomatic_complexity", "CYCLOMATIC_COMPLEXITY"})
    @ParameterizedTest(name = "{0} should be converted to metric COMPLEXITY")
    void shouldMapFromName(final String name) {
        assertThat(Metric.fromName(name)).isSameAs(Metric.CYCLOMATIC_COMPLEXITY);
    }

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
                Metric.INSTRUCTION,
                Metric.MCDC_PAIR,
                Metric.FUNCTION_CALL);
    }

    /**
     * TestCount if the object in the evaluator-attribute of class {@link Metric}
     * correctly overrides its isAggregatingChildren-method.
     */
    @Test
    void shouldCorrectlyImplementIsContainer() {
        assertThat(Metric.MODULE.isContainer()).isTrue();
        assertThat(Metric.LINE.isContainer()).isFalse();
        assertThat(Metric.CYCLOMATIC_COMPLEXITY_DENSITY.isContainer()).isFalse();
        assertThat(Metric.LOC.isContainer()).isFalse();
    }

    @Test
    void shouldCorrectlyComputeDensityEvaluator() {
        var node = new PackageNode("package");
        node.addValue(new Coverage.CoverageBuilder().withMetric(Metric.LINE).withCovered(5).withMissed(5).build());
        node.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));

        Value loc = Metric.LOC.getValueFor(node).get();
        assertThat(loc.asInteger()).isEqualTo(10);
        assertThat(loc)
                .hasMetric(Metric.LOC)
                .hasFraction(Fraction.getFraction(10, 1));
        Value complexityDensity = Metric.CYCLOMATIC_COMPLEXITY_DENSITY.getValueFor(node).get();
        assertThat(complexityDensity)
                .hasMetric(Metric.CYCLOMATIC_COMPLEXITY_DENSITY)
                .hasFraction(Fraction.getFraction(10, 10));
    }

    @Test
    void shouldReturnEmptyOptionalOnComputeDensityEvaluator() {
        var zeroLines = new Coverage.CoverageBuilder().withMetric(Metric.LINE).withCovered(0).withMissed(0).build();
        var tenLines = new Coverage.CoverageBuilder().withMetric(Metric.LINE).withCovered(5).withMissed(5).build();
        var cyclomaticComplexity = new Value(Metric.CYCLOMATIC_COMPLEXITY, 10);

        var onlyLinesOfCode = new PackageNode("package");
        onlyLinesOfCode.addValue(tenLines);
        var onlyCyclomaticComplexity = new PackageNode("package");
        onlyCyclomaticComplexity.addValue(cyclomaticComplexity);
        var zeroLinesOfCodeWithComplexity = new PackageNode("package");
        zeroLinesOfCodeWithComplexity.addValue(zeroLines);
        zeroLinesOfCodeWithComplexity.addValue(cyclomaticComplexity);

        assertThat(Metric.CYCLOMATIC_COMPLEXITY_DENSITY.getValueFor(onlyLinesOfCode)).isEmpty();
        assertThat(Metric.CYCLOMATIC_COMPLEXITY_DENSITY.getValueFor(onlyCyclomaticComplexity)).isEmpty();
        assertThat(Metric.CYCLOMATIC_COMPLEXITY_DENSITY.getValueFor(zeroLinesOfCodeWithComplexity)).isEmpty();
    }
}
