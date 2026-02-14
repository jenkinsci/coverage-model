package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Value;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class GoCovParserTest extends AbstractParserTest {
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
    void shouldHandleDemoExample() {
        var report = readReport("go-demo.coverprofile");

        assertThat(report).hasName("ext");

        assertThat(report.aggregateValues()).contains(
                new CoverageBuilder().withMetric(MODULE).withCovered(3).withTotal(3).build());

        assertThat(report.getAll(PACKAGE)).hasSize(4);
        
        assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "sum.go",
                "stat/minmax.go",
                "main.go",
                "cpu/main.go");

        assertThat(report.getAllFileNodes()).map(FileNode::getFileName).containsExactlyInAnyOrder(
                "sum.go", "minmax.go", "main.go", "main.go");
    }
}
