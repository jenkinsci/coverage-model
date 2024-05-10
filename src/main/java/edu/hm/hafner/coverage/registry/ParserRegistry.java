package edu.hm.hafner.coverage.registry;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.parser.CoberturaParser;
import edu.hm.hafner.coverage.parser.JacocoParser;
import edu.hm.hafner.coverage.parser.JunitParser;
import edu.hm.hafner.coverage.parser.NunitParser;
import edu.hm.hafner.coverage.parser.OpenCoverParser;
import edu.hm.hafner.coverage.parser.PitestParser;
import edu.hm.hafner.coverage.parser.XunitParser;
import edu.hm.hafner.coverage.parser.VectorCASTParser;

/**
 * Provides a registry for all available {@link CoverageParserType parsers}.
 *
 * @author Ullrich Hafner
 */
public class ParserRegistry {
    /** Supported parsers. */
    public enum CoverageParserType {
        COBERTURA,
        NUNIT,
        OPENCOVER,
        JACOCO,
        PIT,
        JUNIT,
        VECTORCAST,
        XUNIT
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
    public CoverageParser get(final String parserName, final ProcessingMode processingMode) {
        try {
            return get(CoverageParserType.valueOf(StringUtils.upperCase(parserName)), processingMode);
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
    public CoverageParser get(final CoverageParserType parser, final ProcessingMode processingMode) {
        switch (parser) {
            case COBERTURA:
                return new CoberturaParser(processingMode);
            case OPENCOVER:
                return new OpenCoverParser(processingMode);
            case NUNIT:
                return new NunitParser(processingMode);
            case JACOCO:
                return new JacocoParser(processingMode);
            case PIT:
                return new PitestParser(processingMode);
            case JUNIT:
                return new JunitParser(processingMode);
            case XUNIT:
                return new XunitParser(processingMode);
            case VECTORCAST:
                return new VectorCASTParser(processingMode);
        }
        throw new IllegalArgumentException("Unknown parser type: " + parser);
    }
}
