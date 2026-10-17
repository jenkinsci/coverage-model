package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.Metric.CYCLOMATIC_COMPLEXITY;
import static edu.hm.hafner.coverage.Metric.LINE;
import static edu.hm.hafner.coverage.Metric.LOC;
import static edu.hm.hafner.coverage.assertions.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the class {@link ValueCalculator}: the computation of the values of the metrics for a tree of nodes using the
 * different {@link MetricAggregation aggregations}.
 */
class ValueCalculatorTest {
    @ParameterizedTest(name = "[{index}] {0} of (6, 12, 3) should be {1}")
    @CsvSource({"MAXIMUM, 12", "MINIMUM, 3", "OVERALL, 21"})
    void shouldComputeAggregation(final MetricAggregation aggregation, final int expected) {
        var classNode = createClassNodeWithComplexityValues(6, 12, 3);

        assertThat(classNode.getValue(CYCLOMATIC_COMPLEXITY, aggregation))
                .contains(Value.valueOf("CYCLOMATIC_COMPLEXITY: " + expected));
    }

    @ParameterizedTest(name = "[{index}] {0} of (6, 12, 3) should be {1}")
    @CsvSource({"MAXIMUM, 12", "MINIMUM, 3", "OVERALL, 21"})
    void shouldAggregateAllValuesWithAggregation(final MetricAggregation aggregation, final int expected) {
        var classNode = createClassNodeWithComplexityValues(6, 12, 3);

        assertThat(classNode.aggregateValues(aggregation))
                .contains(Value.valueOf("CYCLOMATIC_COMPLEXITY: " + expected));
    }

    @Test
    void shouldAggregateAllValuesWithDefaultAggregation() {
        var classNode = createClassNodeWithComplexityValues(6, 12, 3);

        assertThat(classNode.aggregateValues(MetricAggregation.getDefault()))
                .containsExactlyElementsOf(classNode.aggregateValues());
    }

    @Test
    void shouldReturnNoAggregatedValuesWhenNoValues() {
        var classNode = new ClassNode("TestClass");

        for (MetricAggregation aggregation : MetricAggregation.values()) {
            assertThat(classNode.aggregateValues(aggregation)).isEmpty();
        }
    }

