package edu.hm.hafner.coverage;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A {@link Node} for a specific class.
 */
public final class ClassNode extends Node {
    @Serial
    private static final long serialVersionUID = 1621410859864978552L;

    @SuppressWarnings("serial")
    private List<TestCase> testCases = new ArrayList<>();

    /**
     * Creates a new {@link ClassNode} with the given name.
     *
     * @param name
     *         the name of the class
     */
    public ClassNode(final String name) {
        super(Metric.CLASS, PackageNode.normalizePackageName(name));
    }

    /**
     * Returns the package name of this class. The package name is either the package part of this class' name, or if it
     * does not exist, the name of the parent package node.
     *
     * @return the package name
     */
    public String getPackageName() {
        if (getName().contains(".")) {
            return StringUtils.substringBeforeLast(getName(), ".");
        }
        if (hasParent() && getParent().getMetric() == Metric.PACKAGE) {
            return getParentName();
        }
        return EMPTY_NAME;
    }

    @Override
    public ClassNode copy() {
        var copy = new ClassNode(getName());
        copy.testCases.addAll(testCases);
        return copy;
    }

    /**
     * Called after deserialization to retain backward compatibility.
     *
     * @return this
     */
    @Serial
    @SuppressFBWarnings(value = "RCN", justification = "Value might be null in old serializations")
    private Object readResolve() {
        if (testCases == null) {
            testCases = new ArrayList<>();
        }
        return this;
    }

    @Override
    public boolean isAggregation() {
        return false;
    }

    /**
     * Adds a new test case to this class.
     *
     * @param testCase
     *         the test case to add
     */
    public void addTestCase(final TestCase testCase) {
        addTestCases(List.of(testCase));
    }

    /**
     * Adds all given test cases to this class.
     *
     * @param additionalTestCases
     *         the test cases to add
     */
    public void addTestCases(final Collection<TestCase> additionalTestCases) {
        this.testCases.addAll(additionalTestCases);

        updateTestCount();
    }

    private void updateTestCount() {
        replaceValue(new Value(Metric.TESTS, testCases.size()));
    }

    @Override
    public List<TestCase> getTestCases() {
        return new ArrayList<>(testCases);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        var classNode = (ClassNode) o;

        return testCases.equals(classNode.testCases);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return 31 * result + testCases.hashCode();
    }
}
