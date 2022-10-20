package edu.hm.hafner.metric;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.Ensure;

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
    private final SortedSet<Integer> changedCodeLines = new TreeSet<>();
    private final NavigableMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>();
    private final NavigableMap<Metric, Fraction> fileCoverageDelta = new TreeMap<>();

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
    public FileNode copy() {
        var file = new FileNode(getName());
        file.lineNumberToBranchCoverage.putAll(lineNumberToBranchCoverage);
        file.lineNumberToInstructionCoverage.putAll(lineNumberToInstructionCoverage);
        file.lineNumberToLineCoverage.putAll(lineNumberToLineCoverage);
        file.changedCodeLines.addAll(changedCodeLines);
        file.indirectCoverageChanges.putAll(indirectCoverageChanges);
        file.fileCoverageDelta.putAll(fileCoverageDelta);
        return file;
    }

    // TODO: check if it makes sense to use a map of maps?

    /**
     * Sets the branch coverage for a given line number.
     *
     * @param lineNumber
     *         the line number
     * @param coverage
     *         the leaf to add
     */
    public void addBranchCoverage(final int lineNumber, final Coverage coverage) {
        addCoverage(lineNumber, coverage, lineNumberToBranchCoverage, Metric.BRANCH);
    }

    /**
     * Sets the line coverage for a given line number.
     *
     * @param lineNumber
     *         the line number
     * @param coverage
     *         the leaf to add
     */
    public void addLineCoverage(final int lineNumber, final Coverage coverage) {
        addCoverage(lineNumber, coverage, lineNumberToLineCoverage, Metric.LINE);
    }

    /**
     * Sets the instruction coverage for a given line number.
     *
     * @param lineNumber
     *         the line number
     * @param coverage
     *         the leaf to add
     */
    public void addInstructionCoverage(final int lineNumber, final Coverage coverage) {
        addCoverage(lineNumber, coverage, lineNumberToInstructionCoverage, Metric.INSTRUCTION);
    }

    private void addCoverage(final int line, final Coverage coverage,
            final NavigableMap<Integer, Coverage> mapping,
            final Metric expectedMetric) {
        Ensure.that(line >= 0).isTrue("Line number must not be negative: %d", line);
        Ensure.that(coverage.getMetric().equals(expectedMetric))
                .isTrue("The coverage '%s' is not a coverage of type", coverage, expectedMetric);

        if (coverage.isSet()) {
            mapping.put(line, coverage);
        }
    }

    public Map<Integer, Coverage> getLineNumberToBranchCoverage() {
        return Map.copyOf(lineNumberToBranchCoverage);
    }

    public Map<Integer, Coverage> getLineNumberToInstructionCoverage() {
        return Map.copyOf(lineNumberToInstructionCoverage);
    }

    public NavigableMap<Integer, Coverage> getLineNumberToLineCoverage() {
        return new TreeMap<>(lineNumberToLineCoverage);
    }

    public SortedSet<Integer> getChangedLines() {
        return changedCodeLines;
    }

    /**
     * Returns whether this file has been modified in the active change set.
     *
     * @return {@code true} if this file has been modified in the active change set, {@code false} otherwise
     */
    public boolean hasCodeChanges() {
        return !changedCodeLines.isEmpty();
    }

    /**
     * Returns whether this file has been modified at the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return {@code true} if this file has been modified at the specified line, {@code false} otherwise
     */
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
    public void addIndirectCoverageChange(final int line, final int hitsDelta) {
        indirectCoverageChanges.put(line, hitsDelta);
    }

    // TODO: do not expose map
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

    private Coverage getCoverage(final Value value) {
        return (Coverage) value;
    }

    /**
     * Returns whether this file has a coverage result for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return {@code true} if this file has a coverage result for the specified line, {@code false} otherwise
     */
    public boolean hasCoverageForLine(final int line) {
        return lineNumberToLineCoverage.containsKey(line);
    }

    /**
     * Returns the line coverage result for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return  the line coverage result for the specified line.
     */
    public Coverage getLineCoverage(final int line) {
        return lineNumberToLineCoverage.getOrDefault(line, Coverage.nullObject(Metric.LINE));
    }

    /**
     * Returns the line coverage result for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return  the line coverage result for the specified line.
     */
    public Coverage getBranchCoverage(final int line) {
        return lineNumberToBranchCoverage.getOrDefault(line, Coverage.nullObject(Metric.BRANCH));
    }

    @Override
    public String getPath() {
        return mergePath(getName());
    }

    @Override
    public List<String> getFiles() {
        return List.of(getPath());
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

    /**
     * Computes a delta coverage between this node and the given reference node.
     *
     * @param referenceNode
     *         the node to compare with this node
     */
    // TODO: wouldn't it make more sense to return a independent object?
    public void computeDelta(final FileNode referenceNode) {
        NavigableMap<Metric, Value> referenceCoverage = referenceNode.getMetricsDistribution();
        this.getMetricsDistribution().forEach((metric, value) -> {
            if (referenceCoverage.containsKey(metric)) {
                fileCoverageDelta.put(metric, value.delta(referenceCoverage.get(metric)));
            }
        });
    }

    /**
     * Returns whether the coverage of this node is affected indirectly by the tests in the change set.
     *
     * @return {@code true} if this node is affected indirectly by the tests.
     */
    public boolean hasIndirectCoverageChanges() {
        return !indirectCoverageChanges.isEmpty();
    }

    /**
     * Returns whether this file has coverage results for changed code lines (for the specified metric).
     *
     * @param metric
     *         the metric to check
     *
     * @return {@code true} has coverage results for changed code lines, {@code false} otherwise
     */
    public boolean hasChangeCoverage(final Metric metric) {
        return fileCoverageDelta.containsKey(metric);
    }

    /**
     * Returns whether this file has coverage results for changed code lines.
     *
     * @return {@code true} if this file has coverage results for changed code lines, {@code false} otherwise
     */
    public boolean hasChangeCoverage() {
        return hasChangeCoverage(Metric.LINE) || hasChangeCoverage(Metric.BRANCH);
    }

    /**
     * Returns the lines with code coverage that are part of the change set.
     *
     * @return the lines with code coverage that are part of the change set
     */
    public SortedSet<Integer> getCoveredLinesOfChangeSet() {
        SortedSet<Integer> coveredDelta = getCoveredLines();
        coveredDelta.retainAll(getChangedLines());
        return coveredDelta;
    }

    /**
     * Returns the line coverage results for lines that are part of the change set.
     *
     * @return the line coverage results for lines that are part of the change set.
     */
    public List<Coverage> getCoverageOfChangeSet() {
        SortedSet<Integer> coveredDelta = getCoveredLines();
        coveredDelta.retainAll(getChangedLines());
        return coveredDelta.stream().map(lineNumberToLineCoverage::get).collect(Collectors.toList());
    }

    /**
     * Returns whether this file has lines with code coverage that are part of the change set.
     *
     * @return {@code true} if this file has lines with code coverage that are part of the change set, {@code false}
     *         otherwise.
     */
    public boolean hasCoveredLinesInChangeSet() {
        return !getCoveredLinesOfChangeSet().isEmpty();
    }

    /**
     * Returns the coverage percentage for changed code lines (for the specified metric).
     *
     * @param metric
     *         the metric to check
     *
     * @return the coverage percentage for changed code lines
     */
    public Fraction getChangeCoverage(final Metric metric) {
        return fileCoverageDelta.getOrDefault(metric, Fraction.ZERO);
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
}
