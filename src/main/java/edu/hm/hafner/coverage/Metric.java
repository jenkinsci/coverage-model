package edu.hm.hafner.coverage;

import java.util.Arrays;
import java.util.Locale;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.errorprone.annotations.Immutable;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A metric to identify the type of the results. The enum order will be used to sort the
 * values for display purposes.
 *
 * @author Ullrich Hafner
 */
public enum Metric {
    /** Nodes that can have children. These notes compute their values on the fly based on the children's content. */
    CONTAINER(new CoverageOfChildrenEvaluator()),
    MODULE(new CoverageOfChildrenEvaluator()),
    PACKAGE(new CoverageOfChildrenEvaluator()),
    FILE(new CoverageOfChildrenEvaluator()),
    CLASS(new CoverageOfChildrenEvaluator()),
    METHOD(new CoverageOfChildrenEvaluator()),

    /** Coverage values that are leaves in the tree. */
    LINE(new ValuesAggregator()),
    BRANCH(new ValuesAggregator()),
    INSTRUCTION(new ValuesAggregator()),
    MCDC_PAIR(new ValuesAggregator()),
    FUNCTION_CALL(new ValuesAggregator()),

    /** Additional coverage values obtained from mutation testing. */
    MUTATION(new ValuesAggregator()),
    TEST_STRENGTH(new ValuesAggregator()),

    CYCLOMATIC_COMPLEXITY(new ValuesAggregator(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC),
    CYCLOMATIC_COMPLEXITY_MAXIMUM(new MethodMaxComplexityFinder(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC),
    CYCLOMATIC_COMPLEXITY_DENSITY(new DensityEvaluator(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC),
    LOC(new LocEvaluator(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC),
    TESTS(new ValuesAggregator(), MetricTendency.LARGER_IS_BETTER, MetricValueType.METRIC),
    NCSS(new ValuesAggregator(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC),
    COGNITIVE_COMPLEXITY(new ValuesAggregator(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC),
    NPATH_COMPLEXITY(new ValuesAggregator(), MetricTendency.SMALLER_IS_BETTER, MetricValueType.METRIC);

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
        throw new IllegalArgumentException("No metric found for name: " + name);
    }

    private static String normalize(final String name) {
        return name.toUpperCase(Locale.ENGLISH).replaceAll("[-_]", "");
    }

    @SuppressFBWarnings("SE_BAD_FIELD")
    private final MetricEvaluator evaluator;
    private final MetricTendency tendency;
    private final MetricValueType type;

    Metric(final MetricEvaluator evaluator) {
        this(evaluator, MetricTendency.LARGER_IS_BETTER);
    }

    Metric(final MetricEvaluator evaluator, final MetricTendency tendency) {
        this(evaluator, tendency, MetricValueType.COVERAGE);
    }

    Metric(final MetricEvaluator evaluator, final MetricTendency tendency, final MetricValueType type) {
        this.evaluator = evaluator;
        this.tendency = tendency;
        this.type = type;
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
        /** Software metrics are represented by values of the type {@link Value}. */
        METRIC
    }

    @Immutable
    private abstract static class MetricEvaluator {
        final Optional<Value> compute(final Node node, final Metric searchMetric) {
            return getValue(node, searchMetric).or(() -> computeDerivedValue(node, searchMetric));
        }

        abstract Optional<Value> computeDerivedValue(Node node, Metric searchMetric);

        abstract boolean isAggregatingChildren();

        Optional<Value> getValue(final Node node, final Metric searchMetric) {
            return node.getValues()
                    .stream()
                    .filter(leaf -> leaf.getMetric().equals(searchMetric))
                    .findAny();
        }
    }

    private static class CoverageOfChildrenEvaluator extends MetricEvaluator {
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
                return Optional.of(getValue(node, searchMetric)
                        .orElse(deriveCoverageFromOtherMetrics(node, searchMetric)));
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
                    || hasCoverage(node, FUNCTION_CALL);

            return baseline || additional;
        }

        private boolean hasCoverage(final Node node, final Metric metric) {
            return node.getValue(metric)
                    .filter(value -> ((Coverage) value).getCovered() > 0)
                    .isPresent();
        }
    }

    private static class LocEvaluator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        Optional<Value> computeDerivedValue(final Node node, final Metric searchMetric) {
            return LINE.getValueFor(node).map(this::getTotal);
        }

        @SuppressFBWarnings(value = "BC", justification = "The value is a coverage value as it has the metric LINE")
        private Value getTotal(final Value leaf) {
            var coverage = (Coverage) leaf;
            return new Value(LOC, coverage.getTotal());
        }
    }

    private static class DensityEvaluator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        Optional<Value> computeDerivedValue(final Node node, final Metric searchMetric) {
            var locValue = LOC.getValueFor(node);
            var complexityValue = CYCLOMATIC_COMPLEXITY.getValueFor(node);
            if (locValue.isPresent() && complexityValue.isPresent()) {
                var loc = locValue.get().asInteger();
                if (loc > 0) {
                    var complexity = complexityValue.get();
                    return Optional.of(new Value(CYCLOMATIC_COMPLEXITY_DENSITY, complexity.asInteger(), loc));
                }
            }
            return Optional.empty();
        }
    }

    private static class MethodMaxComplexityFinder extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        Optional<Value> computeDerivedValue(final Node node, final Metric searchMetric) {
            if (node.getMetric() == METHOD) {
                return CYCLOMATIC_COMPLEXITY.getValueFor(node)
                        .map(c -> new Value(CYCLOMATIC_COMPLEXITY_MAXIMUM, c.getFraction()));
            }
            return node.getChildren().stream()
                    .map(c -> compute(c, searchMetric))
                    .flatMap(Optional::stream)
                    .reduce(Value::max);
        }
    }

    private static class ValuesAggregator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        final Optional<Value> computeDerivedValue(final Node node, final Metric searchMetric) {
            // aggregate children values
            return node.getChildren().stream()
                    .map(n -> n.getValue(searchMetric))
                    .flatMap(Optional::stream)
                    .reduce(Value::add);
        }
    }
}
