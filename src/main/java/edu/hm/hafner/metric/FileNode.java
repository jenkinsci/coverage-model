package edu.hm.hafner.metric;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.math.Fraction;

/**
 * A {@link Node} for a specific file. It stores the actual file name along with the coverage information.
 *
 * @author Ullrich Hafner
 */
public final class FileNode extends Node {
    private static final long serialVersionUID = -3795695377267542624L;
    private final NavigableMap<Integer, Coverage> lineNumberToBranchCoverage = new TreeMap<>();
    private final NavigableMap<Integer, Coverage> lineNumberToInstructionCoverage = new TreeMap<>();
    private final NavigableMap<Integer, Coverage> lineNumberToLineCoverage = new TreeMap<>();
    private final SortedSet<Integer> changedCodeLines = new TreeSet<>(); // since 3.0.0
    private final SortedMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>(); // since 3.0.0
    private final SortedMap<Metric, Fraction> fileCoverageDelta = new TreeMap<>(); // since 3.0.0

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

    // FIXME: do not expose fields
    public Map<Integer, Coverage> getLineNumberToBranchCoverage() {
        return lineNumberToBranchCoverage;
    }

    public Map<Integer, Coverage> getLineNumberToInstructionCoverage() {
        return lineNumberToInstructionCoverage;
    }

    public NavigableMap<Integer, Coverage> getLineNumberToLineCoverage() {
        return lineNumberToLineCoverage;
    }

    public SortedSet<Integer> getChangedLines() {
        return changedCodeLines;
    }

    public boolean hasCodeChanges() {
        return !changedCodeLines.isEmpty();
    }

    public boolean hasChangedCodeLine(final int line) {
        return changedCodeLines.contains(line);
    }

    /**
     * Adds an indirect coverage change for a specific line.
     *
     * @param line
     *         The line with the coverage change
     * @param hitsDelta
     *         The delta of the coverage hits before and after the code changes
     */
    public void putIndirectCoverageChange(final int line, final int hitsDelta) {
        indirectCoverageChanges.put(line, hitsDelta);
    }

    public SortedMap<Integer, Integer> getIndirectCoverageChanges() {
        return indirectCoverageChanges;
    }

    public SortedSet<Integer> getCoveredLines() {
        return new TreeSet<>(lineNumberToLineCoverage.keySet());
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
     * Returns the amount of missed lines.
     *
     * @return number of missed lines
     */
    public long getMissedLinesCount() {
        return lineNumberToLineCoverage.values().stream()
                .mapToInt(leaf -> getCoverage(leaf).getMissed())
                .sum();
    }

    /**
     * Returns the amount of covered lines.
     *
     * @return number of covered lines
     */
    public long getCoveredLinesCount() {
        return lineNumberToLineCoverage.values().stream()
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

    /**
     * Adds a new leaf to the instruction coverage map.
     * @param lineNumber the line number
     * @param instructionLeaf the leaf to add
     */
    public void addInstructionCoverage(final int lineNumber, final Coverage instructionLeaf) {
        lineNumberToInstructionCoverage.put(lineNumber, instructionLeaf);
    }

    /**
     * Adds a new leaf to the line coverage map.
     * @param lineNumber the line number
     * @param branchLeaf the leaf to add
     */
    public void addLineCoverage(final int lineNumber, final Coverage branchLeaf) {
        lineNumberToLineCoverage.put(lineNumber, branchLeaf);
    }

    /**
     * Adds a new leaf to the branch coverage map.
     * @param lineNumber the line number
     * @param branchLeaf the leaf to add
     */
    public void addBranchCoverage(final int lineNumber, final Coverage branchLeaf) {
        lineNumberToBranchCoverage.put(lineNumber, branchLeaf);
    }

    private static Coverage getCoverage(final Value value) {
        return (Coverage) value;
    }

    @Override
    public String getPath() {
        return mergePath(getName());
    }

    @Override
    public Set<String> getFiles() {
        return Set.of(getPath());
    }

    /**
     * Adds a code line that has been changed.
     *
     * @param line
     *         The changed code line
     */
    public void addChangedCodeLine(final int line) {
        changedCodeLines.add(line);
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
        return Objects.equals(lineNumberToBranchCoverage, fileNode.lineNumberToBranchCoverage)
                && Objects.equals(lineNumberToInstructionCoverage, fileNode.lineNumberToInstructionCoverage)
                && Objects.equals(lineNumberToLineCoverage, fileNode.lineNumberToLineCoverage)
                && Objects.equals(changedCodeLines, fileNode.changedCodeLines) && Objects.equals(
                indirectCoverageChanges, fileNode.indirectCoverageChanges) && Objects.equals(fileCoverageDelta,
                fileNode.fileCoverageDelta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lineNumberToBranchCoverage, lineNumberToInstructionCoverage,
                lineNumberToLineCoverage, changedCodeLines, indirectCoverageChanges, fileCoverageDelta);
    }

    public void computeDelta(final FileNode referenceNode) {
        NavigableMap<Metric, Value> referenceCoverage = referenceNode.getMetricsDistribution();
        this.getMetricsDistribution().forEach((metric, value) -> {
            if (referenceCoverage.containsKey(metric)) {
                fileCoverageDelta.put(metric, value.delta(referenceCoverage.get(metric)));
            }
        });
    }

    public boolean hasIndirectCoverageChanges() {
        return !indirectCoverageChanges.isEmpty();
    }

    public boolean hasChangeCoverageFor(final Metric metric) {
        return fileCoverageDelta.containsKey(metric);
    }

    public boolean hasChangeCoverage() {
        return fileCoverageDelta.containsKey(Metric.LINE) || fileCoverageDelta.containsKey(Metric.BRANCH);
    }

    public Fraction getChangeCoverageFor(final Metric metric) {
        return fileCoverageDelta.getOrDefault(metric, Fraction.ZERO);
    }

    public boolean hasCoverageForLine(final int line) {
        return lineNumberToLineCoverage.containsKey(line);
    }

    public SortedSet<Integer> getCoveredDelta() {
        SortedSet<Integer> coveredDelta = getCoveredLines();
        coveredDelta.retainAll(getChangedLines());
        return coveredDelta;
    }
}
