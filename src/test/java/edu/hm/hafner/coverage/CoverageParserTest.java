package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Test-class to provide tests for protected (static) methods of abstract class
 * {@link CoverageParser}.
 *
 * @author Jannik Treichel
 */
class CoverageParserTest {
    @Test
    void shouldReturnZeroOnInvalidStringParsing() {
        assertThat(CoverageParser.parseInteger("NO_NUMBER")).isEqualTo(0);
        assertThat(CoverageParser.parseInteger("111")).isEqualTo(111);
    }
}
