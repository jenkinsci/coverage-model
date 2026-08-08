package edu.hm.hafner.coverage;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Defines how metric values should be aggregated when computing statistics across multiple nodes in the coverage tree.
 * For example, when computing the cyclomatic complexity of a class, we can aggregate the complexity values of all methods
 * by using the total (sum), maximum, minimum, or average.
 *
 * @author Akash Manna
 */
public enum MetricAggregation {
    /** Aggregates values by summing them (default for most metrics). */
    TOTAL("Total", "total"),
    
    /** Aggregates values by finding the maximum. */
    MAXIMUM("Maximum", "maximum"),
    
    /** Aggregates values by finding the minimum. */
    MINIMUM("Minimum", "minimum"),
    
    /** Aggregates values by computing the average. */
    AVERAGE("Average", "average");

    private final String displayName;
    private final String id;

    /**
     * Creates a new {@link MetricAggregation} instance.
     *
     * @param displayName
     *         the human-readable display name
     * @param id
     *         the ID of the aggregation
     */
    MetricAggregation(final String displayName, final String id) {
        this.displayName = displayName;
        this.id = id;
    }

    /**
     * Returns the human-readable display name of this aggregation.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the ID of this aggregation.
     *
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the default aggregation type for all metrics.
     *
     * @return the default aggregation type (always TOTAL)
     */
    public static MetricAggregation getDefault() {
        return TOTAL;
    }

    /**
     * Aggregates the specified values using this aggregation strategy.
     *
     * @param values
     *         the values to aggregate
     *
     * @return the aggregated value or an empty result if no values are available
     */
    public Optional<Value> aggregate(final List<Value> values) {
        if (values.isEmpty()) {
            return Optional.empty();
        }

        return switch (this) {
            case TOTAL -> values.stream().reduce(Value::add);
            case MAXIMUM -> values.stream().reduce(Value::max);
            case MINIMUM -> values.stream().reduce(Value::min);
            case AVERAGE -> computeAverage(values);
        };
    }

    /**
     * Converts a string to a {@link MetricAggregation} instance.
     *
     * @param value
     *         the string value
     *
     * @return the corresponding {@link MetricAggregation} instance
     * @throws IllegalArgumentException
     *         if the value is not a valid aggregation type
     */
    public static MetricAggregation fromString(final String value) {
        String normalizedValue = StringUtils.lowerCase(value, Locale.ENGLISH);
        for (MetricAggregation aggregation : values()) {
            if (StringUtils.lowerCase(aggregation.name(), Locale.ENGLISH).equals(normalizedValue)
                    || StringUtils.lowerCase(aggregation.getId(), Locale.ENGLISH).equals(normalizedValue)) {
                return aggregation;
            }
        }
        throw new IllegalArgumentException("Invalid aggregation type: " + value);
    }

    /**
     * Returns the name of the aggregation as a tag, containing only lowercase characters and dashes.
     *
     * @return the aggregation tag name
     */
    public String toTagName() {
        return name().toLowerCase(Locale.ENGLISH).replaceAll("_", "-");
    }

    @Override
    public String toString() {
        return displayName;
    }

    private static Optional<Value> computeAverage(final List<Value> values) {
        var sum = values.stream()
                .reduce(Value::add)
                .orElseThrow(() -> new IllegalStateException("Cannot compute average of empty list"));

        return Optional.of(sum.divide(values.size()));
    }
}
