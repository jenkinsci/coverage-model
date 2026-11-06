package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.util.TreeString;
import java.util.List;
import org.junit.jupiter.api.Test;

class ClassNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.CLASS;
    }

    @Override
    Node createNode(final String name) {
        return new ClassNode(name);
    }

    @Test
    void shouldHandleUnexpectedNodes() {
        var classNode = new ClassNode("Class");
        var main = new MethodNode("main", "String...");
        classNode.addChild(main);
        classNode.addChild(new ClassNode("NestedClass"));

        assertThat(classNode.findMethod("main", "String...")).isPresent().containsSame(main);
        assertThat(classNode.findMethod("main", "Nothing")).isNotPresent();
        assertThat(classNode).isNotAggregation();
    }

    @Test
    void shouldCopyTests() {
        var original = new ClassNode("Copy Me");
        var testCase = new TestCaseBuilder().withTestName("test").build();
        original.addTestCase(testCase);

        assertThat(original.getTestCases()).hasSize(1).contains(testCase);
        assertThat(original.copy().getTestCases()).hasSize(1).contains(testCase);
    }

    @Test
    void shouldHavePackageName() {
        var classWithoutPackage = new ClassNode("Class");
        assertThat(classWithoutPackage.getPackageName()).isEqualTo(Node.EMPTY_NAME);
        var classWithPackage = new ClassNode("edu.hm.hafner.Class");
        assertThat(classWithPackage.getPackageName()).isEqualTo("edu.hm.hafner");

        var packageNode = new PackageNode("edu.hm");
        packageNode.addChild(classWithPackage);
        assertThat(classWithPackage.getPackageName()).isEqualTo("edu.hm.hafner");

        packageNode.addChild(classWithoutPackage);
        assertThat(classWithoutPackage.getPackageName()).isEqualTo("edu.hm");

        var another = new ClassNode("Class");
        var file = new FileNode("a.b.c.file.txt", TreeString.valueOf("/path/to/file.txt"));
        file.addChild(another);
        assertThat(another.getPackageName()).isEqualTo(Node.EMPTY_NAME);
    }

    /**
     * Verifies that the number of tests and the success rate are computed for each class. Skipped tests are not part of
     * the success rate.
     */
    @Test
    void shouldComputeTestValuesOfClass() {
        var classNode = new ClassNode("Tests");

        classNode.addTestCase(createTestCase("passed-1", TestResult.PASSED));
        classNode.addTestCases(List.of(
                createTestCase("passed-2", TestResult.PASSED),
                createTestCase("failed", TestResult.FAILED),
                createTestCase("skipped", TestResult.SKIPPED)));

        assertThat(classNode.getValues())
                .containsExactlyInAnyOrder(new Value(Metric.TESTS, 4), new Rate(Metric.TEST_SUCCESS_RATE, 2, 3));
    }

    /** Verifies that no success rate is computed for a class that contains only skipped tests. */
    @Test
    void shouldNotComputeSuccessRateIfAllTestsAreSkipped() {
        var classNode = new ClassNode("Tests");

        classNode.addTestCase(createTestCase("skipped", TestResult.SKIPPED));

        assertThat(classNode.getValues()).containsExactly(new Value(Metric.TESTS, 1));
    }

    /** Verifies that the number of tests and the success rate of the classes are aggregated to the parent nodes. */
    @Test
    void shouldAggregateTestValuesOfClasses() {
        var module = new ModuleNode("module");
        var packageNode = module.findOrCreatePackageNode("edu.hm.hafner");
        new ClassNodeFixture(packageNode.findOrCreateClassNode("Passed"))
                .withTestCase(createTestCase("passed-1", TestResult.PASSED))
                .withTestCase(createTestCase("passed-2", TestResult.PASSED))
                .withTestCase(createTestCase("passed-3", TestResult.PASSED));
        new ClassNodeFixture(packageNode.findOrCreateClassNode("Failed"))
                .withTestCase(createTestCase("passed", TestResult.PASSED))
                .withTestCase(createTestCase("failed", TestResult.FAILED))
                .withTestCase(createTestCase("skipped", TestResult.SKIPPED));

        assertThat(module.getValues()).isEmpty();
        assertThat(packageNode.aggregateValues())
                .containsExactlyInAnyOrder(new Value(Metric.TESTS, 6), new Rate(Metric.TEST_SUCCESS_RATE, 4, 5));
        assertThat(module.getValue(Metric.TEST_SUCCESS_RATE)).contains(new Rate(Metric.TEST_SUCCESS_RATE, 4, 5));
        assertThat(module.getValue(Metric.TEST_SUCCESS_RATE, MetricAggregation.MINIMUM))
                .contains(new Rate(Metric.TEST_SUCCESS_RATE, 1, 2));
        assertThat(module.getValue(Metric.TEST_SUCCESS_RATE, MetricAggregation.MAXIMUM))
                .contains(new Rate(Metric.TEST_SUCCESS_RATE, 3, 3));
    }

    private TestCase createTestCase(final String name, final TestResult result) {
        return new TestCaseBuilder().withTestName(name).withStatus(result).build();
    }
}
