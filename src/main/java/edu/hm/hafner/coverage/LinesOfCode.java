package edu.hm.hafner.coverage;

/**
 * Represents the lines of code in a particular code block.
 *
 * @author Melissa Bauer
 */
public final class LinesOfCode extends IntegerValue {
    private static final long serialVersionUID = -3098842770938054269L;

    /**
     * Creates a new {@link LinesOfCode} instance with the specified lines of code.
     *
     * @param loc
     *         lines of code
     */
    public LinesOfCode(final int loc) {
        super(Metric.LOC, loc);
    }

    @Override
    protected IntegerValue create(final int value) {
        return new LinesOfCode(value);
    }
}
