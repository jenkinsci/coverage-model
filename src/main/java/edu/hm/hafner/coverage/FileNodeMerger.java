package edu.hm.hafner.coverage;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.util.PitMutator;
import edu.hm.hafner.util.SuppressMutation;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Merges the coverage information of two {@link FileNode file nodes} that represent the same file. The counters of the
 * file (lines, branches, MC/DC pairs, function calls) are combined line by line, the classes, methods and mutations of
 * both files are retained.
 *
 * @author Ullrich Hafner
 * @see FileNode#merge(Node)
 */
final class FileNodeMerger {
    /** Coverages computed from line counters: merged by keeping the better value (see {@link #mergeCounters}). */
    private static final Set<Metric> COUNTER_METRICS =
            Set.of(Metric.LINE, Metric.BRANCH, Metric.INSTRUCTION, Metric.MCDC_PAIR, Metric.FUNCTION_CALL);

    private final FileNode target;
    private final FileNode source;

    /**
     * Creates a new merger.
     *
     * @param target the file to merge into, this file will be modified
     * @param source the file to merge, this file remains unchanged
     */
    FileNodeMerger(final FileNode target, final FileNode source) {
        this.target = target;
        this.source = source;
    }

    /** Merges the {@code source} file into the {@code target} file. */
    void merge() {
        var classes = target.getChildren();

        target.removeValues();
        target.removeChildren();

        mergeCounters();
        mergeClasses(classes);
        mergeMutations();
    }

    /**
     * Merges the classes (and their methods) of both files. The values that are stored in the file (line, branch, MC/DC
     * and function call coverage) are recomputed from the merged counters, but values that are only available in the
     * classes and methods (e.g., mutation coverage and test strength of PIT, or the complexity of methods) must survive
     * the merge.
     *
     * @param classes the classes of the target file before the merge
     */
    private void mergeClasses(final List<Node> classes) {
        target.addAllChildren(classes);
        mergeMembers(target, source);
    }

    /**
     * Merges the classes and methods of {@code source} into {@code target}. The counter based coverages (lines,
     * branches, instructions, MC/DC pairs, function calls) are combined like the counters of the file: two reports of
     * the same code cover what either of them covers, so the better value is retained. All other values (e.g., mutation
     * coverage or complexity) are merged like in {@link Node#merge(Node)}.
     *
     * <p>Note that the counter based coverages of classes and methods are only an approximation (a lower bound) after
     * the merge: the coverages of a file are computed from its line counters, but these counters are not available for
     * classes and methods. If a method has 4 lines and one report covers 2 of them and the other report covers the
     * other 2, then the file aggregates the lines to 4/4, while the method still reports 2/4.
     *
     * <p>Classes and methods are matched by their {@link Node#getId() ID}. If the parsers create different names for
     * the same class, then the classes of both reports are kept side by side. E.g., JaCoCo uses the fully qualified
     * class name while PIT uses the simple class name; Cobertura reports of coverage.py use the file name as class name
     * and contain no methods.
     *
     * @param targetNode the node to merge into
     * @param sourceNode the node to merge
     */
    private static void mergeMembers(final Node targetNode, final Node sourceNode) {
        for (Node sourceChild : sourceNode.getChildren()) {
            var targetChild = targetNode.getChildren().stream()
                    .filter(c -> c.getId().equals(sourceChild.getId()))
                    .findFirst();
            if (targetChild.isPresent()) {
                mergeMemberValues(targetChild.get(), sourceChild);
                mergeMembers(targetChild.get(), sourceChild);
            } else {
                targetNode.addChild(sourceChild.copyTree());
            }
        }
    }

    private static void mergeMemberValues(final Node targetNode, final Node sourceNode) {
        for (Value sourceValue : sourceNode.getValues()) {
            var metric = sourceValue.getMetric();
            var targetValue = targetNode.getValues().stream()
                    .filter(value -> value.getMetric() == metric)
                    .findFirst();
            if (targetValue.isEmpty()) {
                targetNode.addValue(sourceValue);
            } else if (COUNTER_METRICS.contains(metric)) {
                targetNode.replaceValue(targetValue.get().max(sourceValue)); // covered by either report
            } else if (metric.getTendency() == Metric.MetricTendency.SMALLER_IS_BETTER) {
                targetNode.replaceValue(targetValue.get().max(sourceValue)); // worse value, as in Node
            } else {
                targetNode.replaceValue(targetValue.get().min(sourceValue)); // worse value, as in Node
            }
        }
    }

    private void mergeMutations() {
        for (var mutation : List.copyOf(source.getMutations())) {
            target.addMutation(mutation);
        }
    }

