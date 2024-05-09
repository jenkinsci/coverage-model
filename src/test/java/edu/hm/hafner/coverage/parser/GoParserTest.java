package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.*;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.assertions.Assertions;
import edu.hm.hafner.util.SecureXmlParserFactory;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import java.util.NoSuchElementException;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.assertions.Assertions.*;


@DefaultLocale("en")
class GoParserTest extends AbstractParserTest {
    @Override
    protected String getFolder() {
        return "go";
    }

    @Override
    GoParser createParser(ProcessingMode processingMode) {
        return new GoParser(processingMode);
    }

    @Test
    void testAtomic() throws Exception{
        var root = readReport("go-coverage-atomic.out");
        for (FileNode f: root.getAllFileNodes()) {
            switch (f.getFileName()) {
                case "file1.go":
                    assertThat(f).hasMissedLines(19, 20).hasCoveredLines(5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 24, 25, 30, 31, 32);
                    break;
                case "file2.go":
                    assertThat(f).hasMissedLines(12, 13, 14, 15).hasCoveredLines(17, 18, 19, 20, 21, 22);
                    break;
                case "file3.go":
                    assertThat(f).hasMissedLines(10, 15, 16, 17).hasCoveredLines();
                    break;
                case "file4.go":
                    assertThat(f).hasMissedLines().hasCoveredLines(10, 11);
                    break;
                default:
                    throw new Exception("Unexpected file: " + f.getFileName());
            }
        }

        var builder = new Coverage.CoverageBuilder();
        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(4);
        assertThat(root.getAll(FILE)).hasSize(4);
        assertThat(root.getAll(CLASS)).hasSize(0);
        assertThat(root.aggregateValues()).contains(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(3).withMissed(1).build(),
                builder.withMetric(FILE).withCovered(3).withMissed(1).build());
    }

    @Override
    @Test
    void shouldFailWhenParsingInvalidFiles() {
        super.shouldFailWhenParsingInvalidFilesHelper("/design.puml", "empty.out");
    }

    @Override
    protected String getEmptyFilePath() {
        return "empty.out";
    }
}