package edu.hm.hafner.parser;

import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.model.Node;

import static edu.hm.hafner.model.Metric.*;
import static edu.hm.hafner.model.Metric.CLASS;
import static edu.hm.hafner.model.Metric.FILE;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

class PitestParserTest {

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldConvertMutationsToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(2);
        assertThat(tree.getAll(FILE)).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(10);
        assertThat(tree.getAll(METHOD)).hasSize(99);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, MUTATION)
                .hasToString("[Module]" + "mutations.xml");
    }

    private Node readExampleReport() {
        PitestParser parser = new PitestParser("src/test/resources/mutations.xml");
        return parser.getRootNode();
    }

}