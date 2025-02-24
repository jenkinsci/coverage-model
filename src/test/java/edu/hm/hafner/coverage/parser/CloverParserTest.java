package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.assertions.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
    @SuppressWarnings({"PMD.OptimizableToArrayCall", "PMD.AvoidThrowingRawExceptionTypes"})
    void testBasic() {
        var root = readReport("clover.xml");
        for (FileNode f : root.getAllFileNodes()) {
            switch (f.getFileName()) {
                case "File1.js":
                    Set<Integer> covered = new HashSet<>();
                    addRange(covered, 4, 7);
                    addRange(covered, 12, 22);
                    addRange(covered, 24, 43);
                    addRange(covered, 45, 77);
                    addRange(covered, 79, 77);
                    Assertions.assertThat(f).hasMissedLines().hasCoveredLines(covered.toArray(new Integer[covered.size()]));

                    break;
                case "File2.js":
                    Assertions.assertThat(f).hasMissedLines(92, 127, 204, 369, 492, 503, 515).hasCoveredLines(21, 38, 51, 65, 79, 105, 117, 138, 151, 164, 176, 190, 215, 228, 243, 257, 268, 287, 303, 317, 329, 339, 349, 359, 380, 393, 405, 416, 429, 443, 456, 467, 480);
                    break;
                case "File3.jsx":
                    Assertions.assertThat(f).hasMissedLines(45, 46, 78, 104, 105, 106).hasCoveredLines(13, 21, 26, 29, 32, 60, 61, 62, 89, 93, 103);
                    break;
                case "File4.jsx":
                    Assertions.assertThat(f).hasMissedLines(8, 50, 51, 58).hasCoveredLines(4, 11, 14, 30, 38, 43, 46, 49, 57, 61, 68, 72, 77, 81, 85, 89, 90);
                    break;
                case "File5.jsx":
                    Assertions.assertThat(f).hasMissedLines(25).hasCoveredLines(18, 19, 20, 24, 31, 50, 59);
                    break;
                case "File6.jsx":
                    Assertions.assertThat(f).hasMissedLines(32, 33, 35, 92).hasCoveredLines(23, 24, 31, 38, 72, 79, 90);
                    break;
                default:
                    return;
            }
        }
    }

    private static void addRange(final Set<Integer> collection, final int start, final int end) {
        // generate a range of integers from start to end (inclusive)
        for (int i = start; i <= end; i++) {
            collection.add(i);
        }
    }

    @Test
    void shouldCreateEmptyPackage() {
        var root = readReport("clover-empty-package.xml");
        var line = new Coverage.CoverageBuilder().withMetric(Metric.LINE);
        assertThat(root.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                file -> Assertions.assertThat(file)
                        .hasFileName("HelloWidget.jsx")
                        .hasNoMissedLines()
                        .hasCoveredLines(15, 19, 36, 37, 38, 46, 47, 48, 59, 71, 80, 81)
                        .hasValues(line.withCovered(12).withTotal(12).build()),
                file -> Assertions.assertThat(file)
                        .hasFileName("HelpInfo.jsx")
                        .hasNoMissedLines()
                        .hasCoveredLines(7, 12, 16, 24, 29, 36, 37, 41, 55, 56, 59, 64, 78, 79, 150)
                        .hasValues(line.withCovered(15).withTotal(15).build()),
                file -> Assertions.assertThat(file)
                        .hasFileName("UserInfoForm.jsx")
                        .hasNoMissedLines()
                        .hasCoveredLines(9, 13, 17, 22, 29, 30, 31, 32, 33, 34, 35, 36, 42, 45, 49, 58, 62, 65, 66, 68, 72, 79, 83, 86, 87, 95, 96, 98, 99, 103, 104, 105, 106, 109, 110, 111, 115, 116, 124, 131, 133, 166)
                        .hasValues(line.withCovered(42).withTotal(42).build())
        );
    }
}
