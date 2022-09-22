package edu.hm.hafner.parser;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.model.FileNode;
import edu.hm.hafner.model.Metric;
import edu.hm.hafner.model.Node;

import static edu.hm.hafner.assertions.Assertions.*;
import static edu.hm.hafner.model.Metric.CLASS;
import static edu.hm.hafner.model.Metric.FILE;
import static edu.hm.hafner.model.Metric.*;

/**
 * Tests the class {@link JacocoParser}.
 *
 * @author Ullrich Hafner
 */
class JacocoParserTest {
    private static final String PROJECT_NAME = "Java coding style: jacoco-codingstyle.xml";

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldReturnEmptyCoverageIfNotFound() {
        Node root = readExampleReport();

        assertThat(root.getCoverage(Metric.valueOf("new"))).isNotSet();
    }

    @Test
    void shouldConvertCodingStyleToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        List<Node> files = tree.getAll(FILE);
        assertThat(files).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, INSTRUCTION, COMPLEXITY)
                .hasToString("[Module] " + PROJECT_NAME);
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(1, 0)),
                entry(PACKAGE, new Coverage(1, 0)),
                entry(FILE, new Coverage(7, 3)),
                entry(CLASS, new Coverage(15, 3)),
                entry(METHOD, new Coverage(97, 5)),
                entry(LINE, new Coverage(294, 29)),
                entry(INSTRUCTION, new Coverage(1260, 90)),
                entry(BRANCH, new Coverage(109, 7)));
        assertThat(tree.getMetricsPercentages()).containsExactly(
                entry(MODULE, Fraction.ONE),
                entry(PACKAGE, Fraction.ONE),
                entry(FILE, Fraction.getFraction(7, 7 + 3)),
                entry(CLASS, Fraction.getFraction(15, 15 + 3)),
                entry(METHOD, Fraction.getFraction(97, 97 + 5)),
                entry(LINE, Fraction.getFraction(294, 294 + 29)),
                entry(INSTRUCTION, Fraction.getFraction(1260, 1260 + 90)),
                entry(BRANCH, Fraction.getFraction(109, 109 + 7)));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util")

        );

        verifyCoverageMetrics(tree);
        assertThat(tree.getComplexity()).isEqualTo(160);
        assertThat(tree.getMutationResult()).hasKilled(0);
    }

    @Test
    void shouldSplitPackages() {
        Node tree = readExampleReport();

        tree.splitPackages();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        assertThat(tree.getMetricsDistribution()).contains(
                entry(PACKAGE, new Coverage(4, 0)));

        assertThat(tree.getChildren()).hasSize(1).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("edu")
                        .hasParent()
                        .hasParentName(PROJECT_NAME)
        );
    }

    @Test
    void shouldNotSplitPackagesIfOnWrongHierarchyNode() {
        Node tree = readExampleReport();
        Node packageNode = tree.getChildren().get(0);
        assertThat(packageNode).hasName("edu.hm.hafner.util").hasPath("edu/hm/hafner/util");

        List<Node> files = packageNode.getChildren();

        packageNode.splitPackages();
        assertThat(packageNode).hasName("edu.hm.hafner.util").hasChildren(files);
    }

    @Test
    void shouldThrowExceptionWhenObtainingAllBasicBlocks() {
        Node tree = readExampleReport();

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(LINE));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(BRANCH));
        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> tree.getAll(INSTRUCTION));
    }

    @Test
    void shouldCreatePackageName() {
        Node tree = readExampleReport();

        String fileName = "Ensure.java";
        assertThat(tree.find(FILE, fileName)).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot()
        );

        tree.splitPackages();
        assertThat(tree.find(FILE, fileName)).isNotEmpty().hasValueSatisfying(
                node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot()
        );
    }

    private void verifyCoverageMetrics(final Node tree) {
        List<Node> nodes = tree.getAll(FILE);

        long missedInstructions = 0;
        long coveredInstructions = 0;
        long missedBranches = 0;
        long coveredBranches = 0;
        for (Node node : nodes) {
            missedInstructions = missedInstructions + ((FileNode) node).getMissedInstructionsCount();
            coveredInstructions = coveredInstructions + ((FileNode) node).getCoveredInstructionsCount();
            missedBranches = missedBranches + ((FileNode) node).getMissedBranchesCount();
            coveredBranches = coveredBranches + ((FileNode) node).getCoveredBranchesCount();
        }

        assertThat(missedInstructions).isEqualTo(90);
        assertThat(coveredInstructions).isEqualTo(1260);
        assertThat(missedBranches).isEqualTo(7);
        assertThat(coveredBranches).isEqualTo(109);

        assertThat(tree.getCoverage(LINE)).isSet()
                .hasCovered(294)
                .hasCoveredPercentage(Fraction.getFraction(294, 294 + 29))
                .hasMissed(29)
                .hasMissedPercentage(Fraction.getFraction(29, 294 + 29))
                .hasTotal(294 + 29);
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("91.02%");
        assertThat(tree.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("91,02%");

        assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(109)
                .hasCoveredPercentage(Fraction.getFraction(109, 109 + 7))
                .hasMissed(7)
                .hasMissedPercentage(Fraction.getFraction(7, 109 + 7))
                .hasTotal(109 + 7);
        assertThat(tree.printCoverageFor(BRANCH)).isEqualTo("93.97%");
        assertThat(tree.printCoverageFor(BRANCH, Locale.GERMAN)).isEqualTo("93,97%");

        assertThat(tree.getCoverage(INSTRUCTION)).isSet()
                .hasCovered(1260)
                .hasCoveredPercentage(Fraction.getFraction(1260, 1260 + 90))
                .hasMissed(90)
                .hasMissedPercentage(Fraction.getFraction(90, 1260 + 90))
                .hasTotal(1260 + 90);
        assertThat(tree.printCoverageFor(INSTRUCTION)).isEqualTo("93.33%");
        assertThat(tree.printCoverageFor(INSTRUCTION, Locale.GERMAN)).isEqualTo("93,33%");

        assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredPercentage(Fraction.ONE)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");
        assertThat(tree.printCoverageFor(MODULE, Locale.GERMAN)).isEqualTo("100,00%");

        assertThat(tree).hasName(PROJECT_NAME)
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName("^");
    }

    private Node readExampleReport() {
        JacocoParser parser = new JacocoParser("src/test/resources/jacoco-codingstyle.xml");
        return parser.getRootNode();
    }
}
