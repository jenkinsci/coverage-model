package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ClassNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.CLASS;
    }

    @Override
    Node createNode(final String name) {
        return new ClassNode(name);
    }

    @Test
    void shouldHandleUnexpectedNodes() {
        var root = new ClassNode("Class");
        var main = new MethodNode("main", "String...");
        root.addChild(main);
        root.addChild(new ClassNode("NestedClass"));

        assertThat(root.findMethodNode("main", "String..."))
                .isPresent()
                .containsSame(main);
        assertThat(root.findMethodNode("main", "Nothing"))
                .isNotPresent();
    }
}
