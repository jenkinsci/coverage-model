package edu.hm.hafner.metric.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

class CoberturaParserTest {
    @Test
    void shouldReadCoberturaIssue473() {
        Node tree;
        try (FileInputStream stream = new FileInputStream("src/test/resources/cobertura-npe.xml");
                InputStreamReader reader = new InputStreamReader(stream)) {
            tree = new CoberturaParser().parse(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsOnly("");
        assertThat(tree.getAll(PACKAGE)).hasSize(1).extracting(Node::getName).containsOnly("CoverageTest.Service");
        assertThat(tree.getAll(FILE)).hasSize(2).extracting(Node::getName).containsOnly("Program.cs", "Startup.cs");
        assertThat(tree.getAll(CLASS)).hasSize(2).extracting(Node::getName).containsOnly("Lisec.CoverageTest.Program", "Lisec.CoverageTest.Startup");

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY);
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(MODULE, 1, 0)),
                entry(PACKAGE, new Coverage(PACKAGE, 1, 0)),
                entry(FILE, new Coverage(FILE, 2, 0)),
                entry(CLASS, new Coverage(CLASS, 2, 0)),
                entry(METHOD, new Coverage(METHOD, 4, 1)),
                entry(LINE, new Coverage(LINE, 42, 9)),
                entry(BRANCH, new Coverage(BRANCH, 3, 1)),
                entry(COMPLEXITY, new CyclomaticComplexity(8)));
    }

    @Test
    void shouldConvertCoberturaBigToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(5);
        assertThat(tree.getAll(FILE)).hasSize(4);
        assertThat(tree.getAll(CLASS)).hasSize(5);
        assertThat(tree.getAll(METHOD)).hasSize(10);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY);
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, new Coverage(MODULE, 1, 0)),
                entry(PACKAGE, new Coverage(PACKAGE, 4, 1)),
                entry(FILE, new Coverage(FILE, 4, 0)),
                entry(CLASS, new Coverage(CLASS, 5, 0)),
                entry(METHOD, new Coverage(METHOD, 7, 3)),
                entry(LINE, new Coverage(LINE, 61, 19)),
                entry(BRANCH, new Coverage(BRANCH, 2, 2)),
                entry(COMPLEXITY, new CyclomaticComplexity(22)));

        assertThat(tree.getChildren()).extracting(Node::getName)
                .hasSize(5)
                .containsOnly("-");

        verifyCoverageMetrics(tree);
    }

    @Test
    void testAmountOfLinenumberTolines() {
        Node tree = readExampleReport();
        List<Node> nodes = tree.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        for (Node node : nodes) {
            missedLines = missedLines + ((FileNode) node).getMissedLinesCount();
            coveredLines = coveredLines + ((FileNode) node).getCoveredLinesCount();
        }

        assertThat(missedLines).isEqualTo(19);
        assertThat(coveredLines).isEqualTo(61);
    }

    @Test
    void shouldHaveOneSource() {
        ModuleNode tree = readExampleReport();

        assertThat(tree.getSources().size()).isOne();
        assertThat(tree.getSources().get(0)).isEqualTo("/app/app/code/Invocare/InventoryBranch");
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage)node.getValue(metric).get();
    }

    private void verifyCoverageMetrics(final Node tree) {
        assertThat(getCoverage(tree, LINE))
                .hasCovered(61)
                .hasCoveredPercentage(Fraction.getFraction(61, 61 + 19))
                .hasMissed(19)
                .hasMissedPercentage(Fraction.getFraction(19, 61 + 19))
                .hasTotal(61 + 19);

        assertThat(getCoverage(tree, BRANCH))
                .hasCovered(2)
                .hasCoveredPercentage(Fraction.getFraction(2, 2 + 2))
                .hasMissed(2)
                .hasMissedPercentage(Fraction.getFraction(2, 2 + 2))
                .hasTotal(2 + 2);

        assertThat(getCoverage(tree, MODULE))
                .hasCovered(1)
                .hasCoveredPercentage(Fraction.ONE)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(1);

        assertThat(tree).hasName("")
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName("^");
    }

    private ModuleNode readExampleReport() {
        try (FileInputStream stream = new FileInputStream("src/test/resources/cobertura.xml");
                InputStreamReader reader = new InputStreamReader(stream)) {
            return new CoberturaParser().parse(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
