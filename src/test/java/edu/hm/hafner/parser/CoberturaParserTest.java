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

import static edu.hm.hafner.coverage.assertions.Assertions.*;
import static edu.hm.hafner.model.Metric.CLASS;
import static edu.hm.hafner.model.Metric.FILE;
import static edu.hm.hafner.model.Metric.*;

class CoberturaParserTest {

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
    void shouldConvertCoberturaBigToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(5);
        assertThat(tree.getAll(FILE)).hasSize(4);
        assertThat(tree.getAll(CLASS)).hasSize(5);
        assertThat(tree.getAll(METHOD)).hasSize(10);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY)
                .hasToString("[Module] " + "cobertura.xml");
        assertThat(tree.getCoverageMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(1, 0)),
                entry(PACKAGE, new Coverage(4, 1)),
                entry(FILE, new Coverage(4, 0)),
                entry(CLASS, new Coverage(5, 0)),
                entry(METHOD, new Coverage(7, 3)),
                entry(LINE, new Coverage(61, 19)),
                entry(BRANCH, new Coverage(2, 2)));
        assertThat(tree.getCoverageMetricsPercentages()).containsExactly(
                entry(MODULE, Fraction.ONE),
                entry(PACKAGE, Fraction.getFraction(4, 4 + 1)),
                entry(FILE, Fraction.getFraction(4, 4)),
                entry(CLASS, Fraction.getFraction(5, 5)),
                entry(METHOD, Fraction.getFraction(7, 7 + 3)),
                entry(LINE, Fraction.getFraction(61, 61 + 19)),
                entry(BRANCH, Fraction.getFraction(2, 2 + 2)));

        assertThat(tree.getChildren()).hasSize(5).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("")
        );

        verifyCoverageMetrics(tree);
        assertThat(tree.getComplexity()).isEqualTo(22);
        assertThat(tree.getMutationResult()).hasKilled(0);
    }

    @Test
    void testAmountOfLinenumberTolines() {
        Node tree = readExampleReport();
        List<Node> nodes = tree.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        long missedBranches = 0;
        long coveredBranches = 0;
        for (Node node : nodes) {
            missedLines = missedLines + ((FileNode) node).getMissedInstructionsCount();
            coveredLines = coveredLines + ((FileNode) node).getCoveredInstructionsCount();
            missedBranches = missedBranches + ((FileNode) node).getMissedBranchesCount();
            coveredBranches = coveredBranches + ((FileNode) node).getCoveredBranchesCount();
        }

        assertThat(missedLines).isEqualTo(19);
        assertThat(coveredLines).isEqualTo(61);
        assertThat(missedBranches).isEqualTo(1);
        assertThat(coveredBranches).isEqualTo(1);
    }

    @Test
    void shouldHaveOneSource() {
        Node tree = readExampleReport();

        assertThat(tree.getSources().size()).isOne();
        assertThat(tree.getSources().get(0)).isEqualTo("/app/app/code/Invocare/InventoryBranch");
    }

    private void verifyCoverageMetrics(final Node tree) {
        assertThat(tree.getCoverage(LINE)).isSet()
                .hasCovered(61)
                .hasCoveredPercentage(Fraction.getFraction(61, 61 + 19))
                .hasMissed(19)
                .hasMissedPercentage(Fraction.getFraction(19, 61 + 19))
                .hasTotal(61 + 19);
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("76.25%");
        assertThat(tree.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("76,25%");

        assertThat(tree.getCoverage(BRANCH)).isSet()
                .hasCovered(2)
                .hasCoveredPercentage(Fraction.getFraction(2, 2 + 2))
                .hasMissed(2)
                .hasMissedPercentage(Fraction.getFraction(2, 2 + 2))
                .hasTotal(2 + 2);
        assertThat(tree.printCoverageFor(BRANCH)).isEqualTo("50.00%");
        assertThat(tree.printCoverageFor(BRANCH, Locale.GERMAN)).isEqualTo("50,00%");

        assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredPercentage(Fraction.ONE)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");
        assertThat(tree.printCoverageFor(MODULE, Locale.GERMAN)).isEqualTo("100,00%");

        assertThat(tree).hasName("cobertura.xml")
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName("^");
    }

    private Node readExampleReport() {
        CoberturaParser parser = new CoberturaParser("src/test/resources/cobertura.xml");
        return parser.getRootNode();
    }

}