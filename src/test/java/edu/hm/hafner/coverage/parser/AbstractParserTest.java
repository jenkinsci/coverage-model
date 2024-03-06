package edu.hm.hafner.coverage.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.apache.commons.io.input.BOMInputStream;
import org.junit.jupiter.api.Test;

import com.google.errorprone.annotations.MustBeClosed;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Baseclass for parser tests.
 *
 * @author Ullrich Hafner
 */
abstract class AbstractParserTest {
    private final FilteredLog log = new FilteredLog("Errors");

    ModuleNode readReport(final String fileName) {
        return readReport(fileName, ProcessingMode.FAIL_FAST);
    }

    ModuleNode readReport(final String fileName, final ProcessingMode processingMode) {
        var parser = createParser(processingMode);

        return readReport(fileName, parser);
    }

    ModuleNode readReport(final String fileName, final CoverageParser parser) {
        try (InputStream stream = createFile(fileName);
                Reader reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            return parser.parse(reader, fileName, log);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @MustBeClosed
    @SuppressFBWarnings("OBL")
    private InputStream createFile(final String fileName) throws IOException {
        String name;
        if (fileName.startsWith("/")) {
            name = fileName;
        }
        else {
            name = getFolder() + "/" + fileName;
        }
        var inputStream = Objects.requireNonNull(AbstractParserTest.class.getResourceAsStream(name),
                "File not found: " + name);

        return BOMInputStream.builder().setInputStream(inputStream).get();
    }

    protected abstract String getFolder();

    abstract CoverageParser createParser(ProcessingMode processingMode);

    protected FilteredLog getLog() {
        return log;
    }

    @Test
    void shouldFailWhenParsingInvalidFiles() {
        assertThatExceptionOfType(ParsingException.class).isThrownBy(() -> readReport("/design.puml"));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("../empty.xml"));
    }

    @Test
    void shouldFailWhenEmptyFilesAreNotIgnored() {
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("empty.xml"));

        var report = readReport("empty.xml", ProcessingMode.IGNORE_ERRORS);
        assertThat(report).hasNoChildren().hasNoValues();

        var parserName = createParser(ProcessingMode.FAIL_FAST).getClass().getSimpleName();
        assertThat(getLog().getErrorMessages()).contains(String.format("[%s] The processed file 'empty.xml' does not contain data.", parserName));
    }
}
