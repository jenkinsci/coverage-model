package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;

@DefaultLocale("en")
class MetricsParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new MetricsParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "metrics";
    }

    @Test
    void simpleMetrics() {
        readReport("metrics.xml");
    }
}
