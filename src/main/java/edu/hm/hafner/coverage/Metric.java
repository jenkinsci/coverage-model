package edu.hm.hafner.coverage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

/**
 * A metric to identify the type of the results. The enum order will be used to sort the values for display purposes.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"ImmutableEnumChecker", "PMD.ExcessivePublicCount"})
public enum Metric {
    /**
     * Nodes that can have children. These notes compute their coverage values on the fly based on their children's
     * coverage.
     */
    // TODO: why do we need these coverages?
    CONTAINER("Container Coverage", "Container", new CoverageOfChildrenEvaluator()),
    MODULE("Module Coverage", "Module", new CoverageOfChildrenEvaluator()),
    PACKAGE("Package Coverage", "Package", new CoverageOfChildrenEvaluator()),
    FILE("File Coverage", "File", new CoverageOfChildrenEvaluator()),
    CLASS("Class Coverage", "Class", new CoverageOfChildrenEvaluator()),
    METHOD("Method Coverage", "Method", new CoverageOfChildrenEvaluator()),

    /** Coverage values that are leaves in the tree. */
    LINE("Line Coverage", "Line", new ValuesAggregator()),
    BRANCH("Branch Coverage", "Branch", new ValuesAggregator()),
    INSTRUCTION("Instruction Coverage", "Instruction", new ValuesAggregator()),
    MCDC_PAIR("Modified Condition and Decision Coverage", "MC/DC Pair", new ValuesAggregator()),
    FUNCTION_CALL("Function Call Coverage", "Function Call", new ValuesAggregator()),

    /** Additional coverage values obtained from mutation testing. */
    MUTATION("Mutation Coverage", "Mutation", new ValuesAggregator()),
    TEST_STRENGTH("Test Strength", "Test Strength", new ValuesAggregator()),

    // TODO: metrics might be better placed into a class that can have new instances dynamically
    TESTS("Number of Tests", "Tests", new ValuesAggregator(),
            MetricTendency.LARGER_IS_BETTER, MetricValueType.CLASS_METRIC, new IntegerFormatter()),

    /** Metrics from the PMD metrics reporter. */
    LOC("Lines of Code", "LOC", new LocEvaluator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    NCSS("Non Commenting Source Statements", "NCSS", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    CYCLOMATIC_COMPLEXITY("Cyclomatic Complexity", "Complexity", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METHOD_METRIC, new IntegerFormatter()),
    COGNITIVE_COMPLEXITY("Cognitive Complexity", "Cognitive Complexity", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METHOD_METRIC, new IntegerFormatter()),
    NPATH_COMPLEXITY("N-Path Complexity", "N-Path", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METHOD_METRIC, new IntegerFormatter()),
    ACCESS_TO_FOREIGN_DATA("Access to Foreign Data", "Foreign Data", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    COHESION("Class Cohesion", "Cohesion", new ValuesAggregator(Value::max, "maximum"),
            MetricTendency.LARGER_IS_BETTER, MetricValueType.CLASS_METRIC, new PercentageFormatter()),
    FAN_OUT("Fan Out", "Fan Out", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    NUMBER_OF_ACCESSORS("Number of Accessors", "Accessors", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.CLASS_METRIC, new IntegerFormatter()),
    WEIGHT_OF_CLASS("Weight of Class", "Weigth", new ValuesAggregator(Value::max, "maximum"),
            MetricTendency.LARGER_IS_BETTER, MetricValueType.CLASS_METRIC, new PercentageFormatter()),
    WEIGHED_METHOD_COUNT("Weighted Method Count", "Methods", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.CLASS_METRIC, new IntegerFormatter()),

    /** Metrics from the static analysis tools. */
    WARNINGS("Number of Warnings", "Warnings", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    BUGS("Number of Bugs", "Bugs", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    ERRORS("Number of Errors", "Errors", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    DUPLICATIONS("Number of Duplications", "Duplications", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),

    /** Metrics from git forensics. */
    // TODO: should we also expose dates like age of class or date of last commit?
    AUTHORS("Different Authors", "Authors", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    COMMITS("Number of Commits", "Commits", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter()),
    CODE_CHURN("Code Churn", "Code Churn", new ValuesAggregator(),
            MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC, new IntegerFormatter());
    /**
     * Returns the metric that belongs to the specified tag.
     *
     * @param tag
     *         the tag
     *
     * @return the metric
     * @see #toTagName()
     */
    public static Metric fromTag(final String tag) {
        return valueOf(tag.toUpperCase(Locale.ENGLISH).replaceAll("-", "_"));
    }

    /**
     * Returns the metric that belongs to the specified name. The name matching is done in a case-insensitive way.
     * Additionally, all dashes and underscores are removed.
     *
     * @param name
     *         the name
     *
     * @return the metric
     * @throws IllegalArgumentException
     *         if the name is blank or no metric could be found for the specified name
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
    private final MetricEvaluator evaluator;
    private final MetricTendency tendency;
    private final MetricValueType type;
    private final MetricFormatter formatter;

    Metric(final String displayName, final String label, final MetricEvaluator evaluator) {
        this(displayName, label, evaluator, MetricTendency.LARGER_IS_BETTER);
    }

    Metric(final String displayName, final String label, final MetricEvaluator evaluator,
            final MetricTendency tendency) {
        this(displayName, label, evaluator, tendency, MetricValueType.COVERAGE);
    }

    Metric(final String displayName, final String label, final MetricEvaluator evaluator, final MetricTendency tendency,
            final MetricValueType type) {
        this(displayName, label, evaluator, tendency, type, new CoverageFormatter());
    }

    Metric(final String displayName, final String label, final MetricEvaluator evaluator, final MetricTendency tendency,
            final MetricValueType type,
            final MetricFormatter formatter) {
        this.displayName = displayName;
        this.label = label;
        this.evaluator = evaluator;
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
        return evaluator.isAggregatingChildren();
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
     * Returns the aggregated value of this metric for the specified tree of nodes.
     *
     * @param node
     *         the root of the tree
     *
     * @return the aggregated value
     */
    public Optional<Value> getValueFor(final Node node) {
        return evaluator.compute(node, this);
    }

    /**
     * Returns the target nodes that store the values for this metric in the tree spanned by the specified node.
     *
     * @param node
     *         the node to get the target nodes from
     *
     * @return the target nodes
     */
    public List<? extends Node> getTargetNodes(final Node node) {
        if (getType() == MetricValueType.CLASS_METRIC) {
            return node.getAllClassNodes();
        }
        return node.getAllMethodNodes();
    }

    /**
     * Formats the specified value according to the metrics formatter.
     *
     * @param locale
     *         the locale to use
     * @param value
     *         the value to format
     *
     * @return the formatted value
     */
    public String format(final Locale locale, final double value) {
        return formatter.format(locale, value);
    }

    /**
     * Formats the specified value according to the metrics formatter.
     *
     * @param locale
     *         the locale to use
     * @param value
     *         the value to format
     *
     * @return the formatted value
     */
    public String formatDelta(final Locale locale, final double value) {
        return formatter.formatDelta(locale, value);
    }

    /**
     * Formats the specified mean value according to the metrics formatter.
     *
     * @param locale
     *         the locale to use
     * @param value
     *         the mean value to format
     *
     * @return the formatted mean value
     */
    public String formatMean(final Locale locale, final double value) {
        return formatter.formatMean(locale, value);
    }

    public String getAggregationType() {
        return evaluator.getAggregationType();
    }

    /**
     * Parses the specified {@link String} value as a {@link Fraction} and returns a corresponding value instance.
     *
     * @param value
     *         the value to parse as a fraction
     *
     * @return the value instance representing the parsed fraction
     */
    public Value parseValue(final String value) {
        return new Value(this, Fraction.getFraction(value));
    }

    public static NavigableSet<Metric> getCoverageMetrics() {
        return Arrays.stream(values())
                .filter(Metric::isCoverage)
                .collect(TreeSet::new, Set::add, Set::addAll);
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

    /**
     * Metric type: some metrics are represented as coverages, some other metrics are represented as plain values.
     */
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

    private abstract static class MetricEvaluator implements Serializable {
        @Serial
        private static final long serialVersionUID = -537814226149186300L;

        final Optional<Value> compute(final Node node, final Metric searchMetric) {
            return getValue(node, searchMetric).or(() -> computeDerivedValue(node, searchMetric));
        }

        abstract Optional<Value> computeDerivedValue(Node node, Metric searchMetric);

        abstract boolean isAggregatingChildren();

        String getAggregationType() {
            return StringUtils.EMPTY;
        }

        Optional<Value> getValue(final Node node, final Metric searchMetric) {
            return node.getValues()
                    .stream()
                    .filter(leaf -> leaf.getMetric().equals(searchMetric))
                    .findAny();
        }
    }

    private static class CoverageOfChildrenEvaluator extends MetricEvaluator {
        @Serial
        private static final long serialVersionUID = 8788686429559762490L;

        @Override
        public boolean isAggregatingChildren() {
            return true;
        }

        @Override
        Optional<Value> computeDerivedValue(final Node node, final Metric searchMetric) {
            Optional<Value> aggregatedChildrenValue = node.getChildren().stream()
                    .map(n -> n.getValue(searchMetric))
                    .flatMap(Optional::stream)
                    .reduce(Value::add);
            Optional<Value> localMetricValue = getMetricOf(node, searchMetric);
            return Stream.of(localMetricValue, aggregatedChildrenValue)
                    .flatMap(Optional::stream)
                    .reduce(Value::add);
        }

        private Optional<Value> getMetricOf(final Node node, final Metric searchMetric) {
            if (node.getMetric().equals(searchMetric)) {
                return getValue(node, searchMetric).or(() -> deriveFromCoverage(node, searchMetric));
            }
            return Optional.empty();
        }

        private Optional<? extends Value> deriveFromCoverage(final Node node, final Metric searchMetric) {
            var hasCoverage = node.getMetrics().stream().anyMatch(Metric::isCoverage);
            if (hasCoverage) {
                return Optional.ofNullable(deriveCoverageFromOtherMetrics(node, searchMetric));
            }
            return Optional.empty();
        }

        private Coverage deriveCoverageFromOtherMetrics(final Node node, final Metric searchMetric) {
            var builder = new CoverageBuilder().withMetric(searchMetric);
            if (hasCoverage(node)) {
                builder.withCovered(1).withMissed(0);
            }
            else {
                builder.withCovered(0).withMissed(1);
            }
            return builder.build();
        }

        private boolean hasCoverage(final Node node) {
            boolean baseline = hasCoverage(node, INSTRUCTION)
                    || hasCoverage(node, LINE)
                    || hasCoverage(node, BRANCH);

            boolean additional = hasCoverage(node, MCDC_PAIR)
                    || hasCoverage(node, FUNCTION_CALL)
                    || hasCoverage(node, MUTATION);

            return baseline || additional;
        }

        private boolean hasCoverage(final Node node, final Metric metric) {
            return node.getValue(metric)
                    .filter(value -> ((Coverage) value).getCovered() > 0)
                    .isPresent();
        }
    }

    private static class ValuesAggregator extends MetricEvaluator {
        @Serial
        private static final long serialVersionUID = 7908490688181149667L;

        @SuppressFBWarnings("SE_BAD_FIELD")
        @SuppressWarnings("serial")
        private final BinaryOperator<Value> accumulator;
        private final String name;

        ValuesAggregator() {
            this(Value::add, "total");
        }

        ValuesAggregator(final BinaryOperator<Value> accumulator, final String name) {
            super();

            this.accumulator = accumulator;
            this.name = name;
        }

        @Override
        String getAggregationType() {
            return name;
        }

        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        final Optional<Value> computeDerivedValue(final Node node, final Metric searchMetric) {
            var defaultValue = getDefaultValue(node);

            return defaultValue.or(() -> node.getChildren().stream()
                    .map(n -> compute(n, searchMetric))
                    .flatMap(Optional::stream)
                    .reduce(accumulator));
        }

        Optional<Value> getDefaultValue(final Node node) {
            return Optional.empty();
        }
    }

    private static class LocEvaluator extends ValuesAggregator {
        @Serial
        private static final long serialVersionUID = 8819577749737375989L;

        @Override
        Optional<Value> getDefaultValue(final Node node) {
            return LINE.getValueFor(node).map(this::getTotal);
        }

        @SuppressFBWarnings(value = "BC", justification = "The value is a coverage value as it has the metric LINE")
        private Value getTotal(final Value leaf) {
            var coverage = (Coverage) leaf;
            return new Value(LOC, coverage.getTotal());
        }
    }

    private static class MetricFormatter implements Serializable {
        @Serial
        private static final long serialVersionUID = 7402798036375016965L;

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

        final String formatDouble(final Locale locale, final double value) {
            return String.format(locale, "%.2f", value);
        }

        final double toRounded(final double value, final int scale) {
            return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
        }

        String percentage(final String value) {
            return value + "%";
        }
    }

    private static class CoverageFormatter extends MetricFormatter {
        @Serial
        private static final long serialVersionUID = 4337117939462815181L;

        @Override
        String formatMean(final Locale locale, final double value) {
            return percentage(formatDouble(locale, value));
        }

        @Override
        String format(final Locale locale, final double value) {
            return percentage(formatDouble(locale, value));
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
        String format(final Locale locale, final double value) {
            return percentage(formatDouble(locale, value * 100));
        }

        @Override
        String formatMean(final Locale locale, final double value) {
            return percentage(formatDouble(locale, value * 100));
        }

        @Override
        String formatDelta(final Locale locale, final double value) {
            return percentage(super.formatDelta(locale, value * 100));
        }
    }

    private static class IntegerFormatter extends MetricFormatter {
        @Serial
        private static final long serialVersionUID = 8053070560640902081L;

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
    }
}
