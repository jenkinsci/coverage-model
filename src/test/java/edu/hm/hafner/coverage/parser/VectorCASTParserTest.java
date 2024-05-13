package edu.hm.hafner.coverage.parser;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
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
class VectorCASTParserTest extends AbstractParserTest {
    @Override
    VectorCASTParser createParser(final ProcessingMode processingMode) {
        return new VectorCASTParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "vectorcast";
    }
    
/*     @Test
    void shouldIgnoreMissingConditionAttribute() {
        Node missingCondition = readReport("cobertura-missing-condition-coverage.xml");

        assertThat(missingCondition.getAll(FILE)).extracting(Node::getName)
                .containsExactly("DataSourceProvider.cs");
        assertThat(missingCondition.getAll(CLASS)).extracting(Node::getName)
                .containsExactly("VisualOn.Data.DataSourceProvider");
        assertThat(missingCondition.getAll(METHOD)).extracting(Node::getName)
                .containsExactly("Enumerate()");

        assertThat(getLog().hasErrors()).isFalse();

        verifyBranchCoverageOfLine61(missingCondition);
    }
    private void verifyBranchCoverageOfLine61(final Node duplicateMethods) {
        var file = duplicateMethods.getAllFileNodes().get(0);
        assertThat(file.getCoveredOfLine(61)).isEqualTo(2);
        assertThat(file.getMissedOfLine(61)).isEqualTo(0);
    }

     @Test
    void shouldIgnoreDuplicateClasses() {
        Node duplicateClasses = readReport("cobertura-duplicate-classes.xml",
                new VectorCASTParser(ProcessingMode.IGNORE_ERRORS));

        assertThat(duplicateClasses.getAll(FILE)).extracting(Node::getName)
                .containsExactly("DataSourceProvider.cs");
        assertThat(duplicateClasses.getAll(CLASS)).extracting(Node::getName).hasSize(2)
                .contains("VisualOn.Data.DataSourceProvider")
                .element(1).asString().startsWith("VisualOn.Data.DataSourceProvider-");

        assertThat(getLog().hasErrors()).isTrue();
        assertThat(getLog().getErrorMessages())
                .contains("Found a duplicate class 'VisualOn.Data.DataSourceProvider' in 'DataSourceProvider.cs'");

        verifyBranchCoverageOfLine61(duplicateClasses);

        assertThatIllegalArgumentException().isThrownBy(
                () -> readReport("cobertura-duplicate-classes.xml", new VectorCASTParser()));
    }

    @Test
    void shouldIgnoreDuplicateMethods() {
        Node duplicateMethods = readReport("cobertura-duplicate-methods.xml",
                new VectorCASTParser(ProcessingMode.IGNORE_ERRORS));

        assertThat(duplicateMethods.getAll(FILE)).extracting(Node::getName)
                .containsExactly("DataSourceProvider.cs");
        assertThat(duplicateMethods.getAll(CLASS)).extracting(Node::getName)
                .containsExactly("VisualOn.Data.DataSourceProvider");
        assertThat(duplicateMethods.getAll(METHOD)).extracting(Node::getName).hasSize(2)
                .contains("Enumerate()")
                .element(1).asString().startsWith("Enumerate-");

        assertThat(getLog().hasErrors()).isTrue();
        assertThat(getLog().getErrorMessages())
                .contains("Found a duplicate method 'Enumerate' with signature '()' in 'VisualOn.Data.DataSourceProvider'");

        verifyBranchCoverageOfLine61(duplicateMethods);

        assertThatIllegalArgumentException().isThrownBy(
                () -> readReport("cobertura-duplicate-methods.xml", new VectorCASTParser()));
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#729")
    void shouldMergeCorrectly() {
        var builder = new CoverageBuilder();

        Node a = readReport("cobertura-merge-a.xml");
        assertThat(a.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(1).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(3).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(22).withMissed(0).build(),
                builder.withMetric(BRANCH).withCovered(2).withMissed(1).build(),
                new LinesOfCode(22));
        verifyMissedAndCoveredLines(a);

        Node b = readReport("cobertura-merge-b.xml");
        assertThat(b.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(1).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(1).withMissed(2).build(),
                builder.withMetric(LINE).withCovered(16).withMissed(6).build(),
                builder.withMetric(BRANCH).withCovered(0).withMissed(3).build(),
                new LinesOfCode(22));
        assertThat(b.getAllFileNodes()).hasSize(1).element(0).satisfies(
                fileNode -> assertThat(fileNode)
                        .hasMissedLines(36, 37, 38, 40, 41, 42)
                        .doesNotHaveCoveredLines(36, 37, 38, 40, 41, 42)
                        .hasCoveredLines(1, 5, 6, 7, 8, 9, 10, 11, 20, 35, 45, 54, 60, 66, 71, 72));

        var expectedValuesAfterMerge = new Value[] {
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(22).withMissed(0).build(),
                builder.withMetric(BRANCH).withCovered(1).withMissed(1).build(),
                new LinesOfCode(22)};

        var left = a.merge(b);
        assertThat(left.aggregateValues()).containsExactly(expectedValuesAfterMerge);
        verifyMissedAndCoveredLines(left);

        var right = b.merge(a);
        assertThat(right.aggregateValues()).containsExactly(expectedValuesAfterMerge);
        verifyMissedAndCoveredLines(left);
    } 

    private void verifyMissedAndCoveredLines(final Node left) {
        assertThat(left.getAllFileNodes())
                .hasSize(1)
                .element(0).satisfies(fileNode ->
                        assertThat(fileNode)
                                .hasNoMissedLines()
                                .hasCoveredLines(1, 5, 6, 7, 8, 9, 10, 11, 20,
                                        35, 36, 37, 38, 40, 41, 42, 45, 54, 60, 66, 71, 72));
    }
 
    @Test @Issue("jenkinsci/code-coverage-api-plugin#625")
    void shouldCountCorrectly() {
        Node tree = readReport("cobertura-counter-aggregation.xml");

        var expectedValue = new CoverageBuilder().withCovered(31).withMissed(1).withMetric(BRANCH).build();
        assertThat(tree.getValue(BRANCH)).isPresent().contains(expectedValue);
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#610")
    void shouldReadVectorCASTWithMissingSources() {
        Node tree = readReport("coverage-missing-sources.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsExactly("-");
        assertThat(tree.getAll(FILE)).extracting(Node::getName).containsExactly(
                "args.ts", "badge-result.ts", "colors.ts", "index.ts");
        assertThat(tree.getAllFileNodes()).extracting(FileNode::getRelativePath).containsExactly(
                "src/args.ts", "src/badge-result.ts", "src/colors.ts", "src/index.ts");
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#599")
    void shouldReadVectorCASTAggregation() {
        Node tree = readReport("cobertura-ts.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsExactly("-");
        assertThat(tree.getSourceFolders()).containsExactly(
                "/var/jenkins_home/workspace/imdb-songs_imdb-songs_PR-14/PR-14-15");
        assertThat(tree.getAll(PACKAGE)).extracting(Node::getName).containsExactly("libs.env.src",
                "services.api.src",
                "services.api.src.database",
                "services.api.src.graphql",
                "services.ui.libs.client.libs.env.src",
                "services.ui.libs.client.src.util",
                "services.ui.src");
        assertThat(tree.getAll(FILE)).extracting(Node::getName).containsExactly("env.ts",
                "api.ts",
                "app-info.ts",
                "env.ts",
                "movie-store.ts",
                "store.ts",
                "resolver.ts",
                "schema.ts",
                "env.ts",
                "error-util.ts",
                "env.ts",
                "server.ts");
        assertThat(tree.getAll(CLASS))
                .extracting(Node::getName)
                .containsExactly("env.ts",
                        "api.ts",
                        "app-info.ts",
                        "env.ts",
                        "movie-store.ts",
                        "store.ts",
                        "resolver.ts",
                        "schema.ts",
                        "env.ts",
                        "error-util.ts",
                        "env.ts",
                        "server.ts");

        var builder = new CoverageBuilder();

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, LOC);
        assertThat(tree.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(4).withMissed(3).build(),
                builder.withMetric(FILE).withCovered(6).withMissed(6).build(),
                builder.withMetric(CLASS).withCovered(6).withMissed(6).build(),
                builder.withMetric(METHOD).withCovered(14).withMissed(24).build(),
                builder.withMetric(LINE).withCovered(63).withMissed(93).build(),
                builder.withMetric(BRANCH).withCovered(21).withMissed(11).build(),
                new LinesOfCode(63 + 93));

        assertThat(tree.findPackage("libs.env.src")).isNotEmpty().get().satisfies(
                p -> {
                    assertThat(p.getAllFileNodes()).extracting(FileNode::getRelativePath)
                            .containsExactly("libs/env/src/env.ts");
                    assertThat(p).hasFiles("libs/env/src/env.ts");
                    assertThat(p.getAll(CLASS)).extracting(Node::getName).containsExactly("env.ts");
                }
        );
        assertThat(tree.findPackage("services.api.src")).isNotEmpty().get().satisfies(
                p -> {
                    assertThat(p).hasFiles("services/api/src/env.ts");
                    assertThat(p.getAllFileNodes()).extracting(FileNode::getRelativePath)
                            .contains("services/api/src/env.ts");
                    assertThat(p.getAll(CLASS)).extracting(Node::getName).contains("env.ts");
                }
        );
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#473")
    void shouldReadVectorCASTNpe() {
        Node tree = readReport("cobertura-npe.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsOnly("-");
        assertThat(tree.getAll(PACKAGE)).hasSize(1).extracting(Node::getName).containsOnly("CoverageTest.Service");
        assertThat(tree.getAll(FILE)).hasSize(2).extracting(Node::getName).containsOnly("Program.cs", "Startup.cs");
        assertThat(tree.getAll(CLASS)).hasSize(2)
                .extracting(Node::getName)
                .containsOnly("Lisec.CoverageTest.Program", "Lisec.CoverageTest.Startup");

        var builder = new CoverageBuilder();

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY,
                COMPLEXITY_DENSITY, COMPLEXITY_MAXIMUM, LOC);
        assertThat(tree.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(2).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(2).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(4).withMissed(1).build(),
                builder.withMetric(LINE).withCovered(44).withMissed(9).build(),
                builder.withMetric(BRANCH).withCovered(3).withMissed(1).build(),
                new CyclomaticComplexity(8),
                new CyclomaticComplexity(4, COMPLEXITY_MAXIMUM),
                new FractionValue(COMPLEXITY_DENSITY, 8, 44 + 9),
                new LinesOfCode(44 + 9));
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#551")
    void shouldReadVectorCASTAbsolutePath() {
        Node tree = readReport("cobertura-absolute-path.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsOnly("-");
        assertThat(tree.getAll(PACKAGE)).hasSize(1).extracting(Node::getName).containsOnly("Numbers");
        assertThat(tree.getAllFileNodes()).hasSize(1)
                .extracting(Node::getName)
                .containsOnly("PrimeService.cs");
        assertThat(tree.getAllFileNodes()).hasSize(1)
                .extracting(FileNode::getRelativePath)
                .containsOnly("D:/Build/workspace/esignPlugins_test-jenkins-plugin/Numbers/PrimeService.cs");
        assertThat(tree.getAll(CLASS)).hasSize(1)
                .extracting(Node::getName)
                .containsOnly("Numbers.PrimeService");

        assertThat(tree.getAllFileNodes()).hasSize(1).extracting(FileNode::getRelativePath)
                .containsOnly("D:/Build/workspace/esignPlugins_test-jenkins-plugin/Numbers/PrimeService.cs");

        var builder = new CoverageBuilder();

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, COMPLEXITY,
                COMPLEXITY_DENSITY, COMPLEXITY_MAXIMUM, LOC);
        assertThat(tree.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(1).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(1).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(12).withMissed(0).build(),
                builder.withMetric(BRANCH).withCovered(6).withMissed(0).build(),
                new CyclomaticComplexity(0),
                new CyclomaticComplexity(0, COMPLEXITY_MAXIMUM),
                new FractionValue(COMPLEXITY_DENSITY, 0, 12),
                new LinesOfCode(12));
    }
 */
    @Test
    void shouldConvertVectorCASTStatementBranchToTree() {
        Node root = readReport("vectorcast-statement-branch.xml");

        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(3);
        assertThat(root.getAll(FILE)).hasSize(3);
        assertThat(root.getAll(CLASS)).hasSize(3);
        assertThat(root.getAll(METHOD)).hasSize(0);

        assertThat(root).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, LINE, BRANCH, COMPLEXITY, COMPLEXITY_MAXIMUM, COMPLEXITY_DENSITY, LOC);

