package edu.hm.hafner.metric;

import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.assertions.Assertions.*;

abstract class AbstractNodeTest {
    private static final String NAME = "Node Name";
    private static final String CHILD = "Child";

    abstract Metric getMetric();

    abstract Node createNode(String name);

    @Test
    void shouldCreateSingleNode() {
        Node node = createNode(NAME);

        verifySingleNode(node);
    }

    @Test
    void shouldCopyNode() {
        Node parent = createNode(NAME);
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

        verifySingleNode(parent.copyEmpty());
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
                .hasNoValues()
                .isRoot()
                .doesNotHaveParent()
                .hasParentName(Node.ROOT);

        assertThat(node.getAll(getMetric())).containsOnly(node);
        assertThat(node.find(getMetric(), NAME)).contains(node);
        assertThat(node.find(getMetric(), "does not exist")).isEmpty();
        assertThat(node.getAll(Metric.LOC)).isEmpty();
    }

    private TreeMap<Object, Object> createMetricDistributionWithMissed(final int missed) {
        var distribution = new TreeMap<>();
        distribution.put(getMetric(), new Coverage(getMetric(), 0, missed));
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
