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

/**
 * Verifies that the metric specific computation of values ({@link Node#getValue(Metric)}) is consistent with the
 * computation of values using a {@link MetricAggregation} ({@link Node#getValue(Metric, MetricAggregation)}).
 */
class MetricAggregationConsistencyTest {
    private static final String MAXIMUM = "maximum";

    private static Stream<Metric> aggregatedMetrics() {
        return Arrays.stream(Metric.values()).filter(metric -> !metric.isContainer());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("aggregatedMetrics")
    void shouldComputeSameValueWithDefaultAggregationOfMetric(final Metric metric) {
        var module = createTree(metric);
        var packageNode = module.getChildren().getFirst();
        var fileNode = packageNode.getChildren().getFirst();

        var aggregation = getDefaultAggregation(metric);
        for (Node node : List.of(module, packageNode, fileNode)) {
            assertThat(node.getValue(metric, aggregation))
                    .as("%s of %s using %s", metric, node, aggregation)
                    .isEqualTo(node.getValue(metric));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(
            value = Metric.class,
            names = {"COHESION", "WEIGHT_OF_CLASS"},
            mode = Mode.INCLUDE)
    void shouldUseMaximumAsDefaultAggregation(final Metric metric) {
        assertThat(metric.getAggregationType()).isEqualTo(MAXIMUM);

        var module = createTree(metric);

        assertThat(module.getValue(metric)).contains(new Value(metric, 5));
        assertThat(module.getValue(metric, MetricAggregation.MAXIMUM)).contains(new Value(metric, 5));
        assertThat(module.getValue(metric, MetricAggregation.TOTAL)).contains(new Value(metric, 8));
    }

    /**
     * The aggregation {@link MetricAggregation#TOTAL} yields the same values as {@link Node#aggregateValues()}, i.e. it
     * selects the metric specific default when aggregating all values of a tree.
     *
     * @param metric a metric that uses the maximum as the default aggregation
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @EnumSource(
            value = Metric.class,
            names = {"COHESION", "WEIGHT_OF_CLASS"},
            mode = Mode.INCLUDE)
    void shouldAggregateAllValuesUsingDefaultAggregationOfMetric(final Metric metric) {
        var module = createTree(metric);

        var expected = new Value(metric, 5);
        assertThat(module.aggregateValues()).contains(expected);
        assertThat(module.aggregateValues(MetricAggregation.TOTAL)).contains(expected);
        assertThat(module.aggregateValues(MetricAggregation.MAXIMUM)).contains(expected);
        assertThat(module.aggregateValues(MetricAggregation.MINIMUM)).contains(new Value(metric, 3));
    }

    private static MetricAggregation getDefaultAggregation(final Metric metric) {
        if (MAXIMUM.equals(metric.getAggregationType())) {
            return MetricAggregation.MAXIMUM;
        }
        return MetricAggregation.getDefault();
    }

    /**
     * Creates a tree (module, package, file, two classes with two methods each) that stores values for the specified
     * metric only. Method metrics are stored in the methods (1, 2, 3, and 4), coverage metrics are stored in the
     * classes (8/2 and 3/2), and all other metrics are stored in the classes (3 and 5).
     *
     * @param metric the metric to store the values for
     * @return the root of the tree
     */
    private static ModuleNode createTree(final Metric metric) {
        var module = new ModuleNode("module");
        var packageNode = new PackageNode("package");
        var fileNode = new FileNode("File.java", "package/File.java");
        module.addChild(packageNode);
        packageNode.addChild(fileNode);

        var first = createClass("First", metric, 3, 2, 1, 2);
        var second = createClass("Second", metric, 5, 2, 3, 4);
        fileNode.addChild(first);
        fileNode.addChild(second);

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
