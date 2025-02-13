package edu.hm.hafner.coverage;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

/**
 * Represents a coverage percentage value which can be used in order to show and serialize coverage values. The class
 * can also be used for transforming a coverage fraction into its percentage representation. The percentage is
 * represented by a numerator and a denominator.
 *
 * @author Florian Orendi
 */
public final class Percentage implements Serializable {
    @Serial
    private static final long serialVersionUID = 3324942976687883481L;

    /** null value. */
    public static final Percentage ZERO = new Percentage(0, 1);
    private static final Percentage ALMOST_HUNDRED = new Percentage(9_999, 10_000);

    static final String TOTALS_ZERO_MESSAGE = "Totals must not greater than zero.";
    private static final int ALMOST_PERFECT_INTEGER = 99;
    private static final double ALMOST_PERFECT_DOUBLE = 99.99;

    /**
     * Creates an instance of {@link Percentage} in the range [0,100] from a {@link Fraction fraction} within the range
     * [0,1]. I.e., a percentage is a fraction value multiplied by one hundert.
     *
     * @param fraction
     *         the value as a fraction within the range [0,1]
     *
     * @return the created instance
     */
    public static Percentage valueOf(final Fraction fraction) {
        return new Percentage(fraction.getNumerator(), fraction.getDenominator());
    }

    /**
     * Creates an instance of {@link Percentage} from the two number items and total.
     * The percentage is calculated as value (items / total) * 100.
     *
     * @param items
     *         the number of items in the range [0,total]
     * @param total
     *         the total number of items available
     *
     * @return the created instance
     * @throws IllegalArgumentException
     *         if the denominator is zero or items are greater than total
     */
    public static Percentage valueOf(final int items, final int total) {
        return new Percentage(items, total);
    }

    /**
     * Creates a new {@link Percentage} instance from the provided string representation. The string representation is
     * expected to contain the numerator and the denominator - separated by a slash, e.g. "300/345", or "1/100".
     * Whitespace characters will be ignored.
     *
     * @param stringRepresentation
     *         string representation to convert from
     *
     * @return the created {@link Percentage}
     * @throws IllegalArgumentException
     *         if the string is not a valid Percentage instance
     */
    public static Percentage valueOf(final String stringRepresentation) {
        try {
            var cleanedFormat = StringUtils.deleteWhitespace(stringRepresentation);
            if (StringUtils.contains(cleanedFormat, "/")) {
                var extractedNumerator = StringUtils.substringBefore(cleanedFormat, "/");
                var extractedDenominator = StringUtils.substringAfter(cleanedFormat, "/");

                int numerator = Integer.parseInt(extractedNumerator);
                int denominator = Integer.parseInt(extractedDenominator);

                return new Percentage(numerator, denominator);
            }
        }
        catch (NumberFormatException exception) {
            // ignore and throw a specific exception
        }
        throw new IllegalArgumentException(
                "Cannot convert %s to a valid Percentage instance.".formatted(stringRepresentation));
    }

    private final int items;
    private final int total;

    /**
     * Creates an instance of {@link Percentage}.
     *
     * @param items
     *         the number of items in the range [0,total]
     * @param total
     *         the total number of items available
     */
    private Percentage(final int items, final int total) {
        if (total <= 0) {
            throw new IllegalArgumentException(TOTALS_ZERO_MESSAGE);
        }
        if (items > total) {
            throw new IllegalArgumentException(
                    String.format(Locale.ENGLISH, "The number of items %d must be less or equal the total number %d.",
                            items, total));
        }
        this.items = items;
        this.total = total;
    }

    /**
     * Returns this percentage as a double value in the interval [0, 100].
     *
     * @return the coverage percentage
     */
    public double toDouble() {
        return items * 100.0 / total;
    }

    /**
     * Returns this percentage as a double value in the interval [0, 100]. The returned value is rounded to 2 digits aft
     *
     * @return the coverage percentage
     */
    public double toRounded() {
        var value = BigDecimal.valueOf(toDouble());
        var rounded = value.setScale(2, RoundingMode.HALF_UP).doubleValue();
        if (rounded == 100.0 && isNotPerfect()) {
            return ALMOST_PERFECT_DOUBLE;
        }
        return rounded;
    }

    /**
     * Returns this percentage as an int value in the interval [0, 100].
     *
     * @return the coverage percentage
     */
    public int toInt() {
        var value = Math.round(items * 100.0f / total);
        if (value == 100 && isNotPerfect()) {
            return ALMOST_PERFECT_INTEGER;
        }
        return value;
    }

    /**
     * Formats a percentage to plain text and rounds the value to two decimals. By default, the English locale is used.
     *
     * @return the formatted percentage as plain text
     */
    public String formatPercentage() {
        return formatPercentage(Locale.ENGLISH);
    }

    /**
     * Formats a percentage to plain text and rounds the value to two decimals.
     *
     * @param locale
     *         the used locale
     *
     * @return the formatted percentage as plain text
     */
    public String formatPercentage(final Locale locale) {
        var formatted = String.format(locale, "%.2f%%", toDouble());
        if (formatted.startsWith("100") && isNotPerfect()) {
            return ALMOST_HUNDRED.formatPercentage(locale);
        }
        return formatted;
    }

    private boolean isNotPerfect() {
        return items != total;
    }

    /**
     * Subtracts the other percentage from this percentage, returning the result as a {@link Fraction}.
     *
     * @param subtrahend
     *         the percentage to subtract
     *
     * @return a {@code Fraction} instance with the resulting values
     */
    public Fraction subtract(final Percentage subtrahend) {
        return new SafeFraction(items, total).subtract(subtrahend.getItems(), subtrahend.getTotal());
    }

    /**
     * Formats a delta percentage to its plain text representation with a leading sign and rounds the value to two
     * decimals.
     *
     * @param locale
     *         the used locale
     *
     * @return the formatted delta percentage as plain text with a leading sign
     */
    public String formatDeltaPercentage(final Locale locale) {
        return String.format(locale, "%+.2f%%", toDouble());
    }

    public int getItems() {
        return items;
    }

    public int getTotal() {
        return total;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (Percentage) o;
        return items == that.items && total == that.total;
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, total);
    }

    /**
     * Returns a string representation for this {@link Percentage} that can be used to serialize this instance in a
     * simple but still readable way. The serialization contains the numerator and the denominator - separated by a
     * slash, e.g. "100/345", or "0/1".
     *
     * @return a string representation for this {@link Percentage}
     */
    public String serializeToString() {
        return String.format(Locale.ENGLISH, "%d/%d", getItems(), getTotal());
    }

    @Override
    public String toString() {
        return formatPercentage(Locale.ENGLISH);
    }
}
