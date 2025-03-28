package edu.hm.hafner.coverage.registry;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.parser.CoberturaParser;
import edu.hm.hafner.coverage.parser.JacocoParser;
import edu.hm.hafner.coverage.parser.JunitParser;
import edu.hm.hafner.coverage.parser.MetricsParser;
import edu.hm.hafner.coverage.parser.NunitParser;
import edu.hm.hafner.coverage.parser.OpenCoverParser;
import edu.hm.hafner.coverage.parser.PitestParser;
import edu.hm.hafner.coverage.parser.VectorCastParser;
import edu.hm.hafner.coverage.parser.XunitParser;
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
        assertThat(registry.get(CoverageParserType.JUNIT, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(JunitParser.class);
        assertThat(registry.get(CoverageParserType.METRICS, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(MetricsParser.class);
        assertThat(registry.get(CoverageParserType.NUNIT, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(NunitParser.class);
        assertThat(registry.get(CoverageParserType.OPENCOVER, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(OpenCoverParser.class);
        assertThat(registry.get(CoverageParserType.PIT.name(), ProcessingMode.FAIL_FAST))
                .isInstanceOf(PitestParser.class);
        assertThat(registry.get(CoverageParserType.VECTORCAST, ProcessingMode.FAIL_FAST))
                .isInstanceOf(VectorCastParser.class);
        assertThat(registry.get(CoverageParserType.XUNIT, ProcessingMode.IGNORE_ERRORS))
                .isInstanceOf(XunitParser.class);

        for (var parser : CoverageParserType.values()) {
            assertThat(registry.get(parser.name(), ProcessingMode.FAIL_FAST))
                    .isNotNull();
        }
    }

    @Test
    void shouldThrowExceptionForNotSupportedTypes() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ParserRegistry().get("UNKNOWN", ProcessingMode.FAIL_FAST))
                .withMessageContaining("Unknown parser name: UNKNOWN");
    }
}
