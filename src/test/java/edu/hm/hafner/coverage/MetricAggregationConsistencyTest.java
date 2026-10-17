package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.assertThat;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.Metric.MetricValueType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.MethodSource;

/** Verifies the aggregation of the values of all metrics using the available {@link MetricAggregation aggregations}. */
class MetricAggregationConsistencyTest {
    private static Stream<Metric> aggregatedMetrics() {
        return Arrays.stream(Metric.values()).filter(metric -> !metric.isContainer());
    }

    private static Stream<Metric> containerMetrics() {
        return Arrays.stream(Metric.values()).filter(Metric::isContainer);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("aggregatedMetrics")
    void shouldUseDefaultAggregationForRegularValue(final Metric metric) {
        var module = createTree(metric);

        for (Node node : getNodes(module)) {
            assertThat(node.getValue(metric))
                    .as("%s of %s", metric, node)
                    .isEqualTo(node.getValue(metric, metric.getDefaultAggregation()));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("aggregatedMetrics")
    void shouldSelectMaximumAndMinimumOfTargetNodes(final Metric metric) {
        var module = createTree(metric);

        for (Node node : getNodes(module)) {
            assertThat(node.getValue(metric, MetricAggregation.MAXIMUM))
                    .as("Maximum of %s in %s", metric, node)
                    .contains(createExpectedValue(metric, 4, 5, 2));
            assertThat(node.getValue(metric, MetricAggregation.MINIMUM))
                    .as("Minimum of %s in %s", metric, node)
                    .contains(createExpectedValue(metric, 1, 3, 2));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("containerMetrics")
    void shouldProvideOnlyOverallValueForContainerMetrics(final Metric metric) {
        var module = createTree(Metric.LINE);

        assertThat(module.getValue(metric, MetricAggregation.MAXIMUM)).isEmpty();
        assertThat(module.getValue(metric, MetricAggregation.MINIMUM)).isEmpty();
        assertThat(module.getValue(metric, MetricAggregation.OVERALL)).isEqualTo(module.getValue(metric));
    }

    /**
     * The overall value of these metrics is the mean of all values, since these metrics are percentages without the
     * underlying parts.
     *
     * @param metric a metric that uses the mean as the overall value
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(
            value = Metric.class,
            names = {"COHESION", "WEIGHT_OF_CLASS"},
            mode = Mode.INCLUDE)
    void shouldUseMeanAsOverallValue(final Metric metric) {
        var module = createTree(metric);

        assertThat(module.getValue(metric)).contains(new Value(metric, 4));
        assertThat(module.getValue(metric, MetricAggregation.OVERALL)).contains(new Value(metric, 4));
        assertThat(module.aggregateValues()).contains(new Value(metric, 4));
        assertThat(module.aggregateValues(MetricAggregation.MAXIMUM)).contains(new Value(metric, 5));
        assertThat(module.aggregateValues(MetricAggregation.MINIMUM)).contains(new Value(metric, 3));
    }

    private static List<Node> getNodes(final ModuleNode module) {
        return List.of(module, module.getChildren().get(0));
    }

    private static Value createExpectedValue(
            final Metric metric, final int methodValue, final int classValue, final int missed) {
        if (metric.getType() == MetricValueType.METHOD_METRIC) {
            return new Value(metric, methodValue);
        } else if (metric.isCoverage()) {
            return new CoverageBuilder()
                    .withMetric(metric)
                    .withCovered(classValue)
                    .withMissed(missed)
                    .build();
        } else {
            return new Value(metric, classValue);
        }
    }

    /**
     * Creates a tree (module, package, two files with one class each, two methods per class) that stores values for the
     * specified metrics only. Method metrics are stored in the methods (1, 2, 3, and 4), coverage metrics are stored in
     * the classes (3/2 and 5/2), and all other metrics are stored in the classes (3 and 5).
     *
     * @param metric the metric to store the values for
     * @return the root of the tree
     */
    private static ModuleNode createTree(final Metric metric) {
        var module = new ModuleNode("module");
        var packageNode = new PackageNode("package");
        module.addChild(packageNode);

        var firstFile = new FileNode("First.java", "package/First.java");
        firstFile.addChild(createClass("First", metric, 3, 2, 1, 2));
        packageNode.addChild(firstFile);

        var secondFile = new FileNode("Second.java", "package/Second.java");
        secondFile.addChild(createClass("Second", metric, 5, 2, 3, 4));
        packageNode.addChild(secondFile);

        return module;
    }

    private static ClassNode createClass(
            final String name,
            final Metric metric,
            final int classValue,
            final int missed,
            final int firstMethodValue,
            final int secondMethodValue) {
        var classNode = new ClassNode(name);
        if (metric.getType() == MetricValueType.METHOD_METRIC) {
            classNode.addChild(createMethod("first", metric, firstMethodValue));
            classNode.addChild(createMethod("second", metric, secondMethodValue));
        } else if (metric.isCoverage()) {
            classNode.addValue(new CoverageBuilder()
                    .withMetric(metric)
                    .withCovered(classValue)
                    .withMissed(missed)
                    .build());
        } else {
            classNode.addValue(new Value(metric, classValue));
        }
        return classNode;
    }

    private static MethodNode createMethod(final String name, final Metric metric, final int value) {
        var method = new MethodNode(name, "()V");
        method.addValue(new Value(metric, value));
        return method;
    }
}
