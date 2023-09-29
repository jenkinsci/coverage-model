package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.MutationStatus;
import edu.hm.hafner.coverage.Node;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class PitestParserTest extends AbstractParserTest {
    private static final String LOOKAHEAD_STREAM = "LookaheadStream.java";
    private static final String FILTERED_LOG = "FilteredLog.java";
    private static final String ENSURE = "Ensure.java";

    @Override
    CoverageParser createParser() {
        return new PitestParser();
    }

    @Test
    void shouldReadAllMutationProperties() {
        ModuleNode tree = readReport("mutation.xml");
        assertThat(tree.getAllFileNodes()).first()
                .satisfies(file -> assertThat(file.getMutations())
                        .first().satisfies(mutation ->
                                assertThat(mutation).isDetected()
                                        .hasMethod("add")
                                        .hasSignature("(Ledu/hm/hafner/coverage/CoverageNode;)V")
                                        .hasLine(175)
                                        .hasMutator("NotExisting")
                                        .hasKillingTest("edu.hm.hafner.coverage.CoverageNodeTest.[engine:junit-jupiter]/[class:edu.hm.hafner.coverage.CoverageNodeTest]/[method:shouldAddChildren()]")
                                        .hasDescription("removed call to edu/hm/hafner/coverage/CoverageNode::setParent")
                        ));
    }

    @Test
    void shouldReadAllMutationPropertiesEvenIfXmlContainsBlocksAndIndexes() {
        ModuleNode tree = readReport("mutation-with-blocks-and-indexes.xml");
        assertThat(tree.getAllFileNodes()).first()
                .satisfies(file -> assertThat(file.getMutations())
                        .first().satisfies(mutation ->
                                assertThat(mutation).isNotDetected()
                                        .hasMethod("getFileName")
                                        .hasSignature("()Ljava/lang/String;")
                                        .hasLine(5555)
                                        .hasMutator("org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator")
                                        .hasKillingTest("")
                                        .hasDescription("replaced return value with \"\" for edu/hm/hafner/util/LookaheadStream::getFileName")
                        ));
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
        assertThat(file.getSurvivedMutationsPerLine()).containsOnlyKeys(
                238, 303, 340, 476, 620, 651);
        assertThat(file.getMutationsPerLine()).hasSize(66);
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
        assertThat(file.getSurvivedMutationsPerLine()).containsOnlyKeys(50);
        assertThat(file.getSurvivedMutationsPerLine().values()).hasSize(1)
                .first().asList()
                .hasSize(1).first()
                .isInstanceOfSatisfying(Mutation.class, this::verifyMutation);
        assertThat(file.getMissedLines()).isEmpty();
    }

    private void verifyMutation(final Mutation mutation) {
        assertThat(mutation).hasSurvived()
                .hasKillingTest("")
                .hasMethod("getFileName")
                .hasSignature("()Ljava/lang/String;")
                .hasMutator("org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator")
                .hasDescription("replaced return value with \"\" for edu/hm/hafner/util/LookaheadStream::getFileName")
                .hasLine(50);
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

        assertThat(tree.find(METHOD, "endElement(Ljavax/xml/stream/events/EndElement;)V")).isPresent().hasValueSatisfying(
                node -> assertThat(node.getValue(MUTATION))
                        .isNotEmpty().get()
                        .isInstanceOfSatisfying(Coverage.class, m -> assertThat(m).hasCovered(3).hasMissed(2)));
    }
}
