package edu.hm.hafner.coverage;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Creates reduced copies of a {@link FileNode} that contain only the coverage information of a change set: either the
 * lines that have been modified or the lines whose coverage has been affected indirectly by the changes.
 *
 * @author Ullrich Hafner
 */
final class FileNodeFilter {
    private FileNodeFilter() {
        // prevents instantiation
    }

    /**
     * Creates a copy of the specified file that contains only the coverage information of the modified lines.
     *
     * @param file the file to filter
     * @param modifiedLines the modified lines of the file
     * @return the filtered copy, or an empty result if none of the modified lines has been covered
     */
    @SuppressWarnings("deprecation")
    static Optional<Node> byModifiedLines(final FileNode file, final NavigableSet<Integer> modifiedLines) {
        var coveredAndModifiedLines = file.getLinesWithCoverage();
        coveredAndModifiedLines.retainAll(modifiedLines);
        if (coveredAndModifiedLines.isEmpty()) {
            return Optional.empty();
        }

        var copy = file.createEmptyCopy();
        // TODO: remove together with the deprecated API of the modified lines in FileNode
        copy.addModifiedLines(modifiedLines.stream().mapToInt(Integer::intValue).toArray());

        filterLineAndBranchCoverage(file, copy, coveredAndModifiedLines);
        filterExtendedCoverage(file, copy, coveredAndModifiedLines);
        filterMutations(file, copy, modifiedLines);

        return Optional.of(copy);
    }

    private static void filterLineAndBranchCoverage(
            final FileNode file, final FileNode copy, final NavigableSet<Integer> lines) {
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var lineBuilder = new CoverageBuilder().withMetric(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        var branchBuilder = new CoverageBuilder().withMetric(Metric.BRANCH);
        for (int line : lines) {
            var covered = file.getCoveredOfLine(line);
            var missed = file.getMissedOfLine(line);
            var total = covered + missed;
            copy.addCounters(line, covered, missed);
            if (total == 0) {
                throw new IllegalArgumentException("No coverage for line " + line);
            } else if (total == 1) {
                lineCoverage = lineCoverage.add(
                        lineBuilder.withCovered(covered).withMissed(missed).build());
            } else {
                var branchCoveredAsLine = covered > 0 ? 1 : 0;
                lineCoverage = lineCoverage.add(lineBuilder
                        .withCovered(branchCoveredAsLine)
                        .withMissed(1 - branchCoveredAsLine)
                        .build());
                branchCoverage = branchCoverage.add(
                        branchBuilder.withCovered(covered).withMissed(missed).build());
            }
        }
        addLineAndBranchCoverage(copy, lineCoverage, branchCoverage);
    }

    private static void filterExtendedCoverage(
            final FileNode file, final FileNode copy, final NavigableSet<Integer> lines) {
        var mcdcCoverage = new CoverageBuilder()
                .withMetric(Metric.MCDC_PAIR)
                .withCovered(0)
                .withMissed(0);
        var callCoverage = new CoverageBuilder()
                .withMetric(Metric.FUNCTION_CALL)
                .withCovered(0)
                .withMissed(0);
        for (int line : lines) {
            int mcdcCovered = file.getMcdcPairCoveredOfLine(line);
            int mcdcMissed = file.getMcdcPairMissedOfLine(line);
            copy.addMcdcPairCounters(line, mcdcCovered, mcdcMissed);
            mcdcCoverage.incrementCovered(mcdcCovered);
            mcdcCoverage.incrementMissed(mcdcMissed);

            int callCovered = file.getFunctionCallCoveredOfLine(line);
            int callMissed = file.getFunctionCallMissedOfLine(line);
            copy.addFunctionCallCounters(line, callCovered, callMissed);
            callCoverage.incrementCovered(callCovered);
            callCoverage.incrementMissed(callMissed);
        }
        var mcdcValue = mcdcCoverage.build();
        if (mcdcValue.isSet()) {
            copy.addValue(mcdcValue);
        }
        var callValue = callCoverage.build();
        if (callValue.isSet()) {
            copy.addValue(callValue);
        }
    }

    private static void filterMutations(
            final FileNode file, final FileNode copy, final NavigableSet<Integer> modifiedLines) {
        file.getMutations().stream()
                .filter(mutation -> modifiedLines.contains(mutation.getLine()))
                .forEach(copy::addMutation);
        if (!copy.getMutations().isEmpty()) {
            var builder = new CoverageBuilder()
                    .withMetric(Metric.MUTATION)
                    .withMissed(0)
                    .withCovered(0);
            copy.getMutations().stream().filter(Mutation::isDetected).forEach(mutation -> builder.incrementCovered());
            copy.getMutations().stream()
                    .filter(Predicate.not(Mutation::isDetected))
                    .forEach(mutation -> builder.incrementMissed());
            copy.addValue(builder.build());
        }
    }

    /**
     * Creates a copy of the specified file that contains only the coverage information of the lines that have been
     * affected indirectly by the changes, i.e., lines that are not modified but whose coverage has changed.
     *
     * @param file the file to filter
     * @param indirectCoverageChanges the indirect coverage changes of the file
     * @return the filtered copy, or an empty result if the file has no indirect coverage changes
     */
    // TODO: the API for indirect changes does not work yet for mutations
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    static Optional<Node> byIndirectChanges(
            final FileNode file, final NavigableMap<Integer, Integer> indirectCoverageChanges) {
        if (indirectCoverageChanges.isEmpty()) {
            return Optional.empty();
        }

        var copy = file.createEmptyCopy();
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);
        for (Map.Entry<Integer, Integer> change : indirectCoverageChanges.entrySet()) {
            int delta = change.getValue();
            var currentCoverage = getBranchCoverage(file, change.getKey());
            if (!currentCoverage.isSet()) {
                currentCoverage = getLineCoverage(file, change.getKey());
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
            } else if (delta < 0) {
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

    private static Coverage getLineCoverage(final FileNode file, final int line) {
        if (file.hasCoverageForLine(line)) {
            var covered = file.getCoveredOfLine(line) > 0 ? 1 : 0;
            return new CoverageBuilder()
                    .withMetric(Metric.LINE)
                    .withCovered(covered)
                    .withMissed(1 - covered)
                    .build();
        }
        return Coverage.nullObject(Metric.LINE);
    }

    private static Coverage getBranchCoverage(final FileNode file, final int line) {
        if (file.hasCoverageForLine(line)) {
            var covered = file.getCoveredOfLine(line);
            var missed = file.getMissedOfLine(line);
            if (covered + missed > 1) {
                return new CoverageBuilder()
                        .withMetric(Metric.BRANCH)
                        .withCovered(covered)
                        .withMissed(missed)
                        .build();
            }
        }
        return Coverage.nullObject(Metric.BRANCH);
    }

    private static void addLineAndBranchCoverage(
            final FileNode copy, final Coverage lineCoverage, final Coverage branchCoverage) {
        if (lineCoverage.isSet()) {
            copy.addValue(lineCoverage);
        }
        if (branchCoverage.isSet()) {
            copy.addValue(branchCoverage);
        }
    }
}
