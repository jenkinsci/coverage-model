package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.assertThat;
import static edu.hm.hafner.coverage.assertions.Assertions.assertThatIllegalArgumentException;

import edu.hm.hafner.coverage.ChangeSet.ChangeSetBuilder;
import edu.hm.hafner.coverage.Mutation.MutationBuilder;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.TreeString;
import java.io.IOException;
import java.util.List;
import java.util.NavigableMap;
import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.api.EqualsVerifierApi;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

class FileNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.FILE;
    }

    @Override
    void configureEqualsVerifier(final EqualsVerifierApi<? extends Node> verifier) {
        verifier.withPrefabValues(TreeString.class, TreeString.valueOf("src"), TreeString.valueOf("test"))
                .suppress(Warning.NONFINAL_FIELDS);
    }

    @Override
    FileNode createNode(final String name) {
        var fileNode = new FileNode(name, "path");
        fileNode.addCounters(10, 1, 0);
        fileNode.addCounters(11, 2, 2);
        var empty = new FileNode("empty", "path");
        fileNode.computeDelta(empty);
        return fileNode;
    }

    @Test
    void shouldSearchForFilesByNameOrPath() {
        var file = "File.java";
        var left = new FileNode(file, "/root/left/File.java");
        left.addChild(new ClassNode("FileClass.java"));

        var right = new FileNode(file, "/root/right/File.java");
        right.addChild(new ClassNode("FileClass.java"));

        var root = new PackageNode("root");
        root.addAllChildren(left, right);

        assertThat(root.findFile("/root/right/File.java")).contains(right);
        assertThat(root.findFile("/root/left/File.java")).contains(left);
        assertThat(root).hasOnlyAllFileNodes(left, right);

        assertThat(root.findFile(file)).isNotEmpty(); // it does not matter which file is returned
    }

    @Test
    void shouldFailWhenPathIsIdentical() {
        var file = "File.java";
        var path = "/root/File.java";

        var red = new FileNode(file, path);
        var blue = new FileNode(file, path);

        var root = new PackageNode("root");
        assertThatIllegalArgumentException().isThrownBy(() -> root.addAllChildren(red, blue));
    }

    @Test
    void shouldGetFilePath() {
        var module = new ModuleNode("top-level"); // just for testing
        var folder = new PackageNode("folder"); // just for testing
        var fileName = "Coverage.java";
        var relativePath = "relative/path/to/file";
        var file = new FileNode(fileName, relativePath);
        folder.addChild(file);
        module.addChild(folder);

        assertThat(file).hasRelativePath(relativePath).isNotAggregation();
        var otherPath = "other";

        assertThat(file.getFiles()).containsExactly(relativePath);
        assertThat(folder.getFiles()).containsExactly(relativePath);
        assertThat(module.getFiles()).containsExactly(relativePath);

        assertThat(module.getAll(Metric.FILE)).containsExactly(file);
        assertThat(folder.getAll(Metric.FILE)).containsExactly(file);
        assertThat(file.getAll(Metric.FILE)).containsExactly(file);

        file.setRelativePath(TreeString.valueOf(otherPath));
        assertThat(file.getRelativePath()).isEqualTo(otherPath);

        assertThat(file.matches(Metric.FILE, fileName)).isTrue();
        assertThat(file.matches(Metric.FILE, otherPath)).isTrue();
        assertThat(file.matches(Metric.FILE, "wrong")).isFalse();

        assertThat(file.matches(Metric.FILE, fileName.hashCode())).isTrue();
        assertThat(file.matches(Metric.FILE, otherPath.hashCode())).isTrue();
        assertThat(file.matches(Metric.FILE, "wrong".hashCode())).isFalse();
    }

    @Test
    void shouldReadOldVersion() {
        var restored = readAllBytes("version-0.49.0.ser");

        assertThat(restore(restored))
                .usingRecursiveComparison()
                .ignoringFields("lineCounters.covered", "lineCounters.missed") // transient
                .isEqualTo(createSerializable());
    }

    @Test
    void shouldComputeDelta() {
        var builder = new Coverage.CoverageBuilder();

        var fileA = new FileNode("FileA.java", ".");
        var fileALineCoverage =
                builder.withMetric(Metric.LINE).withCovered(10).withMissed(10).build();
        fileA.addValue(fileALineCoverage);

        var fileB = new FileNode("FileB.java", ".");
        var fileBLineCoverage =
                builder.withMetric(Metric.LINE).withCovered(20).withMissed(0).build();
        var fileABranchCoverage =
                builder.withMetric(Metric.BRANCH).withCovered(10).withMissed(5).build();
        fileB.addValue(fileBLineCoverage);
        fileB.addValue(fileABranchCoverage);

        fileB.computeDelta(fileA);

        assertThat(fileB.hasDelta(Metric.LINE)).isTrue();
        assertThat(fileB.hasDelta(Metric.BRANCH)).isFalse();
        assertThat(fileB.getDelta(Metric.LINE).asDouble()).isEqualTo(50);
    }

    @Test
    void shouldGetCounters() {
        var fileA = new FileNode("FileA.java", ".");
        fileA.addCounters(10, 2, 0);
        fileA.addCounters(15, 3, 1);
        fileA.addCounters(28, 0, 5);

        NavigableMap<Integer, Integer> counters = fileA.getCounters();

        assertThat(counters).containsKeys(10, 15, 28).containsValues(2, 3, 0).hasSize(3);
        assertThat(fileA).hasLinesWithCoverage(10, 15, 28);
        assertThat(fileA.hasCoverageForLine(20)).isFalse();
    }

    @Test
    @Issue("https://github.com/jenkinsci/coverage-model/issues/244")
    void shouldMergeMismatchingBranchTotalsUsingValuesWithLargerTotal() {
        var left = new FileNode("File.java", ".");
        left.addCounters(79, 0, 3);

        var right = new FileNode("File.java", ".");
        right.addCounters(79, 0, 4);

        assertThat(left.merge(right)).isInstanceOfSatisfying(FileNode.class, file -> {
            assertThat(file.getCoveredOfLine(79)).isEqualTo(0);
            assertThat(file.getMissedOfLine(79)).isEqualTo(4);
        });

        assertThat((FileNode) right.merge(left)).isInstanceOfSatisfying(FileNode.class, file -> {
            assertThat(file.getCoveredOfLine(79)).isEqualTo(0);
            assertThat(file.getMissedOfLine(79)).isEqualTo(4);
        });
    }

    @Test
    @Issue("https://github.com/jenkinsci/coverage-model/issues/244")
    void shouldMergeMismatchingBranchTotalsBySelectingLargerTotalWithoutMixingValues() {
        var left = new FileNode("File.java", ".");
        left.addCounters(79, 2, 1);

        var right = new FileNode("File.java", ".");
        right.addCounters(79, 1, 3);

        assertThat(left.merge(right)).isInstanceOfSatisfying(FileNode.class, file -> {
            assertThat(file.getCoveredOfLine(79)).isEqualTo(1);
            assertThat(file.getMissedOfLine(79)).isEqualTo(3);
        });

        assertThat(right.merge(left)).isInstanceOfSatisfying(FileNode.class, file -> {
            assertThat(file.getCoveredOfLine(79)).isEqualTo(1);
            assertThat(file.getMissedOfLine(79)).isEqualTo(3);
        });
    }

    @Test
    void shouldKeepMutationsWhenMergingLineCoverageWithPitResults() {
        var lines = new FileNode("Calc.java", "edu/hm/hafner/Calc.java");
        lines.addCounters(10, 1, 0);
        lines.addCounters(11, 1, 0);
        lines.addCounters(12, 0, 1);
        var linesClass = new ClassNode("Calc");
        linesClass.addChild(new MethodNode("add", "(II)I", 10));
        lines.addChild(linesClass);

        var pit = new FileNode("Calc.java", "edu/hm/hafner/Calc.java");
        var killed = new Mutation.MutationBuilder()
                .withLine(10)
                .withIsDetected(true)
                .withStatus(MutationStatus.KILLED)
                .withMutatedClass("edu.hm.hafner.Calc")
                .withMutatedMethod("add")
                .withMutatedMethodSignature("(II)I")
                .withSourceFile("Calc.java")
                .withMutator("MATH")
                .withDescription("replaced + with -")
                .build();
        var survived = new Mutation.MutationBuilder()
                .withLine(11)
                .withIsDetected(false)
                .withStatus(MutationStatus.SURVIVED)
                .withMutatedClass("edu.hm.hafner.Calc")
                .withMutatedMethod("add")
                .withMutatedMethodSignature("(II)I")
                .withSourceFile("Calc.java")
                .withMutator("RETURN")
                .withDescription("replaced return value")
                .build();
        pit.addMutation(killed);
        pit.addMutation(survived);
        var pitClass = new ClassNode("Calc");
        var pitMethod = new MethodNode("add", "(II)I", 10);
        pitMethod.addValue(new Coverage.CoverageBuilder(Metric.MUTATION)
                .withCovered(1)
                .withMissed(1)
                .build());
        pitClass.addChild(pitMethod);
        pit.addChild(pitClass);

        for (var merged : List.of((FileNode) lines.merge(pit), (FileNode) pit.merge(lines))) {
            assertThat((Coverage) merged.getValue(Metric.LINE).orElseThrow())
                    .hasCovered(2)
                    .hasMissed(1);
            assertThat((Coverage) merged.getValue(Metric.MUTATION).orElseThrow())
                    .hasCovered(1)
                    .hasMissed(1);
            assertThat(merged.getMutations()).containsExactlyInAnyOrder(killed, survived);
            assertThat(merged.getAll(Metric.METHOD)).hasSize(1);
        }
    }

    @Test
    void shouldKeepAllMutationsWithTheSamePropertiesWhenMerging() {
        var mutation = new Mutation.MutationBuilder()
                .withLine(10)
                .withIsDetected(true)
                .withStatus(MutationStatus.KILLED)
                .withMutatedClass("edu.hm.hafner.Calc")
                .withMutatedMethod("add")
                .withSourceFile("Calc.java")
                .withMutator("MATH")
                .withDescription("replaced + with -");

        var twice = new FileNode("Calc.java", "edu/hm/hafner/Calc.java");
        twice.addMutation(mutation.build());
        twice.addMutation(mutation.build());
        var once = new FileNode("Calc.java", "edu/hm/hafner/Calc.java");
        once.addMutation(mutation.build());

        assertThat(((FileNode) twice.merge(once)).getMutations()).hasSize(3);
        assertThat(((FileNode) once.merge(twice)).getMutations()).hasSize(3);
    }

    @Test
    void shouldKeepComplexityOfMethodsWhenMergingFiles() {
        var left = new FileNode("Calc.java", "edu/hm/hafner/Calc.java");
        left.addCounters(10, 1, 0);
        var leftClass = new ClassNode("Calc");
        var leftMethod = new MethodNode("add", "(II)I", 10);
        leftMethod.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 3));
        leftClass.addChild(leftMethod);
        left.addChild(leftClass);

        var right = new FileNode("Calc.java", "edu/hm/hafner/Calc.java");
        right.addCounters(10, 0, 1);
        var rightClass = new ClassNode("Calc");
        var rightMethod = new MethodNode("sub", "(II)I", 20);
        rightMethod.addValue(new Value(Metric.CYCLOMATIC_COMPLEXITY, 5));
        rightClass.addChild(rightMethod);
        right.addChild(rightClass);

        var merged = (FileNode) left.merge(right);

        assertThat(merged.getAll(Metric.METHOD)).hasSize(2);
        assertThat(merged.findMethod("sub", "(II)I")).isPresent();
        assertThat((Coverage) merged.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasMissed(0);
    }

    @Test
    void shouldMergeAllVectorCastMetricsIndependently() {
        var left = new FileNode("condition.c", "condition.c");
        left.addCounters(10, 1, 1);
        left.addMcdcPairCounters(10, 0, 1);
        left.addFunctionCallCounters(10, 0, 1);
        var right = new FileNode("condition.c", "condition.c");
        right.addCounters(10, 2, 0);
        right.addMcdcPairCounters(10, 1, 0);
        right.addFunctionCallCounters(10, 1, 0);

        var merged = (FileNode) left.merge(right);
        assertThat((Coverage) merged.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat((Coverage) merged.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(2)
                .hasTotal(2);
        assertThat((Coverage) merged.getValue(Metric.MCDC_PAIR).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat((Coverage) merged.getValue(Metric.FUNCTION_CALL).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat(merged.getMcdcPairCoveredCounters()).containsExactly(1);
        assertThat(merged.getFunctionCallCoveredCounters()).containsExactly(1);
        assertThat(right.merge(left)).isEqualTo(merged);
    }

    @Test
    void shouldPreserveLineCoverageWhenMergingMultiplePairsAndCalls() {
        var left = new FileNode("condition.c", "condition.c");
        left.addCounters(10, 1, 0);
        left.addMcdcPairCounters(10, 1, 2);
        left.addFunctionCallCounters(10, 1, 1);
        var right = new FileNode("condition.c", "condition.c");
        right.addCounters(10, 1, 0);
        right.addMcdcPairCounters(10, 2, 1);
        right.addFunctionCallCounters(10, 2, 0);

        var merged = (FileNode) left.merge(right);
        assertThat((Coverage) merged.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat((Coverage) merged.getValue(Metric.MCDC_PAIR).orElseThrow())
                .hasCovered(2)
                .hasTotal(3);
        assertThat((Coverage) merged.getValue(Metric.FUNCTION_CALL).orElseThrow())
                .hasCovered(2)
                .hasTotal(2);
    }

    @Test
    void shouldMergeReportsWithMissingExtendedMetrics() {
        var left = new FileNode("condition.c", "condition.c");
        left.addCounters(10, 1, 0);
        var right = new FileNode("condition.c", "condition.c");
        right.addCounters(10, 1, 0);
        right.addMcdcPairCounters(10, 0, 1);
        right.addFunctionCallCounters(10, 1, 0);

        var merged = (FileNode) left.merge(right);
        assertThat((Coverage) merged.getValue(Metric.MCDC_PAIR).orElseThrow())
                .hasCovered(0)
                .hasTotal(1);
        assertThat((Coverage) merged.getValue(Metric.FUNCTION_CALL).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat(right.merge(left)).isEqualTo(merged);
    }

    @Test
    void shouldRejectIncompatibleExtendedTotalsEvenWhenBranchTotalsDiffer() {
        var left = new FileNode("condition.c", "condition.c");
        left.addCounters(10, 1, 1);
        left.addMcdcPairCounters(10, 0, 1);
        var right = new FileNode("condition.c", "condition.c");
        right.addCounters(10, 1, 2);
        right.addMcdcPairCounters(10, 0, 2);

        assertThatIllegalArgumentException().isThrownBy(() -> left.merge(right));
    }

    private ChangeSet createModifiedLines(final FileNode file, final int... lines) {
        return new ChangeSetBuilder()
                .addModifiedLines(file.getRelativePath(), lines)
                .build();
    }

    private ChangeSet createIndirectChanges(final FileNode file, final int line, final int hitsDelta) {
        return new ChangeSetBuilder()
                .addIndirectCoverageChange(file.getRelativePath(), line, hitsDelta)
                .build();
    }

    @Test
    void shouldRetainExtendedCoverageOnlyForModifiedLines() {
        var file = new FileNode("condition.c", "condition.c");
        file.addCounters(10, 1, 1);
        file.addMcdcPairCounters(10, 0, 3);
        file.addFunctionCallCounters(10, 1, 0);
        file.addCounters(20, 2, 0);
        file.addMcdcPairCounters(20, 3, 0);
        file.addFunctionCallCounters(20, 0, 2);
        var changes = createModifiedLines(file, 10);

        var filtered = (FileNode) file.filterTreeByModifiedLines(changes).orElseThrow();
        assertThat((Coverage) filtered.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat((Coverage) filtered.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(1)
                .hasTotal(2);
        assertThat((Coverage) filtered.getValue(Metric.MCDC_PAIR).orElseThrow())
                .hasCovered(0)
                .hasTotal(3);
        assertThat((Coverage) filtered.getValue(Metric.FUNCTION_CALL).orElseThrow())
                .hasCovered(1)
                .hasTotal(1);
        assertThat(filtered.getMcdcPairMissedCounters()).containsExactly(3);
        assertThat(filtered.getFunctionCallCoveredCounters()).containsExactly(1);
    }

    @Test
    void shouldGetPartiallyCoveredLines() {
        var fileNode = new FileNode("NoModified.java", ".");

        fileNode.addCounters(1, 2, 1);
        fileNode.addCounters(2, 1, 0);
        fileNode.addCounters(3, 0, 1);
        fileNode.addCounters(4, 4, 3);

        assertThat(fileNode.getPartiallyCoveredLines()).containsOnlyKeys(1, 4).containsValues(1, 3);
    }

    @Test
    void shouldThrowExceptionOnFilterTreeByModifiedLinesIfCoverageTotalIsZero() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(2, 0, 0);
        var changes = createModifiedLines(fileNode, 2);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> fileNode.filterTreeByModifiedLines(changes))
                .withMessageContaining("No coverage for line");
    }

    @Test
    void shouldFilterTreeByModifiedLinesWithMutation() {
        var mutationBuilder = new MutationBuilder();
        var fileNode = new FileNode("file.java", ".");
        var mutation = mutationBuilder.withLine(2).build();
        var otherMutation = mutationBuilder.withLine(3).build();

        fileNode.addCounters(2, 1, 0);
        fileNode.addMutation(mutation);
        fileNode.addMutation(otherMutation);

        fileNode = (FileNode) fileNode.filterTreeByModifiedLines(createModifiedLines(fileNode, 2))
                .orElseThrow();

        assertThat(fileNode).hasOnlyMutations(mutation);
    }

    @Test
    void shouldFilterTreeByModifiedLinesWithNoMutations() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(2, 1, 0);

        fileNode = (FileNode) fileNode.filterTreeByModifiedLines(createModifiedLines(fileNode, 2))
                .orElseThrow();

        assertThat(fileNode).hasNoMutations();
    }

    @Test
    void shouldFilterTreeByIndirectChangesPositiveDelta() {
        var lineCoverage = new FileNode("lineCoverage.java", ".");
        var branchCoverage = new FileNode("branchCoverage.java", ".");
        var lineAndBranchCoverage = new FileNode("lineAndBranchCoverage.java", ".");

        lineCoverage.addCounters(1, 1, 0);
        branchCoverage.addCounters(1, 2, 0);
        lineAndBranchCoverage.addCounters(1, 2, 0);
        lineAndBranchCoverage.addCounters(2, 1, 0);

        var filteredLineCoverage = lineCoverage
                .filterTreeByIndirectChanges(createIndirectChanges(lineCoverage, 1, 1))
                .orElseThrow();
        var filteredBranchCoverage = branchCoverage
                .filterTreeByIndirectChanges(createIndirectChanges(branchCoverage, 1, 3))
                .orElseThrow();
        var filteredLineAndBranchCoverage = lineAndBranchCoverage
                .filterTreeByIndirectChanges(createIndirectChanges(lineAndBranchCoverage, 1, 2))
                .orElseThrow();

        assertThat(filteredLineCoverage).hasOnlyValueMetrics(Metric.LINE);
        assertThat((Coverage) filteredLineCoverage.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasMissed(0);
        assertThat(filteredBranchCoverage).hasOnlyValueMetrics(Metric.BRANCH);
        assertThat((Coverage) filteredBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(3)
                .hasMissed(0);
        assertThat(filteredLineAndBranchCoverage).hasOnlyValueMetrics(Metric.LINE, Metric.BRANCH);
        assertThat((Coverage)
                        filteredLineAndBranchCoverage.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasMissed(0);
        assertThat((Coverage)
                        filteredLineAndBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(2)
                .hasMissed(0);
    }

    @Test
    void shouldFilterTreeByIndirectChangesNegativeDelta() {
        var lineCoverage = new FileNode("lineCoverage.java", ".");
        var branchCoverage = new FileNode("branchCoverage.java", ".");
        var lineAndBranchCoverage = new FileNode("lineAndBranchCoverage.java", ".");

        lineCoverage.addCounters(1, 1, 0);
        branchCoverage.addCounters(1, 2, 0);
        lineAndBranchCoverage.addCounters(1, 2, 0);
        lineAndBranchCoverage.addCounters(2, 1, 0);

        var filteredLineCoverage = lineCoverage
                .filterTreeByIndirectChanges(createIndirectChanges(lineCoverage, 1, -1))
                .orElseThrow();
        var filteredBranchCoverage = branchCoverage
                .filterTreeByIndirectChanges(createIndirectChanges(branchCoverage, 1, -3))
                .orElseThrow();
        var filteredLineAndBranchCoverage = lineAndBranchCoverage
                .filterTreeByIndirectChanges(createIndirectChanges(lineAndBranchCoverage, 1, -2))
                .orElseThrow();

        assertThat(filteredLineCoverage).hasNoValueMetrics();
        assertThat(filteredBranchCoverage).hasOnlyValueMetrics(Metric.BRANCH);
        assertThat((Coverage) filteredBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(0)
                .hasMissed(3);
        assertThat(filteredLineAndBranchCoverage).hasOnlyValueMetrics(Metric.BRANCH);
        assertThat((Coverage)
                        filteredLineAndBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(0)
                .hasMissed(2);
    }

    @Test
    void shouldFilterTreeByIndirectChangesNoDelta() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(1, 2, 0);
        fileNode.addCounters(2, 0, 1);
        var changes = new ChangeSetBuilder()
                .addIndirectCoverageChange(fileNode.getRelativePath(), 1, 0)
                .addIndirectCoverageChange(fileNode.getRelativePath(), 2, 0)
                .build();

        var filteredFileNode = fileNode.filterTreeByIndirectChanges(changes).orElseThrow();

        assertThat(filteredFileNode).hasNoValueMetrics();
    }

    @Test
    void shouldFilterTreeByIndirectChangeWhenDeltaForNonCoveredLine() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(1, 2, 0);
        fileNode.addCounters(2, 1, 0);

        var filteredFileNode = fileNode.filterTreeByIndirectChanges(createIndirectChanges(fileNode, 3, 1))
                .orElseThrow();

        assertThat(filteredFileNode).hasNoValueMetrics();
    }

    @Test
    void shouldReturnMissedLineRanges() {
        var fileNode = new FileNode("file.java", ".");
        assertThat(fileNode.getMissedLineRanges()).isEmpty();

        fileNode.addCounters(1, 1, 0);
        assertThat(fileNode.getMissedLineRanges()).isEmpty();

        fileNode.addCounters(2, 0, 1);
        assertThat(fileNode.getMissedLineRanges()).containsExactly(new LineRange(2));

        fileNode.addCounters(3, 0, 1);
        assertThat(fileNode.getMissedLineRanges()).containsExactly(new LineRange(2, 3));

        fileNode.addCounters(5, 0, 1); // belongs to the same range
        assertThat(fileNode.getMissedLineRanges()).containsExactly(new LineRange(2, 5));

        fileNode.addCounters(6, 1, 1);
        fileNode.addCounters(7, 0, 1); // now a new range
        assertThat(fileNode.getMissedLineRanges()).containsExactly(new LineRange(2, 5), new LineRange(7));

        fileNode.addCounters(8, 1, 1);
        assertThat(fileNode.getMissedLineRanges()).containsExactly(new LineRange(2, 5), new LineRange(7));
    }

    public static void main(final String... args) throws IOException {
        new FileNodeTest().createSerializationFile();
    }
}