    @Test
    void shouldReturnEmptyForAggregationWhenNoValues() {
        var classNode = new ClassNode("TestClass");

        assertThat(classNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MAXIMUM))
                .isEmpty();
        assertThat(classNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MINIMUM))
                .isEmpty();
        assertThat(classNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.OVERALL))
                .isEmpty();
    }

    @Test
    void shouldAggregateCoverageByPercentage() {
        var packageNode = new PackageNode("com.example");
        var classOne = new ClassNode("ClassOne");
        var classTwo = new ClassNode("ClassTwo");

        classOne.addValue(new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(8)
                .withMissed(2)
                .build());
        classTwo.addValue(new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(3)
                .withMissed(2)
                .build());

        var fileOne = new FileNode("ClassOne.java", "com/example/ClassOne.java");
        fileOne.addChild(classOne);
        var fileTwo = new FileNode("ClassTwo.java", "com/example/ClassTwo.java");
        fileTwo.addChild(classTwo);
        packageNode.addChild(fileOne);
        packageNode.addChild(fileTwo);

        assertThat(packageNode.getValue(LINE, MetricAggregation.MAXIMUM))
                .isPresent()
                .hasValueSatisfying(value -> assertThat(value.asDouble()).isEqualTo(80.0));
        assertThat(packageNode.getValue(LINE, MetricAggregation.MINIMUM))
                .isPresent()
                .hasValueSatisfying(value -> assertThat(value.asDouble()).isEqualTo(60.0));
    }

    @Test
    void shouldUseFilesForMaximumAndMinimumOfCoverage() {
        var packageNode = new PackageNode("com.example");
        var file = new FileNode("Classes.java", "com/example/Classes.java");
        packageNode.addChild(file);

        var classOne = new ClassNode("ClassOne");
        classOne.addValue(new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(8)
                .withMissed(2)
                .build());
        var classTwo = new ClassNode("ClassTwo");
        classTwo.addValue(new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(2)
                .withMissed(8)
                .build());
        file.addChild(classOne);
        file.addChild(classTwo);

        var fileCoverage = new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(10)
                .withMissed(10)
                .build();
        assertThat(packageNode.getValue(LINE, MetricAggregation.MAXIMUM)).contains(fileCoverage);
        assertThat(packageNode.getValue(LINE, MetricAggregation.MINIMUM)).contains(fileCoverage);
    }

    @Test
    void shouldIgnoreEmptyCoverageForMaximumAndMinimum() {
        var packageNode = new PackageNode("com.example");
        var withCode = new FileNode("WithCode.java", "com/example/WithCode.java");
        var coverage = new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(3)
                .withMissed(2)
                .build();
        withCode.addValue(coverage);
        var withoutCode = new FileNode("WithoutCode.java", "com/example/WithoutCode.java");
        withoutCode.addValue(Coverage.nullObject(LINE));
        packageNode.addChild(withCode);
        packageNode.addChild(withoutCode);

        assertThat(packageNode.getValue(LINE, MetricAggregation.MINIMUM)).contains(coverage);
        assertThat(packageNode.getValue(LINE, MetricAggregation.MAXIMUM)).contains(coverage);
        assertThat(packageNode.getValue(LINE, MetricAggregation.OVERALL)).contains(coverage);
    }

    @Test
    void shouldWorkWithNestedStructure() {
        var moduleNode = new ModuleNode("TestModule");
        var packageNode = new PackageNode("com.example");
        var fileNode = new FileNode("TestFile.java", "src/com/example/TestFile.java");
        var classNode = new ClassNode("TestClass");

        var method1 = new MethodNode("method1", "()V");
        var method2 = new MethodNode("method2", "()V");

        method1.addValue(new Value(CYCLOMATIC_COMPLEXITY, 8));
        method2.addValue(new Value(CYCLOMATIC_COMPLEXITY, 12));

        classNode.addChild(method1);
        classNode.addChild(method2);
        fileNode.addChild(classNode);
        packageNode.addChild(fileNode);
        moduleNode.addChild(packageNode);

        var classMax = classNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MAXIMUM);
        assertThat(classMax).isPresent();
        assertThat(classMax.get().asDouble()).isEqualTo(12.0);

        var fileMax = fileNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MAXIMUM);
        assertThat(fileMax).isPresent();
        assertThat(fileMax.get().asDouble()).isEqualTo(12.0);

        var packageMax = packageNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MAXIMUM);
        assertThat(packageMax).isPresent();
        assertThat(packageMax.get().asDouble()).isEqualTo(12.0);

        var moduleMax = moduleNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MAXIMUM);
        assertThat(moduleMax).isPresent();
        assertThat(moduleMax.get().asDouble()).isEqualTo(12.0);
    }

    @Test
    void shouldComputeAggregationForMultipleClasses() {
        var packageNode = new PackageNode("com.example");

        var class1 = new ClassNode("Class1");
        var method1 = new MethodNode("method1", "()V");
        method1.addValue(new Value(CYCLOMATIC_COMPLEXITY, 5));
        class1.addChild(method1);

        var class2 = new ClassNode("Class2");
        var method2 = new MethodNode("method2", "()V");
        method2.addValue(new Value(CYCLOMATIC_COMPLEXITY, 15));
        class2.addChild(method2);

        var fileNode1 = new FileNode("Class1.java", "src/Class1.java");
        fileNode1.addChild(class1);

        var fileNode2 = new FileNode("Class2.java", "src/Class2.java");
        fileNode2.addChild(class2);

        packageNode.addChild(fileNode1);
        packageNode.addChild(fileNode2);

        var maximum = packageNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MAXIMUM);
        assertThat(maximum).isPresent();
        assertThat(maximum.get().asDouble()).isEqualTo(15.0);

        var minimum = packageNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.MINIMUM);
        assertThat(minimum).isPresent();
        assertThat(minimum.get().asDouble()).isEqualTo(5.0);

        var overall = packageNode.getValue(CYCLOMATIC_COMPLEXITY, MetricAggregation.OVERALL);
        assertThat(overall).isPresent();
        assertThat(overall.get().asDouble()).isEqualTo(20.0);
    }

    @Test
    void shouldUseDefaultAggregationForRegularValue() {
        var classNode = new ClassNode("TestClass");
        var method1 = new MethodNode("method1", "()V");
        method1.addValue(new Value(CYCLOMATIC_COMPLEXITY, 5));
        classNode.addChild(method1);

        var overall = classNode.getValue(CYCLOMATIC_COMPLEXITY, CYCLOMATIC_COMPLEXITY.getDefaultAggregation());
        var regular = classNode.getValue(CYCLOMATIC_COMPLEXITY);

        assertThat(overall).isEqualTo(regular);
    }

    @Test
    void shouldProvideOnlyOverallValueForContainerMetrics() {
        var classNode = new ClassNode("TestClass");
        var covered = new MethodNode("covered", "()V");
        covered.addValue(new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(1)
                .withMissed(0)
                .build());
        var missed = new MethodNode("missed", "()V");
        missed.addValue(new Coverage.CoverageBuilder()
                .withMetric(LINE)
                .withCovered(0)
                .withMissed(1)
                .build());
        classNode.addChild(covered);
        classNode.addChild(missed);

        var expected = new Coverage.CoverageBuilder()
                .withMetric(Metric.METHOD)
                .withCovered(1)
                .withMissed(1)
                .build();
        assertThat(classNode.getValue(Metric.METHOD, MetricAggregation.OVERALL)).contains(expected);
        assertThat(classNode.getValue(Metric.METHOD, MetricAggregation.MAXIMUM)).isEmpty();
        assertThat(classNode.getValue(Metric.METHOD, MetricAggregation.MINIMUM)).isEmpty();
    }

    @Test
    void shouldReturnEmptyWhenNoChildrenHaveMetricForAggregation() {
        var packageNode = new PackageNode("com.example");
        var classNode = new ClassNode("TestClass");
        classNode.addValue(new Value(CYCLOMATIC_COMPLEXITY, 10));
        packageNode.addChild(classNode);

        var maximum = packageNode.getValue(LOC, MetricAggregation.MAXIMUM);
        assertThat(maximum).isEmpty();

        var minimum = packageNode.getValue(LOC, MetricAggregation.MINIMUM);
        assertThat(minimum).isEmpty();
    }

    private static ClassNode createClassNodeWithComplexityValues(final int... complexityValues) {
        var classNode = new ClassNode("TestClass");

        for (int index = 0; index < complexityValues.length; index++) {
            var method = new MethodNode("method" + (index + 1), "()V");
            method.addValue(new Value(CYCLOMATIC_COMPLEXITY, complexityValues[index]));
            classNode.addChild(method);
        }

        return classNode;
    }
}
