package edu.hm.hafner.coverage;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/** A {@link Node} for a specific class. */
public final class ClassNode extends Node {
    @Serial
    private static final long serialVersionUID = 1621410859864978552L;

    @SuppressWarnings("serial")
    private List<TestCase> testCases = new ArrayList<>();

    /**
     * Creates a new {@link ClassNode} with the given name.
     *
     * @param name the name of the class
     */
    public ClassNode(final String name) {
        super(PackageNode.normalizePackageName(name));
    }

    @Override
    public Metric getMetric() {
        return Metric.CLASS;
    }

    /**
     * Returns the package name of this class. The package name is either the package part of this class' name or if it
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
     * Adds a new test case to this class. Parsers use {@link CoverageParser#addTestCase(ClassNode, TestCase)}.
     *
     * @param testCase the test case to add
     */
    void addTestCase(final TestCase testCase) {
        addTestCases(List.of(testCase));
    }

    /**
     * Adds all given test cases to this class. The number of tests and the test success rate of this class are updated
     * accordingly. The values of the parent nodes are aggregated from the values of their classes.
     *
     * @param additionalTestCases the test cases to add
     */
    void addTestCases(final Collection<TestCase> additionalTestCases) {
        this.testCases.addAll(additionalTestCases);

        replaceValue(new Value(Metric.TESTS, testCases.size()));
        computeSuccessRate(additionalTestCases).ifPresent(this::addSuccessRate);
    }

    /**
     * Computes the success rate of the specified test cases. Skipped tests are not part of the success rate.
     *
     * @param tests the test cases to evaluate
     * @return the success rate or an empty result if none of the test cases has been executed
     */
    private Optional<Value> computeSuccessRate(final Collection<TestCase> tests) {
        var passed = countTests(tests, TestCase::isPassed);
        var executed = passed + countTests(tests, TestCase::isFailed);
        if (executed > 0) {
            return Optional.of(new Rate(Metric.TEST_SUCCESS_RATE, passed, executed));
        }
        return Optional.empty();
    }

    private long countTests(final Collection<TestCase> tests, final Predicate<TestCase> filter) {
        return tests.stream().filter(filter).count();
    }

    /**
     * Adds the specified success rate to the stored success rate of this class. Only the newly added test cases need to
     * be evaluated, so adding the test cases of a class one by one runs in linear time.
     *
     * @param additionalRate the success rate of the newly added test cases
     */
    private void addSuccessRate(final Value additionalRate) {
        var storedRate = getValues().stream()
                .filter(value -> value.getMetric() == Metric.TEST_SUCCESS_RATE)
                .findAny();
        replaceValue(storedRate.map(rate -> rate.add(additionalRate)).orElse(additionalRate));
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
