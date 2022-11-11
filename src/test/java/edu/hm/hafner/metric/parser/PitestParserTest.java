package edu.hm.hafner.metric.parser;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.MutationValue;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

@DefaultLocale("en")
class PitestParserTest extends ParserTest {
    @Test
    void shouldConvertMutationsToTree() {
        ModuleNode tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        // edu.hm.hafner.coverage, edu.hm.hafner.metric.parser, edu.hm.hafner.metric
        assertThat(tree.getAll(PACKAGE)).hasSize(3);
        assertThat(tree.getAll(FILE)).hasSize(10);
        // CoverageNode, FileCoverageNode, CoverageLeaf, MethodCoverageNode, PackageCoverageNode
        // CoberturaParser, JacocoParser, XmlParser
        // Metric, Coverage
        assertThat(tree.getAll(CLASS)).hasSize(10);
        assertThat(tree.getAll(METHOD)).hasSize(99);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, MUTATION);

        MutationValue mutationValue = (MutationValue) tree.getValue(MUTATION).get();

        // Total 246
        assertThat(mutationValue).hasKilled(222).hasSurvived(24);

        assertThat(mutationValue.getMutations().stream()
                .filter(mutation -> mutation.getMutator().name().equals("NOT_SPECIFIED"))
                .count()).isOne();
    }

    private ModuleNode readExampleReport() {
        return readReport("src/test/resources/mutations.xml", new PitestParser());
    }
}
