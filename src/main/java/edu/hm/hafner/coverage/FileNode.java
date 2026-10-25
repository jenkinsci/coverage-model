package edu.hm.hafner.coverage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.LineRangeList;
import edu.hm.hafner.util.TreeString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A {@link Node} for a specific file. It stores the actual file name along with the coverage information.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects", "PMD.ExcessivePublicCount"
})
public final class FileNode extends Node {
    @Serial
    private static final long serialVersionUID = -3795695377267542624L; // Set to 1 when release 1.0.0 is ready

    @SuppressWarnings("serial")
    private final NavigableMap<Integer, Integer> coveredPerLine = new TreeMap<>();

    @SuppressWarnings("serial")
    private final NavigableMap<Integer, Integer> missedPerLine = new TreeMap<>();

    @SuppressWarnings("serial")
    private NavigableMap<Integer, Integer> mcdcPairCoveredPerLine = new TreeMap<>();
    // metrics for MC/DC pairs per line
    @SuppressWarnings("serial")
    private NavigableMap<Integer, Integer> mcdcPairMissedPerLine = new TreeMap<>();

    // metrics for function calls per line
    @SuppressWarnings("serial")
    private NavigableMap<Integer, Integer> functionCallCoveredPerLine = new TreeMap<>();

    @SuppressWarnings("serial")
    private NavigableMap<Integer, Integer> functionCallMissedPerLine = new TreeMap<>();

    @SuppressWarnings("serial")
    private final List<Mutation> mutations = new ArrayList<>();

    @SuppressWarnings("serial")
    private final SortedSet<Integer> modifiedLines = new TreeSet<>();

    @SuppressWarnings("serial")
    private final NavigableMap<Integer, Integer> indirectCoverageChanges = new TreeMap<>();

    @SuppressWarnings("serial")
    private final NavigableMap<Metric, Value> coverageDelta = new TreeMap<>();

    private TreeString relativePath; // @since 0.22.0

    /**
     * Creates a new {@link FileNode} with the given name.
     *
     * @param name the human-readable name of the node
     * @param relativePath the relative path of the file
     */
    public FileNode(final String name, final TreeString relativePath) {
        super(name);

        this.relativePath = relativePath;
    }

    @Override
    public Metric getMetric() {
        return Metric.FILE;
    }

    /**
     * Creates a new {@link FileNode} with the given name.
     *
     * @param name the human-readable name of the node
     * @param relativePath the relative path of the file
     */
    public FileNode(final String name, final String relativePath) {
        this(name, TreeString.valueOf(relativePath));
    }

    @Override
    public String getId() {
        return relativePath.toString() + getName();
    }

    /**
     * Called after deserialization to retain backward compatibility.
     *
     * @return this
     */
    @Serial
    @SuppressFBWarnings(value = "RCN", justification = "Value might be null in old serializations")
    private Object readResolve() {
        if (relativePath == null) {
            relativePath = TreeString.valueOf(StringUtils.EMPTY);
        }
        if (mcdcPairCoveredPerLine == null) {
            mcdcPairCoveredPerLine = new TreeMap<>();
        }
        if (mcdcPairMissedPerLine == null) {
            mcdcPairMissedPerLine = new TreeMap<>();
        }
        if (functionCallCoveredPerLine == null) {
            functionCallCoveredPerLine = new TreeMap<>();
        }
        if (functionCallMissedPerLine == null) {
            functionCallMissedPerLine = new TreeMap<>();
        }
        return this;
    }

    @Override
    public FileNode copy() {
        var copy = new FileNode(getName(), relativePath);

        copy.coveredPerLine.putAll(coveredPerLine);
        copy.missedPerLine.putAll(missedPerLine);

        copy.mcdcPairCoveredPerLine.putAll(mcdcPairCoveredPerLine);
        copy.mcdcPairMissedPerLine.putAll(mcdcPairMissedPerLine);

        copy.functionCallCoveredPerLine.putAll(functionCallCoveredPerLine);
        copy.functionCallMissedPerLine.putAll(functionCallMissedPerLine);

        copy.modifiedLines.addAll(modifiedLines);

        copy.mutations.addAll(mutations);

        copy.indirectCoverageChanges.putAll(indirectCoverageChanges);
        copy.coverageDelta.putAll(coverageDelta);

        return copy;
    }

    @Override
    protected boolean filterByRelativePath(final Collection<String> fileNames) {
        return fileNames.contains(getRelativePath());
    }

    @Override
    public boolean matches(final Metric searchMetric, final String searchName) {
        return getMetric() == searchMetric
                && (getRelativePath().equals(searchName) || getName().equals(searchName));
    }

