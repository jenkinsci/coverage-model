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
    void shouldHaveCorrectDisplayNameIdAndToString() {
        assertThat(MetricAggregation.TOTAL).hasDisplayName("Total").hasId("total").hasToString("Total");
        assertThat(MetricAggregation.MAXIMUM).hasDisplayName("Maximum").hasId("maximum").hasToString("Maximum");
        assertThat(MetricAggregation.MINIMUM).hasDisplayName("Minimum").hasId("minimum").hasToString("Minimum");
        assertThat(MetricAggregation.AVERAGE).hasDisplayName("Average").hasId("average").hasToString("Average");
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
}
