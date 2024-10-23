package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

@DefaultLocale("en")
class MetricsParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new MetricsParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "metrics";
    }

    @Test
    void shouldParseMetrics() {
        ModuleNode tree = readReport("metrics.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(FILE)).hasSize(2);
        assertThat(tree.getAll(CLASS)).hasSize(3);
        assertThat(tree.getAll(METHOD)).hasSize(6);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, CYCLOMATIC_COMPLEXITY,
                CYCLOMATIC_COMPLEXITY_MAXIMUM,
                NPATH_COMPLEXITY, NCSS, COGNITIVE_COMPLEXITY);

        assertThat(tree.aggregateValues()).contains(
                new Value(CYCLOMATIC_COMPLEXITY, 2),
                new Value(CYCLOMATIC_COMPLEXITY_MAXIMUM, 1),
                new Value(NCSS, 1000),
                new Value(COGNITIVE_COMPLEXITY, 0),
                new Value(NPATH_COMPLEXITY, 2));

        assertThat(tree).hasName("testProject");

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(packageNode ->
                        assertThat(packageNode.getValue(NCSS)).contains(new Value(NCSS, 1000)))
                .satisfies(this::checkPackageNode);
    }

    private void checkPackageNode(final Node packageNode) {
        assertThat(packageNode).hasName("edu.hm.hafner.util").hasValueMetrics(NCSS);
        assertThat(packageNode.getChildren()).hasSize(2).first()
                .satisfies(fileNode ->
                        assertThat(fileNode.getValue(NCSS)).contains(new Value(NCSS, 500)))
                .satisfies(fileNode ->
                        assertThat(fileNode).hasName("Ensure.java").hasValueMetrics(NCSS))
                .satisfies(fileNode ->
                        assertThat(fileNode.getChildren()).hasSize(2))
                .satisfies(fileNode ->
                        assertThat(fileNode.getChildren()).first().satisfies(this::checkEnsure))
                .satisfies(fileNode ->
                        assertThat(fileNode.getChildren()).element(1).satisfies(this::checkIterableCondition));
        assertThat(packageNode.getChildren()).element(1)
                .satisfies(fileNode -> assertThat(fileNode).hasName("FilteredLog.java"))
                .satisfies(this::checkFilteredLog);
    }

    private void checkIterableCondition(final Node node) {
        assertThat(node).satisfies(
                        classNode -> assertThat(classNode).hasName("edu.hm.hafner.util.IterableCondition"))
                .satisfies(classNode -> assertThat(classNode.getChildren()).hasSize(2))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(0)
                        .satisfies(methodNode -> assertThat(methodNode).hasName("IterableCondition#205"))
                        .satisfies(methodNode -> assertThat(methodNode).hasNoValueMetrics()))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(1)
                        .satisfies(methodNode -> checkMethod(methodNode, "isNotEmpty#216", COGNITIVE_COMPLEXITY, 0)));
    }

    private void checkEnsure(final Node node) {
        assertThat(node).satisfies(classNode -> assertThat(classNode).hasName("edu.hm.hafner.util.Ensure"))
                .satisfies(classNode -> assertThat(classNode).hasValueMetrics(NCSS))
                .satisfies(classNode -> assertThat(classNode.getValue(NCSS)).contains(new Value(NCSS, 149)))
                .satisfies(classNode -> assertThat(classNode.getChildren()).hasSize(1).first()
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", COGNITIVE_COMPLEXITY, 0))
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", CYCLOMATIC_COMPLEXITY, 1))
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", NCSS, 2))
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", NPATH_COMPLEXITY, 1)));
    }

    private void checkFilteredLog(final Node fileNode) {
        assertThat(fileNode.getChildren()).hasSize(1)
                .element(0)
                .satisfies(classNode ->
                        assertThat(classNode).hasName("edu.hm.hafner.util.FilteredLog").hasValueMetrics(NCSS))
                .satisfies(classNode ->
                        assertThat(classNode.getValue(NCSS)).contains(new Value(NCSS, 96)))
                .satisfies(classNode -> assertThat(classNode.getChildren()).hasSize(3))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(0)
                        .satisfies(methodNode -> checkMethod(methodNode, "FilteredLog#41", CYCLOMATIC_COMPLEXITY, 1)))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(1)
                        .satisfies(methodNode -> checkMethod(methodNode, "FilteredLog#51", NCSS, 2)))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(2)
                        .satisfies(methodNode -> checkMethod(methodNode, "FilteredLog#63", NPATH_COMPLEXITY, 1)));
    }

    private void checkMethod(final Node methodNode, final String name, final Metric metric, final int expected) {
        assertThat(methodNode).hasName(name).hasValueMetrics(metric);
        assertThat(methodNode.getValue(metric)).contains(new Value(metric, expected));
    }
}
