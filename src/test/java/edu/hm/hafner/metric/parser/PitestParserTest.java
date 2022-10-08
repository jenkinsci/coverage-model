package edu.hm.hafner.metric.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.MutationValue;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

class PitestParserTest {
    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

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
        try (FileInputStream stream = new FileInputStream("src/test/resources/mutations.xml");
                InputStreamReader reader = new InputStreamReader(stream)) {
            return new PitestParser().parse(reader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
