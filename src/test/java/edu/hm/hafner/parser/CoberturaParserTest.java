package edu.hm.hafner.parser;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageMetric;
import edu.hm.hafner.coverage.CoverageNode;
import edu.hm.hafner.coverage.FileCoverageNode;

import static edu.hm.hafner.coverage.CoverageMetric.*;
import static edu.hm.hafner.coverage.CoverageMetric.CLASS;
import static edu.hm.hafner.coverage.CoverageMetric.FILE;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class CoberturaParserTest {

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldReturnEmptyCoverageIfNotFound() {
        CoverageNode root = readExampleReport();

        assertThat(root.getCoverage(CoverageMetric.valueOf("new"))).isNotSet();
    }

    @Test
    void shouldConvertCoberturaBigToTree() {
        CoverageNode tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(5);
        assertThat(tree.getAll(FILE)).hasSize(4);
        assertThat(tree.getAll(CLASS)).hasSize(4);
        assertThat(tree.getAll(METHOD)).hasSize(10);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY)
                .hasToString("[Module] " + "cobertura-big.xml");
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(1, 0)),
                entry(PACKAGE, new Coverage(4, 1)),
                entry(FILE, new Coverage(4, 0)),
                entry(CLASS, new Coverage(4, 0)),
                entry(METHOD, new Coverage(7, 3)),
                entry(LINE, new Coverage(63, 19)),
                entry(BRANCH, new Coverage(0, 0)),
                entry(COMPLEXITY, new Coverage(22, 0)));
        assertThat(tree.getMetricsPercentages()).containsExactly(
                entry(MODULE, Fraction.ONE),
                entry(PACKAGE, Fraction.getFraction(4, 4 + 1)),
                entry(FILE, Fraction.getFraction(4, 4)),
                entry(CLASS, Fraction.getFraction(4, 4)),
                entry(METHOD, Fraction.getFraction(7, 7 + 3)),
                entry(LINE, Fraction.getFraction(63, 63 + 19)),
                entry(BRANCH, Fraction.ZERO),
                entry(COMPLEXITY, Fraction.getFraction(22, 22)));

        assertThat(tree.getChildren()).hasSize(5).element(0).satisfies(
                packageNode -> assertThat(packageNode).hasName("")
        );

        verifyCoverageMetrics(tree);
    }

    @Test
    void testAmountOfLinenumberTolines() {
        CoverageNode tree = readExampleReport();
        List<CoverageNode> nodes = tree.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        long missedBranches = 0;
        long coveredBranches = 0;
        for (CoverageNode node : nodes) {
            missedLines = missedLines + ((FileCoverageNode) node).getMissedInstructionsCount();
            coveredLines = coveredLines + ((FileCoverageNode) node).getCoveredInstructionsCount();
            missedBranches = missedBranches + ((FileCoverageNode) node).getMissedBranchesCount();
            coveredBranches = coveredBranches + ((FileCoverageNode) node).getCoveredBranchesCount();
        }

        assertThat(missedLines).isEqualTo(19);
        assertThat(coveredLines).isEqualTo(63);
        assertThat(missedBranches).isEqualTo(0);
        assertThat(coveredBranches).isEqualTo(0);
    }

    private void verifyCoverageMetrics(final CoverageNode tree) {
        assertThat(tree.getCoverage(LINE)).isSet() // 20 + 17 + 7 + 19
                .hasCovered(63) // 8 + 1 + 10
                .hasCoveredPercentage(Fraction.getFraction(63, 63 + 19))
                .hasMissed(19)
                .hasMissedPercentage(Fraction.getFraction(19, 63 + 19))
                .hasTotal(63 + 19);
        assertThat(tree.printCoverageFor(LINE)).isEqualTo("76.83%");
        assertThat(tree.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("76,83%");

        assertThat(tree.getCoverage(BRANCH)).isNotSet();

        assertThat(tree.getCoverage(COMPLEXITY)).isSet()
                .hasCovered(22)
                .hasCoveredPercentage(Fraction.getFraction(22, 22))
                .hasMissed(0)
                .hasMissedPercentage(Fraction.getFraction(0, 22))
                .hasTotal(22);
        assertThat(tree.printCoverageFor(COMPLEXITY)).isEqualTo("100.00%");
        assertThat(tree.printCoverageFor(COMPLEXITY, Locale.GERMAN)).isEqualTo("100,00%");

        assertThat(tree.getCoverage(MODULE)).isSet()
                .hasCovered(1)
                .hasCoveredPercentage(Fraction.ONE)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(1);
        assertThat(tree.printCoverageFor(MODULE)).isEqualTo("100.00%");
        assertThat(tree.printCoverageFor(MODULE, Locale.GERMAN)).isEqualTo("100,00%");

        assertThat(tree).hasName("cobertura-big.xml")
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName("^");
    }

    private CoverageNode readExampleReport() {
        CoberturaParser parser = new CoberturaParser("src/test/resources/cobertura-big.xml");
        return parser.getRootNode();
    }

}