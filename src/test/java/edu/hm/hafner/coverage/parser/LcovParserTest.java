package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Value;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.assertThat;

@DefaultLocale("en")
class LcovParserTest extends AbstractParserTest {
    @Override
    protected String getFolder() {
        return "lcov";
    }

    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new LcovParser(processingMode);
    }

    @Test
    void shouldCreateSimpleCoverage() {
        var report = readReport("single.lcov", ProcessingMode.IGNORE_ERRORS);

        assertThat(report).hasName("-");

        Assertions.assertThat(report.getAll(PACKAGE)).hasSize(1);
        Assertions.assertThat(report.getFiles()).containsExactlyInAnyOrder(
                "src/main.ts");
        Assertions.assertThat(report.getAll(CLASS)).isEmpty();

        var builder = new Coverage.CoverageBuilder();
        Assertions.assertThat(report.aggregateValues()).contains(
                builder.withMetric(PACKAGE).withCovered(1).withTotal(1).build(),
                builder.withMetric(FILE).withCovered(1).withTotal(1).build(),
                builder.withMetric(LINE).withCovered(1).withTotal(1).build(),
                new Value(LOC, 1));

        Assertions.assertThat(report.getAllFileNodes()).map(FileNode::getFileName).containsExactlyInAnyOrder(
                "main.ts");

        Assertions.assertThat(report.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                one -> assertThat(one).hasName("main.ts")
                        .hasNoMissedLines()
                        .hasCoveredLines(12));

        Assertions.assertThat(report.aggregateValues()).contains(
                new Coverage.CoverageBuilder().withMetric(MODULE).withCovered(2).withTotal(2).build());
    }

    @Test
    void shouldCreateCoverages() {
        var report = readReport("report.lcov", ProcessingMode.IGNORE_ERRORS);

        assertThat(report).hasName("-");

        var builder = new Coverage.CoverageBuilder();

        Assertions.assertThat(report.aggregateValues()).contains(
                builder.withMetric(FILE).withCovered(2).withTotal(3).build(),
                builder.withMetric(LINE).withCovered(4).withTotal(6).build(),
                builder.withMetric(INSTRUCTION).withCovered(3).withTotal(6).build(),
                new Value(LOC, 6)
        );

        // file node basenames
        Assertions.assertThat(report.getAllFileNodes()).map(FileNode::getFileName).containsExactlyInAnyOrder(
                "main.ts", "helper.ts", "module.c");
    }
}
