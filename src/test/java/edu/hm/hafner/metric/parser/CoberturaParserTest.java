package edu.hm.hafner.metric.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.LinesOfCode;
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
        Node tree = readReport("src/test/resources/cobertura-npe.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsOnly("");
        assertThat(tree.getAll(PACKAGE)).hasSize(1).extracting(Node::getName).containsOnly("CoverageTest.Service");
        assertThat(tree.getAll(FILE)).hasSize(2).extracting(Node::getName).containsOnly("Program.cs", "Startup.cs");
        assertThat(tree.getAll(CLASS)).hasSize(2)
                .extracting(Node::getName)
                .containsOnly("Lisec.CoverageTest.Program", "Lisec.CoverageTest.Startup");

        var builder = new CoverageBuilder();

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY, LOC);
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, builder.setMetric(MODULE).setCovered(1).setMissed(0).build()),
                entry(PACKAGE, builder.setMetric(PACKAGE).setCovered(1).setMissed(0).build()),
                entry(FILE, builder.setMetric(FILE).setCovered(2).setMissed(0).build()),
                entry(CLASS, builder.setMetric(CLASS).setCovered(2).setMissed(0).build()),
                entry(METHOD, builder.setMetric(METHOD).setCovered(4).setMissed(1).build()),
                entry(LINE, builder.setMetric(LINE).setCovered(42).setMissed(9).build()),
                entry(BRANCH, builder.setMetric(BRANCH).setCovered(3).setMissed(0).build()),
                entry(COMPLEXITY, new CyclomaticComplexity(8)),
                entry(LOC, new LinesOfCode(42 + 9)));
    }

    @Test
    void shouldConvertCoberturaBigToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(5);
        assertThat(tree.getAll(FILE)).hasSize(4);
        assertThat(tree.getAll(CLASS)).hasSize(5);
        assertThat(tree.getAll(METHOD)).hasSize(10);

        var builder = new CoverageBuilder();

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY, LOC);
        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, builder.setMetric(MODULE).setCovered(1).setMissed(0).build()),
                entry(PACKAGE, builder.setMetric(PACKAGE).setCovered(4).setMissed(1).build()),
                entry(FILE, builder.setMetric(FILE).setCovered(4).setMissed(0).build()),
                entry(CLASS, builder.setMetric(CLASS).setCovered(5).setMissed(0).build()),
                entry(METHOD, builder.setMetric(METHOD).setCovered(7).setMissed(3).build()),
                entry(LINE, builder.setMetric(LINE).setCovered(61).setMissed(19).build()),
                entry(BRANCH, builder.setMetric(BRANCH).setCovered(2).setMissed(2).build()),
                entry(COMPLEXITY, new CyclomaticComplexity(22)),
                entry(LOC, new LinesOfCode(61 + 19)));

        assertThat(tree.getChildren()).extracting(Node::getName)
                .hasSize(5)
                .containsOnly("-");

        verifyCoverageMetrics(tree);
    }

    @Test
    void shouldComputeAmountOfLineNumberToLines() {
        Node tree = readExampleReport();
        List<Node> nodes = tree.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        for (Node node : nodes) {
            var lineCoverage = (Coverage) node.getValue(LINE).get();
            missedLines = missedLines + lineCoverage.getMissed();
            coveredLines = coveredLines + lineCoverage.getCovered();
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
        return (Coverage) node.getValue(metric).get();
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
        return readReport("src/test/resources/cobertura.xml");
    }

    private ModuleNode readReport(final String fileName) {
        try (FileInputStream stream = new FileInputStream(fileName);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new CoberturaParser().parse(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