        var files = root.getAllFileNodes();
        assertThat(files).hasSize(3).extracting(FileNode::getFileName)
                .containsExactlyInAnyOrder("database.c", "manager.c", "whitebox.c");

        var builder = new CoverageBuilder();
        assertThat(root.find(FILE, "tutorial/c/database/database.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(10, 9, 19, 17, 20, 23)
                                .hasCoveredLines(7, 12)));
                                
        assertThat(root.find(CLASS, "database")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(4),
                                           builder.withMetric(LINE).withCovered(3).withMissed(7).build(),
                                           builder.withMetric(BRANCH).withCovered(2).withMissed(4).build()
                                           )));
           
        assertThat(root.find(FILE, "tutorial/c/order_entry/manager.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(80, 89, 78, 81, 82, 88, 76, 75, 83, 86, 74, 79)
                                .hasCoveredLines(46, 60, 39, 42, 43, 62, 53, 63, 56, 66, 59, 51, 
                                    48, 67, 41, 54, 57, 23, 19, 20, 21, 17, 25, 26, 27, 29, 95, 96, 
                                    103, 101, 106, 107, 109, 105, 102, 116, 114, 115)));
                                    
        assertThat(root.find(CLASS, "manager")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(18),
                                           builder.withMetric(LINE).withCovered(48).withMissed(13).build(),
                                           builder.withMetric(BRANCH).withCovered(21).withMissed(4).build()
                                           )));

        assertThat(root.find(FILE, "tutorial/c/utils/whitebox.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(17, 22, 27, 29, 28)
                                .hasCoveredLines(30)));
                                    
        assertThat(root.find(CLASS, "whitebox")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(3),
                                           builder.withMetric(LINE).withCovered(1).withMissed(8).build(),
                                           builder.withMetric(BRANCH).withCovered(0).withMissed(3).build()
                                           )));

        assertThat(root.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(3).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(3).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(3).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(52).withMissed(28).build(),
                builder.withMetric(BRANCH).withCovered(23).withMissed(11).build(),
                new CyclomaticComplexity(25),
                new FractionValue(COMPLEXITY_DENSITY, 25, 80),
                new LinesOfCode(80));

        verifyCoverageMetrics(root);

        List<Node> nodes = root.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        for (Node node : nodes) {
            var lineCoverage = (Coverage) node.getValue(LINE).get();
            missedLines = missedLines + lineCoverage.getMissed();
            coveredLines = coveredLines + lineCoverage.getCovered();
        }

        assertThat(missedLines).isEqualTo(28);
        assertThat(coveredLines).isEqualTo(52);
    }

    @Test
    void shouldHaveOneSource() {
        ModuleNode tree = readExampleReport();

        assertThat(tree.getSourceFolders())
                .hasSize(1)
                .containsExactly("");
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    private void verifyCoverageMetrics(final Node tree) {
        assertThat(getCoverage(tree, LINE))
                .hasCovered(52)
                .hasCoveredPercentage(Percentage.valueOf(52, 52 + 28))
                .hasMissed(28)
                .hasTotal(52 + 28);

        assertThat(getCoverage(tree, BRANCH))
                .hasCovered(23)
                .hasCoveredPercentage(Percentage.valueOf(23, 23 + 11))
                .hasMissed(11)
                .hasTotal(23 + 11);

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
    void shouldReturnCorrectPathsInFileCoverageNodesFromVectorCASTReport() {
        Node result = readReport("vectorcast-statement-mcdc-fcc.xml");
        assertThat(result.getAllFileNodes())
                .hasSize(5)
                .extracting(FileNode::getRelativePath)
                .containsOnly(
                    "CurrentRelease/database/src/database.c",
                    "CurrentRelease/encrypt/src/encrypt.c",
                    "CurrentRelease/encrypt/src/matrix_multiply.c",
                    "CurrentRelease/order_entry/src/manager.c",
                    "CurrentRelease/utils/src/linked_list.c");
    }

/*     @Test
    void shouldReturnCorrectPathsInFileCoverageNodesFromPythonVectorCASTReport() {
        Node result = readReport("cobertura-python.xml");
        assertThat(result.getAllFileNodes())
                .hasSize(1)
                .extracting(FileNode::getRelativePath)
                .containsOnly("__init__.py");

        assertThat(result.getValue(LINE)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(19).hasMissed(0));
        assertThat(result.getValue(BRANCH)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(4).hasMissed(0));
        assertThat(result).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, LINE, BRANCH, LOC, COMPLEXITY,
                COMPLEXITY_DENSITY, COMPLEXITY_MAXIMUM);

        var fileNode = result.getAllFileNodes().get(0);
        assertThat(fileNode.getLinesWithCoverage())
                .containsExactly(6, 8, 9, 10, 11, 13, 16, 25, 41, 42, 46, 48, 49, 50, 54, 55, 56, 57, 60);
        assertThat(fileNode.getMissedCounters())
                .containsExactly(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertThat(fileNode.getCoveredCounters())
                .containsExactly(1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1);
        assertThat(fileNode).hasNoMissedLines()
                .hasCoveredLines(6, 8, 9, 10, 11, 13, 16, 25, 41, 42, 46, 48, 49, 50, 54, 55, 56, 57, 60);
    }
 */

    private ModuleNode readExampleReport() {
        return readReport("vectorcast-statement-branch.xml");
    }
}
