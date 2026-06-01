package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.MutationStatus;

import static edu.hm.hafner.coverage.Metric.MUTATION;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link StrykerParser}.
 *
 * @author Akash Manna
 */
class StrykerParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new StrykerParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "stryker";
    }

    @Test
    void shouldParseStrykerMutationReport() {
        var tree = readReport("mutation-report.json");

        assertThat(tree.getAllFileNodes()).hasSize(1);

        var file = tree.getAllFileNodes().getFirst();
        assertThat(file).hasName("add.js").hasRelativePath("src/math/add.js");
        assertThat(file.getMutations()).hasSize(4).extracting(Mutation::getStatus)
                .containsExactly(
                        MutationStatus.KILLED,
                        MutationStatus.SURVIVED,
                        MutationStatus.NO_COVERAGE,
                        MutationStatus.TIMED_OUT);
        assertThat(file.getValue(MUTATION)).hasValueSatisfying(value ->
            assertThat(value).isInstanceOfSatisfying(Coverage.class,
                coverage -> assertThat(coverage).hasCovered(1).hasMissed(3)));
        assertThat(getLog().hasErrors()).isFalse();
    }
}