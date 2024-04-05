package edu.hm.hafner.coverage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A {@link Node} for a specific class.
 */
public final class ClassNode extends Node {
    private static final long serialVersionUID = 1621410859864978552L;

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
     * Returns the package name of this class. The package name is either the name of the parent package node or the
     * package part of this class' name.
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
    @SuppressFBWarnings("RCN")
    private Object readResolve() {
        if (testCases == null) {
            testCases = new ArrayList<>();
        }
        return this;
    }

    /**
     * Create a new method node with the given method name and signature and add it to the list of children.
     *
     * @param methodName
     *         the method name
     * @param signature
     *         the signature of the method
     *
     * @return the created and linked package node
     */
    public MethodNode createMethodNode(final String methodName, final String signature) {
        var fileNode = new MethodNode(methodName, signature);
        addChild(fileNode);
        return fileNode;
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
        replaceValue(new TestCount(testCases.size()));
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

        ClassNode classNode = (ClassNode) o;

        return testCases.equals(classNode.testCases);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + testCases.hashCode();
        return result;
    }
}
