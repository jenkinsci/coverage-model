package edu.hm.hafner.complexity;

import java.util.Objects;

import edu.hm.hafner.model.Leaf;
import edu.hm.hafner.model.Metric;

/**
 * Leaf which represents the complexity.
 *
 * @author Melissa Bauer
 */
public class ComplexityLeaf extends Leaf {
    private static final long serialVersionUID = -1626223071392791727L;

    private final int complexity;

    /**
     * Creates a new complexity leaf with the specified value.
     *
     * @param complexity
     *         the cyclomatic complexity
     */
    public ComplexityLeaf(final int complexity) {
        super(Metric.COMPLEXITY);
        this.complexity = complexity;
    }

    /**
     * Returns the cyclomatic complexity.
     *
     * @return the cyclomatic complexity
     */
    public int getComplexity() {
        return complexity;
    }

    /**
     * Add the complexity from the specified instance to the complexity of this instance.
     *
     * @param additional
     *         the additional coverage details
     *
     * @return the sum of this and the additional coverage
     */
    public ComplexityLeaf add(final ComplexityLeaf additional) {
        return new ComplexityLeaf(complexity + additional.getComplexity());
    }

    /**
     * Returns if the current complexity is set.
     *
     * @return if the complexity is set
     */
    public boolean isSet() {
        return complexity > 0;
    }

    @Override
    public String toString() {
        return String.format("[Complexity]: %s", complexity);
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
        ComplexityLeaf that = (ComplexityLeaf) o;
        return complexity == that.complexity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), complexity);
    }
}
