package edu.hm.hafner.metric.parser;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.FractionValue;
import edu.hm.hafner.metric.LinesOfCode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Percentage;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

@DefaultLocale("en")
class CoberturaParserTest extends AbstractParserTest {
    @Override
    CoberturaParser createParser() {
        return new CoberturaParser();
    }

    @Test
    void shouldReadCoberturaIssue473() {
        Node tree = readReport("cobertura-npe.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsOnly("-");
        assertThat(tree.getAll(PACKAGE)).hasSize(1).extracting(Node::getName).containsOnly("CoverageTest.Service");
        assertThat(tree.getAll(FILE)).hasSize(2).extracting(Node::getName).containsOnly("Program.cs", "Startup.cs");
        assertThat(tree.getAll(CLASS)).hasSize(2)
                .extracting(Node::getName)
                .containsOnly("Lisec.CoverageTest.Program", "Lisec.CoverageTest.Startup");

        var builder = new CoverageBuilder();

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY, COMPLEXITY_DENSITY, LOC);
        assertThat(tree.aggregateValues()).containsExactly(
                builder.setMetric(MODULE).setCovered(1).setMissed(0).build(),
                builder.setMetric(PACKAGE).setCovered(1).setMissed(0).build(),
                builder.setMetric(FILE).setCovered(2).setMissed(0).build(),
                builder.setMetric(CLASS).setCovered(2).setMissed(0).build(),
                builder.setMetric(METHOD).setCovered(4).setMissed(1).build(),
                builder.setMetric(LINE).setCovered(42).setMissed(9).build(),
                builder.setMetric(BRANCH).setCovered(3).setMissed(1).build(),
                new CyclomaticComplexity(8),
                new FractionValue(COMPLEXITY_DENSITY, 8, 42 + 9),
                new LinesOfCode(42 + 9));
    }

    @Test
    void shouldConvertCoberturaBigToTree() {
        Node root = readExampleReport();

        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        assertThat(root.getAll(FILE)).hasSize(4);
        assertThat(root.getAll(CLASS)).hasSize(5);
        assertThat(root.getAll(METHOD)).hasSize(10);

        var builder = new CoverageBuilder();

        assertThat(root).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY, COMPLEXITY_DENSITY, LOC);
        assertThat(root.aggregateValues()).containsExactly(
                builder.setMetric(MODULE).setCovered(1).setMissed(0).build(),
                builder.setMetric(PACKAGE).setCovered(1).setMissed(0).build(),
                builder.setMetric(FILE).setCovered(4).setMissed(0).build(),
                builder.setMetric(CLASS).setCovered(5).setMissed(0).build(),
                builder.setMetric(METHOD).setCovered(7).setMissed(3).build(),
                builder.setMetric(LINE).setCovered(61).setMissed(19).build(),
                builder.setMetric(BRANCH).setCovered(2).setMissed(2).build(),
                new CyclomaticComplexity(22),
                new FractionValue(COMPLEXITY_DENSITY, 22, 61 + 19),
                new LinesOfCode(61 + 19));

        assertThat(root.getChildren()).extracting(Node::getName)
                .containsExactly("-");

        verifyCoverageMetrics(root);
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
                .hasCoveredPercentage(Percentage.valueOf(61, 61 + 19))
                .hasMissed(19)
                .hasTotal(61 + 19);

        assertThat(getCoverage(tree, BRANCH))
                .hasCovered(2)
                .hasCoveredPercentage(Percentage.valueOf(2, 2 + 2))
                .hasMissed(2)
                .hasTotal(2 + 2);

        assertThat(getCoverage(tree, MODULE))
                .hasCovered(1)
                .hasCoveredPercentage(Percentage.valueOf(1, 1))
                .hasMissed(0)
                .hasTotal(1);

        assertThat(tree).hasName("-")
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName("^");
    }

    @Test
    void shouldReturnCorrectPathsInFileCoverageNodesFromCoberturaReport() {
        Node result = readReport("cobertura-lots-of-data.xml");
        assertThat(result.getAllFileNodes())
                .hasSize(19)
                .extracting(FileNode::getPath)
                .containsOnly("org/apache/commons/cli/AlreadySelectedException.java",
                        "org/apache/commons/cli/BasicParser.java",
                        "org/apache/commons/cli/CommandLine.java",
                        "org/apache/commons/cli/CommandLineParser.java",
                        "org/apache/commons/cli/GnuParser.java",
                        "org/apache/commons/cli/HelpFormatter.java",
                        "org/apache/commons/cli/MissingArgumentException.java",
                        "org/apache/commons/cli/MissingOptionException.java",
                        "org/apache/commons/cli/NumberUtils.java",
                        "org/apache/commons/cli/Option.java",
                        "org/apache/commons/cli/OptionBuilder.java",
                        "org/apache/commons/cli/OptionGroup.java",
                        "org/apache/commons/cli/Options.java",
                        "org/apache/commons/cli/ParseException.java",
                        "org/apache/commons/cli/Parser.java",
                        "org/apache/commons/cli/PatternOptionBuilder.java",
                        "org/apache/commons/cli/PosixParser.java",
                        "org/apache/commons/cli/TypeHandler.java",
                        "org/apache/commons/cli/UnrecognizedOptionException.java");
    }

    @Test
    void shouldReturnCorrectPathsInFileCoverageNodesFromPythonCoberturaReport() {
        Node result = readReport("cobertura-python.xml");
        assertThat(result.getAllFileNodes())
                .hasSize(1)
                .extracting(FileNode::getPath)
                .containsOnly("__init__.py");

        assertThat(result.getValue(LINE)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(17).hasMissed(0));
        assertThat(result.getValue(BRANCH)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(4).hasMissed(0));
        assertThat(result).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, LINE, BRANCH, LOC, COMPLEXITY, COMPLEXITY_DENSITY);

        var fileNode = result.getAllFileNodes().get(0);
        assertThat(fileNode.getLinesWithCoverage())
                .containsExactly(6, 8, 9, 10, 11, 13, 16, 25, 41, 42, 46, 48, 49, 50, 54, 55, 56, 57, 60);
        assertThat(fileNode.getMissedCounters())
                .containsExactly(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(fileNode.getCoveredCounters())
                .containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1);
    }

    private ModuleNode readExampleReport() {
        return readReport("cobertura.xml");
    }
}
