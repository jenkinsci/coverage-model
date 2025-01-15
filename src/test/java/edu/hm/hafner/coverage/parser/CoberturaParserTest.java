package edu.hm.hafner.coverage.parser;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.Issue;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Percentage;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.coverage.assertions.Assertions;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

@DefaultLocale("en")
class CoberturaParserTest extends AbstractParserTest {
    private static final int COVERED_LINES = 20 + 17 + 6 + 12 + 7;
    private static final int MISSED_LINES = 8 + 1 + 1 + 10;

    @Override
    CoberturaParser createParser(final ProcessingMode processingMode) {
        return new CoberturaParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "cobertura";
    }

    @Test
    @Issue("JENKINS-73635")
    void shouldRemovePrefixOfDeterministicCoverageReport() {
        var root = readReport("c#-cobertura.xml");

        assertThat(root.getAllFileNodes()).hasSize(20).map(FileNode::getRelativePath)
                .allSatisfy(
                        file -> assertThat(file)
                                .doesNotStartWith("/_/")
                                .startsWith("Lib.LicenseScanner/"));

        assertThat(root.getAllFileNodes()).hasSize(20)
                .first().satisfies(
                        file -> assertThat(file)
                                .hasName("IssueKeys.cs")
                                .hasRelativePath("Lib.LicenseScanner/IssueKeys.cs"));
    }

    @Test
    @Issue("JENKINS-73325")
    void shouldUseFullPathWhenParsingFileNodes() {
        var root = readReport("cobertura-same-filename.xml");

        assertThat(root.getAllFileNodes()).hasSize(2)
                .satisfiesExactlyInAnyOrder(
                        file -> assertThat(file).hasName("MyClass.cs").hasRelativePath("/src/NamespaceA/MyClass.cs"),
                        file -> assertThat(file).hasName("MyClass.cs").hasRelativePath("/src/NamespaceB/MyClass.cs"));
    }

    @Test
    @Issue("JENKINS-73325")
    void shouldMergeFilesThatUseSameFileNameInDifferentFolder() {
        var left = readReport("merge-duplicate-a.xml");
        var right = readReport("merge-duplicate-b.xml");

        var aggregation = left.merge(right);
        assertThat(aggregation.getAllFileNodes()).hasSize(2)
                .satisfiesExactlyInAnyOrder(
                        file -> Assertions.assertThat(file).hasName("MyClass.cs").hasRelativePath("/src/Domain/NamespaceA/MyClass.cs"),
                        file -> Assertions.assertThat(file).hasName("MyClass.cs").hasRelativePath("/src/Domain/NamespaceB/MyClass.cs"));
    }

    @Test
    @Issue("JENKINS-73175")
    void shouldAutoGenerateNamesForRuby() {
        var root = readReport("cobertura-ruby.xml");

        assertThat(root.getAllFileNodes()).hasSize(3)
                .satisfiesExactlyInAnyOrder(
                        foobar -> assertThat(foobar).hasName("foobar.rb").hasRelativePath("lib/foobar.rb"),
                        bar -> assertThat(bar).hasName("my_class.rb").hasRelativePath("lib/foobar/bar/my_class.rb"),
                        baz -> assertThat(baz).hasName("my_class.rb").hasRelativePath("lib/foobar/baz/my_class.rb"));
    }

    @Test @Issue("JENKINS-73175")
    void shouldAutoGenerateNamesForJavaScript() {
        var root = readReport("cobertura-js.xml", ProcessingMode.IGNORE_ERRORS);

        assertThat(root.getAllMethodNodes()).hasSize(3).map(Node::getName).satisfiesExactly(
                first -> assertThat(first).isEqualTo("Foo()V"),
                second -> assertThat(second).isEqualTo("bar()V"),
                third -> assertThat(third).startsWith("bar-"));
    }

    @Test @Issue("JENKINS-73175")
    void shouldAutoGenerateNamesForCpp() {
        var root = readReport("cobertura-cpp.xml", ProcessingMode.IGNORE_ERRORS);

        assertThat(root.getAllMethodNodes()).hasSize(2).map(Node::getName).satisfiesExactly(
                first -> assertThat(first).isEqualTo("calculate::[lambda][](int)"),
                second -> assertThat(second).startsWith("calculate::[lambda]-"));
    }

