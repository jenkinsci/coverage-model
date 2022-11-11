package edu.hm.hafner.metric.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
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

class JacocoParserTest {
    private static final String PROJECT_NAME = "Java coding style";

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    @Test
    void shouldConvertCodingStyleToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(FILE)).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, INSTRUCTION, BRANCH, COMPLEXITY, LOC);

        var builder = new CoverageBuilder();

        assertThat(tree.getMetricsDistribution()).containsExactly(
                entry(MODULE, builder.setMetric(MODULE).setCovered(1).setMissed(0).build()),
                entry(PACKAGE, builder.setMetric(PACKAGE).setCovered(1).setMissed(0).build()),
                entry(FILE, builder.setMetric(FILE).setCovered(7).setMissed(3).build()),
                entry(CLASS, builder.setMetric(CLASS).setCovered(15).setMissed(3).build()),
                entry(METHOD, builder.setMetric(METHOD).setCovered(97).setMissed(5).build()),
                entry(LINE, builder.setMetric(LINE).setCovered(294).setMissed(29).build()),
                entry(INSTRUCTION, builder.setMetric(INSTRUCTION).setCovered(1260).setMissed(90).build()),
                entry(BRANCH, builder.setMetric(BRANCH).setCovered(109).setMissed(7).build()),
                entry(COMPLEXITY, new CyclomaticComplexity(160)),
                entry(LOC, new LinesOfCode(294 + 29)));

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util"));

        Node any = tree.getAll(FILE)
                .stream()
                .filter(n -> n.getName().equals("Ensure.java"))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Blub"));
        assertThat(any.getValue(LINE)).contains(builder.setMetric(LINE).setCovered(100).setMissed(25).build());
        assertThat(any.getValue(LOC)).contains(new LinesOfCode(125));
        assertThat(any.getValue(BRANCH)).contains(builder.setMetric(BRANCH).setCovered(40).setMissed(6).build());
        assertThat(any.getValue(COMPLEXITY)).contains(new CyclomaticComplexity(68));

        verifyCoverageMetrics(tree);
    }

    @Test
    void shouldSplitPackages() {
        ModuleNode tree = readExampleReport();

        tree.splitPackages();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        var coverage = new CoverageBuilder().setMetric(PACKAGE).setCovered(4).setMissed(0).build();
        assertThat(tree.getMetricsDistribution()).contains(entry(PACKAGE, coverage));

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
                .hasCoveredPercentage(Fraction.getFraction(294, 294 + 29))
                .hasMissed(29)
                .hasMissedPercentage(Fraction.getFraction(29, 294 + 29))
                .hasTotal(294 + 29);

        assertThat(getCoverage(tree, BRANCH)).hasCovered(109)
                .hasCoveredPercentage(Fraction.getFraction(109, 109 + 7))
                .hasMissed(7)
                .hasMissedPercentage(Fraction.getFraction(7, 109 + 7))
                .hasTotal(109 + 7);

        assertThat(getCoverage(tree, INSTRUCTION)).hasCovered(1260)
                .hasCoveredPercentage(Fraction.getFraction(1260, 1260 + 90))
                .hasMissed(90)
                .hasMissedPercentage(Fraction.getFraction(90, 1260 + 90))
                .hasTotal(1260 + 90);

        assertThat(getCoverage(tree, MODULE)).hasCovered(1)
                .hasCoveredPercentage(Fraction.ONE)
                .hasMissed(0)
                .hasMissedPercentage(Fraction.ZERO)
                .hasTotal(1);

        assertThat(tree).hasName(PROJECT_NAME).doesNotHaveParent().isRoot().hasMetric(MODULE).hasParentName("^");
    }

    @Test
    void shouldThrowExceptionWhenAttributesAreMissing() {
        assertThatThrownBy(() -> readReport("src/test/resources/jacoco-missing-attribute.xml"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not obtain attribute 'sourcefilename' from element '<class name='edu/hm/hafner/util/NoSuchElementException'>'");
    }

    private ModuleNode readExampleReport() {
        return readReport("src/test/resources/jacoco-codingstyle.xml");
    }

    private ModuleNode readReport(final String fileName) {
        try (FileInputStream stream = new FileInputStream(fileName);
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JacocoParser().parse(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
