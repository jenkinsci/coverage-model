package edu.hm.hafner.coverage.parser;

import java.util.Collection;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.coverage.TestCount;

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
        ModuleNode tree = readReport("archunit1.xml");

        assertThat(tree.getAll(Metric.MODULE)).hasSize(1);
        assertThat(tree.getAll(Metric.PACKAGE)).hasSize(1);
        assertThat(tree.getAll(Metric.CLASS)).hasSize(1);

        assertThat(tree.aggregateValues()).contains(new TestCount(1));

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName(EMPTY);

        var testClass = getFirstClass(tree);
        assertThat(testClass).hasName("Aufgabe3Test");
        assertThat(testClass.aggregateValues()).contains(new TestCount(1));
        assertThat(testClass.getTestCases()).hasSize(1);

        var testCase = getFirstTest(tree);
        assertThat(testCase).hasResult(TestResult.FAILED)
                .hasClassName("Aufgabe3Test")
                .hasTestName("shouldSplitToEmptyRight(int)[1]")
                .hasType("org.opentest4j.AssertionFailedError");
        assertThat(testCase.getMessage()).isEmpty();
        assertThat(testCase.getDescription()).contains(
                "at Aufgabe3Test.shouldSplitToEmptyRight(Aufgabe3Test.java:254)");

        ModuleNode node = readReport("archunit2.xml");

        assertThat(node.getAll(Metric.CLASS)).hasSize(1);
        assertThat(node.aggregateValues()).contains(new TestCount(3));
    }

    @Test
    void shouldReadWithNameOnly() {
        ModuleNode tree = readReport("cfn-lint.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName(EMPTY);
        assertThat(getFirstClass(tree)).hasName("CloudFormation Lint");

        assertThat(tree.aggregateValues()).contains(new TestCount(141));
        assertThat(tree.getTestCases()).hasSize(141)
                .filteredOn(test -> test.getResult() == TestResult.SKIPPED).hasSize(19);
    }

    @Test
    void shouldReadFailure() {
        ModuleNode tree = readReport("issue-113.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName(EMPTY);
        assertThat(getFirstClass(tree)).hasName("Assignment1Test");
        assertThat(getFirstTest(tree).getDescription()).contains("Die Welten sind nicht korrekt");

        assertThat(tree.aggregateValues()).contains(new TestCount(1));
    }

    @Test
    void shouldReadError() {
        ModuleNode tree = readReport("JENKINS-64117.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("eu.pinteam.kyoto.gunit.testenv.test");
        assertThat(getFirstClass(tree)).hasName("eu.pinteam.kyoto.gunit.testenv.test.CalculationUtilTest");
        assertThat(getFirstTest(tree).getMessage()).isEqualTo("The container NewcontTest0 does not allow a parameter of type f1");
        assertThat(getFirstTest(tree).getDescription()).contains("ava.lang.IllegalStateException: The container NewcontTest0 does not allow a parameter of type f1");

        assertThat(tree.aggregateValues()).contains(new TestCount(3));
    }

    @Test
    void shouldReadBrokenClassNames() {
        ModuleNode tree = readReport("jest-junit.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName(EMPTY);
        assertThat(getFirstClass(tree)).hasName("snapshots should display correct snapshot");
        assertThat(getFirstTest(tree).getDescription()).contains("Error: expect.assertions(3)");

        assertThat(tree.aggregateValues()).contains(new TestCount(11));
    }

    @Test
    void shouldReadJavaClassNames() {
        ModuleNode tree = readReport("junit.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("com.example.jenkinstest");
        assertThat(getFirstClass(tree)).hasName("com.example.jenkinstest.ExampleUnitTest");
        assertThat(getFirstTest(tree).getDescription()).contains("com.example.jenkinstest.ExampleUnitTest.failTest4");

        assertThat(tree.aggregateValues()).contains(new TestCount(6));
    }

    @Test
    void shouldReadAndroidTestResults() {
        ModuleNode tree = readReport("junit2.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("my.company");
        assertThat(getFirstClass(tree)).hasName("my.company.MainActivityTest");
        assertThat(getFirstTest(tree).getDescription()).contains("Looped for 3838 iterations over 60 SECONDS");

        assertThat(tree.aggregateValues()).contains(new TestCount(1));
    }

    @Test
    void shouldReadNoMessageAndType() {
        ModuleNode tree = readReport("junit-no-message-or-type.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("timrAPITests");
        assertThat(getFirstClass(tree)).hasName("timrAPITests.UtilTests");
        assertThat(getFirstTest(tree).getDescription()).contains("timrAPITests/Tests/Utils/UtilTests.swift:23");

        assertThat(tree.aggregateValues()).contains(new TestCount(3));
    }

    @Test
    void shouldReadErrorWithoutSuite() {
        ModuleNode tree = readReport("plainerror.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("edu.hm.hafner.analysis.parser");
        assertThat(getFirstClass(tree)).hasName("edu.hm.hafner.analysis.parser.SonarQubeDiffParserTest");
        assertThat(getFirstTest(tree).getDescription()).contains("org.json.JSONException: Missing value at 0");

        assertThat(tree.aggregateValues()).contains(new TestCount(1));
    }

    @Test
    void shouldReadPerformanceTestResults() {
        ModuleNode tree = readReport("TEST-org.jenkinsci.plugins.jvctb.perform.JvctbPerformerTest.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("org.jenkinsci.plugins.jvctb.perform");
        assertThat(getFirstClass(tree)).hasName("org.jenkinsci.plugins.jvctb.perform.JvctbPerformerTest");
        assertThat(getFirstTest(tree).getDescription()).contains("org.junit.ComparisonFailure");

        assertThat(tree.aggregateValues()).contains(new TestCount(1));
    }

    @Test
    void shouldReadTestSuites() {
        ModuleNode tree = readReport("TESTS-TestSuites.xml");

        assertThat(tree).hasName(EMPTY);
        assertThat(getPackage(tree)).hasName("ch.bdna.tsm.service");
        assertThat(getFirstClass(tree)).hasName("ch.bdna.tsm.service.PollingServiceTest");
        assertThat(getFirstTest(tree).getDescription()).contains("Missing CPU value");

        assertThat(tree.aggregateValues()).contains(new TestCount(2));
    }

    private PackageNode getPackage(final Node node) {
        var children = node.getChildren();
        assertThat(children).hasSize(1).first().isInstanceOf(PackageNode.class);

        return (PackageNode) children.get(0);
    }

    private ClassNode getFirstClass(final Node node) {
        var packageNode = getPackage(node);

        var children = packageNode.getChildren();
        assertThat(children).isNotEmpty().first().isInstanceOf(ClassNode.class);

        return (ClassNode) children.get(0);
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
}