    @Test @Issue("JENKINS-72757")
    void shouldMergeIfCountersAreNotCompatible() {
        var left = readReport("merge-a.xml");
        assertThat(left.getAllFileNodes()).hasSize(1)
                .element(0).satisfies(fileNode -> {
                    assertThat(fileNode.getCoveredOfLine(61)).isEqualTo(2);
                    assertThat(fileNode.getMissedOfLine(61)).isEqualTo(0);
                });

        var right = readReport("merge-b.xml");
        assertThat(right.getAllFileNodes()).hasSize(1)
                .element(0).satisfies(fileNode -> {
                    assertThat(fileNode.getCoveredOfLine(61)).isEqualTo(0);
                    assertThat(fileNode.getMissedOfLine(61)).isEqualTo(4);
                });

        assertThat(Node.merge(List.of(left, right)).getAllFileNodes()).hasSize(1)
                .element(0).satisfies(fileNode -> {
                    assertThat(fileNode.getCoveredOfLine(61)).isEqualTo(4);
                    assertThat(fileNode.getMissedOfLine(61)).isEqualTo(0);
                });
        assertThat(Node.merge(List.of(right, left)).getAllFileNodes()).hasSize(1)
                .element(0).satisfies(fileNode -> {
                    assertThat(fileNode.getCoveredOfLine(61)).isEqualTo(4);
                    assertThat(fileNode.getMissedOfLine(61)).isEqualTo(0);
                });
    }

