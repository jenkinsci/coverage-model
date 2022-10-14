package edu.hm.hafner.metric;

import java.util.TreeMap;

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
        parent.addChild(child);

        assertThat(parent)
                .hasChildren(child)
                .hasMetricsDistribution(createMetricDistributionWithMissed(2));
        assertThat(parent.getAll(getMetric())).containsOnly(parent, child);

        assertThat(parent.find(getMetric(), NAME)).contains(parent);
        assertThat(parent.find(getMetric(), CHILD)).contains(child);
        assertThat(child.find(getMetric(), NAME)).isEmpty();
        assertThat(child.find(getMetric(), CHILD)).contains(child);

        verifySingleNode(parent.copyNode());
        assertThat(parent.combineWith(parent)).isEqualTo(parent);
        assertThat(parent.copyTree()).isEqualTo(parent);
    }

    private void verifySingleNode(final Node node) {
        assertThat(node)
                .hasName(NAME)
                .hasMetrics(getMetric())
                .hasMetricsDistribution(createMetricDistributionWithMissed(1))
                .hasNoChildren()
                .doesNotHaveChildren()
                .isRoot()
                .doesNotHaveParent()
                .hasParentName(Node.ROOT);

        assertThat(node.getAll(getMetric())).containsOnly(node);
        assertThat(node.find(getMetric(), NAME)).contains(node);
        assertThat(node.find(getMetric(), "does not exist")).isEmpty();
        assertThat(node.getAll(Metric.LOC)).isEmpty();

        assertThat(node.copyTree()).isEqualTo(node);
        assertThat(node.copy()).hasNoValues();
    }

    private TreeMap<Object, Object> createMetricDistributionWithMissed(final int missed) {
        var distribution = new TreeMap<>();
        var builder = new CoverageBuilder();
        builder.setMetric(getMetric()).setCovered(0).setMissed(missed);
        distribution.put(getMetric(), builder.build());
        distribution.put(Metric.BRANCH, BRANCH_COVERAGE);
        return distribution;
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
