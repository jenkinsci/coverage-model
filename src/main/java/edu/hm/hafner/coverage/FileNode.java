package edu.hm.hafner.coverage;

import java.util.ArrayList;
import java.util.Collection;
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

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.util.Ensure;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.LineRangeList;
import edu.hm.hafner.util.TreeString;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A {@link Node} for a specific file. It stores the actual file name along with the coverage information.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity", "PMD.CouplingBetweenObjects", "PMD.ExcessivePublicCount"})
public final class FileNode extends Node {
    private static final long serialVersionUID = -3795695377267542624L; // Set to 1 when release 1.0.0 is ready
    private static final int UNSET = -1;

    private final NavigableMap<Integer, Integer> coveredPerLine = new TreeMap<>();
    private final NavigableMap<Integer, Integer> missedPerLine = new TreeMap<>();

    // metrics for MC/DC pairs per line
    private NavigableMap<Integer, Integer> mcdcPairCoveredPerLine = new TreeMap<>();
    private NavigableMap<Integer, Integer> mcdcPairMissedPerLine = new TreeMap<>();

    // metrics for function calls per line
    private NavigableMap<Integer, Integer> functionCallCoveredPerLine = new TreeMap<>();
    private NavigableMap<Integer, Integer> functionCallMissedPerLine = new TreeMap<>();

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

    @Override
    public String getId() {
        return relativePath.toString() + getName();
    }

