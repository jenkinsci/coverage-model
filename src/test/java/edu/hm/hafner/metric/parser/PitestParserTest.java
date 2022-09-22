package edu.hm.hafner.metric.parser;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.MutationValue;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

@Disabled("FIXME: one leaf with the same name only?")
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

        assertThat(((MutationValue) tree.getValue(MUTATION).get()).getResult()).hasKilled(222).hasSurvived(24);

        assertThat(getMutationLeaves(tree).stream()
                .map(MutationValue.class::cast)
                .filter(leaf -> leaf.getMutator().name().equals("NOT_SPECIFIED"))
                .count()).isOne();
    }

    private List<Value> getMutationLeaves(final Node tree) {

        List<Value> children = tree.getChildren().stream()
                .map(this::getMutationLeaves)
                .flatMap(List::stream).collect(Collectors.toList());

        children.addAll(tree.getValues());
        return children;
    }

    private Node readExampleReport() {
        PitestParser parser = new PitestParser("src/test/resources/mutations.xml");
        return parser.getRootNode();
    }
}
