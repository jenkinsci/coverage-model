package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static edu.hm.hafner.coverage.assertions.Assertions.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link MetricAggregation}.
 *
 * @author Akash Manna
 */
class MetricAggregationTest {
    @Test
    void shouldHaveCorrectDisplayNames() {
        assertThat(MetricAggregation.TOTAL).hasDisplayName("Total").hasId("total");
        assertThat(MetricAggregation.MAXIMUM).hasDisplayName("Maximum").hasId("maximum");
        assertThat(MetricAggregation.MINIMUM).hasDisplayName("Minimum").hasId("minimum");
        assertThat(MetricAggregation.AVERAGE).hasDisplayName("Average").hasId("average");
    }

    @Test
    void shouldSupportTotalForAllMetrics() {
        for (Metric metric : Metric.values()) {
            assertThat(MetricAggregation.TOTAL.isSupported(metric))
                    .as("TOTAL should be supported for " + metric)
                    .isTrue();
        }
    }

    @Test
    void shouldSupportAggregationsForMethodMetrics() {
        assertThat(MetricAggregation.MAXIMUM.isSupported(Metric.CYCLOMATIC_COMPLEXITY)).isTrue();
        assertThat(MetricAggregation.MINIMUM.isSupported(Metric.CYCLOMATIC_COMPLEXITY)).isTrue();
        assertThat(MetricAggregation.AVERAGE.isSupported(Metric.CYCLOMATIC_COMPLEXITY)).isTrue();
    }

    @Test
    void shouldSupportAggregationsForClassMetrics() {
        assertThat(MetricAggregation.MAXIMUM.isSupported(Metric.COHESION)).isTrue();
        assertThat(MetricAggregation.MINIMUM.isSupported(Metric.COHESION)).isTrue();
        assertThat(MetricAggregation.AVERAGE.isSupported(Metric.COHESION)).isTrue();
    }

    @Test
    void shouldSupportAggregationsForGeneralMetrics() {
        assertThat(MetricAggregation.MAXIMUM.isSupported(Metric.LOC)).isTrue();
        assertThat(MetricAggregation.MINIMUM.isSupported(Metric.LOC)).isTrue();
        assertThat(MetricAggregation.AVERAGE.isSupported(Metric.LOC)).isTrue();
    }

    @Test
    void shouldSupportNonTotalAggregationsForCoverageMetrics() {
        assertThat(MetricAggregation.MAXIMUM.isSupported(Metric.LINE)).isTrue();
        assertThat(MetricAggregation.MINIMUM.isSupported(Metric.BRANCH)).isTrue();
        assertThat(MetricAggregation.AVERAGE.isSupported(Metric.INSTRUCTION)).isTrue();
    }

    @Test
    void shouldReturnDefaultAggregation() {
        assertThat(MetricAggregation.getDefault())
                .isEqualTo(MetricAggregation.TOTAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TOTAL", "total", "Total"})
    void shouldParseTotal(final String value) {
        assertThat(MetricAggregation.fromString(value)).isEqualTo(MetricAggregation.TOTAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MAXIMUM", "maximum", "Maximum"})
    void shouldParseMaximum(final String value) {
        assertThat(MetricAggregation.fromString(value)).isEqualTo(MetricAggregation.MAXIMUM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MINIMUM", "minimum", "Minimum"})
    void shouldParseMinimum(final String value) {
        assertThat(MetricAggregation.fromString(value)).isEqualTo(MetricAggregation.MINIMUM);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AVERAGE", "average", "Average"})
    void shouldParseAverage(final String value) {
        assertThat(MetricAggregation.fromString(value)).isEqualTo(MetricAggregation.AVERAGE);
    }

    @Test
    void shouldParseFromId() {
        assertThat(MetricAggregation.fromString("total")).isEqualTo(MetricAggregation.TOTAL);
        assertThat(MetricAggregation.fromString("maximum")).isEqualTo(MetricAggregation.MAXIMUM);
        assertThat(MetricAggregation.fromString("minimum")).isEqualTo(MetricAggregation.MINIMUM);
        assertThat(MetricAggregation.fromString("average")).isEqualTo(MetricAggregation.AVERAGE);
    }

    @Test
    void shouldThrowExceptionForInvalidAggregation() {
        assertThatThrownBy(() -> MetricAggregation.fromString("INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid aggregation type");
    }

    @EnumSource(MetricAggregation.class)
    @ParameterizedTest(name = "{0} should be converted to a tag name and then back to an aggregation")
    void shouldConvertToTags(final MetricAggregation aggregation) {
        assertThat(MetricAggregation.fromString(aggregation.toTagName())).isEqualTo(aggregation);
    }

    @Test
    void shouldHaveCorrectToString() {
        assertThat(MetricAggregation.TOTAL).hasToString("Total");
        assertThat(MetricAggregation.MAXIMUM).hasToString("Maximum");
        assertThat(MetricAggregation.MINIMUM).hasToString("Minimum");
        assertThat(MetricAggregation.AVERAGE).hasToString("Average");
    }
}
