package edu.hm.hafner.coverage;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import edu.hm.hafner.coverage.Metric.MetricTendency;
import edu.hm.hafner.coverage.Metric.MetricValueType;

import java.util.Locale;
import java.util.NavigableSet;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link Metric}.
 *
 * @author Ullrich Hafner
 */
class MetricTest {
    @ValueSource(strings = {"COMPLEXITY", "CyclomaticComplexity",
            "cyclomatic-complexity", "cyclomatic_complexity", "CYCLOMATIC_COMPLEXITY"})
    @ParameterizedTest(name = "{0} should be converted to metric COMPLEXITY")
    void shouldMapFromName(final String name) {
        assertThat(Metric.fromName(name)).isSameAs(Metric.CYCLOMATIC_COMPLEXITY)
                .hasTendency(MetricTendency.SMALLER_IS_BETTER)
                .isNotCoverage()
                .isNotContainer()
                .hasDisplayName("Cyclomatic Complexity");
    }

    @Test
    void shouldProvideContextWhenMetricIsWrong() {
        assertThatIllegalArgumentException().isThrownBy(() -> Metric.fromName("undefined"))
                .withMessageContaining("No metric found for name 'undefined'");
        assertThatIllegalArgumentException().isThrownBy(() -> Metric.fromName(""))
                .withMessageContaining("No metric defined");
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
                Metric.FUNCTION_CALL,
                Metric.STATEMENT,
                Metric.STMT_DC,
                Metric.STMT_CC,
                Metric.CONDITION,
                Metric.DECISION,
                Metric.FUNCTION,
                Metric.OBJECT_CODE,
                Metric.BYTES,
                Metric.MUTATION);
    }

    /**
     * TestCount if the object in the evaluator-attribute of class {@link Metric}
     * correctly overrides its isAggregatingChildren-method.
     */
    @Test
    void shouldCorrectlyImplementIsContainer() {
        assertThat(Metric.MODULE)
                .isContainer()
                .isCoverage()
                .hasDisplayName("Module Coverage")
                .hasLabel("Module");
        assertThat(Metric.FILE)
                .isContainer()
                .isCoverage()
                .hasDisplayName("File Coverage")
                .hasLabel("File");
        assertThat(Metric.LINE)
                .isNotContainer()
                .isCoverage()
                .hasDisplayName("Line Coverage")
                .hasLabel("Line");
        assertThat(Metric.LOC)
                .isNotContainer()
                .isNotCoverage()
                .hasDisplayName("Lines of Code")
                .hasLabel("LOC");
    }

    @Test
    void shouldCorrectlyComputeLoc() {
        var node = new PackageNode("package");

        node.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 10));
        assertThat(Metric.LOC.getValueFor(node)).isEmpty(); // no line coverage yet

        node.addValue(new Coverage.CoverageBuilder().withMetric(Metric.LINE).withCovered(5).withMissed(5).build());

        assertThat(Metric.LOC.getValueFor(node)).hasValueSatisfying(loc -> {
            assertThat(loc.asInteger()).isEqualTo(10);
            assertThat(loc.asText(Locale.ENGLISH)).isEqualTo("10");
            assertThat(loc)
                    .hasMetric(Metric.LOC)
                    .hasFraction(Fraction.getFraction(10, 1));
        });
    }

    @Test
    void shouldFormatMetricValues() {
        var root = new PackageNode("package");
        root.createClassNode("class").createMethodNode("method", "()");

        var complexity = Metric.CYCLOMATIC_COMPLEXITY;
        assertThat(complexity).hasTendency(MetricTendency.SMALLER_IS_BETTER)
                .isNotContainer().isNotCoverage().hasDisplayName("Cyclomatic Complexity");
        assertThat(complexity.format(Locale.ENGLISH, 355)).isEqualTo("355");
        assertThat(complexity.formatMean(Locale.ENGLISH, 355)).isEqualTo("355.00");
        assertThat(complexity.getAggregationType()).isEqualTo("total");
        assertThat(complexity.getType()).isEqualTo(MetricValueType.METHOD_METRIC);
        assertThat(complexity.parseValue("355.7")).satisfies(value ->
                assertThat(value.asText(Locale.ENGLISH)).isEqualTo("356"));

        assertThat(complexity.getTargetNodes(root)).hasSize(1)
                .first().extracting(Node::getName).isEqualTo("method()");

        var cohesion = Metric.COHESION;
        assertThat(cohesion).hasTendency(MetricTendency.LARGER_IS_BETTER)
                .isNotContainer().isNotCoverage().hasDisplayName("Class Cohesion");
        assertThat(cohesion.format(Locale.ENGLISH, 0.355)).isEqualTo("35.50%");
        assertThat(cohesion.formatMean(Locale.ENGLISH, 0.355)).isEqualTo("35.50%");
        assertThat(cohesion.getAggregationType()).isEqualTo("maximum");
        assertThat(cohesion.getType()).isEqualTo(MetricValueType.CLASS_METRIC);
        assertThat(cohesion.parseValue("0.355")).satisfies(value ->
                assertThat(value.asText(Locale.ENGLISH)).isEqualTo("35.50%"));

        assertThat(cohesion.getTargetNodes(root)).hasSize(1)
                .first().extracting(Node::getName).isEqualTo("class");

        var coverage = Metric.PACKAGE;
        assertThat(coverage).hasTendency(MetricTendency.LARGER_IS_BETTER)
                .isContainer().isCoverage().hasDisplayName("Package Coverage");
        assertThat(coverage.getAggregationType()).isEmpty();
        assertThat(coverage.getType()).isEqualTo(MetricValueType.COVERAGE);
        assertThat(coverage.getTargetNodes(root)).hasSize(1)
                .first().extracting(Node::getName).isEqualTo("class");
    }
}
