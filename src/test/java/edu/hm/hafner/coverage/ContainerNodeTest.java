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
        var containerNode = new ContainerNode("root");
        var left = new ModuleNode("left");
        containerNode.addChild(left);
        var right = new ModuleNode("right");
        containerNode.addChild(right);

        assertThat(containerNode).hasNoSourceFolders().hasOnlyChildren(left, right).isAggregation();

        left.addSource("left/path");
        right.addSource("right/path");
        assertThat(containerNode).hasSourceFolders("left/path", "right/path");
    }
}