    /**
     * Called after deserialization to retain backward compatibility.
     *
     * @return this
     */
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
        return getMetric().equals(searchMetric)
                && (getRelativePath().equals(searchName) || getName().equals(searchName));
    }

    @Override
    public boolean matches(final Metric searchMetric, final int searchNameHashCode) {
        return getMetric().equals(searchMetric)
                && (getRelativePath().hashCode() == searchNameHashCode || getName().hashCode() == searchNameHashCode);
    }

    @Override
    protected void mergeNode(final Node other) {
        Ensure.that(other).isInstanceOf(FileNode.class);

        removeValues();
        removeChildren();

        mergeCounters((FileNode) other);
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private void mergeCounters(final FileNode otherFile) {
        var lines = new TreeSet<Integer>();

        lines.addAll(coveredPerLine.keySet());
        lines.addAll(mcdcPairCoveredPerLine.keySet());
        lines.addAll(functionCallCoveredPerLine.keySet());
        lines.addAll(otherFile.coveredPerLine.keySet());

        var lineCoverage = new CoverageBuilder().withMetric(Metric.LINE).withCovered(0).withMissed(0);
        var branchCoverage = new CoverageBuilder().withMetric(Metric.BRANCH).withCovered(0).withMissed(0);
        var mcdcPairCoverage = new CoverageBuilder().withMetric(Metric.MCDC_PAIR).withCovered(0).withMissed(0);
        var functionCallCoverage = new CoverageBuilder().withMetric(Metric.FUNCTION_CALL).withCovered(0).withMissed(0);
                
        for (final int line : lines) {
            var left = new CoverageMetricsValues(coveredPerLine.getOrDefault(line, 0), missedPerLine.getOrDefault(line, 0));
            var leftMcdcPair = new CoverageMetricsValues(mcdcPairCoveredPerLine.getOrDefault(line, 0), mcdcPairMissedPerLine.getOrDefault(line, 0));
            var leftFunctionCall = new CoverageMetricsValues(functionCallCoveredPerLine.getOrDefault(line, 0), functionCallMissedPerLine.getOrDefault(line, 0));
            var right = new CoverageMetricsValues(otherFile.coveredPerLine.getOrDefault(line, 0), otherFile.missedPerLine.getOrDefault(line, 0));
            var rightMcdcPair = new CoverageMetricsValues(otherFile.mcdcPairCoveredPerLine.getOrDefault(line, 0), otherFile.mcdcPairMissedPerLine.getOrDefault(line, 0));
            var rightFunctionCall = new CoverageMetricsValues(otherFile.functionCallCoveredPerLine.getOrDefault(line, 0), otherFile.functionCallMissedPerLine.getOrDefault(line, 0));
            
            // check for errors in branch, mcdc pair and function call coverages
            if (left.totalsNotEqual(right)) {
                if (left.noMissing() || right.noMissing()) {
                    left.setCoveredFromMax(right);
                    left.clearMissed();
                    left.setTotalFromCovered();
                }
                else {
                    throw new IllegalArgumentException(
                            String.format("Cannot merge coverage information for line %d in %s",
                                    line, this));
                }
            }
            else if (leftMcdcPair.totalsNotEqual(rightMcdcPair) || leftFunctionCall.totalsNotEqual(rightFunctionCall)) {
                throw new IllegalArgumentException(
                        String.format("Cannot merge coverage information for line %d in %s",
                                line, this));
            }
            
            if (left.hasAnyInfo()) {
                // exact branch coverage cannot be computed, so choose the higher value
                mergeLeftRight(line, left.getCovered(), left.getMissed(), right.getCovered(), right.getMissed(), coveredPerLine, missedPerLine);
                updateLineCoverage(line, lineCoverage);
                updateBranchCoverage(line, branchCoverage);
            }
            else if (leftMcdcPair.hasAnyInfo()) {
                mergeLeftRight(line, leftMcdcPair.getCovered(), leftMcdcPair.getMissed(), 
                        rightMcdcPair.getCovered(), rightMcdcPair.getMissed(), 
                        mcdcPairCoveredPerLine, mcdcPairMissedPerLine);
                updateMcdcPairCoverage(line, mcdcPairCoverage);
            }
            else if (leftFunctionCall.hasAnyInfo()) {
                mergeLeftRight(line, leftFunctionCall.getCovered(), leftFunctionCall.getMissed(), rightFunctionCall.getCovered(), rightFunctionCall.getMissed(), functionCallCoveredPerLine, functionCallMissedPerLine);
                updateFunctionCallCoverage(line, functionCallCoverage);
            }
            else {
                coveredPerLine.put(line, left.getMaxCovered(right));
                missedPerLine.put(line, left.getMinMissed(right));

                updateLineCoverage(line, lineCoverage);
            }
        }
        
        setValues(lineCoverage, branchCoverage, mcdcPairCoverage, functionCallCoverage);

        otherFile.getValues().stream()
                .filter(value -> value.getMetric() == Metric.COMPLEXITY)
                .forEach(this::addValue);
    }
    
    private void setValues(final CoverageBuilder lineCoverage, final CoverageBuilder branchCoverage, final CoverageBuilder mcdcPairCoverage, final CoverageBuilder functionCallCoverage) {
        var lineValue = lineCoverage.build();
        if (lineValue.isSet()) {
            addValue(lineValue);
        }
        var branchValue = branchCoverage.build();
        if (branchValue.isSet()) {
            addValue(branchValue);
        }

        var mcdcPairValue = mcdcPairCoverage.build();
        if (mcdcPairValue.isSet()) {
            addValue(mcdcPairValue);
        }

        var functionCallValue = functionCallCoverage.build();
        if (functionCallValue.isSet()) {
            addValue(functionCallValue);
        }
    }
            
    private void mergeLeftRight(final int line, final int leftCovered, final int leftMissed, final int rightCovered, final int rightMissed, 
            final NavigableMap<Integer, Integer> localCoveredPerLine, final NavigableMap<Integer, Integer> localMissedPerLine) {
        if (leftCovered > rightCovered) { 
            localCoveredPerLine.put(line, leftCovered);
            localMissedPerLine.put(line, leftMissed);
        }
        else {
            localCoveredPerLine.put(line, rightCovered);
            localMissedPerLine.put(line, rightMissed);
        }
    }

    private void updateBranchCoverage(final int line, final CoverageBuilder branchCoverage) {
        branchCoverage.incrementCovered(getCoveredOfLine(line));
        branchCoverage.incrementMissed(getMissedOfLine(line));
    }

    private void updateMcdcPairCoverage(final int line, final CoverageBuilder mcdcPairCoverage) {
        mcdcPairCoverage.incrementCovered(getMcdcPairCoveredOfLine(line));
        mcdcPairCoverage.incrementMissed(getMcdcPairMissedOfLine(line));
    }

    private void updateFunctionCallCoverage(final int line, final CoverageBuilder functionCallCoverage) {
        functionCallCoverage.incrementCovered(getFunctionCallCoveredOfLine(line));
        functionCallCoverage.incrementMissed(getFunctionCallMissedOfLine(line));
    }

    private void updateLineCoverage(final int line, final CoverageBuilder lineCoverage) {
        if (getCoveredOfLine(line) > 0) {
            lineCoverage.incrementCovered();
        }
        else {
            lineCoverage.incrementMissed();
        }
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
        var lineBuilder = new CoverageBuilder().withMetric(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        var branchBuilder = new CoverageBuilder().withMetric(Metric.BRANCH);
        for (int line : getCoveredAndModifiedLines()) {
            var covered = coveredPerLine.getOrDefault(line, 0);
            var missed = missedPerLine.getOrDefault(line, 0);
            var total = covered + missed;
            copy.addCounters(line, covered, missed);
            if (total == 0) {
                throw new IllegalArgumentException("No coverage for line " + line);
            }
            else if (total == 1) {
                lineCoverage = lineCoverage.add(lineBuilder.withCovered(covered).withMissed(missed).build());
            }
            else {
                var branchCoveredAsLine = covered > 0 ? 1 : 0;
                lineCoverage = lineCoverage.add(
                        lineBuilder.withCovered(branchCoveredAsLine).withMissed(1 - branchCoveredAsLine).build());
                branchCoverage = branchCoverage.add(branchBuilder.withCovered(covered).withMissed(missed).build());
            }
        }
        addLineAndBranchCoverage(copy, lineCoverage, branchCoverage);
    }

    private void filterMutations(final FileNode copy) {
        mutations.stream().filter(mutation -> modifiedLines.contains(mutation.getLine())).forEach(copy::addMutation);
        if (!copy.mutations.isEmpty()) {
            var builder = new CoverageBuilder().withMetric(Metric.MUTATION).withMissed(0).withCovered(0);
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
                // the line is fully covered - even in the case of branch coverage
                if (delta == currentCoverage.getCovered()) {
                    builder.withMetric(Metric.LINE).withCovered(1).withMissed(0);
                    lineCoverage = lineCoverage.add(builder.build());
                }
                // the branch coverage increased for 'delta' hits
                if (currentCoverage.getTotal() > 1) {
                    builder.withMetric(Metric.BRANCH).withCovered(delta).withMissed(0);
                    branchCoverage = branchCoverage.add(builder.build());
                }
            }
            else if (delta < 0) {
                // the line is not covered anymore
                if (currentCoverage.getCovered() == 0) {
                    builder.withMetric(Metric.LINE).withCovered(0).withMissed(1);
                    lineCoverage = lineCoverage.add(builder.build());
                }
                // the branch coverage is decreased by 'delta' hits
                if (currentCoverage.getTotal() > 1) {
                    builder.withMetric(Metric.BRANCH).withCovered(0).withMissed(Math.abs(delta));
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
        return coveredPerLine.containsKey(line) ||  mcdcPairCoveredPerLine.containsKey(line) ||  functionCallCoveredPerLine.containsKey(line);
    }

    private Coverage getLineCoverage(final int line) {
        if (hasCoverageForLine(line)) {
            var covered = getCoveredOfLine(line) > 0 ? 1 : 0;
            return new CoverageBuilder().withMetric(Metric.LINE)
                    .withCovered(covered)
                    .withMissed(1 - covered)
                    .build();
        }
        return Coverage.nullObject(Metric.LINE);
    }

    private Coverage getBranchCoverage(final int line) {
        if (hasCoverageForLine(line)) {
            var covered = getCoveredOfLine(line);
            var missed = getMissedOfLine(line);
            if (covered + missed > 1) {
                return new CoverageBuilder().withMetric(Metric.BRANCH)
                        .withCovered(covered)
                        .withMissed(missed)
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
     *
     * @return this instance
     */
    @CanIgnoreReturnValue
    public FileNode addCounters(final int lineNumber, final int covered, final int missed) {
        coveredPerLine.put(lineNumber, covered);
        missedPerLine.put(lineNumber, missed);

        return this;
    }
    
    /**
     * Add the MCDC coverage  counters for the specified line.
     *
     * @param lineNumber
     *         the line number to add the counters for
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     *
     * @return this instance
     */
    @CanIgnoreReturnValue
    public FileNode addMcdcPairCounters(final int lineNumber, final int covered, final int missed) {
        mcdcPairCoveredPerLine.put(lineNumber, covered);
        mcdcPairMissedPerLine.put(lineNumber, missed);

        return this;
    }
    
    /**
     * Add the function call coverage  counters for the specified line.
     *
     * @param lineNumber
     *         the line number to add the counters for
     * @param covered
     *         the number of covered items
     * @param missed
     *         the number of missed items
     *
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
     * @param line
     *         the line to check
     *
     * @return the number of covered items for the specified line
     */
    public int getCoveredOfLine(final int line) {
        return coveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the number of covered items for the specified line
     */
    private int getMcdcPairCoveredOfLine(final int line) {
        return mcdcPairCoveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the number of covered items for the specified line
     */
    private int getFunctionCallCoveredOfLine(final int line) {
        return functionCallCoveredPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the number of covered items for the specified line
     */
    private int getMcdcPairMissedOfLine(final int line) {
        return mcdcPairMissedPerLine.getOrDefault(line, 0);
    }

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line
     *         the line to check
     *
     * @return the number of covered items for the specified line
     */
    private int getFunctionCallMissedOfLine(final int line) {
        return functionCallMissedPerLine.getOrDefault(line, 0);
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
     * Returns the lines that have no line coverage grouped in LineRanges. E.g., the lines [1, 2, 3] will be grouped in
     * one {@link LineRange} instance.
     *
     * @return the aggregated LineRanges that have no line coverage
     */
    @SuppressWarnings("PMD.LooseCoupling")
    public LineRangeList getMissedLineRanges() {
        var lineRanges = new LineRangeList();

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
     * @param mutation
     *         the mutation to add
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
        return Objects.hash(super.hashCode(), coveredPerLine, missedPerLine, mutations, modifiedLines,
                mcdcPairCoveredPerLine, mcdcPairMissedPerLine, functionCallCoveredPerLine, functionCallMissedPerLine,
                indirectCoverageChanges, coverageDelta, relativePath);
    }

    @Override
    public boolean isAggregation() {
        return false;
    }
}
