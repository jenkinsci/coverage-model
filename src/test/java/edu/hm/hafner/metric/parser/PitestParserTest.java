package edu.hm.hafner.metric.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.CoverageParser;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Mutation;
import edu.hm.hafner.metric.MutationStatus;
import edu.hm.hafner.metric.Node;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

class PitestParserTest extends AbstractParserTest {
    private static final String LOOKAHEAD_STREAM = "LookaheadStream.java";
    private static final String FILTERED_LOG = "FilteredLog.java";
    private static final String ENSURE = "Ensure.java";

    @Override
    CoverageParser createParser() {
        return new PitestParser();
    }

    @Test
    void shouldMapLineCoveragesForPainting() {
        ModuleNode tree = readReport("mutations-codingstyle.xml");

        assertThat(tree.getAllFileNodes()).extracting(FileNode::getName).containsExactly("PathUtil.java",
                "SecureXmlParserFactory.java",
                "TreeStringBuilder.java",
                LOOKAHEAD_STREAM,
                FILTERED_LOG,
                ENSURE,
                "TreeString.java",
                "ResourceExtractor.java",
                "PrefixLogger.java");
        assertThat(tree.findFile(LOOKAHEAD_STREAM)).isPresent().get().satisfies(this::verifyLookaheadStream);
        assertThat(tree.findFile(FILTERED_LOG)).isPresent().get().satisfies(this::verifyFilteredLog);
        assertThat(tree.findFile(ENSURE)).isPresent().get().satisfies(this::verifyEnsure);
    }

    private void verifyEnsure(final FileNode file) {
        assertThat(file.getMutations().stream()
                .filter(mutation -> mutation.getStatus() == MutationStatus.NO_COVERAGE)
                .map(Mutation::getLine).sorted())
                .containsExactly(81, 248, 486, 631);
        assertThat(file.getMutations().stream()
                .filter(mutation -> mutation.getStatus() == MutationStatus.SURVIVED)
                .map(Mutation::getLine).sorted())
                .containsExactly(238, 303, 340, 476, 620, 651);
        assertThat(file.getPartiallyCoveredLines()).isEmpty();
        assertThat(file.getSurvivedMutations()).containsExactly(
                entry(238, 1), entry(303, 1), entry(340, 1), entry(476, 1), entry(620, 1), entry(651, 1));
        assertThat(file.getMissedLines()).containsExactly(81, 248, 486, 631);
        assertThat(file.getCoveredCounters()).containsExactly(1, 1,
                0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
                0, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        assertThat(file.getMissedCounters()).containsExactly(0, 0,
                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                1, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private void verifyFilteredLog(final FileNode file) {
        assertThat(file.getValue(MUTATION)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                c -> assertThat(c).hasCovered(15).hasMissed(2));
        assertThat(file.getLinesWithCoverage()).containsExactly(
                94, 96, 99, 103, 122, 124, 128, 137, 145, 146, 156, 165, 174);
        assertThat(file.getCoveredCounters()).containsOnly(1).hasSize(13);
        assertThat(file.getMissedCounters()).containsOnly(0).hasSize(13);
    }

    private void verifyLookaheadStream(final FileNode file) {
        assertThat(file.getValue(MUTATION)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                c -> assertThat(c).hasCovered(18).hasMissed(1));
        assertThat(file.getLinesWithCoverage()).containsExactly(
                50, 55, 65, 77, 78, 79, 81, 84, 96, 97, 99, 115, 117, 119, 121, 130);
        assertThat(file.getCoveredCounters()).containsOnly(1).hasSize(16);
        assertThat(file.getMissedCounters()).containsOnly(0).hasSize(16);
        assertThat(file.getSurvivedMutations()).containsExactly(entry(50, 1));
        assertThat(file.getMissedLines()).isEmpty();
    }

    @Test
    void shouldConvertMutationsToTree() {
        ModuleNode tree = readReport("mutations.xml");

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

        assertThat(tree.find(METHOD, "endElement")).isPresent().hasValueSatisfying(
                node -> assertThat(node.getValue(MUTATION))
                        .isNotEmpty().get()
                        .isInstanceOfSatisfying(Coverage.class, m -> assertThat(m).hasCovered(3).hasMissed(2)));
    }
}
