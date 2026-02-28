package edu.hm.hafner.coverage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;

/**
 * Defines how metric values should be aggregated when computing statistics across multiple nodes in the coverage tree.
 * For example, when computing the cyclomatic complexity of a class, we can aggregate the complexity values of all methods
 * by using the total (sum), maximum, minimum, or average.
 *
 * @author Akash Manna
 */
public enum MetricAggregation implements Serializable {
    /** Aggregates values by summing them (default for most metrics). */
    TOTAL("Total", "total"),
    
    /** Aggregates values by finding the maximum. */
    MAXIMUM("Maximum", "maximum"),
    
    /** Aggregates values by finding the minimum. */
    MINIMUM("Minimum", "minimum"),
    
    /** Aggregates values by computing the average. */
    AVERAGE("Average", "average");

    @Serial
    private static final long serialVersionUID = 1L;

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
     * Returns whether this aggregation is supported for the specified metric.
     * Aggregation types other than TOTAL are only supported for method-level and class-level metrics.
     *
     * @param metric
     *         the metric to check
     *
     * @return {@code true} if this aggregation is supported for the metric, {@code false} otherwise
     */
    public boolean isSupported(final Metric metric) {
        if (this == TOTAL) {
            return true; 
        }
        
        return metric.getType() == Metric.MetricValueType.METHOD_METRIC
                || metric.getType() == Metric.MetricValueType.CLASS_METRIC
                || metric.getType() == Metric.MetricValueType.METRIC;
    }

    /**
     * Returns the default aggregation type for the specified metric.
     *
     * @param metric
     *         the metric
     *
     * @return the default aggregation type
     */
    public static MetricAggregation getDefault(final Metric metric) {
        return TOTAL;
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
        for (MetricAggregation aggregation : values()) {
            if (aggregation.name().equalsIgnoreCase(value) || aggregation.id.equalsIgnoreCase(value)) {
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
}
