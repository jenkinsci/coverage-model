package edu.hm.hafner.metric;

import java.util.List;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.assertions.Assertions.*;

abstract class AbstractNodeTest {
    private static final String NAME = "Node Name";
    private static final String CHILD = "Child";
    private static final Coverage BRANCH_COVERAGE = new CoverageBuilder().setMetric(Metric.BRANCH)
            .setCovered(5)
            .setMissed(10)
            .build();

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
        node.addValue(BRANCH_COVERAGE);
        return node;
    }

    @Test
    void shouldCopyNode() {
        Node parent = createParentWithValues();
        Node child = createNode(CHILD);
        child.addValue(BRANCH_COVERAGE);
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
        return List.of(builder.build(), BRANCH_COVERAGE);
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(createNode(NAME).getClass()).withPrefabValues(
                Node.class,
                new PackageNode("src"),
                new PackageNode("test")
        ).withIgnoredFields("parent").withRedefinedSuperclass().verify();
    }
}
