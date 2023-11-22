package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

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
        var classNode = new ClassNode("Class");
        var main = new MethodNode("main", "String...");
        classNode.addChild(main);
        classNode.addChild(new ClassNode("NestedClass"));

        assertThat(classNode.findMethod("main", "String..."))
                .isPresent()
                .containsSame(main);
        assertThat(classNode.findMethod("main", "Nothing"))
                .isNotPresent();
        assertThat(classNode).isNotAggregation();
    }

    @Test
    void shouldCopyTests() {
        var original = new ClassNode("Copy Me");
        var testCase = new TestCaseBuilder().withTestName("test").build();
        original.addTestCase(testCase);

        assertThat(original.getTestCases()).hasSize(1).contains(testCase);
        assertThat(original.copy().getTestCases()).hasSize(1).contains(testCase);
    }
}
