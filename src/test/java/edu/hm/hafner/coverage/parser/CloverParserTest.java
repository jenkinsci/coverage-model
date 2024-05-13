package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.Metric;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class CloverParserTest extends AbstractParserTest {
    @Override
    protected String getFolder() {
        return "clover";
    }

    @Override
    CoverageParser createParser(final CoverageParser.ProcessingMode processingMode) {
        return new CloverParser(processingMode);
    }

    @Test
    void shouldReadCloverReport() {
        var root = readReport("clover.xml");
        var line = new CoverageBuilder().withMetric(Metric.LINE);
        var branch = new CoverageBuilder().withMetric(Metric.BRANCH);
        assertThat(root.getAllFileNodes().get(2))
                .hasFileName("File3.jsx")
                .hasMissedLines(45, 46, 78, 104, 105, 106)
                .hasCoveredLines(13, 21, 26, 29, 32, 60, 61, 62, 89, 93, 103)
                .hasValues(line.withCovered(11).withTotal(17).build());
        assertThat(root.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                file -> assertThat(file)
                        .hasFileName("File1.js")
                        .hasNoMissedLines()
                        .hasCoveredLines(4, 5, 6, 7, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 24, 25, 26, 27, 28,
                                29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 45, 46, 47, 48, 49, 50,
                                51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
                                73, 74, 75, 76, 77)
                        .hasValues(line.withCovered(68).withTotal(68).build()),
                file -> assertThat(file)
                        .hasFileName("File2.js")
                        .hasMissedLines(92, 127, 204, 369, 492, 503, 515)
                        .hasCoveredLines(21, 38, 51, 65, 79, 105, 117, 138, 151, 164, 176, 190, 215, 228, 243, 257,
                                268, 287, 303, 317, 329, 339, 349, 359, 380, 393, 405, 416, 429, 443, 456, 467, 480)
                        .hasValues(line.withCovered(33).withTotal(40).build()),
                file -> assertThat(file)
                        .hasFileName("File3.jsx")
                        .hasMissedLines(45, 46, 78, 104, 105, 106)
                        .hasCoveredLines(13, 21, 26, 29, 32, 60, 61, 62, 89, 93, 103)
                        .hasValues(line.withCovered(11).withTotal(17).build(),
                                branch.withCovered(2).withTotal(2).build()),
                file -> assertThat(file)
                        .hasFileName("File4.jsx")
                        .hasMissedLines(8, 50, 51, 58)
                        .hasCoveredLines(4, 11, 14, 30, 38, 43, 46, 49, 57, 61, 68, 72, 77, 81, 85, 89, 90)
                        .hasValues(line.withCovered(17).withTotal(21).build()),
                file -> assertThat(file)
                        .hasFileName("File5.jsx")
                        .hasMissedLines(25)
                        .hasCoveredLines(18, 19, 20, 24, 31, 50, 59)
                        .hasValues(line.withCovered(7).withTotal(8).build()),
                file -> assertThat(file)
                        .hasFileName("File6.jsx")
                        .hasMissedLines(32, 33, 35, 92)
                        .hasCoveredLines(23, 24, 31, 38, 72, 79, 90)
                        .hasValues(line.withCovered(7).withTotal(11).build())
        );
    }
}