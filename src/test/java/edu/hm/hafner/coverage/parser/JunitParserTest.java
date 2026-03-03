package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ParsingException;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.coverage.Rate;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.coverage.Value;

import java.util.Collection;
import java.util.NoSuchElementException;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class JunitParserTest extends AbstractParserTest {
    private static final String EMPTY = "-";

    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new JunitParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "junit";
    }

    @Test
    void shouldReadArchUnitTests() {
        var tree = readJunitReport("archunit1.xml");

        assertThat(tree.getAll(Metric.MODULE)).hasSize(1);
        assertThat(tree.getAll(Metric.PACKAGE)).hasSize(1);
        assertThat(tree.getAll(Metric.CLASS)).hasSize(1);

        assertThat(tree.aggregateValues()).contains(new Value(Metric.TESTS, 1));

        assertThat(getPackage(tree)).hasName(EMPTY);

        var testClass = getFirstClass(tree);
        assertThat(testClass).hasName("Aufgabe3Test");
        assertThat(testClass.aggregateValues()).contains(new Value(Metric.TESTS, 1));
        assertThat(testClass.getTestCases()).hasSize(1);

        var testCase = getFirstTest(tree);
        assertThat(testCase).hasResult(TestResult.FAILED)
                .hasClassName("Aufgabe3Test")
                .hasTestName("shouldSplitToEmptyRight(int)[1]")
                .hasType("org.opentest4j.AssertionFailedError");
        assertThat(testCase.getMessage()).isEmpty();
        assertThat(testCase.getDescription()).contains(
                "at Aufgabe3Test.shouldSplitToEmptyRight(Aufgabe3Test.java:254)");

        var node = readReport("archunit2.xml");

        assertThat(node.getAll(Metric.CLASS)).hasSize(1);
        assertThat(node.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 3),
                new Rate(Metric.TEST_SUCCESS_RATE, 1, 3));
    }

    @Test
    void shouldReadWithNameOnly() {
        var tree = readJunitReport("cfn-lint.xml");
        assertThat(getPackage(tree)).hasName(EMPTY);
        assertThat(getFirstClass(tree)).hasName("CloudFormation Lint");

        assertThat(tree.getTestCases()).hasSize(141)
                .filteredOn(test -> test.getResult() == TestResult.SKIPPED).hasSize(19);
        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 141),
                new Rate(Metric.TEST_SUCCESS_RATE, 121, 122));
    }

    @Test
    void shouldReadFailure() {
        var tree = readJunitReport("issue-113.xml");
        assertThat(getPackage(tree)).hasName(EMPTY);
        assertThat(getFirstClass(tree)).hasName("Assignment1Test");
        assertThat(getFirstTest(tree).getDescription()).contains("Die Welten sind nicht korrekt");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 1),
                new Rate(Metric.TEST_SUCCESS_RATE, 0, 1));
    }

    @Test
    void shouldReadError() {
        var tree = readJunitReport("JENKINS-64117.xml");
        assertThat(getPackage(tree)).hasName("eu.pinteam.kyoto.gunit.testenv.test");
        assertThat(getFirstClass(tree)).hasName("eu.pinteam.kyoto.gunit.testenv.test.CalculationUtilTest");
        assertThat(getFirstTest(tree).getMessage()).isEqualTo(
                "The container NewcontTest0 does not allow a parameter of type f1");
        assertThat(getFirstTest(tree).getDescription()).contains(
                "ava.lang.IllegalStateException: The container NewcontTest0 does not allow a parameter of type f1");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 3),
                new Rate(Metric.TEST_SUCCESS_RATE, 2, 3));
    }

    @Test
    void shouldReadBrokenClassNames() {
        var tree = readJunitReport("jest-junit.xml");
        assertThat(getPackage(tree)).hasName(EMPTY);
        assertThat(getFirstClass(tree)).hasName("snapshots should display correct snapshot");
        assertThat(getFirstTest(tree).getDescription()).contains("Error: expect.assertions(3)");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 11),
                new Rate(Metric.TEST_SUCCESS_RATE, 10, 11));
    }

    @Test
    void shouldReadJavaClassNames() {
        var tree = readJunitReport("junit.xml");
        assertThat(getPackage(tree)).hasName("com.example.jenkinstest");
        assertThat(getFirstClass(tree)).hasName("com.example.jenkinstest.ExampleUnitTest");
        assertThat(getFirstTest(tree).getDescription()).contains("com.example.jenkinstest.ExampleUnitTest.failTest4");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 6),
                new Rate(Metric.TEST_SUCCESS_RATE, 4, 6));
    }

    @Test
    void shouldReadAndroidTestResults() {
        var tree = readJunitReport("junit2.xml");
        assertThat(getPackage(tree)).hasName("my.company");
        assertThat(getFirstClass(tree)).hasName("my.company.MainActivityTest");
        assertThat(getFirstTest(tree).getDescription()).contains("Looped for 3838 iterations over 60 SECONDS");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 1),
                new Rate(Metric.TEST_SUCCESS_RATE, 0, 1));
    }

    @Test
    void shouldReadNoMessageAndType() {
        var tree = readJunitReport("junit-no-message-or-type.xml");
        assertThat(getPackage(tree)).hasName("timrAPITests");
        assertThat(getFirstClass(tree)).hasName("timrAPITests.UtilTests");
        assertThat(getFirstTest(tree).getDescription()).contains("timrAPITests/Tests/Utils/UtilTests.swift:23");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 3),
                new Rate(Metric.TEST_SUCCESS_RATE, 2, 3));
    }

    @Test
    void shouldReadErrorWithoutSuite() {
        var tree = readJunitReport("plainerror.xml");
        assertThat(getPackage(tree)).hasName("edu.hm.hafner.analysis.parser");
        assertThat(getFirstClass(tree)).hasName("edu.hm.hafner.analysis.parser.SonarQubeDiffParserTest");
        assertThat(getFirstTest(tree).getDescription()).contains("org.json.JSONException: Missing value at 0");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 1),
                new Rate(Metric.TEST_SUCCESS_RATE, 0, 1));
    }

    @Test
    void shouldReadPerformanceTestResults() {
        var tree = readJunitReport("TEST-org.jenkinsci.plugins.jvctb.perform.JvctbPerformerTest.xml");
        assertThat(getPackage(tree)).hasName("org.jenkinsci.plugins.jvctb.perform");
        assertThat(getFirstClass(tree)).hasName("org.jenkinsci.plugins.jvctb.perform.JvctbPerformerTest");
        assertThat(getFirstTest(tree).getDescription()).contains("org.junit.ComparisonFailure");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 1),
                new Rate(Metric.TEST_SUCCESS_RATE, 0, 1));
    }

    @Test
    void shouldReadTestSuites() {
        var tree = readJunitReport("TESTS-TestSuites.xml");
        assertThat(getPackage(tree)).hasName("ch.bdna.tsm.service");
        assertThat(getFirstClass(tree)).hasName("ch.bdna.tsm.service.PollingServiceTest");
        assertThat(getFirstTest(tree).getDescription()).contains("Missing CPU value");

        assertThat(tree.aggregateValues()).containsExactly(
                new Value(Metric.TESTS, 2),
                new Rate(Metric.TEST_SUCCESS_RATE, 0, 2));
    }

    private ModuleNode readJunitReport(final String fileName) {
        var tree = readReport(fileName);
        assertThat(tree).hasName(fileName);
        return tree;
    }

    private PackageNode getPackage(final Node node) {
        var children = node.getChildren();
        assertThat(children).hasSize(1).first().isInstanceOf(PackageNode.class);

        return (PackageNode) children.getFirst();
    }

    private ClassNode getFirstClass(final Node node) {
        var packageNode = getPackage(node);

        var children = packageNode.getChildren();
        assertThat(children).isNotEmpty().first().isInstanceOf(ClassNode.class);

        return (ClassNode) children.getFirst();
    }

    private TestCase getFirstTest(final Node node) {
        return node.getAll(Metric.CLASS).stream()
                .map(ClassNode.class::cast)
                .map(ClassNode::getTestCases)
                .flatMap(Collection::stream)
                .filter(test -> test.getResult() == TestResult.FAILED)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("No failed test found"));
    }

    @Test
    void shouldFailWhenParsingInvalidFiles() {
        assertThatExceptionOfType(ParsingException.class).isThrownBy(() -> readReport("/design.puml"));
    }
}
