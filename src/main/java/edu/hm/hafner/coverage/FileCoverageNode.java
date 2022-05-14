package edu.hm.hafner.coverage;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link CoverageNode} for a specific file. It stores the actual file name along the coverage information.
 *
 * @author Ullrich Hafner
 */
public class FileCoverageNode extends CoverageNode {
    private static final long serialVersionUID = -3795695377267542624L;

    private final Map<Integer, CoverageLeaf> lineToBranchCoverage = new HashMap<>();
    private final Map<Integer, CoverageLeaf> lineToInstructionCoverage = new HashMap<>();

    /**
     * Creates a new {@link FileCoverageNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public FileCoverageNode(final String name) {
        super(CoverageMetric.FILE, name);
    }

    public Map<Integer, CoverageLeaf> getLineToBranchCoverage() {
        return lineToBranchCoverage;
    }

    public Map<Integer, CoverageLeaf> getLineToInstructionCoverage() {
        return lineToInstructionCoverage;
    }

    /**
     * Returns the amount of missed instructions.
     *
     * @return number of missed instructions
     */
    public long getMissedInstructions() {
        return lineToInstructionCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered instructions.
     *
     * @return number of covered instructions
     */
    public long getCoveredInstructions() {
        return lineToInstructionCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getCovered())
                .sum();
    }

    /**
     * Returns the amount of missed branches.
     *
     * @return number of missed branches
     */
    public long getMissedBranches() {
        return lineToBranchCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered instructions.
     *
     * @return number of covered instructions
     */
    public long getCoveredBranches() {
        return lineToBranchCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getCovered())
                .sum();
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
