package edu.hm.hafner.parser;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.model.Leaf;
import edu.hm.hafner.model.Node;
import edu.hm.hafner.mutation.MutationLeaf;

import static edu.hm.hafner.assertions.Assertions.*;
import static edu.hm.hafner.model.Metric.CLASS;
import static edu.hm.hafner.model.Metric.FILE;
import static edu.hm.hafner.model.Metric.*;

class PitestParserTest {

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldConvertMutationsToTree() {
        Node tree = readExampleReport();

        assertThat(tree.getAll(MODULE)).hasSize(1);
        assertThat(tree.getAll(PACKAGE)).hasSize(4);
        assertThat(tree.getAll(FILE)).hasSize(10);
        assertThat(tree.getAll(CLASS)).hasSize(10);
        assertThat(tree.getAll(METHOD)).hasSize(92);

        assertThat(tree).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, METHOD, MUTATION)
                .hasToString("[Module]" + " mutations.xml");

        assertThat(tree.getMutationResult()).hasKilled(222).hasSurvived(24);

        assertThat(getMutationLeaves(tree).stream()
                .map(MutationLeaf.class::cast)
                .filter(leaf -> leaf.getMutator().name().equals("NOT_SPECIFIED"))
                .count()).isOne();
    }

    private List<Leaf> getMutationLeaves(final Node tree) {

        List<Leaf> children = tree.getChildren().stream()
                .map(this::getMutationLeaves)
                .flatMap(List::stream).collect(Collectors.toList());

        children.addAll(tree.getLeaves());
        return children;
    }

    private Node readExampleReport() {
        PitestParser parser = new PitestParser("src/test/resources/mutations.xml");
        return parser.getRootNode();
    }

}