package edu.hm.hafner.metric;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
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
    private final SortedSet<Integer> changedLines = new TreeSet<>();
    private final NavigableMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>();
    private final NavigableMap<Metric, Fraction> coverageDelta = new TreeMap<>();

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
        file.changedLines.addAll(changedLines);
        file.indirectCoverageChanges.putAll(indirectCoverageChanges);
        file.coverageDelta.putAll(coverageDelta);
        return file;
    }

    public SortedSet<Integer> getChangedLines() {
        return changedLines;
    }

    /**
     * Returns whether this file has been modified in the active change set.
     *
     * @return {@code true} if this file has been modified in the active change set, {@code false} otherwise
     */
    @Override
    public boolean hasChangedLines() {
        return !changedLines.isEmpty();
    }

    /**
     * Returns whether this file has been modified at the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return {@code true} if this file has been modified at the specified line, {@code false} otherwise
     */
    public boolean hasChangedLine(final int line) {
        return changedLines.contains(line);
    }

    /**
     * Mark a specific line as being changed.
     *
     * @param line
     *         the changed code line
     */
    public void addChangedLine(final int line) {
        changedLines.add(line);
    }

    @Override
    protected Optional<Node> filterByChanges() {
        if (!hasCoveredLinesInChangeSet()) {
            return Optional.empty();
        }

        var copy = new FileNode(getName());
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var lineBuilder = new CoverageBuilder().setMetric(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        var branchBuilder = new CoverageBuilder().setMetric(Metric.BRANCH);
        for (int line : getCoveredLinesOfChangeSet()) {
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
            copy.addChangedLine(line);
        }
        addLineAndBranchCoverage(copy, lineCoverage, branchCoverage);
        return Optional.of(copy);
    }

    private static void addLineAndBranchCoverage(final FileNode copy, final Coverage lineCoverage, final Coverage branchCoverage) {
        if (lineCoverage.isSet()) {
            copy.addValue(lineCoverage);
        }
        if (branchCoverage.isSet()) {
            copy.addValue(branchCoverage);
        }
    }

    @Override
    protected Optional<Node> filterByIndirectChanges() {
        if (!hasIndirectCoverageChanges()) {
            return Optional.empty();
        }

        var copy = new FileNode(getName());
        Coverage lineCoverage = Coverage.nullObject(Metric.LINE);
        Coverage branchCoverage = Coverage.nullObject(Metric.BRANCH);
        for (Map.Entry<Integer, Integer> change : getIndirectCoverageChanges().entrySet()) {
            int delta = change.getValue();
            Coverage currentCoverage = getBranchCoverage(change.getKey());
            if (!currentCoverage.isSet()) {
                currentCoverage = getLineCoverage(change.getKey());
            }
            CoverageBuilder builder = new CoverageBuilder();
            if (delta > 0) {
                // the line is fully covered - even in case of branch coverage
                if (delta == currentCoverage.getCovered()) {
                    builder.setMetric(Metric.LINE).setCovered(1).setMissed(0);
                    lineCoverage = lineCoverage.add(builder.build());
                }
                // the branch coverage increased for 'delta' hits
                if (currentCoverage.getTotal() > 1) {
                    builder.setMetric(Metric.BRANCH).setCovered(delta).setMissed(0);
                    branchCoverage = branchCoverage.add(builder.build());
                }
            }
            else if (delta < 0) {
                // the line is not covered anymore
                if (currentCoverage.getCovered() == 0) {
                    builder.setMetric(Metric.LINE).setCovered(0).setMissed(1);
                    lineCoverage = lineCoverage.add(builder.build());
                }
                // the branch coverage is decreased by 'delta' hits
                if (currentCoverage.getTotal() > 1) {
                    builder.setMetric(Metric.BRANCH).setCovered(0).setMissed(Math.abs(delta));
                    branchCoverage = branchCoverage.add(builder.build());
                }
            }
        }
        addLineAndBranchCoverage(copy, lineCoverage, branchCoverage);

        return Optional.of(copy);
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

    public SortedMap<Integer, Integer> getIndirectCoverageChanges() {
        return new TreeMap<>(indirectCoverageChanges);
    }

    public SortedSet<Integer> getCoveredLines() {
        return new TreeSet<>(coveredPerLine.keySet());
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
     * Computes a delta coverage between this node and the given reference node.
     *
     * @param referenceNode
     *         the node to compare with this node
     */
    // TODO: wouldn't it make more sense to return an independent object?
    public void computeDelta(final FileNode referenceNode) {
        NavigableMap<Metric, Value> referenceCoverage = referenceNode.getMetricsDistribution();
        getMetricsDistribution().forEach((metric, value) -> {
            if (referenceCoverage.containsKey(metric)) {
                coverageDelta.put(metric, value.delta(referenceCoverage.get(metric)));
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
     * Returns the coverage percentage for changed code lines (for the specified metric).
     *
     * @param metric
     *         the metric to check
     *
     * @return the coverage percentage for changed code lines
     */
    public Fraction getChangeCoverage(final Metric metric) {
        return coverageDelta.getOrDefault(metric, Fraction.ZERO);
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
        return coverageDelta.containsKey(metric);
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
                fileNode.missedPerLine) && Objects.equals(changedLines, fileNode.changedLines)
                && Objects.equals(indirectCoverageChanges, fileNode.indirectCoverageChanges)
                && Objects.equals(coverageDelta, fileNode.coverageDelta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coveredPerLine, missedPerLine, changedLines, indirectCoverageChanges,
                coverageDelta);
    }

}
