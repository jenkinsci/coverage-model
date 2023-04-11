package edu.hm.hafner.coverage;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.util.SerializableTest;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.api.EqualsVerifierApi;
import nl.jqno.equalsverifier.api.SingleTypeEqualsVerifierApi;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

abstract class AbstractNodeTest extends SerializableTest<Node> {
    private static final String NAME = "Node Name";
    private static final String CHILD = "Child";
    private static final Coverage MUTATION_COVERAGE = new CoverageBuilder().setMetric(Metric.MUTATION)
            .setCovered(5)
            .setMissed(10)
            .build();

    @Override
    protected Node createSerializable() {
        return createNode("Serialized");
    }

    abstract Metric getMetric();

    abstract Node createNode(String name);

    @Test
    void shouldCreateSingleNode() {
        Node node = createParentWithValues();

        verifySingleNode(node);
    }

    private Node createParentWithValues() {
        Node node = createNode(NAME);
        node.addValue(new LinesOfCode(15));
        node.addValue(MUTATION_COVERAGE);
        return node;
    }

    @Test
    void shouldCopyNode() {
        Node parent = createParentWithValues();
        Node child = createNode(CHILD);
        child.addValue(MUTATION_COVERAGE);
        parent.addChild(child);

        assertThat(parent)
                .hasChildren(child);
        assertThat(parent.aggregateValues()).containsExactlyElementsOf(
                createMetricDistributionWithMissed(2));
        assertThat(parent.getAll(getMetric())).containsOnly(parent, child);

        assertThat(parent.find(getMetric(), NAME)).contains(parent);
        assertThat(parent.find(getMetric(), CHILD)).contains(child);
        assertThat(child.find(getMetric(), NAME)).isEmpty();
        assertThat(child.find(getMetric(), CHILD)).contains(child);

        verifySingleNode(parent.copyNode());
        assertThat(parent.merge(parent)).isEqualTo(parent);
        assertThat(parent.copyTree()).isEqualTo(parent);
    }

    private void verifySingleNode(final Node node) {
        assertThat(node)
                .hasName(NAME)
                .hasMetrics(getMetric())
                .hasNoChildren()
                .doesNotHaveChildren()
                .isRoot()
                .doesNotHaveParent()
                .hasParentName(Node.ROOT);
        assertThat(node.aggregateValues()).containsExactlyElementsOf(
                createMetricDistributionWithMissed(1));

        assertThat(node.getAll(getMetric())).containsOnly(node);
        assertThat(node.find(getMetric(), NAME)).contains(node);
        assertThat(node.find(getMetric(), "does not exist")).isEmpty();
        assertThat(node.getAll(Metric.LOC)).isEmpty();

        assertThat(node.copyTree()).isEqualTo(node);
        assertThat(node.copy()).hasNoValues();
    }

    private List<? extends Value> createMetricDistributionWithMissed(final int missed) {
        var builder = new CoverageBuilder();
        builder.setMetric(getMetric()).setCovered(0).setMissed(missed);
        return List.of(builder.build(), MUTATION_COVERAGE);
    }

    @Test
    void shouldAdhereToEquals() {
        SingleTypeEqualsVerifierApi<? extends Node> equalsVerifier = EqualsVerifier.forClass(
                        createNode(NAME).getClass())
                .withPrefabValues(Node.class, new PackageNode("src"), new PackageNode("test"))
                .withIgnoredFields("parent")
                .withRedefinedSuperclass();
        configureEqualsVerifier(equalsVerifier);
        equalsVerifier.verify();
    }

    void configureEqualsVerifier(final EqualsVerifierApi<? extends Node> verifier) {
        // no additional configuration
    }
}
