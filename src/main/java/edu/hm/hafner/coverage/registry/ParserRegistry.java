package edu.hm.hafner.coverage.registry;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.parser.CoberturaParser;
import edu.hm.hafner.coverage.parser.JacocoParser;
import edu.hm.hafner.coverage.parser.PitestParser;

/**
 * Provides a registry for all available {@link CoverageParserType parsers}.
 *
 * @author Ullrich Hafner
 */
public class ParserRegistry {
    /** Supported parsers. */
    public enum CoverageParserType {
        COBERTURA,
        JACOCO,
        PIT
    }

    /**
     * Returns the parser for the specified name.
     *
     * @param parserName
     *         the unique name of the parser
     * @param processingMode
     *         determines whether to ignore errors
     *
     * @return the created parser
     */
    public CoverageParser getParser(final String parserName, final ProcessingMode processingMode) {
        try {
            return getParser(CoverageParserType.valueOf(parserName), processingMode);
        }
        catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown parser name: " + parserName, exception);
        }
    }

    /**
     * Returns the parser for the specified name.
     *
     * @param parser
     *         the parser
     * @param processingMode
     *         determines whether to ignore errors
     *
     * @return the created parser
     */
    public CoverageParser getParser(final CoverageParserType parser, final ProcessingMode processingMode) {
        switch (parser) {
            case COBERTURA:
                return new CoberturaParser(processingMode);
            case JACOCO:
                return new JacocoParser();
            case PIT:
                return new PitestParser();
        }
        throw new IllegalArgumentException("Unknown parser type: " + parser);
    }
}
