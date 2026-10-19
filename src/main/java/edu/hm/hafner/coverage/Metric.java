package edu.hm.hafner.coverage;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

/**
 * A metric to identify the type of the results. The enum order will be used to sort the values for display purposes.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"ImmutableEnumChecker", "PMD.ExcessivePublicCount"})
public enum Metric {
    /**
     * Coverage nodes that can have children. These nodes compute their coverage values on the fly based on their
     * children's coverage.
     */
    CONTAINER("Container Coverage", "Container", Computation.CONTAINER),
    MODULE("Module Coverage", "Module", Computation.CONTAINER),
    PACKAGE("Package Coverage", "Package", Computation.CONTAINER),
    FILE("File Coverage", "File", Computation.CONTAINER),
    CLASS("Class Coverage", "Class", Computation.CONTAINER),
    METHOD("Method Coverage", "Method", Computation.CONTAINER),

    /** Coverage values that are leaves in the tree. */
    LINE("Line Coverage", "Line", Computation.SUM),
    BRANCH("Branch Coverage", "Branch", Computation.SUM),
    INSTRUCTION("Instruction Coverage", "Instruction", Computation.SUM),
    MCDC_PAIR("Modified Condition and Decision Coverage", "MC/DC Pair", Computation.SUM),
    FUNCTION_CALL("Function Call Coverage", "Function Call", Computation.SUM),

    /** Additional metrics for TRACE32 coverage. */
    STMT_DC("Statement + Decision Coverage", "Statement + DC", Computation.SUM),
    STMT_CC("Statement + Condition Coverage", "Statement + CC", Computation.SUM),
    CONDITION("Condition Coverage", "Condition", Computation.SUM),
    BYTES("Bytes Coverage", "Bytes", Computation.SUM),

    /** Additional coverage values obtained from mutation testing. */
    MUTATION("Mutation Coverage", "Mutation", Computation.SUM),
    TEST_STRENGTH("Test Strength", "Test Strength", Computation.SUM),

    TESTS(
            "Number of Tests",
            "Tests",
            Computation.SUM,
            MetricTendency.LARGER_IS_BETTER,
            MetricValueType.CLASS_METRIC,
            new IntegerFormatter()),
    TEST_SUCCESS_RATE(
            "Test Success Rate",
            "Test Success %",
            Computation.SUM,
            MetricTendency.LARGER_IS_BETTER,
            MetricValueType.METRIC,
            new RateFormatter()),

    /** Metrics from the PMD metrics reporter. */
    LOC(
            "Lines of Code",
            "LOC",
            Computation.LINES_OF_CODE,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    NCSS(
            "Non Commenting Source Statements",
            "NCSS",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    CYCLOMATIC_COMPLEXITY(
            "Cyclomatic Complexity",
            "Complexity",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METHOD_METRIC,
            new IntegerFormatter()),
    COGNITIVE_COMPLEXITY(
            "Cognitive Complexity",
            "Cognitive Complexity",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METHOD_METRIC,
            new IntegerFormatter()),
    NPATH_COMPLEXITY(
            "N-Path Complexity",
            "N-Path",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METHOD_METRIC,
            new IntegerFormatter()),
    ACCESS_TO_FOREIGN_DATA(
            "Access to Foreign Data",
            "Foreign Data",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    COHESION(
            "Class Cohesion",
            "Cohesion",
            Computation.MEAN,
            MetricTendency.LARGER_IS_BETTER,
            MetricValueType.CLASS_METRIC,
            new PercentageFormatter()),
    FAN_OUT(
            "Fan Out",
            "Fan Out",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    NUMBER_OF_ACCESSORS(
            "Number of Accessors",
            "Accessors",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.CLASS_METRIC,
            new IntegerFormatter()),
    WEIGHT_OF_CLASS(
            "Weight of Class",
            "Weigth",
            Computation.MEAN,
            MetricTendency.LARGER_IS_BETTER,
            MetricValueType.CLASS_METRIC,
            new PercentageFormatter()),
    WEIGHED_METHOD_COUNT(
            "Weighted Method Count",
            "Methods",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.CLASS_METRIC,
            new IntegerFormatter()),

    /** Metrics from the static analysis tools. */
    WARNINGS(
            "Number of Warnings",
            "Warnings",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    BUGS(
            "Number of Bugs",
            "Bugs",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    ERRORS(
            "Number of Errors",
            "Errors",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    DUPLICATIONS(
            "Number of Duplications",
            "Duplications",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    VULNERABILITIES(
            "Number of Vulnerabilities",
            "Vulnerabilities",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),

    /** Metrics from git forensics. */
    // TODO: should we also expose dates like age of class or date of last commit?
    AUTHORS(
            "Different Authors",
            "Authors",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    COMMITS(
            "Number of Commits",
            "Commits",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter()),
    CODE_CHURN(
            "Code Churn",
            "Code Churn",
            Computation.SUM,
            MetricTendency.SMALLER_IS_BETTER,
            MetricValueType.METRIC,
            new IntegerFormatter());

    /**
     * Returns the metric that belongs to the specified tag.
     *
     * @param tag the tag
     * @return the metric
     * @see #toTagName()
     */
    public static Metric fromTag(final String tag) {
        return valueOf(tag.toUpperCase(Locale.ENGLISH).replace("-", "_"));
    }

    /**
     * Returns the metric that belongs to the specified name. The name matching is done in a case-insensitive way.
     * Additionally, all dashes and underscores are removed. This method also handles legacy metric names like
     * COMPLEXITY_MAXIMUM, COMPLEXITY_MINIMUM, and COMPLEXITY_AVERAGE by mapping them to CYCLOMATIC_COMPLEXITY.
     *
     * @param name the name
     * @return the metric
     * @throws IllegalArgumentException if the name is blank or no metric could be found for the specified name
     */
    public static Metric fromName(final String name) {
        var normalizedName = normalize(name);
        var normalizedFallback = normalize("CYCLOMATIC_" + name);
        for (Metric metric : values()) {
            if (normalizedName.equals(normalize(metric.name()))
                    || normalizedFallback.equals(normalize(metric.name()))) { // support old serialization format
                return metric;
            }
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("No metric defined");
        }
        throw new IllegalArgumentException("No metric found for name '" + name + "'");
    }

    private static String normalize(final String name) {
        return name.toUpperCase(Locale.ENGLISH).replaceAll("[-_]", "");
    }

    private final String displayName;
    private final String label;
    private final Computation computation;
    private final MetricTendency tendency;
    private final MetricValueType type;
    private final MetricFormatter formatter;

    Metric(final String displayName, final String label, final Computation computation) {
        this(displayName, label, computation, MetricTendency.LARGER_IS_BETTER);
    }

    Metric(final String displayName, final String label, final Computation computation, final MetricTendency tendency) {
        this(displayName, label, computation, tendency, MetricValueType.COVERAGE);
    }

    Metric(
            final String displayName,
            final String label,
            final Computation computation,
            final MetricTendency tendency,
            final MetricValueType type) {
        this(displayName, label, computation, tendency, type, new CoverageFormatter());
    }

    Metric(
            final String displayName,
            final String label,
            final Computation computation,
            final MetricTendency tendency,
            final MetricValueType type,
            final MetricFormatter formatter) {
        this.displayName = displayName;
        this.label = label;
        this.computation = computation;
        this.tendency = tendency;
        this.type = type;
        this.formatter = formatter;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLabel() {
        return label;
    }

    public MetricTendency getTendency() {
        return tendency;
    }

    /**
     * Returns if a given metric is a node metric.
     *
     * @return if the metric is a node metric
     */
    public boolean isContainer() {
        return computation == Computation.CONTAINER;
    }

    /**
     * Returns how the overall value of this metric is computed for a node that does not store a value of this metric,
     * e.g., the sum of the values of all children.
     *
     * @return the computation of the overall value
     */
    public Computation getComputation() {
        return computation;
    }

    /**
     * Returns the unit of the values of this metric. The unit defines how the values are scaled and displayed, e.g., as
     * a percentage or as a number of elements.
     *
     * @return the unit of the values
     */
    public MetricUnit getUnit() {
        return formatter.getUnit();
    }

    public MetricValueType getType() {
        return type;
    }

    /**
     * Returns if a given metric is a coverage metric.
     *
     * @return if the metric is a coverage metric
     */
    public boolean isCoverage() {
        return type == MetricValueType.COVERAGE;
    }

    /**
     * Returns the name of the metric as a tag, containing only lowercase characters and dashes.
     *
     * @return the metric tag name
     */
    public String toTagName() {
        return name().toLowerCase(Locale.ENGLISH).replaceAll("_", "-");
    }

    /**
     * Returns the overall value of this metric for the specified tree of nodes.
     *
     * @param node the root of the tree
     * @return the overall value
     * @see Node#getValue(Metric, MetricAggregation)
     */
    public Optional<Value> getValueFor(final Node node) {
        return node.getValue(this, MetricAggregation.OVERALL);
    }

    /**
     * Returns the target nodes that provide the values for this metric in the tree spanned by the specified node. These
     * nodes are used to compute the maximum, minimum, or mean of the values: method metrics are provided by the
     * methods, coverage values by the files (some parsers report the line coverage for files only), and all other
     * metrics by the classes.
     *
     * @param node the node to get the target nodes from
     * @return the target nodes
     */
    public List<? extends Node> getTargetNodes(final Node node) {
        return switch (getTargetNodeMetric()) {
            case METHOD -> node.getAllMethodNodes();
            case FILE -> node.getAllFileNodes();
            default -> node.getAllClassNodes();
        };
    }

    /**
     * Returns the metric of the type of the target nodes that provide the values for this metric, see
     * {@link #getTargetNodes(Node)}.
     *
     * @return the metric of the type of the target nodes ({@link #METHOD}, {@link #FILE}, or {@link #CLASS})
     */
    public Metric getTargetNodeMetric() {
        if (getType() == MetricValueType.METHOD_METRIC) {
            return METHOD;
        }
        if (isCoverage()) {
            return FILE;
        }
        return CLASS;
    }

    /**
     * Formats the specified value according to the metrics formatter.
     *
     * @param locale the locale to use
     * @param value the value to format
     * @return the formatted value
     */
    public String format(final Locale locale, final double value) {
        return formatter.format(locale, value);
    }

    /**
     * Formats the specified value according to the metrics formatter.
     *
     * @param locale the locale to use
     * @param value the value to format
     * @return the formatted value
     */
    public String formatDelta(final Locale locale, final double value) {
        return formatter.formatDelta(locale, value);
    }

    /**
     * Formats the specified mean value according to the metrics formatter.
     *
     * @param locale the locale to use
     * @param value the mean value to format
     * @return the formatted mean value
     */
    public String formatMean(final Locale locale, final double value) {
        return formatter.formatMean(locale, value);
    }

    /**
     * Rounds and formats the specified value according to the metrics formatter.
     *
     * @param locale the locale to use
     * @param value the value to format
     * @return the formatted mean value
     */
    public String formatRounded(final Locale locale, final double value) {
        return formatter.formatRounded(locale, value);
    }

    /**
     * Returns the aggregation that is used by default to compute the value of this metric for a tree of nodes, see
     * {@link Node#getValue(Metric)}. Currently, all metrics use the {@link MetricAggregation#OVERALL overall} value.
     *
     * @return the default aggregation of this metric
     */
    public MetricAggregation getDefaultAggregation() {
        return MetricAggregation.getDefault();
    }

    /**
     * Parses the specified {@link String} value as a {@link Fraction} and returns a corresponding value instance.
     *
     * @param value the value to parse as a fraction
     * @return the value instance representing the parsed fraction
     */
    public Value parseValue(final String value) {
        return new Value(this, Fraction.getFraction(value));
    }

    public static NavigableSet<Metric> getCoverageMetrics() {
        return Arrays.stream(values()).filter(Metric::isCoverage).collect(TreeSet::new, Set::add, Set::addAll);
    }

    /**
     * Metric tendency: some metrics are getting better when the value is getting larger, some other metrics are getting
     * better when the value is getting smaller.
     */
    public enum MetricTendency {
        /** Larger values will indicate better results. */
        LARGER_IS_BETTER,
        /** Smaller values will indicate better results. */
        SMALLER_IS_BETTER
    }

    /** The unit of the values of a metric: defines how the values are scaled and displayed. */
    public enum MetricUnit {
        /**
         * The coverage of elements that are covered or missed (e.g., lines or mutations). The values are represented by
         * {@link Coverage} instances and are displayed as a percentage in the interval [0, 100].
         */
        COVERAGE,
        /**
         * A fraction in the interval [0, 1] that is displayed as a percentage (e.g., the class cohesion). The parts of
         * the fraction are not available.
         */
        PERCENTAGE,
        /**
         * A rate that is displayed as a percentage (e.g., the test success rate). A rate is shown as 100% only if it is
         * perfect.
         */
        RATE,
        /** A number of elements (e.g., the lines of code or the cyclomatic complexity). */
        COUNT;

        /**
         * Returns whether the values of this unit are displayed as a percentage.
         *
         * @return {@code true} if the values are displayed as a percentage, {@code false} otherwise
         */
        public boolean isPercentage() {
            return this != COUNT;
        }
    }

    /** Metric type: some metrics are represented as coverages, some other metrics are represented as plain values. */
    public enum MetricValueType {
        /** Coverages are represented by values of the type {@link Coverage}. */
        COVERAGE,
        /** Software metrics for methods and classes are represented by values of the type {@link Value}. */
        METRIC,
        /** Software metrics for methods are represented by values of the type {@link Value}. */
        METHOD_METRIC,
        /** Software metrics for classes are represented by values of the type {@link Value}. */
        CLASS_METRIC
    }

    /** Defines how the overall value of a metric is computed for a node that does not store a value of this metric. */
    public enum Computation {
        /** The coverage of the nodes of the corresponding type, e.g., the number of covered files. */
        CONTAINER,
        /** The sum of the values of all children. Coverage values sum up their covered and missed parts. */
        SUM,
        /** The mean of the values of all target nodes, used for percentages without the underlying parts. */
        MEAN,
        /** The total number of lines of the line coverage, otherwise the sum of the values of all children. */
        LINES_OF_CODE
    }

    private abstract static class MetricFormatter implements Serializable {
        @Serial
        private static final long serialVersionUID = 7402798036375016965L;

        abstract MetricUnit getUnit();

        String format(final Locale locale, final double value) {
            return formatDouble(locale, value);
        }

        String formatMean(final Locale locale, final double value) {
            return formatDouble(locale, value);
        }

        String formatDelta(final Locale locale, final double value) {
            var rounded = toRounded(value, 2);
            if (rounded == 0) {
                return "±0";
            }
            return String.format(locale, "%+.2f", rounded);
        }

        String formatRounded(final Locale locale, final double value) {
            return formatDouble(locale, toRounded(value, 2));
        }

        final String formatDouble(final Locale locale, final double value) {
            return String.format(locale, "%.2f", value);
        }

        final double toRounded(final double value, final int scale) {
            return BigDecimal.valueOf(value)
                    .setScale(scale, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        String percentage(final String value) {
            return value + "%";
        }
    }

    private static class CoverageFormatter extends MetricFormatter {
        @Serial
        private static final long serialVersionUID = 4337117939462815181L;

        @Override
        MetricUnit getUnit() {
            return MetricUnit.COVERAGE;
        }

        @Override
        String format(final Locale locale, final double value) {
            return percentage(formatDouble(locale, value));
        }

        @Override
        String formatMean(final Locale locale, final double value) {
            return format(locale, value);
        }

        @Override
        String formatDelta(final Locale locale, final double value) {
            return percentage(super.formatDelta(locale, value));
        }
    }

    private static class PercentageFormatter extends MetricFormatter {
        @Serial
        private static final long serialVersionUID = -4995914265987128828L;

        @Override
        MetricUnit getUnit() {
            return MetricUnit.PERCENTAGE;
        }

        @Override
        String format(final Locale locale, final double value) {
            return percentage(formatDouble(locale, value * 100));
        }

        @Override
        String formatMean(final Locale locale, final double value) {
            return format(locale, value);
        }

        @Override
        String formatDelta(final Locale locale, final double value) {
            return percentage(super.formatDelta(locale, value * 100));
        }
    }

    /** A rate cannot show up as 100% unless it is exactly 1.0. */
    private static class RateFormatter extends PercentageFormatter {
        @Serial
        private static final long serialVersionUID = 6718758308735877295L;

        @Override
        MetricUnit getUnit() {
            return MetricUnit.RATE;
        }

        @Override
        String format(final Locale locale, final double value) {
            var percentage = super.format(locale, value);
            var perfect = super.format(locale, 1.0);
            if (percentage.equals(perfect) && value != 1.0) {
                return super.format(locale, 9_999.0 / 10_000.0);
            }
            return percentage;
        }
    }

    private static class IntegerFormatter extends MetricFormatter {
        @Serial
        private static final long serialVersionUID = 8053070560640902081L;

        @Override
        MetricUnit getUnit() {
            return MetricUnit.COUNT;
        }

        @Override
        String format(final Locale locale, final double value) {
            return String.format(locale, "%d", Math.round(toRounded(value, 0)));
        }

        @Override
        String formatDelta(final Locale locale, final double value) {
            var rounded = toRounded(value, 0);
            if (rounded == 0) {
                return "±0";
            }
            return String.format(locale, "%+d", Math.round(toRounded(value, 0)));
        }

        @Override
        String formatRounded(final Locale locale, final double value) {
            return format(locale, toRounded(value, 2));
        }
    }
}
