package edu.hm.hafner.coverage;
import org.junit.jupiter.api.Test;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;
import static edu.hm.hafner.coverage.assertions.Assertions.*;
/**
 * Test-class to provide tests for protected (static) methods of abstract class
 * {@link CoverageParser}.
 *
 * @author Jannik Treichel
 */
class CoverageParserTest {

    @Test
    void shouldCreateEofException() {
        var parsingException = CoverageParser.createEofException();

        assertThat(parsingException)
                .isInstanceOf(ParsingException.class)
                .hasMessage("Unexpected end of file");
    }

    @Test
    void shouldReturnZeroOnInvalidStringParsing() {
        assertThat(CoverageParser.parseInteger("NO_NUMBER"))
                .isEqualTo(0);
    }
}
