package edu.hm.hafner.coverage.registry;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.parser.CoberturaParser;
import edu.hm.hafner.coverage.parser.JacocoParser;
import edu.hm.hafner.coverage.parser.JunitParser;
import edu.hm.hafner.coverage.parser.PitestParser;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;

import static org.assertj.core.api.Assertions.*;

class ParserRegistryTest {
    @Test
    void shouldCreateSomeParsers() {
        var registry = new ParserRegistry();

        assertThat(registry.get(CoverageParserType.COBERTURA.name(), ProcessingMode.FAIL_FAST))
                .isInstanceOf(CoberturaParser.class);
        assertThat(registry.get(CoverageParserType.JACOCO, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(JacocoParser.class);
        assertThat(registry.get(CoverageParserType.PIT.name(), ProcessingMode.FAIL_FAST))
                .isInstanceOf(PitestParser.class);
        assertThat(registry.get(CoverageParserType.JUNIT, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(JunitParser.class);
    }

    @Test
    void shouldThrowExceptionForNotSupportedTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParserRegistry().get("UNKNOWN", ProcessingMode.FAIL_FAST))
                .withMessageContaining("Unknown parser name: UNKNOWN");
    }
}
