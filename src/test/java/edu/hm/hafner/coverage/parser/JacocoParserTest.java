package edu.hm.hafner.coverage.parser;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FractionValue;
import edu.hm.hafner.coverage.LinesOfCode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Percentage;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

@DefaultLocale("en")
class JacocoParserTest extends AbstractParserTest {
    private static final String PROJECT_NAME = "Java coding style";

    @Override
    CoverageParser createParser() {
        return new JacocoParser();
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    @Test
    void shouldDetectMethodCoverage() {
        ModuleNode module = readReport("jacocoTestReport.xml");

        assertThat(module.getAll(PACKAGE)).hasSize(1);
        assertThat(module.findFile("CodeCoverageCategory.groovy")).isPresent().hasValueSatisfying(
                file -> assertThat(file.findClass("org/aboe026/CodeCoverageCategory")).isPresent()
                        .hasValueSatisfying(
                                classNode -> assertThat(file.getAll(METHOD).size()).isEqualTo(3)));

        var methods = module.getAll(METHOD);
        assertThat(methods).hasSize(68);
        assertThat(module.getValue(METHOD)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasTotal(68).hasCovered(68));
    }

    @ParameterizedTest(name = "Read and parse coding style report \"{0}\" to tree of nodes")
    @ValueSource(strings = {"jacoco-codingstyle.xml", "jacoco-codingstyle-no-sourcefilename.xml"})
    void shouldConvertCodingStyleToTree(final String fileName) {
        Node tree = readReport(fileName);

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(FILE)).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, INSTRUCTION, BRANCH,
                COMPLEXITY, COMPLEXITY_DENSITY, COMPLEXITY_MAXIMUM, LOC);

        var builder = new CoverageBuilder();

        assertThat(tree.aggregateValues()).containsExactly(
                builder.setMetric(MODULE).setCovered(1).setMissed(0).build(),
                builder.setMetric(PACKAGE).setCovered(1).setMissed(0).build(),
                builder.setMetric(FILE).setCovered(7).setMissed(3).build(),
                builder.setMetric(CLASS).setCovered(15).setMissed(3).build(),
                builder.setMetric(METHOD).setCovered(97).setMissed(5).build(),
                builder.setMetric(LINE).setCovered(294).setMissed(29).build(),
                builder.setMetric(BRANCH).setCovered(109).setMissed(7).build(),
                builder.setMetric(INSTRUCTION).setCovered(1260).setMissed(90).build(),
                new CyclomaticComplexity(160),
                new FractionValue(COMPLEXITY_DENSITY, 160, 294 + 29),
                new CyclomaticComplexity(6, COMPLEXITY_MAXIMUM),
                new LinesOfCode(294 + 29));

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util"));

        Node any = tree.getAll(FILE)
                .stream()
                .filter(n -> "Ensure.java".equals(n.getName()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Blub"));
        assertThat(any.getValue(LINE)).contains(builder.setMetric(LINE).setCovered(100).setMissed(25).build());
        assertThat(any.getValue(LOC)).contains(new LinesOfCode(125));
        assertThat(any.getValue(BRANCH)).contains(builder.setMetric(BRANCH).setCovered(40).setMissed(6).build());
        assertThat(any.getValue(COMPLEXITY)).contains(new CyclomaticComplexity(68));

        verifyCoverageMetrics(tree);

        var log = tree.findFile("TreeStringBuilder.java").orElseThrow();
        assertThat(log.getMissedLines()).containsExactly(61, 62);
        assertThat(log.getPartiallyCoveredLines()).containsExactly(entry(113, 1));
    }

    @Test
    void shouldSplitPackages() {
        ModuleNode tree = readExampleReport();

        tree.splitPackages();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        var coverage = new CoverageBuilder().setMetric(PACKAGE).setCovered(4).setMissed(0).build();
        assertThat(tree.aggregateValues()).contains(coverage);

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(
                        packageNode -> assertThat(packageNode).hasName("edu").hasParent().hasParentName(PROJECT_NAME));
    }

    @Test
    void shouldCreatePackageName() {
        ModuleNode tree = readExampleReport();

        String fileName = "Ensure.java";
        assertThat(tree.find(FILE, fileName)).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot());

        tree.splitPackages();
        assertThat(tree.find(FILE, fileName)).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node).hasName(fileName)
                        .hasParentName("edu.hm.hafner.util")
                        .hasParent()
                        .isNotRoot());
    }

    private void verifyCoverageMetrics(final Node tree) {
        List<Node> nodes = tree.getAll(FILE);

        long missedInstructions = 0;
        long coveredInstructions = 0;
        long missedBranches = 0;
        long coveredBranches = 0;
        long missedLines = 0;
        long coveredLines = 0;
        for (Node node : nodes) {
            var instructionCoverage = (Coverage) node.getValue(INSTRUCTION).orElse(Coverage.nullObject(INSTRUCTION));
            missedInstructions = missedInstructions + instructionCoverage.getMissed();
            coveredInstructions = coveredInstructions + instructionCoverage.getCovered();
            var branchCoverage = (Coverage) node.getValue(BRANCH).orElse(Coverage.nullObject(BRANCH));
            missedBranches = missedBranches + branchCoverage.getMissed();
            coveredBranches = coveredBranches + branchCoverage.getCovered();
            var lineCoverage = (Coverage) node.getValue(LINE).orElse(Coverage.nullObject(LINE));
            missedLines = missedLines + lineCoverage.getMissed();
            coveredLines = coveredLines + lineCoverage.getCovered();
        }

        assertThat(missedInstructions).isEqualTo(90);
        assertThat(coveredInstructions).isEqualTo(1260);
        assertThat(missedBranches).isEqualTo(7);
        assertThat(coveredBranches).isEqualTo(109);
        assertThat(missedLines).isEqualTo(29);
        assertThat(coveredLines).isEqualTo(294);

        assertThat(getCoverage(tree, LINE)).hasCovered(294)
                .hasCoveredPercentage(Percentage.valueOf(294, 294 + 29))
                .hasMissed(29)
                .hasTotal(294 + 29);

        assertThat(getCoverage(tree, BRANCH)).hasCovered(109)
                .hasCoveredPercentage(Percentage.valueOf(109, 109 + 7))
                .hasMissed(7)
                .hasTotal(109 + 7);

        assertThat(getCoverage(tree, INSTRUCTION)).hasCovered(1260)
                .hasCoveredPercentage(Percentage.valueOf(1260, 1260 + 90))
                .hasMissed(90)
                .hasTotal(1260 + 90);

        assertThat(getCoverage(tree, MODULE)).hasCovered(1)
                .hasCoveredPercentage(Percentage.valueOf(1, 1))
                .hasMissed(0)
                .hasTotal(1);

        assertThat(tree).hasName(PROJECT_NAME).doesNotHaveParent().isRoot().hasMetric(MODULE).hasParentName("^");
    }

    private ModuleNode readExampleReport() {
        return readReport("jacoco-codingstyle.xml");
    }
}
