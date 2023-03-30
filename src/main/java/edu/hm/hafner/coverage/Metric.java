package edu.hm.hafner.coverage;

import java.util.Locale;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import com.google.errorprone.annotations.Immutable;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;

/**
 * A coverage metric to identify the coverage result type. Note the enum order since the ordinal is used to sort the
 * values for display purposes.
 *
 * @author Ullrich Hafner
 */
public enum Metric {
    /** Nodes that can have children. */
    CONTAINER(new LocOfChildrenEvaluator()),
    MODULE(new LocOfChildrenEvaluator()),
    PACKAGE(new LocOfChildrenEvaluator()),
    FILE(new LocOfChildrenEvaluator()),
    CLASS(new LocOfChildrenEvaluator()),
    METHOD(new LocOfChildrenEvaluator()),

    /** Coverage values without children. */
    LINE(new ValuesAggregator()),
    BRANCH(new ValuesAggregator()),
    INSTRUCTION(new ValuesAggregator()),

    /** Additional metrics without children. */
    MUTATION(new ValuesAggregator()),
    COMPLEXITY(new ValuesAggregator()),
    COMPLEXITY_DENSITY(new DensityEvaluator()),
    LOC(new LocEvaluator());

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

    private final MetricEvaluator evaluator;

    Metric(final MetricEvaluator evaluator) {
        this.evaluator = evaluator;
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
        return new TreeSet<>(Set.of(
                Metric.CONTAINER,
                Metric.MODULE,
                Metric.PACKAGE,
                Metric.FILE,
                Metric.CLASS,
                Metric.METHOD,
                Metric.LINE,
                Metric.BRANCH,
                Metric.INSTRUCTION
        ));
    }

    @Immutable
    private abstract static class MetricEvaluator {
        abstract Optional<Value> compute(Node node, Metric searchMetric);

        abstract boolean isAggregatingChildren();
    }

    private static class LocOfChildrenEvaluator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return true;
        }

        protected Optional<Value> getMetricOf(final Node node, final Metric searchMetric) {
            if (node.getMetric().equals(searchMetric)) {
                var builder = new CoverageBuilder().setMetric(searchMetric);
                if (hasCoverage(node)) {
                    builder.setCovered(1).setMissed(0);
                }
                else {
                    builder.setCovered(0).setMissed(1);
                }
                return Optional.of(builder.build());
            }
            return Optional.empty();
        }

        private boolean hasCoverage(final Node node) {
            return hasCoverage(node, Metric.INSTRUCTION)
                    || hasCoverage(node, Metric.LINE)
                    || hasCoverage(node, Metric.BRANCH);
        }

        private boolean hasCoverage(final Node node, final Metric metric) {
            return node.getValue(metric)
                    .filter(value -> ((Coverage) value).getCovered() > 0)
                    .isPresent();

        }

        @Override
        final Optional<Value> compute(final Node node, final Metric searchMetric) {
            Optional<Value> aggregatedChildrenValue = node.getChildren().stream()
                    .map(n -> n.getValue(searchMetric))
                    .flatMap(Optional::stream)
                    .reduce(Value::add);
            Optional<Value> localMetricValue = getMetricOf(node, searchMetric);
            return Stream.of(localMetricValue, aggregatedChildrenValue)
                    .flatMap(Optional::stream)
                    .reduce(Value::add);
        }
    }

    private static class LocEvaluator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        Optional<Value> compute(final Node node, final Metric searchMetric) {
            return LINE.getValueFor(node).map(leaf -> new LinesOfCode(((Coverage) leaf).getTotal()));
        }
    }

    private static class DensityEvaluator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        Optional<Value> compute(final Node node, final Metric searchMetric) {
            var locValue = LOC.getValueFor(node);
            var complexityValue = COMPLEXITY.getValueFor(node);
            if (locValue.isPresent() && complexityValue.isPresent()) {
                var loc = (LinesOfCode) locValue.get();
                if (loc.getValue() > 0) {
                    var complexity = (CyclomaticComplexity) complexityValue.get();
                    return Optional.of(new FractionValue(COMPLEXITY_DENSITY, complexity.getValue(), loc.getValue()));
                }
            }
            return Optional.empty();
        }
    }

    private static class ValuesAggregator extends MetricEvaluator {
        @Override
        public boolean isAggregatingChildren() {
            return false;
        }

        @Override
        final Optional<Value> compute(final Node node, final Metric searchMetric) {
            Optional<Value> localMetricValue = node.getValues()
                    .stream()
                    .filter(leaf -> leaf.getMetric().equals(searchMetric))
                    .findAny();
            if (localMetricValue.isPresent()) {
                return localMetricValue;
            }
            // aggregate children
            return node.getChildren().stream()
                    .map(n -> n.getValue(searchMetric))
                    .flatMap(Optional::stream)
                    .reduce(Value::add);
        }
    }
}
