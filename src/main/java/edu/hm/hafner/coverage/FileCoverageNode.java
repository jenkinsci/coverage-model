package edu.hm.hafner.coverage;

/**
 * A {@link CoverageNode} for a specific file. It stores the actual file name along the coverage information.
 *
 * @author Ullrich Hafner
 */
public class FileCoverageNode extends CoverageNode {
    private static final long serialVersionUID = -3795695377267542624L;

    /**
     * Creates a new {@link FileCoverageNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public FileCoverageNode(final String name) {
        super(CoverageMetric.FILE, name);
    }

    @Override
    public String getPath() {
        return mergePath(getName());
    }

    @Override
    protected CoverageNode copyEmpty() {
        return new FileCoverageNode(getName());
    }
}
