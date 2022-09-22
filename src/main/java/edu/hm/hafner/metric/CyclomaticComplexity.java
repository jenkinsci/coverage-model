package edu.hm.hafner.metric;

/**
 * Represents the cyclomatic complexity in a particular code block.
 *
 * @author Melissa Bauer
 */
public final class CyclomaticComplexity extends IntegerValue {
    private static final long serialVersionUID = -1626223071392791727L;

    /**
     * Creates a new {@link CyclomaticComplexity} instance with the specified complexity.
     *
     * @param complexity
     *         the cyclomatic complexity
     */
    public CyclomaticComplexity(final int complexity) {
        super(Metric.COMPLEXITY, complexity);
    }

    @Override
    protected IntegerValue create(final int value) {
        return new CyclomaticComplexity(value);
    }
}
