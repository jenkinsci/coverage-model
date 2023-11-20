package edu.hm.hafner.coverage;

/**
 * Represents the total number of tests.
 *
 * @author Ullrich Hafner
 */
public final class TestCount extends IntegerValue {
    private static final long serialVersionUID = -3098842770938054269L;

    /**
     * Creates a new {@link TestCount} instance with the number of tests.
     *
     * @param tests
     *         the number of tests
     */
    public TestCount(final int tests) {
        super(Metric.TESTS, tests);
    }

    @Override
    protected IntegerValue create(final int value) {
        return new TestCount(value);
    }
}
