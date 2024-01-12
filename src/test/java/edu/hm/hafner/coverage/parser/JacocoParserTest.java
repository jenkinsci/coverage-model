package edu.hm.hafner.coverage.parser;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.FractionValue;
import edu.hm.hafner.coverage.LinesOfCode;
import edu.hm.hafner.coverage.MethodNode;
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
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new JacocoParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "jacoco";
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    @Test
    void shouldMergeBranches() {
        var a = getFileNode(readReport("jacoco-merge-a.xml"));
        assertThat(a).hasMissedLines(38, 39, 46, 47).hasCoveredLines(36, 37, 41, 42, 43, 49);
        verifyLineCoverage(a, 4);

        var b = getFileNode(readReport("jacoco-merge-b.xml"));
        assertThat(b).hasMissedLines(38, 39, 42, 43).hasCoveredLines(36, 37, 41, 46, 47, 49);
        verifyLineCoverage(b, 4);

        var c = getFileNode(readReport("jacoco-merge-c.xml"));
        assertThat(c).hasMissedLines(41, 42, 43, 46, 47).hasCoveredLines(36, 37, 38, 39, 49);
        verifyLineCoverage(c, 5);

        var ab = (FileNode)a.merge(b);
        assertThat(ab)
                .hasMissedLines(38, 39)
                .doesNotHaveMissedLines(36, 37, 41, 42, 43, 46, 47, 49)
                .hasCoveredLines(36, 37, 41, 42, 43, 46, 47, 49)
                .hasValues(
                        createFileCoverageForFile(2),
                        createBranchCoverage(2, 12));

        var abc = (FileNode)ab.merge(c);
        assertThat(abc)
                .doesNotHaveMissedLines(36, 37, 38, 39, 41, 42, 43, 46, 47, 49)
                .hasCoveredLines(36, 37, 38, 39, 41, 42, 43, 46, 47, 49)
                .hasValues(
                        createFileCoverageForFile(0),
                        createBranchCoverage(2, 12),
                        new CyclomaticComplexity(14));
    }

    private void verifyLineCoverage(final FileNode a, final int missed) {
        var children = a.getAll(METHOD).stream()
                .filter(m -> "<init>(II)V".equals(m.getName()))
                .collect(Collectors.toList());

        assertThat(children).hasSize(1)
                .element(0)
                .isInstanceOfSatisfying(MethodNode.class,
                        m -> assertThat(m)
                                .hasName("<init>(II)V")
                                .hasSignature("(II)V")
                                .hasValues(
                                        createLineCoverage(10 - missed, missed),
                                        createBranchCoverage(2 + 4 - missed, 2 - (4 - missed)),
                                        new CyclomaticComplexity(3)));

        assertThat(a).hasValues(createFileCoverageForFile(missed),
                createBranchCoverage(2 + 4 - missed, 2 - (4 - missed) + 10),
                new CyclomaticComplexity(14));
    }

    private Coverage createFileCoverageForFile(final int missed) {
        return createLineCoverage(12 - missed, missed + 14);
    }

    private Coverage createLineCoverage(final int covered, final int missed) {
        return new CoverageBuilder().withMetric(LINE).withCovered(covered).withMissed(missed).build();
    }

    private Coverage createBranchCoverage(final int covered, final int missed) {
        return new CoverageBuilder().withMetric(BRANCH).withCovered(covered).withMissed(missed).build();
    }

    private FileNode getFileNode(final ModuleNode a) {
        var fileNodes = a.getAllFileNodes();
        assertThat(fileNodes).hasSize(1);

        var lineRange = fileNodes.get(0);
        assertThat(lineRange)
                .hasName("LineRange.java")
                .hasRelativePath("edu/hm/hafner/util/LineRange.java");

        return lineRange;
    }

    @ParameterizedTest(name = "[{index}] Split packages after read: {0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("Read and merge two coverage reports with different packages")
    void shouldMergeProjects(final boolean splitPackages) {
        ModuleNode model = readReport("jacoco-analysis-model.xml");

        assertThat(model.getAll(PACKAGE)).extracting(Node::getName).containsExactly(
                "edu.hm.hafner.analysis.parser.dry.simian",
                "edu.hm.hafner.analysis.parser.gendarme",
                "edu.hm.hafner.analysis.parser.dry",
                "edu.hm.hafner.analysis.parser.checkstyle",
                "edu.hm.hafner.analysis.registry",
                "edu.hm.hafner.analysis.parser.findbugs",
                "edu.hm.hafner.analysis.parser.pmd",
                "edu.hm.hafner.analysis",
                "edu.hm.hafner.analysis.parser.fxcop",
                "edu.hm.hafner.analysis.parser.dry.dupfinder",
                "edu.hm.hafner.analysis.parser.jcreport",
                "edu.hm.hafner.analysis.parser.pylint",
                "edu.hm.hafner.analysis.parser.pvsstudio",
                "edu.hm.hafner.analysis.parser.dry.cpd",
                "edu.hm.hafner.util",
                "edu.hm.hafner.analysis.parser",
                "edu.hm.hafner.analysis.parser.ccm",
                "edu.hm.hafner.analysis.parser.violations");

        ModuleNode style = readReport("jacoco-codingstyle.xml");

        assertThat(style.getAll(PACKAGE)).extracting(Node::getName).containsExactly(
                "edu.hm.hafner.util");

        var builder = new CoverageBuilder().withMetric(LINE);

        var left = new ModuleNode("root");
        model.getAll(PACKAGE).forEach(p -> left.addChild(p.copyTree()));

        assertThat(left.find(PACKAGE, "edu.hm.hafner.util")).isPresent()
                .get().satisfies(p -> assertThat(p.getValue(LINE)).contains(
                        builder.withCovered(60).withTotal(62).build()));

        var right = new ModuleNode("root");
        style.getAll(PACKAGE).forEach(p -> right.addChild(p.copyTree()));

        assertThat(right.find(PACKAGE, "edu.hm.hafner.util")).isPresent()
                .get().satisfies(p -> assertThat(p.getValue(LINE)).contains(
                        builder.withCovered(294).withTotal(323).build()));

        if (splitPackages) {
            left.splitPackages();
            right.splitPackages();
        }

        var merged = left.merge(right);

        var packageName = splitPackages ? "util" : "edu.hm.hafner.util";
        assertThat(merged.find(PACKAGE, packageName)).isPresent()
                .get().satisfies(p -> assertThat(p.getValue(LINE)).contains(
                        builder.withCovered(294 + 60).withTotal(323 + 62).build()));
    }

    @Test
    void shouldReadAndSplitSubpackage() {
        ModuleNode model = readReport("file-subpackage.xml");

        model.splitPackages();
        assertThat(model.getAll(PACKAGE)).extracting(Node::getName).containsExactly(
                "util", "hafner", "hm", "edu");
    }

    @Test
    void shouldDetectMethodCoverage() {
        ModuleNode module = readReport("jacocoTestReport.xml");

        assertThat(module.getAll(PACKAGE)).hasSize(1);
        assertThat(module.findFile("CodeCoverageCategory.groovy")).isPresent().hasValueSatisfying(
                file -> assertThat(file.findClass("org.aboe026.CodeCoverageCategory")).isPresent()
                        .hasValueSatisfying(
                                classNode -> assertThat(file.getAll(METHOD).size()).isEqualTo(3)));

        var methods = module.getAll(METHOD);
        assertThat(methods).hasSize(68);
        assertThat(module.getValue(METHOD)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasTotal(68).hasCovered(68));
    }

    @Test
    void shouldConvertCodingStyleToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(FILE)).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, INSTRUCTION, BRANCH,
                COMPLEXITY, COMPLEXITY_DENSITY, COMPLEXITY_MAXIMUM, LOC);

        var builder = new CoverageBuilder();

        assertThat(tree.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(7).withMissed(3).build(),
                builder.withMetric(CLASS).withCovered(15).withMissed(3).build(),
                builder.withMetric(METHOD).withCovered(97).withMissed(5).build(),
                builder.withMetric(LINE).withCovered(294).withMissed(29).build(),
                builder.withMetric(BRANCH).withCovered(109).withMissed(7).build(),
                builder.withMetric(INSTRUCTION).withCovered(1260).withMissed(90).build(),
                new CyclomaticComplexity(160),
                new CyclomaticComplexity(6, COMPLEXITY_MAXIMUM),
                new FractionValue(COMPLEXITY_DENSITY, 160, 294 + 29),
                new LinesOfCode(294 + 29));

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util"));

        Node any = tree.getAll(FILE)
                .stream()
                .filter(n -> "Ensure.java".equals(n.getName()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Blub"));
        assertThat(any.getValue(LINE)).contains(builder.withMetric(LINE).withCovered(100).withMissed(25).build());
        assertThat(any.getValue(LOC)).contains(new LinesOfCode(125));
        assertThat(any.getValue(BRANCH)).contains(builder.withMetric(BRANCH).withCovered(40).withMissed(6).build());
        assertThat(any.getValue(COMPLEXITY)).contains(new CyclomaticComplexity(68));

        verifyCoverageMetrics(tree);

        var log = tree.findFile("TreeStringBuilder.java").orElseThrow();
        assertThat(log.getMissedLines()).containsExactly(61, 62);
        assertThat(log.getPartiallyCoveredLines()).containsExactly(entry(113, 1));
    }

    @Test
    void shouldConvertCodingStyleWithoutSourceFilenames() {
        Node tree = readReport("jacoco-codingstyle-no-sourcefilename.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(1);
        assertThat(tree.getAll(FILE)).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(18);
        assertThat(tree.getAll(METHOD)).hasSize(102);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, INSTRUCTION, BRANCH,
                COMPLEXITY, COMPLEXITY_DENSITY, COMPLEXITY_MAXIMUM, LOC);

        var builder = new CoverageBuilder();

        assertThat(tree.aggregateValues()).contains(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(7).withMissed(3).build(),
                builder.withMetric(CLASS).withCovered(15).withMissed(3).build(),
                builder.withMetric(METHOD).withCovered(97).withMissed(5).build());

        assertThat(tree.getChildren()).hasSize(1)
                .element(0)
                .satisfies(packageNode -> assertThat(packageNode).hasName("edu.hm.hafner.util"));

        Node any = tree.getAll(FILE)
                .stream()
                .filter(n -> "Ensure.java".equals(n.getName()))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException("Blub"));
        assertThat(any.getValue(LINE)).contains(builder.withMetric(LINE).withCovered(100).withMissed(25).build());
        assertThat(any.getValue(LOC)).contains(new LinesOfCode(125));
        assertThat(any.getValue(BRANCH)).contains(builder.withMetric(BRANCH).withCovered(40).withMissed(6).build());
        assertThat(any.getValue(COMPLEXITY)).contains(new CyclomaticComplexity(68));

        var log = tree.findFile("TreeStringBuilder.java").orElseThrow();
        assertThat(log.getMissedLines()).containsExactly(61, 62);
        assertThat(log.getPartiallyCoveredLines()).containsExactly(entry(113, 1));
    }

    @Test
    void shouldFilterByFiles() {
        var root = readExampleReport();

        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        assertThat(root.getAll(FILE)).hasSize(10);
        assertThat(root.getAll(CLASS)).hasSize(18);
        assertThat(root.getAll(METHOD)).hasSize(102);

        assertThat(root.aggregateValues()).contains(
                new CyclomaticComplexity(160),
                Coverage.valueOf(BRANCH, "109/116"),
                Coverage.valueOf(LINE, "294/323"),
                Coverage.valueOf(INSTRUCTION, "1260/1350"),
                new LinesOfCode(294 + 29));

        var includedNames = root.getFiles()
                .stream()
                .filter(name -> StringUtils.containsAny(name, "Ensure.java", "TreeStringBuilder.java"))
                .collect(Collectors.toList());
        var includedFiles = root.filterByFileNames(includedNames);

        assertThat(includedFiles.getAll(MODULE)).hasSize(1);
        assertThat(includedFiles.getAll(PACKAGE)).hasSize(1);
        assertThat(includedFiles.getAll(FILE)).hasSize(2);
        assertThat(includedFiles.getAll(CLASS)).hasSize(10);
        assertThat(includedFiles.getAll(METHOD)).hasSize(59);

        assertThat(includedFiles.aggregateValues()).contains(
                new CyclomaticComplexity(91),
                Coverage.valueOf(BRANCH, "57/64"),
                Coverage.valueOf(LINE, "151/178"),
                Coverage.valueOf(INSTRUCTION, "606/690"),
                new LinesOfCode(178));

        var excludedNames = root.getFiles()
                .stream()
                .filter(f -> !StringUtils.containsAny(f, "Ensure.java", "TreeStringBuilder.java"))
                .collect(Collectors.toList());
        var excludedFiles = root.filterByFileNames(excludedNames);

        assertThat(excludedFiles.getAll(MODULE)).hasSize(1);
        assertThat(excludedFiles.getAll(PACKAGE)).hasSize(1);
        assertThat(excludedFiles.getAll(FILE)).hasSize(8);
        assertThat(excludedFiles.getAll(CLASS)).hasSize(8);
        assertThat(excludedFiles.getAll(METHOD)).hasSize(43);

        assertThat(excludedFiles.aggregateValues()).contains(
                new CyclomaticComplexity(69),
                Coverage.valueOf(BRANCH, "52/52"),
                Coverage.valueOf(LINE, "143/145"),
                Coverage.valueOf(INSTRUCTION, "654/660"),
                new LinesOfCode(145));
    }

    @Test
    void shouldSplitPackages() {
        ModuleNode tree = readExampleReport();

        tree.splitPackages();

        verifyCoverageMetrics(tree);

        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        var coverage = new CoverageBuilder().withMetric(PACKAGE).withCovered(4).withMissed(0).build();
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
