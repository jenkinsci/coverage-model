package edu.hm.hafner.coverage;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Computes the values of the metrics for a tree of {@link Node nodes}. A value that is stored in a node is used as it
 * is. All other values are derived from the nodes of the subtree, depending on the {@link Metric.Computation
 * computation} of the metric and the requested {@link MetricAggregation aggregation}.
 *
 * @author Ullrich Hafner
 */
final class ValueCalculator {
    private static final List<Metric> COVERAGE_INDICATORS = List.of(
            Metric.INSTRUCTION, Metric.LINE, Metric.BRANCH, Metric.MCDC_PAIR, Metric.FUNCTION_CALL, Metric.MUTATION);

    /**
     * Returns the value of the specified metric for the subtree that is spanned by the specified node. The
     * {@link MetricAggregation#OVERALL overall} value combines all values of the subtree. The
     * {@link MetricAggregation#MAXIMUM maximum} and {@link MetricAggregation#MINIMUM minimum} select the largest or
     * smallest value of the elements that provide the metric (see {@link Metric#getTargetNodes(Node)}). Empty coverage
     * values are ignored. Container metrics (e.g., the file coverage) provide only an overall value.
     *
     * @param node the root of the subtree
     * @param metric the metric to get the value for
     * @param aggregation the aggregation to use
     * @return the value or an empty result if no value has been defined
     */
    Optional<Value> getValue(final Node node, final Metric metric, final MetricAggregation aggregation) {
        if (aggregation == MetricAggregation.OVERALL) {
            return computeOverallValue(node, metric);
        }
        if (metric.isContainer()) {
            return Optional.empty();
        }
        var metricValues = metric.getTargetNodes(node).stream()
                .map(target -> computeOverallValue(target, metric))
                .flatMap(Optional::stream)
                .filter(this::isNotEmpty)
                .toList();
        return aggregation.aggregate(metricValues);
    }

    /**
     * Returns whether the specified value contains any data. A coverage without any covered or missed elements (e.g.,
     * the line coverage of a file without code) is empty and is therefore ignored when selecting the maximum or
     * minimum.
     *
     * @param value the value to check
     * @return {@code true} if the value contains data, {@code false} otherwise
     */
    private boolean isNotEmpty(final Value value) {
        return !(value instanceof Coverage coverage) || coverage.isSet();
    }

    /**
     * Computes the overall value of the specified metric for the subtree that is spanned by the specified node. If the
     * node stores a value for the metric, then this value is returned. Otherwise, the value is derived from the nodes
     * of the subtree, depending on the {@link Metric.Computation computation} of the metric.
     *
     * @param node the root of the subtree
     * @param metric the metric to get the value for
     * @return the overall value or an empty result if no value has been defined
     */
    private Optional<Value> computeOverallValue(final Node node, final Metric metric) {
        return getStoredValue(node, metric).or(() -> computeDerivedValue(node, metric));
    }

    private Optional<Value> getStoredValue(final Node node, final Metric metric) {
        return node.getValues().stream()
                .filter(value -> value.getMetric() == metric)
                .findAny();
    }

    private Optional<Value> computeDerivedValue(final Node node, final Metric metric) {
        return switch (metric.getComputation()) {
            case CONTAINER -> computeContainerCoverage(node, metric);
            case SUM -> sumValuesOfChildren(node, metric);
            case MEAN -> computeMeanOfTargetNodes(node, metric);
            case LINES_OF_CODE -> computeLinesOfCode(node).or(() -> sumValuesOfChildren(node, metric));
        };
    }

    private Optional<Value> sumValuesOfChildren(final Node node, final Metric metric) {
        return node.getChildren().stream()
                .map(child -> computeOverallValue(child, metric))
                .flatMap(Optional::stream)
                .reduce(Value::add);
    }

    private Optional<Value> computeMeanOfTargetNodes(final Node node, final Metric metric) {
        var targetValues = metric.getTargetNodes(node).stream()
                .map(target -> getStoredValue(target, metric))
                .flatMap(Optional::stream)
                .toList();
        return targetValues.stream().reduce(Value::add).map(sum -> sum.divide(targetValues.size()));
    }

    private Optional<Value> computeLinesOfCode(final Node node) {
        return computeOverallValue(node, Metric.LINE).map(this::getTotalLines);
    }

    private Value getTotalLines(final Value lineCoverage) {
        return new Value(Metric.LOC, ((Coverage) lineCoverage).getTotal());
    }

    /**
     * Computes the coverage of the nodes of the type of the specified container metric, e.g., the number of covered
     * files for {@link Metric#FILE}. A node is covered if at least one of its elements has been covered.
     *
     * @param node the root of the subtree
     * @param metric the container metric
     * @return the coverage of the nodes or an empty result if the subtree contains no coverage
     */
    private Optional<Value> computeContainerCoverage(final Node node, final Metric metric) {
        var coverageOfChildren = sumValuesOfChildren(node, metric);
        var coverageOfNode = node.getMetric() == metric ? computeCoverageOfNode(node, metric) : Optional.<Value>empty();
        return Stream.of(coverageOfNode, coverageOfChildren)
                .flatMap(Optional::stream)
                .reduce(Value::add);
    }

    private Optional<Value> computeCoverageOfNode(final Node node, final Metric metric) {
        if (node.getMetrics().stream().noneMatch(Metric::isCoverage)) {
            return Optional.empty();
        }
        var builder = new Coverage.CoverageBuilder().withMetric(metric);
        if (hasCoveredElements(node)) {
            builder.withCovered(1).withMissed(0);
        } else {
            builder.withCovered(0).withMissed(1);
        }
        return Optional.of(builder.build());
    }

    private boolean hasCoveredElements(final Node node) {
        return COVERAGE_INDICATORS.stream().anyMatch(metric -> hasCoveredElements(node, metric));
    }

    private boolean hasCoveredElements(final Node node, final Metric metric) {
        return computeOverallValue(node, metric)
                .filter(value -> ((Coverage) value).getCovered() > 0)
                .isPresent();
    }
}
