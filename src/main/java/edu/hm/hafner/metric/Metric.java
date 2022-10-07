package edu.hm.hafner.metric;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A coverage metric to identify the coverage result type. Note: this class has a natural ordering that is inconsistent
 * with equals.
 *
 * @author Ullrich Hafner
 */
public enum Metric {
    CONTAINER(new LocOfChildrenEvaluator()),
    MODULE(new LocOfChildrenEvaluator()),
    PACKAGE(new LocOfChildrenEvaluator()),
    FILE(new LocOfChildrenEvaluator()),
    CLASS(new LocOfChildrenEvaluator()),
    METHOD(new LocOfChildrenEvaluator()),
    INSTRUCTION(new ValuesAggregator()),
    LINE(new ValuesAggregator()),
    MUTATION(new ValuesAggregator()),
    BRANCH(new ValuesAggregator()),
    COMPLEXITY(new ValuesAggregator()),
    LOC(new LocEvaluator());

    private final MetricEvaluator evaluator;

    Metric(final MetricEvaluator evaluator) {
        this.evaluator = evaluator;
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

    private abstract static class AggregatingMetricEvaluator extends MetricEvaluator {
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

        protected abstract Optional<Value> getMetricOf(Node node, Metric searchMetric);
    }

    private static class LocOfChildrenEvaluator extends AggregatingMetricEvaluator {
        @Override
        protected Optional<Value> getMetricOf(final Node node, final Metric searchMetric) {
            if (node.getMetric().equals(searchMetric)) {
                Optional<Value> lineCoverage = LINE.getValueFor(node);
                if (lineCoverage.isPresent() && ((Coverage) lineCoverage.get()).getCovered() > 0) {
                    return Optional.of(new Coverage(searchMetric, 1, 0));
                }
                return Optional.of(new Coverage(searchMetric, 0, 1));
            }
            return Optional.empty();
        }
    }

    private static class LocEvaluator extends MetricEvaluator {
        @Override
        Optional<Value> compute(final Node node, final Metric searchMetric) {
            return LINE.getValueFor(node).map(leaf -> new LinesOfCode(((Coverage) leaf).getTotal()));
        }
    }

    private static class ValuesAggregator extends AggregatingMetricEvaluator {
        @Override
        protected Optional<Value> getMetricOf(final Node node, final Metric searchMetric) {
            return node.getValues().stream().filter(leaf -> leaf.getMetric().equals(searchMetric)).findAny();
        }
    }
}
