package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class GoCovParserTest extends AbstractParserTest {
    private static final String GO_MOD = "go.mod";
    private static final String DEMO_PROFILE = "go-demo.coverprofile";
    private static final char INVALID_CHARACTER = '\0';

    @Override
    protected String getFolder() {
        return "go";
    }

    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new GoCovParser(processingMode);
    }

    @Test
    void shouldCreateCoverages() {
        var report = readReport("go-coverage-atomic.out", ProcessingMode.IGNORE_ERRORS);

        assertThat(report).hasName("github.com/example");

        assertThat(report.aggregateValues()).contains(
                new CoverageBuilder().withMetric(MODULE).withCovered(2).withTotal(2).build());

        assertThat(report.getAll(PACKAGE)).hasSize(4)
                .map(Node::getName)
                .containsExactlyInAnyOrder("pkg.utils", "pkg.db", "cmd", "pkg.test");
        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "pkg/utils/file1.go",
                "pkg/db/file2.go",
                "pkg/test/file4.go",
                "cmd/file3.go");
        assertThat(report.getAll(CLASS)).isEmpty();

        var builder = new CoverageBuilder();
        assertThat(report.aggregateValues()).contains(
                builder.withMetric(PACKAGE).withCovered(3).withTotal(4).build(),
                builder.withMetric(FILE).withCovered(3).withTotal(4).build(),
                builder.withMetric(LINE).withCovered(23).withTotal(33).build(),
                builder.withMetric(INSTRUCTION).withCovered(14).withTotal(22).build(),
                new Value(LOC, 33));

        assertThat(report.getAllFileNodes()).map(FileNode::getFileName).containsExactlyInAnyOrder(
                "file1.go", "file2.go", "file3.go", "file4.go");

        assertThat(report.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                one -> assertThat(one).hasName("file1.go")
                        .hasMissedLines(19, 20)
                        .hasCoveredLines(
                                5, 6, 7, 8,
                                10, 11, 12, 13, 14, 15,
                                24, 25,
                                30, 31, 32),
                two -> assertThat(two).hasName("file2.go")
                        .hasMissedLines(12, 13, 14, 15)
                        .hasCoveredLines(17, 18, 19, 20, 21, 22),
                three -> assertThat(three).hasName("file3.go")
                        .hasMissedLines(10, 15, 16, 17)
                        .hasNoCoveredLines(),
                four -> assertThat(four).hasName("file4.go")
                        .hasNoMissedLines()
                        .hasCoveredLines(10, 11));
    }

    @Test
    void shouldMapMultipleModulesWithoutOrg() {
        var report = readReport("go-relative-import.out");

        assertThat(report).hasName("example");

        assertThat(report.aggregateValues()).contains(
                new CoverageBuilder().withMetric(MODULE).withCovered(3).withTotal(3).build());

        assertThat(report.getAll(PACKAGE)).hasSize(4)
                .map(Node::getName)
                .containsExactlyInAnyOrder("pkg.utils", "pkg.db", "cmd", "pkg.test");
        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "pkg/utils/file1.go",
                "pkg/db/file2.go",
                "pkg/test/file4.go",
                "cmd/file3.go");
        assertThat(report.getAll(CLASS)).isEmpty();

        var builder = new CoverageBuilder();
        assertThat(report.aggregateValues()).contains(
                builder.withMetric(PACKAGE).withCovered(3).withTotal(4).build(),
                builder.withMetric(FILE).withCovered(3).withTotal(4).build(),
                builder.withMetric(LINE).withCovered(23).withTotal(33).build(),
                builder.withMetric(INSTRUCTION).withCovered(14).withTotal(22).build(),
                new Value(LOC, 33));

        assertThat(report.getAllFileNodes()).map(FileNode::getFileName).containsExactlyInAnyOrder(
                "file1.go", "file2.go", "file3.go", "file4.go");

        assertThat(report.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                one -> assertThat(one).hasName("file1.go")
                        .hasMissedLines(19, 20)
                        .hasCoveredLines(
                                5, 6, 7, 8,
                                10, 11, 12, 13, 14, 15,
                                24, 25,
                                30, 31, 32),
                two -> assertThat(two).hasName("file2.go")
                        .hasMissedLines(12, 13, 14, 15)
                        .hasCoveredLines(17, 18, 19, 20, 21, 22),
                three -> assertThat(three).hasName("file3.go")
                        .hasMissedLines(10, 15, 16, 17)
                        .hasNoCoveredLines(),
                four -> assertThat(four).hasName("file4.go")
                        .hasNoMissedLines()
                        .hasCoveredLines(10, 11));
    }

    @Test
    void shouldHandleSimplePaths() {
        var report = readReport("go-simple-path.out");

        assertThat(report).hasName("example.com");

        assertThat(report.aggregateValues()).contains(
                new CoverageBuilder().withMetric(MODULE).withCovered(2).withTotal(2).build());

        assertThat(report.getAll(PACKAGE)).hasSize(2)
                .map(Node::getName)
                .containsExactlyInAnyOrder("-", "internal");

        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "example.go",
                "internal/helper.go");

        var builder = new CoverageBuilder();
        assertThat(report.aggregateValues()).contains(
                builder.withMetric(PACKAGE).withCovered(2).withTotal(2).build(),
                builder.withMetric(FILE).withCovered(2).withTotal(2).build(),
                builder.withMetric(LINE).withCovered(6).withTotal(12).build(),
                builder.withMetric(INSTRUCTION).withCovered(2).withTotal(4).build());

        assertThat(report.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                one -> assertThat(one).hasName("example.go")
                        .hasCoveredLines(3, 4, 5)
                        .hasMissedLines(7, 8, 9),
                two -> assertThat(two).hasName("helper.go")
                        .hasCoveredLines(5, 6, 7)
                        .hasMissedLines(9, 10, 11));
    }

    @Test
    void shouldGuessPathsOfDemoExampleWithoutModuleDescriptors() {
        var report = readReport(DEMO_PROFILE);

        assertThat(report).hasName("ext");

        assertThat(report.getChildren()).map(Node::getName)
                .containsExactlyInAnyOrder("ext", "hello.test");

        assertThat(report.getAll(PACKAGE)).hasSize(5)
                .map(Node::getName)
                .containsExactlyInAnyOrder("-", "stat", "sub", "-", "cpu");

        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "sum.go",
                "stat/minmax.go",
                "sub/sub.go",
                "main.go",
                "cpu/main.go");

        assertThat(report.aggregateValues()).contains(
                new CoverageBuilder().withMetric(MODULE).withCovered(3).withTotal(3).build());

        verifyDemoCoverage(report);
    }

    @Test
    void shouldResolvePathsOfDemoExampleWithModuleDescriptors(@TempDir final Path workspace) throws IOException {
        createDemoModules(workspace);

        var report = parseReportOf(writeFile(workspace, DEMO_PROFILE, readDemoProfile()));

        assertThat(report).hasName("hello.test");

        assertThat(report.getChildren()).map(Node::getName)
                .containsExactlyInAnyOrder("hello.test", "ext", "ext/sub");

        assertThat(report.getAll(PACKAGE)).hasSize(5)
                .map(Node::getName)
                .containsExactlyInAnyOrder("-", "stat", "-", "-", "cpu");

        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "hack/ext/sum.go",
                "hack/ext/stat/minmax.go",
                "hack/extsub/sub.go",
                "main.go",
                "cpu/main.go");

        assertThat(report.aggregateValues()).contains(
                new CoverageBuilder().withMetric(MODULE).withCovered(4).withTotal(4).build());

        verifyDemoCoverage(report);
    }

    @Test
    void shouldResolvePathsWhenReportIsStoredInSubDirectory(@TempDir final Path workspace) throws IOException {
        createDemoModules(workspace);

        var report = parseReportOf(writeFile(workspace.resolve("build"), DEMO_PROFILE, readDemoProfile()));

        assertThat(report).hasName("hello.test");
        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "hack/ext/sum.go",
                "hack/ext/stat/minmax.go",
                "hack/extsub/sub.go",
                "main.go",
                "cpu/main.go");
    }

    @Test
    void shouldGuessPathsThatDoNotBelongToAModule(@TempDir final Path workspace) throws IOException {
        writeFile(workspace, GO_MOD, "module hello.test\n");

        var report = parseReportOf(writeFile(workspace, DEMO_PROFILE, """
                mode: set
                hello.test/main.go:3.13,5.2 1 1
                github.com/external/counter.go:3.13,5.2 1 0
                """));

        assertThat(report).hasName("hello.test");
        assertThat(report.getChildren()).map(Node::getName)
                .containsExactlyInAnyOrder("hello.test", "github.com");
        assertThat(report.getFiles()).containsExactlyInAnyOrder("main.go", "external/counter.go");
    }

    @Test
    void shouldSkipModuleDescriptorsOfVendoredDependencies(@TempDir final Path workspace) throws IOException {
        writeFile(workspace, GO_MOD, "module hello.test\n");
        writeFile(workspace.resolve("vendor").resolve("example.com").resolve("dep"), GO_MOD,
                "module example.com/dep\n");

        var report = parseReportOf(writeFile(workspace, DEMO_PROFILE, """
                mode: set
                hello.test/main.go:3.13,5.2 1 1
                example.com/dep/dep.go:3.13,5.2 1 1
                """));

        assertThat(report.getFiles()).containsExactlyInAnyOrder("main.go", "dep/dep.go");
    }

    @Test
    void shouldSkipModuleDescriptorsThatCannotBeRead(@TempDir final Path workspace) throws IOException {
        writeFile(workspace, GO_MOD, "module hello.test\n");
        Files.createDirectories(workspace.resolve("broken").resolve(GO_MOD));

        var report = parseReportOf(writeFile(workspace, DEMO_PROFILE, """
                mode: set
                hello.test/main.go:3.13,5.2 1 1
                """));

        assertThat(report).hasName("hello.test");
        assertThat(report.getFiles()).containsExactly("main.go");
        assertThat(getLog().getErrorMessages()).anySatisfy(
                message -> assertThat(message).contains("Cannot read Go module descriptor"));
    }

    @Test
    void shouldSkipModuleDescriptorsWithoutModuleDirective(@TempDir final Path workspace) throws IOException {
        writeFile(workspace, GO_MOD, "go 1.25.0\n");
        writeFile(workspace.resolve("sub"), GO_MOD, "module example.com/sub // the nested module\n");

        var report = parseReportOf(writeFile(workspace, DEMO_PROFILE, """
                mode: set
                example.com/sub/a.go:3.13,5.2 1 1
                """));

        assertThat(report).hasName("example.com/sub");
        assertThat(report.getFiles()).containsExactly("sub/a.go");
        assertThat(getLog().getErrorMessages()).anySatisfy(
                message -> assertThat(message).contains("Skipping Go module descriptor without module directive"));
    }

    @Test
    void shouldGuessPathsWhenNoModuleDescriptorExists(@TempDir final Path workspace) throws IOException {
        var report = parseReportOf(writeFile(workspace, DEMO_PROFILE, readDemoProfile()));

        assertThat(report).hasName("ext");
        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "sum.go",
                "stat/minmax.go",
                "sub/sub.go",
                "main.go",
                "cpu/main.go");
    }

    @Test
    void shouldGuessPathsOfPackagePathsWithoutDirectories() {
        var report = createParser(ProcessingMode.FAIL_FAST).parse(new StringReader("""
                mode: set
                /:3.13,5.2 1 0
                main.go:7.13,9.2 1 1
                """), DEMO_PROFILE, getLog());

        assertThat(report).hasName("main.go");
        assertThat(report.getAllFileNodes()).map(FileNode::getFileName)
                .containsExactlyInAnyOrder("", "main.go");
    }

    @Test
    void shouldGuessPathsWhenReportFileNameIsNotAValidPath() {
        var report = createParser(ProcessingMode.FAIL_FAST).parse(new StringReader("""
                mode: set
                hello.test/main.go:3.13,5.2 1 1
                """), "invalid" + INVALID_CHARACTER + ".coverprofile", getLog());

        assertThat(report).hasName("hello.test");
        assertThat(report.getFiles()).containsExactly("main.go");
    }

    private void verifyDemoCoverage(final ModuleNode report) {
        var builder = new CoverageBuilder();
        assertThat(report.aggregateValues()).contains(
                builder.withMetric(PACKAGE).withCovered(4).withTotal(5).build(),
                builder.withMetric(FILE).withCovered(4).withTotal(5).build(),
                builder.withMetric(INSTRUCTION).withCovered(14).withTotal(21).build());

        assertThat(report.getAllFileNodes()).map(FileNode::getFileName).containsExactlyInAnyOrder(
                "sum.go", "minmax.go", "sub.go", "main.go", "main.go");

        assertThat(report.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                sum -> assertThat(sum).hasName("sum.go")
                        .hasCoveredLines(3, 4, 7, 8, 9, 10, 11)
                        .hasMissedLines(5, 6),
                minmax -> assertThat(minmax).hasName("minmax.go")
                        .hasCoveredLines(3, 4, 7, 8, 9, 10, 11, 12, 13)
                        .hasMissedLines(5, 6),
                sub -> assertThat(sub).hasName("sub.go")
                        .hasCoveredLines(3, 4, 5)
                        .hasNoMissedLines(),
                main -> assertThat(main).hasName("main.go")
                        .hasNoCoveredLines()
                        .hasMissedLines(11, 12, 13, 14, 15, 16, 17),
                cpu -> assertThat(cpu).hasName("main.go")
                        .hasCoveredLines(3, 4, 5)
                        .hasNoMissedLines());
    }

    private void createDemoModules(final Path workspace) throws IOException {
        writeFile(workspace, GO_MOD, """
                module hello.test

                go 1.25.0

                replace ext => ./hack/ext
                replace ext/sub => ./hack/extsub
                """);
        writeFile(workspace.resolve("hack").resolve("ext"), GO_MOD, """
                module ext

                go 1.25.0
                """);
        writeFile(workspace.resolve("hack").resolve("extsub"), GO_MOD, "module ext/sub\n");
    }

    private Path writeFile(final Path directory, final String fileName, final String content) throws IOException {
        Files.createDirectories(directory);

        return Files.writeString(directory.resolve(fileName), content);
    }

    private String readDemoProfile() throws IOException {
        try (var stream = Objects.requireNonNull(
                GoCovParserTest.class.getResourceAsStream(getFolder() + "/" + DEMO_PROFILE))) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private ModuleNode parseReportOf(final Path report) throws IOException {
        try (var reader = Files.newBufferedReader(report)) {
            return createParser(ProcessingMode.FAIL_FAST).parse(reader, report.toString(), getLog());
        }
    }
}
