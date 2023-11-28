package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.util.TreeString;

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

    @Test
    void shouldHavePackageName() {
        var classWithoutPackage = new ClassNode("Class");
        assertThat(classWithoutPackage.getPackageName()).isEqualTo(Node.EMPTY_NAME);
        var classWithPackage = new ClassNode("edu.hm.hafner.Class");
        assertThat(classWithPackage.getPackageName()).isEqualTo("edu.hm.hafner");

        var packageNode = new PackageNode("edu.hm");
        packageNode.addChild(classWithPackage);
        assertThat(classWithPackage.getPackageName()).isEqualTo("edu.hm.hafner");

        packageNode.addChild(classWithoutPackage);
        assertThat(classWithoutPackage.getPackageName()).isEqualTo("edu.hm");

        var another = new ClassNode("Class");
        var file = new FileNode("a.b.c.file.txt", TreeString.valueOf("/path/to/file.txt"));
        file.addChild(another);
        assertThat(another.getPackageName()).isEqualTo(Node.EMPTY_NAME);
    }
}
