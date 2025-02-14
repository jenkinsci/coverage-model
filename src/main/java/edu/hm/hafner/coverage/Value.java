package edu.hm.hafner.coverage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.Metric.MetricTendency;
import edu.hm.hafner.util.Generated;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

/**
 * A leaf in the tree that contains a numeric value. Such values are used for arbitrary software-metric like
 * loc or complexity. The value is stored as a fraction to allow exact calculations.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass") // this is a data class
public class Value implements Serializable, Comparable<Value> {
    @Serial
    private static final long serialVersionUID = -1062406664372222691L;

    private static final String METRIC_SEPARATOR = ":";

    /**
     * Searches for a value with the specified metric in the specified collection of values.
     *
     * @param metric
     *         the metric to search for
     * @param values
     *         the values to search in
     *
     * @return the value with the specified metric
     * @throws NoSuchElementException
     *         if the value is not found
     * @see #findValue(Metric, Collection)
     */
    public static Value getValue(final Metric metric, final Collection<? extends Value> values) {
        return findValue(metric, values)
                .orElseThrow(() -> new NoSuchElementException("No value for metric " + metric + " in " + values));
    }

    /**
     * Searches for a value with the specified metric in the specified list of values.
     *
     * @param metric
     *         the metric to search for
     * @param values
     *         the values to search in
     *
     * @return the value with the specified metric, or an empty optional if the value is not found
     * @see #getValue(Metric, Collection)
     */
    public static Optional<Value> findValue(final Metric metric, final Collection<? extends Value> values) {
        return values.stream()
                .filter(v -> metric.equals(v.getMetric()))
                .findAny()
                .map(Value.class::cast);
    }

    /**
     * Creates a new {@link Value} instance from the provided string representation. The string representation is
     * expected to start with the metric, written in all caps characters and followed by a colon. Then the {@link Value}
     * specific serialization is following. Whitespace characters will be ignored.
     *
     * <p>Examples: LINE: 10/100, BRANCH: 0/5, COMPLEXITY: 160</p>
     *
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created value
     * @throws IllegalArgumentException
     *         if the string is not a valid cov instance
     */
    @SuppressWarnings("PMD.CyclomaticComplexity") // this is a factory method that selects the correct metric
    public static Value valueOf(final String stringRepresentation) {
        var errorMessage = "Cannot convert '%s' to a valid Value instance.".formatted(stringRepresentation);
        try {
            var cleanedFormat = StringUtils.deleteWhitespace(stringRepresentation);
            if (StringUtils.contains(cleanedFormat, METRIC_SEPARATOR)) {
                var metric = Metric.fromName(StringUtils.substringBefore(cleanedFormat, METRIC_SEPARATOR));
                var value = StringUtils.substringAfter(cleanedFormat, METRIC_SEPARATOR);
                if (value.contains("/")) {
                    return Coverage.valueOf(metric, value);
                }
                if (value.startsWith(Difference.DELTA)) {
                    return new Difference(metric, readFraction(value, 1));
                }
                return new Value(metric, readFraction(value, 0));
            }
        }
        catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage, exception);
        }
        throw new IllegalArgumentException(errorMessage);
    }

    private static Fraction readFraction(final String value, final int beginIndex) {
        return Fraction.getFraction(value.substring(beginIndex).replace(':', '/'));
    }

    /**
     * Returns a {@code null} object that indicates that no value has been recorded.
     *
     * @param metric
     *         the coverage metric
     *
     * @return the {@code null} object
     */
    public static Value nullObject(final Metric metric) {
        return new Value(metric, 0);
    }

    private final Metric metric;
    private final Fraction fraction;

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value to store
     */
    public Value(final Metric metric, final Fraction value) {
        this.metric = metric;
        this.fraction = value;
    }

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value to store
     */
    public Value(final Metric metric, final double value) {
        this(metric, Fraction.getFraction(value));
    }

    /**
     * Creates a new leaf with the given value (a fraction) for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param numerator
     *         the numerator, i.e., the three in 'three sevenths'
     * @param denominator
     *         the denominator, i.ee, the seven in 'three sevenths'
     */
    public Value(final Metric metric, final int numerator, final int denominator) {
        this(metric, Fraction.getFraction(numerator, denominator));
    }

    /**
     * Creates a new leaf with the given value for the specified metric.
     *
     * @param metric
     *         the coverage metric
     * @param value
     *         the value
     */
    public Value(final Metric metric, final int value) {
        this(metric, Fraction.getFraction(value, 1));
    }

    public final Metric getMetric() {
        return metric;
    }

    public Fraction getFraction() {
        return fraction;
    }

    protected void ensureSameMetricAndType(final Value other) {
        if (!hasSameMetric(other)) {
            throw new IllegalArgumentException(
                    "Cannot calculate with different metrics: %s and %s".formatted(this, other));
        }
        if (!other.getClass().equals(getClass())) {
            throw new IllegalArgumentException(
                    "Cannot calculate with different types: %s and %s".formatted(this, other));
        }
    }

    /**
     * Add the value from the specified instance to the value of this instance.
     *
     * @param other
     *         the additional coverage details
     *
     * @return the sum of this and the additional coverage
     * @throws IllegalArgumentException
     *         if the metrics of the two instances are different
     */
    @CheckReturnValue
    public Value add(final Value other) {
        ensureSameMetricAndType(other);

        return new Value(getMetric(), asSafeFraction().add(other.fraction));
    }

    /**
     * Computes the delta of this value with the specified value.
     *
     * @param other
     *         the value to compare with
     *
     * @return the delta of this and the additional value
     * @throws IllegalArgumentException
     *         if the metrics of the two instances are different
     */
    @CheckReturnValue
    public Difference subtract(final Value other) {
        ensureSameMetricAndType(other);

        return new Difference(getMetric(), asSafeFraction().subtract(other.fraction));
    }

    /**
     * Computes the maximum of this value and the specified value.
     *
     * @param other
     *         the other coverage
     *
     * @return the maximum value
     */
    @CheckReturnValue
    public Value max(final Value other) {
        ensureSameMetricAndType(other);

        if (fraction.doubleValue() < other.fraction.doubleValue()) {
            return other;
        }
        return this;
    }

    /**
     * Returns whether this value if within the specified threshold (given as double value). For metrics of type
     * {@link MetricTendency#LARGER_IS_BETTER} (like coverage percentage) this value will be checked with greater or
     * equal than the threshold. For metrics of type {@link MetricTendency#SMALLER_IS_BETTER} (like complexity) this
     * value will be checked with less or equal than.
     *
     * @param threshold
     *         the threshold to check against
     *
     * @return {@code true} if this value is within the specified threshold, {@code false} otherwise
     */
    public boolean isOutOfValidRange(final double threshold) {
        if (getMetric().getTendency() == MetricTendency.LARGER_IS_BETTER) {
            return fraction.doubleValue() < threshold;
        }
        return fraction.doubleValue() > threshold;
    }

    private SafeFraction asSafeFraction() {
        return new SafeFraction(fraction);
    }

    /**
     * Serializes this instance into a String.
     *
     * @return serialization of this value as a String
     */
    public final String serialize() {
        return String.format(Locale.ENGLISH, "%s: %s", getMetric(), serializeValue());
    }

    /**
     * Serializes the value of this instance into a String (without the metric).
     *
     * @return the value of this instance as a String
     */
    protected String serializeValue() {
        if (fraction.getDenominator() == 1) {
            return String.valueOf(fraction.getNumerator());
        }
        return String.format(Locale.ENGLISH, "%d:%d", fraction.getNumerator(), fraction.getDenominator());
    }

    /**
     * Returns this value as a text.
     *
     * @param locale
     *         the locale to use
     *
     * @return this value formatted as a String
     */
    public String asText(final Locale locale) {
        return getMetric().format(locale, asDouble());
    }

    /**
     * Returns this value as an informative text.
     *
     * @param locale
     *         the locale to use
     *
     * @return this value formatted as a String
     */
    public String asInformativeText(final Locale locale) {
        return getMetric().format(locale, asDouble());
    }

    /**
     * Returns a short summary of this value as a human-readable text.
     *
     * @param locale the locale to use
     * @return the summary of this value as a human-readable text
     */
    public String getSummary(final Locale locale) {
        return String.format(locale, "%s: %s", getMetric().getDisplayName(), asText(locale));
    }

    /**
     * Returns the details of this value as a human-readable text.
     *
     * @param locale the locale to use
     * @return the details of this value as a human-readable text
     */
    public String getDetails(final Locale locale) {
        return String.format(locale, "%s: %s", getMetric().getDisplayName(), asInformativeText(locale));
    }

    /**
     * Returns this value as an integer.
     *
     * @return this value as an integer
     */
    public int asInteger() {
        return (int) round(fraction.doubleValue(), 0);
    }

    /**
     * Returns this value as a double.
     *
     * @return this value as a double
     */
    public double asDouble() {
        return fraction.doubleValue();
    }

    /**
     * Returns this value as rounded double.
     *
     * @return this value as a double
     */
    public double asRounded() {
        return round(fraction.doubleValue(), 2);
    }

    private double round(final double value, final int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Returns whether this value has the same metric as the specified value.
     *
     * @param other
     *         the other value to compare with
     *
     * @return {@code true} if this value has the same metric as the specified value, {@code false} otherwise
     */
    protected boolean hasSameMetric(final Value other) {
        return other.getMetric().equals(getMetric());
    }

    @Override
    public String toString() {
        return serialize();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var value = (Value) o;
        return metric == value.metric
                && Objects.equals(fraction, value.fraction);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(metric, fraction);
    }

    @Override
    public int compareTo(final Value o) {
        var metricComparison = getMetric().compareTo(o.getMetric());
        if (metricComparison != 0) {
            return metricComparison;
        }
        return getFraction().compareTo(o.getFraction());
    }
}