    @SuppressMutation(
            mutator = PitMutator.CONDITIONALS_BOUNDARY,
            justification = "False positive: For maximum calculation the boundary is irrelevant")
    private void mergeCounters() {
        var lines = target.getLinesWithAnyCounter();
        lines.addAll(source.getLinesWithAnyCounter());

        var lineCoverage = createBuilder(Metric.LINE);
        var branchCoverage = createBuilder(Metric.BRANCH);
        var mcdcPairCoverage = createBuilder(Metric.MCDC_PAIR);
        var functionCallCoverage = createBuilder(Metric.FUNCTION_CALL);

        for (final int line : lines) {
            var left = lineCounters(target, line);
            var right = lineCounters(source, line);

            // check for errors in branch coverages and adjust if necessary
            if (left.totalsNotEqual(right)) {
                if (left.getTotal() > right.getTotal()) {
                    right = left;
                } else {
                    left = right;
                }
            }

            var merged = selectBetter(left, right);
            target.addCounters(line, merged.getCovered(), merged.getMissed());
            updateLineCoverage(merged, lineCoverage);
            if (left.hasAnyInfo() || right.hasAnyInfo()) {
                branchCoverage.incrementCovered(merged.getCovered());
                branchCoverage.incrementMissed(merged.getMissed());
            }

            var mcdcPair = mergeExtendedCounters(line, mcdcPairCounters(target, line), mcdcPairCounters(source, line));
            target.addMcdcPairCounters(line, mcdcPair.getCovered(), mcdcPair.getMissed());
            mcdcPairCoverage.incrementCovered(mcdcPair.getCovered());
            mcdcPairCoverage.incrementMissed(mcdcPair.getMissed());

            var functionCall =
                    mergeExtendedCounters(line, functionCallCounters(target, line), functionCallCounters(source, line));
            target.addFunctionCallCounters(line, functionCall.getCovered(), functionCall.getMissed());
            functionCallCoverage.incrementCovered(functionCall.getCovered());
            functionCallCoverage.incrementMissed(functionCall.getMissed());
        }

        setValues(lineCoverage, branchCoverage, mcdcPairCoverage, functionCallCoverage);

        source.getValues().stream()
                .filter(value -> value.getMetric() == Metric.CYCLOMATIC_COMPLEXITY)
                .forEach(target::addValue);
    }

    private static CoverageBuilder createBuilder(final Metric metric) {
        return new CoverageBuilder().withMetric(metric).withCovered(0).withMissed(0);
    }

    private static CoverageMetricsValues lineCounters(final FileNode file, final int line) {
        return new CoverageMetricsValues(file.getCoveredOfLine(line), file.getMissedOfLine(line));
    }

    private static CoverageMetricsValues mcdcPairCounters(final FileNode file, final int line) {
        return new CoverageMetricsValues(file.getMcdcPairCoveredOfLine(line), file.getMcdcPairMissedOfLine(line));
    }

    private static CoverageMetricsValues functionCallCounters(final FileNode file, final int line) {
        return new CoverageMetricsValues(
                file.getFunctionCallCoveredOfLine(line), file.getFunctionCallMissedOfLine(line));
    }

    private CoverageMetricsValues mergeExtendedCounters(
            final int line, final CoverageMetricsValues left, final CoverageMetricsValues right) {
        if (left.getTotal() > 0 && right.getTotal() > 0 && left.totalsNotEqual(right)) {
            throw new IllegalArgumentException(
                    String.format(Locale.ENGLISH, "Cannot merge coverage information for line %d in %s", line, target));
        }
        if (left.getTotal() == 0) {
            return right;
        }
        if (right.getTotal() == 0) {
            return left;
        }
        // Aggregated counts do not identify individual pairs or calls: retain the higher count.
        return selectBetter(left, right);
    }

    private static CoverageMetricsValues selectBetter(
            final CoverageMetricsValues left, final CoverageMetricsValues right) {
        if (left.getCovered() > right.getCovered()) {
            return left;
        }
        return right;
    }

    private static void updateLineCoverage(final CoverageMetricsValues merged, final CoverageBuilder lineCoverage) {
        if (merged.getCovered() > 0) {
            lineCoverage.incrementCovered();
        } else {
            lineCoverage.incrementMissed();
        }
    }

    private void setValues(
            final CoverageBuilder lineCoverage,
            final CoverageBuilder branchCoverage,
            final CoverageBuilder mcdcPairCoverage,
            final CoverageBuilder functionCallCoverage) {
        for (CoverageBuilder builder : List.of(lineCoverage, branchCoverage, mcdcPairCoverage, functionCallCoverage)) {
            var value = builder.build();
            if (value.isSet()) {
                target.addValue(value);
            }
        }
    }
}
