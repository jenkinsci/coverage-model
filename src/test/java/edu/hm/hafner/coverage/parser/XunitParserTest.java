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
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestResult;
import edu.hm.hafner.coverage.Value;

import java.util.Collection;
import java.util.NoSuchElementException;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class XunitParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new XunitParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "xunit";
    }

    @Test
    void shouldReadReport() {
        var tree = readXunitReport("xunit.xml");
        assertThat(getPackage(tree)).hasName("-");
        assertThat(getFirstClass(tree)).hasName("test.Tests2");
        assertThat(getFirstTest(tree).getDescription()).contains("Assert.Equal() Failure");

        assertThat(tree.aggregateValues()).contains(new Value(Metric.TESTS, 3));
    }

    @Test
    void shouldReadReportWithoutFailure() {
        var tree = readXunitReport("xunit-no-failure-block.xml");
        assertThat(getPackage(tree)).hasName("-");
        assertThat(getFirstClass(tree)).hasName("test.Tests2");
        assertThat(getFirstTest(tree).getDescription()).contains("");
        assertThat(tree.aggregateValues()).contains(new Value(Metric.TESTS, 3));
    }

    @Test
    void shouldReadReportWithInvalidStatus() {
        var tree = readXunitReport("xunit-invalid-status.xml");
        assertThat(getPackage(tree)).hasName("-");
        assertThat(getFirstClass(tree)).hasName("test.Tests2");
        assertThat(tree.aggregateValues()).contains(new Value(Metric.TESTS, 3));
    }

    @Test
    void shouldReadReportWithoutErrorMessage() {
        var tree = readXunitReport("xunit-no-message.xml");
        assertThat(getPackage(tree)).hasName("-");
        assertThat(getFirstClass(tree)).hasName("test.Tests2");
        assertThat(getFirstTest(tree).getDescription()).contains("");
        assertThat(tree.aggregateValues()).contains(new Value(Metric.TESTS, 3));
    }

    private ModuleNode readXunitReport(final String fileName) {
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