    @Override
    public boolean matches(final Metric searchMetric, final int searchNameHashCode) {
        return getMetric() == searchMetric
                && (getRelativePath().hashCode() == searchNameHashCode
                        || getName().hashCode() == searchNameHashCode);
    }

    @Override
    protected void mergeNode(final Node other) {
        Ensure.that(other).isInstanceOf(FileNode.class);

        new FileNodeMerger(this, (FileNode) other).merge();
    }

    public SortedSet<Integer> getModifiedLines() {
        return new TreeSet<>(modifiedLines);
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
     * @param line the line to check
     * @return {@code true} if this file has been modified at the specified line, {@code false} otherwise
     */
    public boolean hasModifiedLine(final int line) {
        return modifiedLines.contains(line);
    }

    /**
     * Marks the specified lines as being modified.
     *
     * @param lines the modified code lines
     */
    public void addModifiedLines(final int... lines) {
        for (int line : lines) {
            modifiedLines.add(line);
        }
    }

    @Override
    protected Optional<Node> filterTreeByModifiedLines() {
        return FileNodeFilter.byModifiedLines(this);
    }

    @Override
    protected Optional<Node> filterTreeByModifiedFiles() {
        return hasCoveredAndModifiedLines() ? Optional.of(copyTree()) : Optional.empty();
    }

    @Override
    protected Optional<Node> filterTreeByIndirectChanges() {
        return FileNodeFilter.byIndirectChanges(this);
    }

    /**
     * Adds an indirect coverage change for a specific line.
     *
     * @param line The line with the coverage change
     * @param hitsDelta The delta of the coverage hits before and after the code changes
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
     * @param line the line to check
     * @return {@code true} if this file has a coverage result for the specified line, {@code false} otherwise
     */
    public boolean hasCoverageForLine(final int line) {
        return coveredPerLine.containsKey(line)
                || mcdcPairCoveredPerLine.containsKey(line)
                || functionCallCoveredPerLine.containsKey(line);
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
     * @param referenceFile the file to compare with this file
     */
    // TODO: wouldn't it make more sense to return an independent object?
    public void computeDelta(final FileNode referenceFile) {
        computeDelta(referenceFile, MetricAggregation.getDefault())
                .forEach(difference -> coverageDelta.put(difference.getMetric(), difference));
    }

    /**
     * Returns the delta for the specified metric. If no delta is available for the specified metric, then 0 is
     * returned.
     *
     * @param metric the metric to get the delta for
     * @return the delta for the specified metric
     */
    public Value getDelta(final Metric metric) {
        return coverageDelta.getOrDefault(metric, Value.nullObject(metric));
    }

    /**
     * Returns whether this file has a delta result for the specified metric.
     *
     * @param metric the metric to check
     * @return {@code true} has delta results are available, {@code false} otherwise
     */
    public boolean hasDelta(final Metric metric) {
        return coverageDelta.containsKey(metric);
    }

    /**
     * Returns all lines that have at least one counter: line and branch, MC/DC pair, or function call coverage.
     *
     * @return the lines with any coverage information
     */
    NavigableSet<Integer> getLinesWithAnyCounter() {
        var lines = new TreeSet<Integer>();
        lines.addAll(coveredPerLine.keySet());
        lines.addAll(mcdcPairCoveredPerLine.keySet());
        lines.addAll(functionCallCoveredPerLine.keySet());
        return lines;
    }

    /**
     * Creates a new file node with the same name and path but without any coverage information.
     *
     * @return the empty copy
     */
    FileNode createEmptyCopy() {
        return new FileNode(getName(), relativePath);
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
     *     otherwise.
     */
    public boolean hasCoveredAndModifiedLines() {
        return !getCoveredAndModifiedLines().isEmpty();
    }

    /**
     * Add the coverage counters for the specified line.
     *
     * @param lineNumber the line number to add the counters for
     * @param covered the number of covered items
     * @param missed the number of missed items
     * @return this instance
     */
    @CanIgnoreReturnValue
    public FileNode addCounters(final int lineNumber, final int covered, final int missed) {
        coveredPerLine.put(lineNumber, covered);
        missedPerLine.put(lineNumber, missed);

        return this;
    }

    /**
     * Add the MCDC coverage counters for the specified line.
     *
     * @param lineNumber the line number to add the counters for
     * @param covered the number of covered items
     * @param missed the number of missed items
     * @return this instance
     */
    @CanIgnoreReturnValue
    public FileNode addMcdcPairCounters(final int lineNumber, final int covered, final int missed) {
        mcdcPairCoveredPerLine.put(lineNumber, covered);
        mcdcPairMissedPerLine.put(lineNumber, missed);

        return this;
    }

    /**
     * Add the function call coverage counters for the specified line.
     *
     * @param lineNumber the line number to add the counters for
     * @param covered the number of covered items
     * @param missed the number of missed items
     * @return this instance
     */
    @CanIgnoreReturnValue
    public FileNode addFunctionCallCounters(final int lineNumber, final int covered, final int missed) {
        functionCallCoveredPerLine.put(lineNumber, covered);
        functionCallMissedPerLine.put(lineNumber, missed);

        return this;
    }

    public int[] getCoveredCounters() {
        return entriesToArray(coveredPerLine);
    }

    public int[] getMissedCounters() {
        return entriesToArray(missedPerLine);
    }

    public int[] getMcdcPairCoveredCounters() {
        return entriesToArray(mcdcPairCoveredPerLine);
    }

    public int[] getMcdcPairMissedCounters() {
        return entriesToArray(mcdcPairMissedPerLine);
    }

    public int[] getFunctionCallCoveredCounters() {
        return entriesToArray(functionCallCoveredPerLine);
    }

    public int[] getFunctionCallMissedCounters() {
        return entriesToArray(functionCallMissedPerLine);
    }

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line the line to check
     * @return the number of covered items for the specified line
     */
    public int getCoveredOfLine(final int line) {
        return coveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of covered items of the MC/DC pair counters for the specified line.
     *
     * @param line the line to check
     * @return the number of covered items for the specified line
     */
    int getMcdcPairCoveredOfLine(final int line) {
        return mcdcPairCoveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of covered items of the function call counters for the specified line.
     *
     * @param line the line to check
     * @return the number of covered items for the specified line
     */
    int getFunctionCallCoveredOfLine(final int line) {
        return functionCallCoveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of missed items of the MC/DC pair counters for the specified line.
     *
     * @param line the line to check
     * @return the number of missed items for the specified line
     */
    int getMcdcPairMissedOfLine(final int line) {
        return mcdcPairMissedPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of missed items of the function call counters for the specified line.
     *
     * @param line the line to check
     * @return the number of missed items for the specified line
     */
    int getFunctionCallMissedOfLine(final int line) {
        return functionCallMissedPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of missed items for the specified line.
     *
     * @param line the line to check
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
        return coveredPerLine.keySet().stream().filter(predicate).collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Returns the lines that have no line coverage grouped in LineRanges. E.g., the lines [1, 2, 3] will be grouped in
     * one {@link LineRange} instance.
     *
     * @return the aggregated LineRanges that have no line coverage
     */
    public LineRangeList getMissedLineRanges() {
        return MissedLineRanges.of(this);
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
     * Returns the lines that contain mutations. The returned map contains the line number as the key and a list of
     * mutations as value.
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
     * @param mutation the mutation to add
     */
    // TODO: not part of API, only for tests?
    public void addMutation(final Mutation mutation) {
        mutations.add(mutation);
    }

    @Override
    public List<Mutation> getMutations() {
        return Collections.unmodifiableList(mutations);
    }

    /**
     * Returns the relative path of the file.
     *
     * @return the relative path of the file
     */
    public String getRelativePath() {
        return relativePath.toString();
    }

    @SuppressFBWarnings(value = "SECWF", justification = "False positive")
    public String getFileName() {
        return FilenameUtils.getName(getRelativePath());
    }

    /**
     * Sets the relative path of the file.
     *
     * @param relativePath the relative path
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
        var fileNode = (FileNode) o;
        return Objects.equals(coveredPerLine, fileNode.coveredPerLine)
                && Objects.equals(missedPerLine, fileNode.missedPerLine)
                && Objects.equals(mcdcPairCoveredPerLine, fileNode.mcdcPairCoveredPerLine)
                && Objects.equals(mcdcPairMissedPerLine, fileNode.mcdcPairMissedPerLine)
                && Objects.equals(functionCallCoveredPerLine, fileNode.functionCallCoveredPerLine)
                && Objects.equals(functionCallMissedPerLine, fileNode.functionCallMissedPerLine)
                && Objects.equals(mutations, fileNode.mutations)
                && Objects.equals(modifiedLines, fileNode.modifiedLines)
                && Objects.equals(indirectCoverageChanges, fileNode.indirectCoverageChanges)
                && Objects.equals(coverageDelta, fileNode.coverageDelta)
                && Objects.equals(relativePath, fileNode.relativePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                coveredPerLine,
                missedPerLine,
                mutations,
                modifiedLines,
                mcdcPairCoveredPerLine,
                mcdcPairMissedPerLine,
                functionCallCoveredPerLine,
                functionCallMissedPerLine,
                indirectCoverageChanges,
                coverageDelta,
                relativePath);
    }

    @Override
    public boolean isAggregation() {
        return false;
    }
}