    @Test
    void shouldIgnoreMissingConditionAttribute() {
        var missingCondition = readReport("cobertura-missing-condition-coverage.xml");

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
        var duplicateClasses = readReport("cobertura-duplicate-classes.xml",
                new CoberturaParser(ProcessingMode.IGNORE_ERRORS));

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
                () -> readReport("cobertura-duplicate-classes.xml", new CoberturaParser()));
    }

    @Test
    void shouldIgnoreDuplicateMethods() {
        var duplicateMethods = readReport("cobertura-duplicate-methods.xml",
                new CoberturaParser(ProcessingMode.IGNORE_ERRORS));

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
                () -> readReport("cobertura-duplicate-methods.xml", new CoberturaParser()));
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#729")
    void shouldMergeCorrectly() {
        var builder = new CoverageBuilder();

        var a = readReport("cobertura-merge-a.xml");
        assertThat(a.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(1).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(3).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(22).withMissed(0).build(),
                builder.withMetric(BRANCH).withCovered(2).withMissed(1).build(),
                new Value(LOC, 22));
        verifyMissedAndCoveredLines(a);

        var b = readReport("cobertura-merge-b.xml");
        assertThat(b.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(1).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(1).withMissed(2).build(),
                builder.withMetric(LINE).withCovered(16).withMissed(6).build(),
                builder.withMetric(BRANCH).withCovered(0).withMissed(3).build(),
                new Value(LOC, 22));
        assertThat(b.getAllFileNodes()).hasSize(1).element(0).satisfies(
                fileNode -> assertThat(fileNode)
                        .hasMissedLines(36, 37, 38, 40, 41, 42)
                        .doesNotHaveCoveredLines(36, 37, 38, 40, 41, 42)
                        .hasCoveredLines(1, 5, 6, 7, 8, 9, 10, 11, 20, 35, 45, 54, 60, 66, 71, 72));

        var expectedValuesAfterMerge = new Value[]{
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(22).withMissed(0).build(),
                builder.withMetric(BRANCH).withCovered(1).withMissed(1).build(),
                new Value(LOC, 22)};

        var left = Node.merge(List.of(a, b));
        assertThat(left.aggregateValues()).containsExactly(expectedValuesAfterMerge);
        verifyMissedAndCoveredLines(left);

        var right = Node.merge(List.of(b, a));
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
        var tree = readReport("cobertura-counter-aggregation.xml");

        var expectedValue = new CoverageBuilder().withCovered(31).withMissed(1).withMetric(BRANCH).build();
        assertThat(tree.getValue(BRANCH)).isPresent().contains(expectedValue);
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#610")
    void shouldReadCoberturaWithMissingSources() {
        var tree = readCoberturaReport("coverage-missing-sources.xml");
        assertThat(tree.getAll(FILE)).extracting(Node::getName).containsExactly(
                "args.ts", "badge-result.ts", "colors.ts", "index.ts");
        assertThat(tree.getAllFileNodes()).extracting(FileNode::getRelativePath).containsExactly(
                "src/args.ts", "src/badge-result.ts", "src/colors.ts", "src/index.ts");
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#599")
    void shouldReadCoberturaAggregation() {
        var tree = readCoberturaReport("cobertura-ts.xml");
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
                new Value(LOC, 63 + 93));

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
    void shouldReadCoberturaNpe() {
        var tree = readReport("cobertura-npe.xml");

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsOnly("-");
        assertThat(tree.getAll(PACKAGE)).hasSize(1).extracting(Node::getName).containsOnly("CoverageTest.Service");
        assertThat(tree.getAll(FILE)).hasSize(2).extracting(Node::getName).containsOnly("Program.cs", "Startup.cs");
        assertThat(tree.getAll(CLASS)).hasSize(2)
                .extracting(Node::getName)
                .containsOnly("Lisec.CoverageTest.Program", "Lisec.CoverageTest.Startup");

        var builder = new CoverageBuilder();

        assertThat(tree)
                .hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, CYCLOMATIC_COMPLEXITY, LOC);
        assertThat(tree.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(2).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(2).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(4).withMissed(1).build(),
                builder.withMetric(LINE).withCovered(44).withMissed(9).build(),
                builder.withMetric(BRANCH).withCovered(3).withMissed(1).build(),
                new Value(CYCLOMATIC_COMPLEXITY, 8),
                new Value(LOC, 44 + 9));
    }

    @Test @Issue("jenkinsci/code-coverage-api-plugin#551")
    void shouldReadCoberturaAbsolutePath() {
        var tree = readReport("cobertura-absolute-path.xml");

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

        assertThat(tree)
                .hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, CYCLOMATIC_COMPLEXITY, LOC);
        assertThat(tree.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(1).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(1).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(1).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(1).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(12).withMissed(0).build(),
                builder.withMetric(BRANCH).withCovered(6).withMissed(0).build(),
                new Value(CYCLOMATIC_COMPLEXITY, 0),
                new Value(LOC, 12));
    }

    @Test
    void shouldConvertCoberturaBigToTree() {
        var root = readExampleReport();

        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(5);
        assertThat(root.getAll(FILE)).hasSize(4);
        assertThat(root.getAll(CLASS)).hasSize(5);
        assertThat(root.getAll(METHOD)).hasSize(10);

        assertThat(root)
                .hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, LINE, BRANCH, CYCLOMATIC_COMPLEXITY, LOC);

        var files = root.getAllFileNodes();
        assertThat(files).hasSize(4).extracting(FileNode::getFileName)
                .containsExactlyInAnyOrder("Branch.php",
                        "IvcBranches.php",
                        "PopulateBranchExtensionAttributesPlugin.php",
                        "SetBranchExtensionAttributesPlugin.php");

        var builder = new CoverageBuilder();
        assertThat(root.find(FILE, "Model/Resolver/DataProvider/Branch.php")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasNoMissedLines()
                                .hasCoveredLines(34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
                                        45, 46, 47, 49, 50, 51, 52, 53, 55)));
        assertThat(root.find(CLASS, "Invocare.InventoryBranch.Model.Resolver.DataProvider.Branch")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(builder.withMetric(LINE).withCovered(20).withMissed(0).build())));

        assertThat(root.find(FILE, "Model/Resolver/IvcBranches.php")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(100, 101, 102, 104, 107, 108, 109, 110)
                                .hasCoveredLines(51, 52, 53, 61, 62, 64, 65, 68, 70, 71, 72,
                                        75, 76, 79, 81, 82, 83)));
        assertThat(root.find(CLASS, "Invocare.InventoryBranch.Model.Resolver.IvcBranches")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(builder.withMetric(LINE).withCovered(17).withMissed(8).build())));

        assertThat(root.find(FILE, "Plugin/InventoryAdminUi/SourceDataProvider/PopulateBranchExtensionAttributesPlugin.php")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(38)
                                .hasCoveredLines(28, 29, 39, 40, 41, 45)));
        assertThat(root.find(CLASS, "Invocare.InventoryBranch.Plugin.InventoryAdminUi.SourceDataProvider.PopulateBranchExtensionAttributesPlugin")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(builder.withMetric(LINE).withCovered(6).withMissed(1).build(),
                                        builder.withMetric(BRANCH).withCovered(2).withMissed(2).build())));

        assertThat(root.find(FILE, "Plugin/InventoryApi/SourceRepository/SetBranchExtensionAttributesPlugin.php")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(30, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116)
                                .hasCoveredLines(45, 46, 61, 62, 63, 64, 65, 66, 67, 68, 69, 72, 89, 90, 92, 93, 94, 96, 97)));
        assertThat(root.find(CLASS, "Invocare.InventoryBranch.Plugin.InventoryApi.SourceRepository.SetBranchExtensionAttributesPlugin")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(builder.withMetric(LINE).withCovered(12).withMissed(1).build())));
        assertThat(root.find(CLASS, "Invocare.InventoryBranch.Plugin.InventoryApi.SourceRepository.SetBranchExtensionPlugin")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(builder.withMetric(LINE).withCovered(7).withMissed(10).build())));

        assertThat(root.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(4).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(4).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(5).withMissed(0).build(),
                builder.withMetric(METHOD).withCovered(7).withMissed(3).build(),
                builder.withMetric(LINE).withCovered(COVERED_LINES).withMissed(MISSED_LINES).build(),
                builder.withMetric(BRANCH).withCovered(2).withMissed(2).build(),
                new Value(CYCLOMATIC_COMPLEXITY, 22),
                new Value(LOC, 63 + 19));

        verifyCoverageMetrics(root);

        List<Node> nodes = root.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        for (Node node : nodes) {
            var lineCoverage = (Coverage) node.getValue(LINE).get();
            missedLines = missedLines + lineCoverage.getMissed();
            coveredLines = coveredLines + lineCoverage.getCovered();
        }

        assertThat(missedLines).isEqualTo(MISSED_LINES);
        assertThat(coveredLines).isEqualTo(COVERED_LINES);
    }

    @Test
    void shouldHaveOneSource() {
        var tree = readExampleReport();

        assertThat(tree.getSourceFolders())
                .hasSize(1)
                .containsExactly("/app/app/code/Invocare/InventoryBranch");
    }

    private Node readCoberturaReport(final String fileName) {
        var tree = readReport(fileName);

        assertThat(tree.getAll(MODULE)).hasSize(1).extracting(Node::getName).containsExactly("-");
        return tree;
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    private void verifyCoverageMetrics(final Node tree) {
        assertThat(getCoverage(tree, LINE))
                .hasCovered(COVERED_LINES)
                .hasCoveredPercentage(Percentage.valueOf(COVERED_LINES, COVERED_LINES + MISSED_LINES))
                .hasMissed(MISSED_LINES)
                .hasTotal(COVERED_LINES + MISSED_LINES);

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
        var result = readReport("cobertura-lots-of-data.xml");
        assertThat(result.getAllFileNodes())
                .hasSize(19)
                .extracting(FileNode::getRelativePath)
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
        var result = readReport("cobertura-python.xml");
        assertThat(result.getAllFileNodes())
                .hasSize(1)
                .extracting(FileNode::getRelativePath)
                .containsOnly("__init__.py");

        assertThat(result.getValue(LINE)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(19).hasMissed(0));
        assertThat(result.getValue(BRANCH)).isPresent().get().isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(4).hasMissed(0));
        assertThat(result).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, LINE, BRANCH, LOC, CYCLOMATIC_COMPLEXITY);

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

    private ModuleNode readExampleReport() {
        return readReport("cobertura.xml");
    }
}
