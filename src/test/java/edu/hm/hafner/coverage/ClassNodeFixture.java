package edu.hm.hafner.coverage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;

/**
 * Creates {@link ClassNode} instances with test cases for tests. The methods to add test cases to a {@link ClassNode}
 * are not part of the public API, they are available for parsers only (see {@link CoverageParser}). This fixture
 * provides access to these methods in tests of the model and of its clients.
 *
 * @author Ullrich Hafner
 */
public final class ClassNodeFixture {
    private final ClassNode classNode;

    /**
     * Creates a fixture for a new class with the specified name.
     *
     * @param name the name of the class
     */
    public ClassNodeFixture(final String name) {
        this(new ClassNode(name));
    }

    /**
     * Creates a fixture for the specified existing class. All test cases will be added to this class.
     *
     * @param classNode the class to modify
     */
    public ClassNodeFixture(final ClassNode classNode) {
        this.classNode = classNode;
    }

    /**
     * Adds the specified test case.
     *
     * @param testCase the test case to add
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public ClassNodeFixture withTestCase(final TestCase testCase) {
        classNode.addTestCase(testCase);

        return this;
    }

    /**
     * Adds the specified test cases.
     *
     * @param testCases the test cases to add
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public ClassNodeFixture withTestCases(final Collection<TestCase> testCases) {
        classNode.addTestCases(testCases);

        return this;
    }

    /**
     * Returns the class with all test cases that have been added.
     *
     * @return the class
     */
    public ClassNode build() {
        return classNode;
    }
}
