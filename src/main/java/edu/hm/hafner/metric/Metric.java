package edu.hm.hafner.metric;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

/**
 * A coverage metric to identify the coverage result type.
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

    /** Values without children. */
    LINE(new ValuesAggregator()),
    INSTRUCTION(new ValuesAggregator()),
    BRANCH(new ValuesAggregator()),
    MUTATION(new ValuesAggregator()),
    COMPLEXITY(new ValuesAggregator()),
    COMPLEXITY_DENSITY(new DensityEvaluator()),
    LOC(new LocEvaluator());

    private final MetricEvaluator evaluator;

    Metric(final MetricEvaluator evaluator) {
        this.evaluator = evaluator;
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
     * Returns if a given metric is a node metric.
     *
     * @param metric
     *         the metric to check
     *
     * @return if the metric is a node metric
     */
    public static boolean isNodeMetric(final Metric metric) {
        Set<Metric> nodeMetrics = new HashSet<>(Arrays.asList(
                CONTAINER, MODULE, PACKAGE, FILE, CLASS, METHOD));

        return nodeMetrics.contains(metric);
    }

    private abstract static class MetricEvaluator {
        abstract Optional<Value> compute(Node node, Metric searchMetric);
    }

    private static class LocOfChildrenEvaluator extends MetricEvaluator {
        protected Optional<Value> getMetricOf(final Node node, final Metric searchMetric) {
            if (node.getMetric().equals(searchMetric)) {
                var builder = new CoverageBuilder().setMetric(searchMetric);
                // FIXME: create a checked method that will return the null object
                Optional<Value> lineCoverage = LINE.getValueFor(node);
                if (lineCoverage.isPresent() && ((Coverage) lineCoverage.get()).getCovered() > 0) {
                    builder.setCovered(1).setMissed(0);
                }
                else {
                    builder.setCovered(0).setMissed(1);
                }
                return Optional.of(builder.build());
            }
            return Optional.empty();
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
        Optional<Value> compute(final Node node, final Metric searchMetric) {
            return LINE.getValueFor(node).map(leaf -> new LinesOfCode(((Coverage) leaf).getTotal()));
        }
    }

    private static class DensityEvaluator extends MetricEvaluator {
        @Override
        Optional<Value> compute(final Node node, final Metric searchMetric) {
            var locValue = LOC.getValueFor(node);
            var complexityValue = COMPLEXITY.getValueFor(node);
            if (locValue.isPresent() && complexityValue.isPresent()) {
                LinesOfCode loc = (LinesOfCode)locValue.get();
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
