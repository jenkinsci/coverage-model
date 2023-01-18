package edu.hm.hafner.metric.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

class PitestParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser() {
        return new PitestParser();
    }

    @Test
    void shouldConvertMutationsToTree() {
        ModuleNode tree = readReport("/mutations.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(3).extracting(Node::getName)
                .containsExactlyInAnyOrder("edu.hm.hafner.coverage",
                        "edu.hm.hafner.metric.parser",
                        "edu.hm.hafner.metric");
        assertThat(tree.getAll(FILE)).hasSize(10).extracting(Node::getName)
                .containsExactlyInAnyOrder("CoverageNode.java",
                        "FileCoverageNode.java",
                        "CoverageLeaf.java",
                        "MethodCoverageNode.java",
                        "PackageCoverageNode.java",
                        "CoberturaParser.java",
                        "JacocoParser.java",
                        "XmlParser.java",
                        "CoverageMetric.java",
                        "Coverage.java");
        assertThat(tree.getAll(CLASS)).hasSize(10).extracting(Node::getName)
                .containsExactlyInAnyOrder("CoverageNode",
                        "FileCoverageNode",
                        "CoverageLeaf",
                        "MethodCoverageNode",
                        "PackageCoverageNode",
                        "CoberturaParser",
                        "JacocoParser",
                        "XmlParser",
                        "Metric",
                        "Coverage");
        assertThat(tree.getAll(METHOD)).hasSize(99);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, MUTATION, LINE, LOC);

        assertThat(tree.getValue(MUTATION)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(222).hasTotal(246));

        assertThat(tree.find(Metric.METHOD, "endElement")).isPresent().hasValueSatisfying(
                        node -> assertThat(node.getValue(MUTATION))
                                .isNotEmpty().get()
                                .isInstanceOfSatisfying(Coverage.class, m -> assertThat(m).hasCovered(3).hasMissed(2)));
    }
}
