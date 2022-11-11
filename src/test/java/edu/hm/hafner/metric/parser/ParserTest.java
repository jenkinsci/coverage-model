package edu.hm.hafner.metric.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.util.ResourceTest;

/**
 * Baseclass for parser tests.
 *
 * @author Ullrich Hafner
 */
class ParserTest extends ResourceTest {
    ModuleNode readReport(final String fileName, final XmlParser parser) {
        try (Reader reader = new InputStreamReader(asInputStream(fileName), StandardCharsets.UTF_8)) {
            return parser.parse(reader);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
