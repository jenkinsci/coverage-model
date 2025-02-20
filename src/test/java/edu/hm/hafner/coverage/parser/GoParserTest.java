package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Node;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import java.util.NoSuchElementException;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.assertThat;
import static edu.hm.hafner.coverage.assertions.Assertions.assertThatExceptionOfType;

@DefaultLocale("en")
class GoParserTest extends AbstractParserTest {
    @Override
    protected String getFolder() {
        return "go";
    }

    @Override
    GoParser createParser(final ProcessingMode processingMode) {
        return new GoParser(processingMode);
    }

    @Test
    void testAtomic() {
        var root = readReport("go-coverage-atomic.out");
        assertThat(root.getAllFileNodes()).satisfiesExactlyInAnyOrder(
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

        assertThat(root.getAll(MODULE))
                .hasSize(1)
                .map(Node::getName)
                .containsExactly("github.com/example/project");
        assertThat(root.getAll(PACKAGE)).hasSize(4).map(Node::getName)
                .containsExactly("pkg.utils", "pkg.db", "cmd", "pkg.test");

        assertThat(root.getAllFileNodes())
                .hasSize(4)
                .map(FileNode::getFileName)
                .containsExactly("file1.go", "file2.go", "file3.go", "file4.go");

        assertThat(root.getAll(CLASS)).isEmpty();

        var builder = new Coverage.CoverageBuilder();
        assertThat(root.aggregateValues()).contains(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(3).withMissed(1).build(),
                builder.withMetric(FILE).withCovered(3).withMissed(1).build());
    }

    @Test
    void testRelativeImport() {
        var root = readReport("go-relative-import.out");
        assertThat(root.getAllFileNodes()).satisfiesExactlyInAnyOrder(
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
        assertThat(root.getAll(MODULE))
                .hasSize(1)
                .map(Node::getName)
                .containsExactly("example");
        assertThat(root.getAll(PACKAGE)).hasSize(4).map(Node::getName)
                .containsExactly("project.pkg.utils", "common.pkg.db", "project.cmd", "common.pkg.test");

        assertThat(root.getAllFileNodes())
                .hasSize(4)
                .map(FileNode::getFileName)
                .containsExactly("file1.go", "file2.go", "file3.go", "file4.go");

        assertThat(root.getAll(CLASS)).isEmpty();

        var builder = new Coverage.CoverageBuilder();
        assertThat(root.aggregateValues()).contains(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(3).withMissed(1).build(),
                builder.withMetric(FILE).withCovered(3).withMissed(1).build());
    }

    @Override
    @Test
    void shouldFailWhenParsingInvalidFiles() {
        assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> readReport("/design.puml"));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("empty.out"));
    }

    @Override
    @Test
    void shouldFailWhenEmptyFilesAreNotIgnored() {
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("empty.out"));

        var report = readReport("empty.out", ProcessingMode.IGNORE_ERRORS);

        assertThat(report).hasNoChildren().hasNoValues();
        assertThat(getLog().getErrorMessages()).contains("[GoParser] The processed file 'empty.out' does not contain data.");
    }
}
