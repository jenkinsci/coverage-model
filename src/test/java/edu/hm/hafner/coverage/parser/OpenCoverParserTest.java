package edu.hm.hafner.coverage.parser;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.Issue;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.MethodNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

@DefaultLocale("en")
class OpenCoverParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new OpenCoverParser(processingMode);
    }

    @Test
    void shouldReadReport() {
        readExampleReport();
    }

    @Test
    void shouldReadEmptyModules() {
        readReport("opencover-empty-module.xml", new OpenCoverParser(ProcessingMode.IGNORE_ERRORS));
    }

    @Test
    void shouldReadWithMissingModuleName() {
        readReport("opencover-missing-module-name.xml", new OpenCoverParser(ProcessingMode.IGNORE_ERRORS));
    }

    @Test
    void shouldReadWithMissingSourceLine() {
        readReport("opencover-missing-source-line.xml", new OpenCoverParser(ProcessingMode.IGNORE_ERRORS));
    }

    @Test
    void shouldCreatePackageName() {
        ModuleNode tree = readExampleReport();
        assertThat(tree.find(PACKAGE, "-")).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node).hasName("-")
                        .hasParentName("-.MyLogging")
                        .hasParent()
                        .isNotRoot());
        String fileName = "MyLogging.FancyClass.cs";
        assertThat(tree.find(FILE, fileName)).isNotEmpty()
                .hasValueSatisfying(node -> assertThat(node).hasName(fileName)
                        .hasParentName("-")
                        .hasParent()
                        .isNotRoot());
    }

    @Test
    void shouldFilterByFiles() {
        var root = readExampleReport();
        assertThat(root.getAll(MODULE)).hasSize(2);
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        assertThat(root.getAll(FILE)).hasSize(3);
        assertThat(root.getAll(CLASS)).hasSize(3);
        assertThat(root.getAll(METHOD)).hasSize(21);

        assertThat(root.aggregateValues()).contains(
                Coverage.valueOf(MODULE, "2/2"),
                Coverage.valueOf(PACKAGE, "1/1"),
                Coverage.valueOf(METHOD, "19/21"),
                Coverage.valueOf(BRANCH, "35/48"),
                Coverage.valueOf(INSTRUCTION, "122/138"),
                new Value(CYCLOMATIC_COMPLEXITY, 61),
                new Value(CYCLOMATIC_COMPLEXITY_MAXIMUM, 16),
                new Value(LOC, 138));
        var fileNode = getFileNode(root);
        assertThat(fileNode).hasMissedLines(32).hasCoveredLines(16, 30, 34, 36, 38, 40, 51, 127, 161, 188, 197, 218, 226);
        verifyCoverageMetrics(root);
        verifyLineCoverage(fileNode);
    }

    @Test
    void shouldDetectMethodCoverage() {
        ModuleNode module = readExampleReport();

        assertThat(module.getAll(PACKAGE)).hasSize(1);
        assertThat(module.findFile("MyLogging.FancyClass.cs")).isPresent().hasValueSatisfying(
                file -> assertThat(file.findClass("MyLogging.FancyClass")).isPresent()
                        .hasValueSatisfying(
                                classNode -> assertThat(file.getAll(METHOD).size()).isEqualTo(14)));

        var methods = module.getAll(METHOD);
        assertThat(methods).hasSize(21);
        assertThat(module.getValue(METHOD)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasTotal(21).hasCovered(19));
    }

    @ParameterizedTest
    @Issue("JENKINS-72595")
    @ValueSource(strings = {"opencover-reporttotestsourcefiles.xml", "opencover-with-bom.xml"})
    void shouldReportTestSourceFiles(final String fileName) {
        ModuleNode module = readReport(fileName);
        assertThat(module.getAll(MODULE)).hasSize(2);
        assertThat(module.getAll(PACKAGE)).hasSize(1);
        assertThat(module.getAll(FILE)).hasSize(1);
        assertThat(module.getAll(CLASS)).hasSize(1);
        assertThat(module.getAll(METHOD)).hasSize(1);

        assertThat(module.aggregateValues()).contains(
                Coverage.valueOf(MODULE, "2/2"),
                Coverage.valueOf(PACKAGE, "1/1"),
                Coverage.valueOf(METHOD, "1/1"),
                Coverage.valueOf(BRANCH, "3/6"),
                Coverage.valueOf(INSTRUCTION, "9/15"),
                new Value(CYCLOMATIC_COMPLEXITY, 6),
                new Value(CYCLOMATIC_COMPLEXITY_MAXIMUM, 6),
                new Value(LOC, 15));
    }

    @Test
    void shouldReportWithSkippedModules() {
        ModuleNode module = readReport("opencover-withskippedmodules.xml");
        assertThat(module.getAll(MODULE)).hasSize(2);
        assertThat(module.getAll(PACKAGE)).hasSize(1);
        assertThat(module.getAll(FILE)).hasSize(25);
        assertThat(module.getAll(CLASS)).hasSize(15);
        assertThat(module.getAll(METHOD)).hasSize(103);

        assertThat(module.aggregateValues()).contains(
                Coverage.valueOf(MODULE, "2/2"),
                Coverage.valueOf(PACKAGE, "1/1"),
                Coverage.valueOf(METHOD, "90/103"),
                Coverage.valueOf(BRANCH, "322/379"),
                Coverage.valueOf(INSTRUCTION, "807/826"),
                new Value(CYCLOMATIC_COMPLEXITY, 256),
                new Value(CYCLOMATIC_COMPLEXITY_MAXIMUM, 18),
                new Value(LOC, 826));
    }

    private void verifyLineCoverage(final FileNode a) {
        var children = a.getAll(METHOD).stream()
                .filter(m -> "System.Boolean MyLogging.FancyClass::get_IsMyCodeWrittenWell()System.Boolean MyLogging.FancyClass::get_IsMyCodeWrittenWell()".equals(m.getName()))
                .collect(Collectors.toList());

        assertThat(children).hasSize(1)
                .element(0)
                .isInstanceOfSatisfying(MethodNode.class,
                        m -> assertThat(m)
                                .hasName("System.Boolean MyLogging.FancyClass::get_IsMyCodeWrittenWell()System.Boolean MyLogging.FancyClass::get_IsMyCodeWrittenWell()")
                                .hasSignature("System.Boolean MyLogging.FancyClass::get_IsMyCodeWrittenWell()")
                                .hasValues(
                                        createLineCoverage(1, 0),
                                        createBranchCoverage(1, 1),
                                        new Value(CYCLOMATIC_COMPLEXITY, 2)));
    }

    private Coverage createBranchCoverage(final int covered, final int missed) {
        return new CoverageBuilder().withMetric(BRANCH).withCovered(covered).withMissed(missed).build();
    }

    private Coverage createLineCoverage(final int covered, final int missed) {
        return new CoverageBuilder().withMetric(LINE).withCovered(covered).withMissed(missed).build();
    }

    private FileNode getFileNode(final ModuleNode a) {
        var fileNodes = a.getAllFileNodes();
        assertThat(fileNodes).hasSize(3);

        var lineRange = fileNodes.get(0);
        assertThat(lineRange)
                .hasName("MyLogging.FancyClass.cs")
                .hasRelativePath("C:/temp/opencovertests/MyLogging.FancyClass.cs");

        return lineRange;
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

        assertThat(missedInstructions).isEqualTo(16L);
        assertThat(coveredInstructions).isEqualTo(122L);
        assertThat(missedBranches).isEqualTo(13L);
        assertThat(coveredBranches).isEqualTo(35L);
        assertThat(missedLines).isEqualTo(16L);
        assertThat(coveredLines).isEqualTo(122L);
    }

    private ModuleNode readExampleReport() {
        return readReport("opencover.xml", new OpenCoverParser());
    }

    @Override
    protected String getFolder() {
        return "opencover";
    }
}
