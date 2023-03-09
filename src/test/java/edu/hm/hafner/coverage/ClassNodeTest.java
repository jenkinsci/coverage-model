package edu.hm.hafner.coverage;

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

        assertThat(root.findMethod("main", "String..."))
                .isPresent()
                .containsSame(main);
        assertThat(root.findMethod("main", "Nothing"))
                .isNotPresent();
    }
}
