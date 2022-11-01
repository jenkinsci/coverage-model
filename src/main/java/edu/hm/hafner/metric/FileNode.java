package edu.hm.hafner.metric;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

/**
 * A {@link Node} for a specific file. It stores the actual file name along with the coverage information.
 *
 * @author Ullrich Hafner
 */
public final class FileNode extends Node {
    private static final long serialVersionUID = -3795695377267542624L;
    private final NavigableMap<Integer, Integer> coveredPerLine = new TreeMap<>();
    private final NavigableMap<Integer, Integer> missedPerLine = new TreeMap<>();
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
        file.coveredPerLine.putAll(coveredPerLine);
        file.missedPerLine.putAll(missedPerLine);
        file.changedCodeLines.addAll(changedCodeLines);
        file.indirectCoverageChanges.putAll(indirectCoverageChanges);
        file.fileCoverageDelta.putAll(fileCoverageDelta);
        return file;
    }


    public FileNode filter() {
        var copy = new FileNode(getName());
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var lineBuilder = new CoverageBuilder().setMetric(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        var branchBuilder = new CoverageBuilder().setMetric(Metric.BRANCH);
        for (int line : changedCodeLines) {
            var covered = coveredPerLine.get(line);
            var missed = missedPerLine.get(line);
            copy.addCounters(line, covered, missed);
            if (covered + missed == 0) {
                throw new IllegalArgumentException("No coverage for line " + line);
            }
            else if (covered + missed == 1) {
                lineCoverage = lineCoverage.add(lineBuilder.setCovered(covered).setMissed(missed).build());
            }
            else {
                var branchCoveredAsLine = covered > 0 ? 1 : 0;
                lineCoverage = lineCoverage.add(lineBuilder.setCovered(branchCoveredAsLine).setMissed(1 - branchCoveredAsLine).build());
                branchCoverage = branchCoverage.add(branchBuilder.setCovered(covered).setMissed(missed).build());
            }
        }
        copy.addValue(lineCoverage);
        copy.addValue(branchCoverage);
        return copy;
    }

    public SortedSet<Integer> getChangedLines() {
        return changedCodeLines;
    }

    public boolean hasChangedLine(final int line) {
        return changedCodeLines.contains(line);
    }

    /**
     * Returns whether this file has been modified in the active change set.
     *
     * @return {@code true} if this file has been modified in the active change set, {@code false} otherwise
     */
    @Override
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
        return new TreeSet<>(coveredPerLine.keySet());
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
        return coveredPerLine.containsKey(line);
    }

    /**
     * Returns the line coverage result for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the line coverage result for the specified line.
     */
    public Coverage getLineCoverage(final int line) {
        if (hasCoverageForLine(line)) {
            var covered = getCoveredOfLine(line) > 0 ? 1 : 0;
            return new CoverageBuilder().setMetric(Metric.LINE)
                    .setCovered(covered)
                    .setMissed(1 - covered)
                    .build();
        }
        return Coverage.nullObject(Metric.LINE);
    }

    /**
     * Returns the branch coverage result for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the line coverage result for the specified line.
     */
    public Coverage getBranchCoverage(final int line) {
        if (hasCoverageForLine(line)) {
            var covered = getCoveredOfLine(line);
            var missed = getMissedOfLine(line);
            if (covered + missed > 1) {
                return new CoverageBuilder().setMetric(Metric.LINE)
                        .setCovered(covered)
                        .setMissed(missed)
                        .build();
            }
        }
        return Coverage.nullObject(Metric.BRANCH);
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
        getMetricsDistribution().forEach((metric, value) -> {
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
     * Returns whether this file has coverage results for ch    anged code lines.
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
        return coveredDelta.stream().map(this::getLineCoverage).collect(Collectors.toList());
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

    public void addCounters(final int lineNumber, final int covered, final int missed) {
        coveredPerLine.put(lineNumber, covered);
        missedPerLine.put(lineNumber, missed);
    }

    public int[] getCoveredCounters() {
        return entriesToArray(coveredPerLine);
    }

    public int[] getMissedCounters() {
        return entriesToArray(missedPerLine);
    }

    private int[] entriesToArray(final NavigableMap<Integer, Integer> map) {
        return map.values().stream().mapToInt(i -> i).toArray();
    }

    public NavigableMap<Integer, Integer> getCounters() {
        return Collections.unmodifiableNavigableMap(coveredPerLine);
    }

    public int getCoveredOfLine(final int line) {
        return coveredPerLine.getOrDefault(line, 0);
    }

    public int getMissedOfLine(final int line) {
        return missedPerLine.getOrDefault(line, 0);
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
        return Objects.equals(coveredPerLine, fileNode.coveredPerLine) && Objects.equals(missedPerLine,
                fileNode.missedPerLine) && Objects.equals(changedCodeLines, fileNode.changedCodeLines)
                && Objects.equals(indirectCoverageChanges, fileNode.indirectCoverageChanges)
                && Objects.equals(fileCoverageDelta, fileNode.fileCoverageDelta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coveredPerLine, missedPerLine, changedCodeLines, indirectCoverageChanges,
                fileCoverageDelta);
    }

}
