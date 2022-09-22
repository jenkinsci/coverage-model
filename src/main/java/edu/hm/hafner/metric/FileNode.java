package edu.hm.hafner.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link Node} for a specific file. It stores the actual file name along with the coverage information.
 *
 * @author Ullrich Hafner
 */
public final class FileNode extends Node {
    private static final long serialVersionUID = -3795695377267542624L;

    // FIXME: are these fields required? Or should those values be stored as leaves?
    private final Map<Integer, Value> lineNumberToBranchCoverage = new HashMap<>();
    private final Map<Integer, Value> lineNumberToInstructionCoverage = new HashMap<>();

    /**
     * Creates a new {@link FileNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     */
    public FileNode(final String name) {
        super(Metric.FILE, name);
    }

    @Override
    public FileNode copyEmpty() {
        return new FileNode(getName());
    }

    public Map<Integer, Value> getLineNumberToBranchCoverage() {
        return lineNumberToBranchCoverage;
    }

    public Map<Integer, Value> getLineNumberToInstructionCoverage() {
        return lineNumberToInstructionCoverage;
    }

    /**
     * Returns the amount of missed instructions.
     *
     * @return number of missed instructions
     */
    public long getMissedInstructionsCount() {
        return lineNumberToInstructionCoverage.values().stream()
                .mapToInt(leaf -> getCoverage(leaf).getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered instructions.
     *
     * @return number of covered instructions
     */
    public long getCoveredInstructionsCount() {
        return lineNumberToInstructionCoverage.values().stream()
                .mapToInt(leaf -> getCoverage(leaf).getCovered())
                .sum();
    }

    /**
     * Returns the amount of missed branches.
     *
     * @return number of missed branches
     */
    public long getMissedBranchesCount() {
        return lineNumberToBranchCoverage.values().stream()
                .mapToInt(leaf -> getCoverage(leaf).getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered instructions.
     *
     * @return number of covered instructions
     */
    public long getCoveredBranchesCount() {
        return lineNumberToBranchCoverage.values().stream()
                .mapToInt(leaf -> getCoverage(leaf).getCovered())
                .sum();
    }

    private static Coverage getCoverage(final Value value) {
        return (Coverage) value;
    }

    @Override
    public String getPath() {
        return mergePath(getName());
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
