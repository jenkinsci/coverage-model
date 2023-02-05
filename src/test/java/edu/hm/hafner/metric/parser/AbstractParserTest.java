package edu.hm.hafner.metric.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

import static org.assertj.core.api.Assertions.*;

/**
 * Baseclass for parser tests.
 *
 * @author Ullrich Hafner
 */
abstract class AbstractParserTest {
    ModuleNode readReport(final String fileName) {
        try (InputStream stream = AbstractParserTest.class.getResourceAsStream(fileName);
                Reader reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
            return createParser().parse(reader);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    abstract CoverageParser createParser();

    @Test
    void shouldFailWhenParsingInvalidFiles() {
        assertThatExceptionOfType(ParsingException.class).isThrownBy(() -> readReport("/design.puml"));
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("empty.xml"));
    }
}
