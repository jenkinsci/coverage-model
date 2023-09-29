package edu.hm.hafner.coverage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.LineRangeList;
import edu.hm.hafner.util.TreeString;

/**
 * A {@link Node} for a specific file. It stores the actual file name along with the coverage information.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
public final class FileNode extends Node {
    private static final long serialVersionUID = -3795695377267542624L; // Set to 1 when release 1.0.0 is ready
    private static final int UNSET = -1;

    private final NavigableMap<Integer, Integer> coveredPerLine = new TreeMap<>();
    private final NavigableMap<Integer, Integer> missedPerLine = new TreeMap<>();

    private final List<Mutation> mutations = new ArrayList<>();

    private final SortedSet<Integer> modifiedLines = new TreeSet<>();
    private final NavigableMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>();
    private final NavigableMap<Metric, Fraction> coverageDelta = new TreeMap<>();

    private TreeString relativePath; // @since 0.22.0

    /**
     * Creates a new {@link FileNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     * @param relativePath
     *         the relative path of the file
     */
    public FileNode(final String name, final TreeString relativePath) {
        super(Metric.FILE, name);

        this.relativePath = relativePath;
    }

    /**
     * Creates a new {@link FileNode} with the given name.
     *
     * @param name
     *         the human-readable name of the node
     * @param relativePath
     *         the relative path of the file
     */
    public FileNode(final String name, final String relativePath) {
        this(name, TreeString.valueOf(relativePath));
    }

    /**
     * Called after deserialization to retain backward compatibility.
     *
     * @return this
     */
    private Object readResolve() {
        if (relativePath == null) {
            relativePath = TreeString.valueOf(StringUtils.EMPTY);
        }
        return this;
    }

    @Override
    public FileNode copy() {
        var file = new FileNode(getName(), relativePath);

        file.coveredPerLine.putAll(coveredPerLine);
        file.missedPerLine.putAll(missedPerLine);

        file.modifiedLines.addAll(modifiedLines);

        file.mutations.addAll(mutations);

        file.indirectCoverageChanges.putAll(indirectCoverageChanges);
        file.coverageDelta.putAll(coverageDelta);

        return file;
    }

    @Override
    public boolean matches(final Metric searchMetric, final String searchName) {
        if (super.matches(searchMetric, searchName)) {
            return true;
        }
        return getRelativePath().equals(searchName);
    }

    @Override
    public boolean matches(final Metric searchMetric, final int searchNameHashCode) {
        if (super.matches(searchMetric, searchNameHashCode)) {
            return true;
        }
        return getRelativePath().hashCode() == searchNameHashCode;
    }

    public SortedSet<Integer> getModifiedLines() {
        return modifiedLines;
    }

    /**
     * Returns whether this file has been modified in the active change set.
     *
     * @return {@code true} if this file has been modified in the active change set, {@code false} otherwise
     */
    @Override
    public boolean hasModifiedLines() {
        return !modifiedLines.isEmpty();
    }

    /**
     * Returns whether this file has been modified at the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return {@code true} if this file has been modified at the specified line, {@code false} otherwise
     */
    public boolean hasModifiedLine(final int line) {
        return modifiedLines.contains(line);
    }

    /**
     * Marks the specified lines as being modified.
     *
     * @param lines
     *         the modified code lines
     */
    public void addModifiedLines(final int... lines) {
        for (int line : lines) {
            modifiedLines.add(line);
        }
    }

    @Override
    protected Optional<Node> filterTreeByModifiedLines() {
        if (!hasCoveredAndModifiedLines()) {
            return Optional.empty();
        }

        var copy = new FileNode(getName(), relativePath);
        copy.modifiedLines.addAll(modifiedLines);

        filterLineAndBranchCoverage(copy);
        filterMutations(copy);

        return Optional.of(copy);
    }

    private void filterLineAndBranchCoverage(final FileNode copy) {
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var lineBuilder = new CoverageBuilder().setMetric(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        var branchBuilder = new CoverageBuilder().setMetric(Metric.BRANCH);
        for (int line : getCoveredAndModifiedLines()) {
            var covered = coveredPerLine.getOrDefault(line, 0);
            var missed = missedPerLine.getOrDefault(line, 0);
            var total = covered + missed;
            copy.addCounters(line, covered, missed);
            if (total == 0) {
                throw new IllegalArgumentException("No coverage for line " + line);
            }
            else if (total == 1) {
                lineCoverage = lineCoverage.add(lineBuilder.setCovered(covered).setMissed(missed).build());
            }
            else {
                var branchCoveredAsLine = covered > 0 ? 1 : 0;
                lineCoverage = lineCoverage.add(
                        lineBuilder.setCovered(branchCoveredAsLine).setMissed(1 - branchCoveredAsLine).build());
                branchCoverage = branchCoverage.add(branchBuilder.setCovered(covered).setMissed(missed).build());
            }
        }
        addLineAndBranchCoverage(copy, lineCoverage, branchCoverage);
    }

    private void filterMutations(final FileNode copy) {
        mutations.stream().filter(mutation -> modifiedLines.contains(mutation.getLine())).forEach(copy::addMutation);
        if (!copy.mutations.isEmpty()) {
            var builder = new CoverageBuilder().setMetric(Metric.MUTATION).setMissed(0).setCovered(0);
            copy.mutations.stream().filter(Mutation::isDetected).forEach(mutation -> builder.incrementCovered());
            copy.mutations.stream()
                    .filter(Predicate.not(Mutation::isDetected))
                    .forEach(mutation -> builder.incrementMissed());
            copy.addValue(builder.build());
        }
    }

    @Override
    protected Optional<Node> filterTreeByModifiedFiles() {
        return hasCoveredAndModifiedLines() ? Optional.of(copyTree()) : Optional.empty();
    }

    private void addLineAndBranchCoverage(final FileNode copy,
            final Coverage lineCoverage, final Coverage branchCoverage) {
        if (lineCoverage.isSet()) {
            copy.addValue(lineCoverage);
        }
        if (branchCoverage.isSet()) {
            copy.addValue(branchCoverage);
        }
    }

    // TODO: the API for indirect changes does not work yet for mutations
    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    protected Optional<Node> filterTreeByIndirectChanges() {
        if (!hasIndirectCoverageChanges()) {
            return Optional.empty();
        }

        var copy = new FileNode(getName(), relativePath);
        Coverage lineCoverage = Coverage.nullObject(Metric.LINE);
        Coverage branchCoverage = Coverage.nullObject(Metric.BRANCH);
        for (Map.Entry<Integer, Integer> change : getIndirectCoverageChanges().entrySet()) {
            int delta = change.getValue();
            Coverage currentCoverage = getBranchCoverage(change.getKey());
            if (!currentCoverage.isSet()) {
                currentCoverage = getLineCoverage(change.getKey());
            }
            var builder = new CoverageBuilder();
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

    // TODO: the API does not work yet for mutations
    public NavigableSet<Integer> getLinesWithCoverage() {
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

    private Coverage getLineCoverage(final int line) {
        if (hasCoverageForLine(line)) {
            var covered = getCoveredOfLine(line) > 0 ? 1 : 0;
            return new CoverageBuilder().setMetric(Metric.LINE)
                    .setCovered(covered)
                    .setMissed(1 - covered)
                    .build();
        }
        return Coverage.nullObject(Metric.LINE);
    }

    private Coverage getBranchCoverage(final int line) {
        if (hasCoverageForLine(line)) {
            var covered = getCoveredOfLine(line);
            var missed = getMissedOfLine(line);
            if (covered + missed > 1) {
                return new CoverageBuilder().setMetric(Metric.BRANCH)
                        .setCovered(covered)
                        .setMissed(missed)
                        .build();
            }
        }
        return Coverage.nullObject(Metric.BRANCH);
    }

    @Override
    public Set<String> getFiles() {
        return Set.of(getRelativePath());
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
     * Computes the delta of all values between this file and the given reference file. Values that are not present in
     * both files are ignored.
     *
     * @param referenceFile
     *         the file to compare with this file
     */
    // TODO: wouldn't it make more sense to return an independent object?
    public void computeDelta(final FileNode referenceFile) {
        NavigableMap<Metric, Value> referenceCoverage = referenceFile.getMetricsDistribution();
        getMetricsDistribution().forEach((metric, value) -> {
            if (referenceCoverage.containsKey(metric)) {
                coverageDelta.put(metric, value.delta(referenceCoverage.get(metric)));
            }
        });
    }

    /**
     * Returns the delta for the specified metric. If no delta is available for the specified metric, then 0 is
     * returned.
     *
     * @param metric
     *         the metric to get the delta for
     *
     * @return the delta for the specified metric
     */
    public Fraction getDelta(final Metric metric) {
        return coverageDelta.getOrDefault(metric, Fraction.ZERO);
    }

    /**
     * Returns whether this file has a delta result for the specified metric.
     *
     * @param metric
     *         the metric to check
     *
     * @return {@code true} has delta results are available, {@code false} otherwise
     */
    public boolean hasDelta(final Metric metric) {
        return coverageDelta.containsKey(metric);
    }

    /**
     * Returns the lines with code coverage that also have been modified.
     *
     * @return the lines with code coverage that also have been modified
     */
    public SortedSet<Integer> getCoveredAndModifiedLines() {
        SortedSet<Integer> coveredDelta = getLinesWithCoverage();
        coveredDelta.retainAll(getModifiedLines());
        return coveredDelta;
    }

    /**
     * Returns whether this file has lines with code coverage that also have been modified.
     *
     * @return {@code true} if this file has lines with code coverage that also have been modified, {@code false}
     *         otherwise.
     */
    public boolean hasCoveredAndModifiedLines() {
        return !getCoveredAndModifiedLines().isEmpty();
    }

    /**
     * Add the coverage counters for the specified line.
     *
     * @param lineNumber
     *         the line number to add the counters for
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     */
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

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the number of covered items for the specified line
     */
    public int getCoveredOfLine(final int line) {
        return coveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of missed items for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the number of missed items for the specified line
     */
    public int getMissedOfLine(final int line) {
        return missedPerLine.getOrDefault(line, 0);
    }

    private int[] entriesToArray(final NavigableMap<Integer, Integer> map) {
        return map.values().stream().mapToInt(i -> i).toArray();
    }

    /**
     * Returns all instrumented lines that are not executed during the tests.
     *
     * @return the missed lines
     */
    public NavigableSet<Integer> getMissedLines() {
        return filterLines(line -> getCoveredOfLine(line) == 0);
    }

    /**
     * Returns all lines containing at least one executed instruction.
     *
     * @return the fully or partially covered lines
     */
    public NavigableSet<Integer> getCoveredLines() {
        return filterLines(line -> getCoveredOfLine(line) != 0);
    }

    private NavigableSet<Integer> filterLines(final Predicate<Integer> predicate) {
        return coveredPerLine.keySet().stream()
                .filter(predicate)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the lines that have no line coverage grouped in LineRanges.
     * E.g., the lines [1, 2, 3] will be grouped in one {@link LineRange} instance.
     *
     * @return the aggregated LineRanges that have no line coverage
     */
    public LineRangeList getMissedLineRanges() {
        LineRangeList lineRanges = new LineRangeList();

        var missedLines = getMissedLines();
        if (missedLines.isEmpty()) {
            return lineRanges;
        }

        if (missedLines.size() == 1) {
            lineRanges.add(new LineRange(missedLines.first()));

            return lineRanges;
        }

        var lines = List.copyOf(getLinesWithCoverage());

        int start = UNSET;
        int end = UNSET;

        for (int line : lines) {
            if (getCoveredOfLine(line) == 0) {
                if (start == UNSET) {
                    start = line;
                }
                end = line;
            }
            else {
                if (start != UNSET) {
                    lineRanges.add(new LineRange(start, end));
                    start = UNSET;
                }
            }
        }
        if (start != UNSET) {
            lineRanges.add(new LineRange(start, end));
        }

        return lineRanges;
    }

    /**
     * Returns all lines that contain survived mutations. The returned map contains the line number as the key and a
     * list of survived mutations as value.
     *
     * @return the lines that have survived mutations
     */
    public NavigableMap<Integer, List<Mutation>> getSurvivedMutationsPerLine() {
        return createMapOfMutations(Mutation::hasSurvived);
    }

    /**
     * Returns the lines that contain mutations. The returned map contains the line number as the key and a
     * list of mutations as value.
     *
     * @return the lines that have no line coverage
     */
    public NavigableMap<Integer, List<Mutation>> getMutationsPerLine() {
        return createMapOfMutations(b -> true);
    }

    private NavigableMap<Integer, List<Mutation>> createMapOfMutations(final Predicate<Mutation> predicate) {
        return getMutations().stream()
                .filter(predicate)
                .collect(Collectors.groupingBy(Mutation::getLine, TreeMap::new, Collectors.toList()));
    }

    /**
     * Returns the lines that have a branch coverage less than 100%. The returned map contains the line number as the
     * key and the number of missed branches as value.
     *
     * @return the mapping of not fully covered lines to the number of missed branches
     */
    public NavigableMap<Integer, Integer> getPartiallyCoveredLines() {
        return getLinesWithCoverage().stream()
                .filter(line -> getCoveredOfLine(line) > 0)
                .filter(line -> getMissedOfLine(line) > 0)
                .collect(Collectors.toMap(line -> line, missedPerLine::get, (a, b) -> a, TreeMap::new));
    }

    public NavigableMap<Integer, Integer> getCounters() {
        return Collections.unmodifiableNavigableMap(coveredPerLine);
    }

    /**
     * Adds a mutation to the method.
     *
     * @param mutation
     *         the mutation to add
     */
    // TODO: not part of API, only for tests?
    public void addMutation(final Mutation mutation) {
        mutations.add(mutation);
    }

    public List<Mutation> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    /**
     * Create a new class node with the given name and add it to the list of children.
     *
     * @param className
     *         the class name
     *
     * @return the created and linked class node
     */
    public ClassNode createClassNode(final String className) {
        var classNode = new ClassNode(className);
        addChild(classNode);
        return classNode;
    }

    /**
     * Searches for the specified class node. If the class node is not found then a new class node will be created and
     * linked to this file node.
     *
     * @param className
     *         the class name
     *
     * @return the created and linked class node
     * @see #createClassNode(String)
     */
    public ClassNode findOrCreateClassNode(final String className) {
        return findClass(className).orElseGet(() -> createClassNode(className));
    }

    /**
     * Returns the relative path of the file. If no relative path is set, then the name of this node is returned.
     *
     * @return the relative path of the file
     */
    public String getRelativePath() {
        return StringUtils.defaultIfBlank(relativePath.toString(), getName());
    }

    /**
     * Sets the relative path of the file.
     *
     * @param relativePath
     *         the relative path
     */
    public void setRelativePath(final TreeString relativePath) {
        this.relativePath = relativePath;
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
        return Objects.equals(coveredPerLine, fileNode.coveredPerLine)
                && Objects.equals(missedPerLine, fileNode.missedPerLine)
                && Objects.equals(mutations, fileNode.mutations)
                && Objects.equals(modifiedLines, fileNode.modifiedLines)
                && Objects.equals(indirectCoverageChanges, fileNode.indirectCoverageChanges)
                && Objects.equals(coverageDelta, fileNode.coverageDelta)
                && Objects.equals(relativePath, fileNode.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), coveredPerLine, missedPerLine, mutations, modifiedLines,
                indirectCoverageChanges, coverageDelta, relativePath);
    }
}
