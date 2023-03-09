package edu.hm.hafner.coverage;

class ContainerNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.CONTAINER;
    }

    @Override
    Node createNode(final String name) {
        return new ContainerNode(name);
    }
}
