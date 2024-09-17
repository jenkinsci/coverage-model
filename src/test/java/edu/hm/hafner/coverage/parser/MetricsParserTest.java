package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.IntegerValue;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.assertions.Assertions;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
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
    void simpleMetrics() {
        ModuleNode tree = readReport("metrics.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(FILE)).hasSize(2);
        assertThat(tree.getAll(CLASS)).hasSize(3);
        assertThat(tree.getAll(METHOD)).hasSize(6);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, COMPLEXITY, COMPLEXITY_MAXIMUM,
                NPATH_COMPLEXITY, NCSS, COGNITIVE_COMPLEXITY);

        assertThat(tree).hasName("testProject");

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util"))
                .satisfies(packageNode -> assertThat(packageNode).hasValueMetrics(NCSS))
                .satisfies(packageNode -> assertThat(
                        ((IntegerValue) packageNode.getValue(NCSS).get()).getValue()).isEqualTo(1000))
                .satisfies(packageNode -> assertThat(packageNode.getChildren()).hasSize(2))
                .satisfies(packageNode -> assertThat(packageNode.getChildren()).element(0)
                        .satisfies(fileNode -> assertThat(fileNode).hasName("Ensure.java"))
                        .satisfies(fileNode -> assertThat(fileNode).hasValueMetrics(NCSS))
                        .satisfies(fileNode -> assertThat(
                                ((IntegerValue) fileNode.getValue(NCSS).get()).getValue()).isEqualTo(500))
                        .satisfies(fileNode -> assertThat(fileNode.getChildren()).hasSize(2))
                        .satisfies(
                                fileNode -> assertThat(fileNode.getChildren()).element(0).satisfies(this::checkEnsure))
                        .satisfies(fileNode -> assertThat(fileNode.getChildren()).element(1)
                                .satisfies(this::checkIterableCondition)))
                .satisfies(packageNode -> assertThat(packageNode.getChildren()).element(1)
                        .satisfies(fileNode -> assertThat(fileNode).hasName("FilteredLog.java"))
                        .satisfies(this::checkFilteredLog));
    }

    private void checkIterableCondition(final Node node) {
        assertThat(node).satisfies(
                        classNode -> Assertions.assertThat(classNode).hasName("edu.hm.hafner.util.IterableCondition"))
                .satisfies(classNode -> assertThat(classNode.getChildren()).hasSize(2))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(0)
                        .satisfies(methodNode -> Assertions.assertThat(methodNode).hasName("IterableCondition#205"))
                        .satisfies(methodNode -> Assertions.assertThat(methodNode).hasNoValueMetrics()))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(1)
                        .satisfies(methodNode -> checkMethod(methodNode, "isNotEmpty#216", COGNITIVE_COMPLEXITY, 0)));
    }

    private void checkEnsure(final Node node) {
        assertThat(node).satisfies(classNode -> Assertions.assertThat(classNode).hasName("edu.hm.hafner.util.Ensure"))
                .satisfies(classNode -> Assertions.assertThat(classNode).hasValueMetrics(NCSS))
                .satisfies(
                        classNode -> assertThat(((IntegerValue) classNode.getValue(NCSS).get()).getValue()).isEqualTo(
                                149))
                .satisfies(classNode -> assertThat(classNode.getChildren()).hasSize(1)
                        .element(0)
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", COGNITIVE_COMPLEXITY, 0))
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", COMPLEXITY, 1))
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", NCSS, 2))
                        .satisfies(methodNode -> checkMethod(methodNode, "that#47", NPATH_COMPLEXITY, 1)));
    }

    private void checkFilteredLog(final Node fileNode) {
        assertThat(fileNode.getChildren()).hasSize(1)
                .element(0)
                .satisfies(classNode -> Assertions.assertThat(classNode).hasName("edu.hm.hafner.util.FilteredLog"))
                .satisfies(classNode -> assertThat(classNode).hasValueMetrics(NCSS))
                .satisfies(
                        classNode -> assertThat(((IntegerValue) classNode.getValue(NCSS).get()).getValue()).isEqualTo(
                                96))
                .satisfies(classNode -> assertThat(classNode.getChildren()).hasSize(3))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(0)
                        .satisfies(methodNode -> checkMethod(methodNode, "FilteredLog#41", COMPLEXITY, 1)))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(1)
                        .satisfies(methodNode -> checkMethod(methodNode, "FilteredLog#51", NCSS, 2)))
                .satisfies(classNode -> assertThat(classNode.getChildren()).element(2)
                        .satisfies(methodNode -> checkMethod(methodNode, "FilteredLog#63", NPATH_COMPLEXITY, 1)));
    }

    private void checkMethod(final Node methodNode, final String name, final Metric metric, final int expected) {
        assertThat(methodNode).hasName(name);
        assertThat(methodNode).hasValueMetrics(metric);
        assertThat(((IntegerValue) methodNode.getValue(metric).get()).getValue()).isEqualTo(expected);
    }
}
