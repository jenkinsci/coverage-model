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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.LinesOfCode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
            missedInstructions = missedInstructions + ((FileNode) node).getMissedInstructionsCount();
            coveredInstructions = coveredInstructions + ((FileNode) node).getCoveredInstructionsCount();
            missedBranches = missedBranches + ((FileNode) node).getMissedBranchesCount();
            coveredBranches = coveredBranches + ((FileNode) node).getCoveredBranchesCount();
            missedLines = missedLines + ((FileNode) node).getMissedLinesCount();
            coveredLines = coveredLines + ((FileNode) node).getCoveredLinesCount();
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

    @Test @Disabled("How should we handle these different results?")
    void shouldCreateTreeOfAnalysisModel() {
        Node tree;
        try (FileInputStream stream = new FileInputStream(
                "src/test/resources/jacoco-analysis-model.xml"); InputStreamReader reader = new InputStreamReader(stream)) {
            tree = new JacocoParser().parse(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        assertThat(tree).hasName("Static Analysis Model and Parsers");

        assertThat(getCoverage(tree, LINE)).hasCovered(5531)
                .hasCoveredPercentage(Fraction.getFraction(5531, 5531 + 267))
                .hasMissed(267)
                .hasMissedPercentage(Fraction.getFraction(267, 5531 + 267))
                .hasTotal(5531 + 267);

        assertThat(getCoverage(tree, BRANCH)).hasCovered(1544)
                .hasCoveredPercentage(Fraction.getFraction(1544, 1544 + 205))
                .hasMissed(205)
                .hasMissedPercentage(Fraction.getFraction(205, 1544 + 205))
                .hasTotal(1544 + 205);

        assertThat(getCoverage(tree, INSTRUCTION)).hasCovered(24057)
                .hasCoveredPercentage(Fraction.getFraction(24057, 24057 + 974))
                .hasMissed(974)
                .hasMissedPercentage(Fraction.getFraction(974, 24057 + 974))
                .hasTotal(24057 + 974);

        assertThat(getCoverage(tree, METHOD)).hasCovered(1615)
                .hasCoveredPercentage(Fraction.getFraction(1615, 1615 + 45))
                .hasMissed(45)
                .hasMissedPercentage(Fraction.getFraction(45, 1615 + 45))
                .hasTotal(1615 + 45);
        // 3 empty classes not counted in report but parser
        assertThat(getCoverage(tree, CLASS)).hasCovered(333)
                .hasCoveredPercentage(Fraction.getFraction(333, 333 + 1))
                .hasMissed(1)
                .hasMissedPercentage(Fraction.getFraction(1, 333 + 1))
                .hasTotal(333 + 1);

        assertThat(getCoverage(tree, FILE)).hasCovered(6083)
                .hasCoveredPercentage(Fraction.getFraction(6083, 6083 + 285))
                .hasMissed(285)
                .hasMissedPercentage(Fraction.getFraction(285, 6083 + 285))
                .hasTotal(6083 + 285);

        assertThat(getCoverage(tree, PACKAGE)).hasCovered(6083)
                .hasCoveredPercentage(Fraction.getFraction(6083, 6083 + 285))
                .hasMissed(285)
                .hasMissedPercentage(Fraction.getFraction(285, 6083 + 285))
                .hasTotal(6083 + 285);

    }

    @Test
    void shouldThrowExceptionWhenAttributesAreMissing() {
        try (FileInputStream stream = new FileInputStream("src/test/resources/jacoco-missing-attribute.xml");
                InputStreamReader reader = new InputStreamReader(stream)) {

            assertThatThrownBy(() -> new JacocoParser().parse(reader))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Could not obtain attribute 'sourcefilename' from element '<class name='edu/hm/hafner/util/NoSuchElementException'>'");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ModuleNode readExampleReport() {
        try (FileInputStream stream = new FileInputStream("src/test/resources/jacoco-codingstyle.xml");
                InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new JacocoParser().parse(reader);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
