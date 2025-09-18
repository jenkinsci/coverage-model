package edu.hm.hafner.coverage.registry;

import org.apache.commons.lang3.Strings;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.registry.ParserRegistry.CoverageParserType;

import static org.assertj.core.api.Assertions.*;

class ParserRegistryTest {
    @Test
    void shouldCreateSomeParsers() {
        var registry = new ParserRegistry();

        for (var parserType : CoverageParserType.values()) {
            var parser = registry.get(parserType.name(), ProcessingMode.FAIL_FAST);
            assertThat(parser).isNotNull();
            assertThat(parser.getClass().toString()).containsIgnoringCase(
                    Strings.CS.remove(parserType.name(), "_"));
        }
    }

    @Test
    void shouldThrowExceptionForNotSupportedTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParserRegistry().get("UNKNOWN", ProcessingMode.FAIL_FAST))
                .withMessageContaining("Unknown parser name: UNKNOWN");
    }
}
