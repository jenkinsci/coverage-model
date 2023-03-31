package edu.hm.hafner.coverage;

/**
 * Represents the cyclomatic complexity in a particular code block.
 *
 * @author Melissa Bauer
 */
public final class CyclomaticComplexity extends IntegerValue {
    private static final long serialVersionUID = -1626223071392791727L;

    /**
     * Creates a new {@link CyclomaticComplexity} instance with the specified complexity.
     * The metric is set to {@link Metric#COMPLEXITY}.
     *
     * @param complexity
     *         the cyclomatic complexity
     */
    public CyclomaticComplexity(final int complexity) {
        this(complexity, Metric.COMPLEXITY);
    }

    /**
     * Creates a new {@link CyclomaticComplexity} instance with the specified complexity.
     *
     * @param complexity
     *         the cyclomatic complexity
     * @param metric
     *         the metric of this value
     */
    public CyclomaticComplexity(final int complexity, final Metric metric) {
        super(metric, complexity);
    }

    @Override
    protected IntegerValue create(final int value) {
        return new CyclomaticComplexity(value);
    }
}
