package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.*;
import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests the class {@link MetricAggregation}.
 *
 * @author Akash Manna
 */
class MetricAggregationTest {
    @Test
    void shouldHaveCorrectDisplayNameIdAndToString() {
        assertThat(MetricAggregation.OVERALL)
                .hasDisplayName("Overall")
                .hasId("overall")
                .hasToString("Overall");
        assertThat(MetricAggregation.MAXIMUM)
                .hasDisplayName("Maximum")
                .hasId("maximum")
                .hasToString("Maximum");
        assertThat(MetricAggregation.MINIMUM)
                .hasDisplayName("Minimum")
                .hasId("minimum")
                .hasToString("Minimum");
    }

    @Test
    void shouldReturnDefaultAggregation() {
        assertThat(MetricAggregation.getDefault()).isEqualTo(MetricAggregation.OVERALL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OVERALL", "overall", "Overall"})
    void shouldParseOverall(final String value) {
        assertThat(MetricAggregation.fromString(value)).isEqualTo(MetricAggregation.OVERALL);
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

    @Test
    void shouldParseFromId() {
        assertThat(MetricAggregation.fromString("overall")).isEqualTo(MetricAggregation.OVERALL);
        assertThat(MetricAggregation.fromString("maximum")).isEqualTo(MetricAggregation.MAXIMUM);
        assertThat(MetricAggregation.fromString("minimum")).isEqualTo(MetricAggregation.MINIMUM);
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
