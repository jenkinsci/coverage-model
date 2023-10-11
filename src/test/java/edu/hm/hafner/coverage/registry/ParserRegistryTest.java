package edu.hm.hafner.coverage.registry;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.parser.CoberturaParser;
import edu.hm.hafner.coverage.parser.JacocoParser;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;

import static org.assertj.core.api.Assertions.*;

class ParserRegistryTest {
    @Test
    void shouldCreateSomeParsers() {
        var registry = new ParserRegistry();

        assertThat(registry.getParser(CoverageParserType.COBERTURA.name(), ProcessingMode.FAIL_FAST)).isInstanceOf(CoberturaParser.class);
        assertThat(registry.getParser(CoverageParserType.JACOCO, ProcessingMode.IGNORE_ERRORS)).isInstanceOf(JacocoParser.class);
    }

    @Test
    void shouldThrowExceptionForNotSupportedTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParserRegistry().getParser("UNKNOWN", ProcessingMode.FAIL_FAST))
                .withMessageContaining("Unknown parser name: UNKNOWN");
    }
}
