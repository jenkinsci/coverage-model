package edu.hm.hafner.coverage;

import java.util.NavigableMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Mutation.MutationBuilder;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.TreeString;

import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.api.EqualsVerifierApi;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

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
        fileNode.addModifiedLines(10);
        fileNode.addIndirectCoverageChange(15, 123);
        var empty = new FileNode("empty", "path");
        fileNode.computeDelta(empty);
        return fileNode;
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

        assertThat(file.getRelativePath()).isEqualTo(relativePath);
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
        byte[] restored = readAllBytes("version-0.21.0.ser");

        var serializable = (FileNode)createSerializable();
        serializable.setRelativePath(TreeString.valueOf(StringUtils.EMPTY));
        assertThatRestoredInstanceEqualsOriginalInstance(serializable, restore(restored));
    }

    @Test
    void shouldComputeDelta() {
        var builder = new Coverage.CoverageBuilder();

        var fileA = new FileNode("FileA.java", ".");
        var fileALineCoverage = builder.setMetric(Metric.LINE).setCovered(10).setMissed(10).build();
        fileA.addValue(fileALineCoverage);

        var fileB = new FileNode("FileB.java", ".");
        var fileBLineCoverage = builder.setMetric(Metric.LINE).setCovered(20).setMissed(0).build();
        var fileABranchCoverage = builder.setMetric(Metric.BRANCH).setCovered(10).setMissed(5).build();
        fileB.addValue(fileBLineCoverage);
        fileB.addValue(fileABranchCoverage);

        fileB.computeDelta(fileA);

        assertThat(fileB.hasDelta(Metric.LINE)).isTrue();
        assertThat(fileB.hasDelta(Metric.BRANCH)).isFalse();
        assertThat(fileB.getDelta(Metric.LINE))
                .isEqualTo(Fraction.getFraction(20 - 10, 20).reduce());
    }

    @Test
    void shouldGetCounters() {
        var fileA = new FileNode("FileA.java", ".");
        fileA.addCounters(10, 2, 0);
        fileA.addCounters(15, 3, 1);
        fileA.addCounters(28, 0, 5);

        NavigableMap<Integer, Integer> counters = fileA.getCounters();

        assertThat(counters)
                .containsKeys(10, 15, 28)
                .containsValues(2, 3, 0)
                .hasSize(3);
        assertThat(fileA).hasLinesWithCoverage(10, 15, 28);
        assertThat(fileA.hasCoverageForLine(20)).isFalse();
    }

    @Test
    void shouldAddModifiedLines() {
        FileNode noModifiedLines = new FileNode("NoModified.java", ".");
        FileNode modifiedLines = new FileNode("Modified.java", ".");

        modifiedLines.addModifiedLines(1, 3, 5);

        assertThat(noModifiedLines.hasModifiedLines()).isFalse();
        assertThat(modifiedLines.hasModifiedLines()).isTrue();
        assertThat(modifiedLines).hasOnlyModifiedLines(1, 3, 5);
        assertThat(modifiedLines.hasModifiedLine(4)).isFalse();
    }

    @Test
    void shouldGetPartiallyCoveredLines() {
        var fileNode = new FileNode("NoModified.java", ".");

        fileNode.addCounters(1, 2, 1);
        fileNode.addCounters(2, 1, 0);
        fileNode.addCounters(3, 0, 1);
        fileNode.addCounters(4, 4, 3);

        assertThat(fileNode.getPartiallyCoveredLines())
                .containsOnlyKeys(1, 4)
                .containsValues(1, 3);
    }

    @Test
    void shouldThrowExceptionOnFilterTreeByModifiedLinesIfCoverageTotalIsZero() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(2, 0, 0);
        fileNode.addModifiedLines(2);

        assertThatIllegalArgumentException()
                .isThrownBy(fileNode::filterTreeByModifiedLines)
                .withMessageContaining("No coverage for line");
    }

    @Test
    void shouldFilterTreeByModifiedLinesWithMutation() {
        var mutationBuilder = new MutationBuilder();
        var fileNode = new FileNode("file.java", ".");
        var mutation = mutationBuilder.setLine(2).build();
        var otherMutation = mutationBuilder.setLine(3).build();

        fileNode.addCounters(2, 1, 0);
        fileNode.addModifiedLines(2);
        fileNode.addMutation(mutation);
        fileNode.addMutation(otherMutation);

        fileNode = (FileNode) fileNode.filterTreeByModifiedLines().orElseThrow();

        assertThat(fileNode).hasOnlyMutations(mutation);
    }

    @Test
    void shouldFilterTreeByModifiedLinesWithNoMutations() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(2, 1, 0);
        fileNode.addModifiedLines(2);

        fileNode = (FileNode) fileNode.filterTreeByModifiedLines().orElseThrow();

        assertThat(fileNode).hasNoMutations();
    }

    @Test
    void shouldFilterTreeByIndirectChangesPositiveDelta() {
        FileNode lineCoverage = new FileNode("lineCoverage.java", ".");
        FileNode branchCoverage = new FileNode("branchCoverage.java", ".");
        FileNode lineAndBranchCoverage = new FileNode("lineAndBranchCoverage.java", ".");

        lineCoverage.addCounters(1, 1, 0);
        lineCoverage.addIndirectCoverageChange(1, 1);
        branchCoverage.addCounters(1, 2, 0);
        branchCoverage.addIndirectCoverageChange(1, 3);
        lineAndBranchCoverage.addCounters(1, 2, 0);
        lineAndBranchCoverage.addCounters(2, 1, 0);
        lineAndBranchCoverage.addIndirectCoverageChange(1, 2);

        Node filteredLineCoverage = lineCoverage.filterTreeByIndirectChanges().orElseThrow();
        Node filteredBranchCoverage = branchCoverage.filterTreeByIndirectChanges().orElseThrow();
        Node filteredLineAndBranchCoverage = lineAndBranchCoverage.filterTreeByIndirectChanges().orElseThrow();

        assertThat(filteredLineCoverage)
                .hasOnlyValueMetrics(Metric.LINE);
        assertThat((Coverage) filteredLineCoverage.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasMissed(0);
        assertThat(filteredBranchCoverage)
                .hasOnlyValueMetrics(Metric.BRANCH);
        assertThat((Coverage) filteredBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(3)
                .hasMissed(0);
        assertThat(filteredLineAndBranchCoverage)
                .hasOnlyValueMetrics(Metric.LINE, Metric.BRANCH);
        assertThat((Coverage) filteredLineAndBranchCoverage.getValue(Metric.LINE).orElseThrow())
                .hasCovered(1)
                .hasMissed(0);
        assertThat((Coverage) filteredLineAndBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(2)
                .hasMissed(0);
    }

    @Test
    void shouldFilterTreeByIndirectChangesNegativeDelta() {
        FileNode lineCoverage = new FileNode("lineCoverage.java", ".");
        FileNode branchCoverage = new FileNode("branchCoverage.java", ".");
        FileNode lineAndBranchCoverage = new FileNode("lineAndBranchCoverage.java", ".");

        lineCoverage.addCounters(1, 1, 0);
        lineCoverage.addIndirectCoverageChange(1, -1);
        branchCoverage.addCounters(1, 2, 0);
        branchCoverage.addIndirectCoverageChange(1, -3);
        lineAndBranchCoverage.addCounters(1, 2, 0);
        lineAndBranchCoverage.addCounters(2, 1, 0);
        lineAndBranchCoverage.addIndirectCoverageChange(1, -2);

        Node filteredLineCoverage = lineCoverage.filterTreeByIndirectChanges().orElseThrow();
        Node filteredBranchCoverage = branchCoverage.filterTreeByIndirectChanges().orElseThrow();
        Node filteredLineAndBranchCoverage = lineAndBranchCoverage.filterTreeByIndirectChanges().orElseThrow();

        assertThat(filteredLineCoverage)
                .hasNoValueMetrics();
        assertThat(filteredBranchCoverage)
                .hasOnlyValueMetrics(Metric.BRANCH);
        assertThat((Coverage) filteredBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(0)
                .hasMissed(3);
        assertThat(filteredLineAndBranchCoverage)
                .hasOnlyValueMetrics(Metric.BRANCH);
        assertThat((Coverage) filteredLineAndBranchCoverage.getValue(Metric.BRANCH).orElseThrow())
                .hasCovered(0)
                .hasMissed(2);
    }

    @Test
    void shouldFilterTreeByIndirectChangesNoDelta() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(1, 2, 0);
        fileNode.addCounters(2, 0, 1);
        fileNode.addIndirectCoverageChange(1, 0);
        fileNode.addIndirectCoverageChange(2, 0);

        var filteredFileNode = fileNode.filterTreeByIndirectChanges().orElseThrow();

        assertThat(filteredFileNode)
                .hasNoValueMetrics();
    }

    @Test
    void shouldFilterTreeByIndirectChangeWhenDeltaForNonCoveredLine() {
        var fileNode = new FileNode("file.java", ".");

        fileNode.addCounters(1, 2, 0);
        fileNode.addCounters(2, 1, 0);
        fileNode.addIndirectCoverageChange(3, 1);

        var filteredFileNode = fileNode.filterTreeByIndirectChanges().orElseThrow();

        assertThat(filteredFileNode)
                .hasNoValueMetrics();
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
        assertThat(fileNode.getMissedLineRanges())
                .containsExactly(new LineRange(2, 5));

        fileNode.addCounters(6, 1, 1);
        fileNode.addCounters(7, 0, 1); // now a new range
        assertThat(fileNode.getMissedLineRanges())
                .containsExactly(new LineRange(2, 5), new LineRange(7));
    }
}
