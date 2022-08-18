package edu.hm.hafner.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import edu.hm.hafner.coverage.CoverageLeaf;

/**
 * A {@link Node} for a specific file. It stores the actual file name along the coverage information.
 *
 * @author Ullrich Hafner
 */
public class FileNode extends Node {
    private static final long serialVersionUID = -3795695377267542624L;

    private final Map<Integer, CoverageLeaf> lineNumberToBranchCoverage = new HashMap<>();
    private final Map<Integer, CoverageLeaf> lineNumberToInstructionCoverage = new HashMap<>();

    /**
     * Creates a new {@link FileNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public FileNode(final String name) {
        super(Metric.FILE, name);
    }

    public Map<Integer, CoverageLeaf> getLineNumberToBranchCoverage() {
        return lineNumberToBranchCoverage;
    }

    public Map<Integer, CoverageLeaf> getLineNumberToInstructionCoverage() {
        return lineNumberToInstructionCoverage;
    }

    /**
     * Returns the amount of missed instructions.
     *
     * @return number of missed instructions
     */
    public long getMissedInstructionsCount() {
        return lineNumberToInstructionCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered instructions.
     *
     * @return number of covered instructions
     */
    public long getCoveredInstructionsCount() {
        return lineNumberToInstructionCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getCovered())
                .sum();
    }

    /**
     * Returns the amount of missed branches.
     *
     * @return number of missed branches
     */
    public long getMissedBranchesCount() {
        return lineNumberToBranchCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered instructions.
     *
     * @return number of covered instructions
     */
    public long getCoveredBranchesCount() {
        return lineNumberToBranchCoverage.values().stream()
                .mapToInt(leaf -> leaf.getCoverage().getCovered())
                .sum();
    }

    @Override
    public String getPath() {
        return mergePath(getName());
    }

    @Override
    public Node copyEmpty() {
        return new FileNode(getName());
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
        FileNode fileNode = (FileNode) o;

        return lineNumberToBranchCoverage.equals(fileNode.lineNumberToBranchCoverage)
                && lineNumberToInstructionCoverage.equals(fileNode.lineNumberToInstructionCoverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lineNumberToBranchCoverage, lineNumberToInstructionCoverage);
    }
}
