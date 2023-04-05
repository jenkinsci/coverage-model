package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class ContainerNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.CONTAINER;
    }

    @Override
    Node createNode(final String name) {
        return new ContainerNode(name);
    }

    @Test
    void shouldAggregateSourceFolders() {
        var root = new ContainerNode("root");
        var left = new ModuleNode("left");
        root.addChild(left);
        var right = new ModuleNode("right");
        root.addChild(right);

        assertThat(root.getSourceFolders()).isEmpty();
        assertThat(root.getChildren()).containsExactly(left, right);

        left.addSource("left/path");
        right.addSource("right/path");
        assertThat(root.getSourceFolders()).containsExactly("left/path", "right/path");
    }
}
