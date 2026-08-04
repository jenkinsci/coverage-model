package edu.hm.hafner.coverage.parser;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ParsingException;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.MutationStatus;
import edu.hm.hafner.coverage.Node;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link StrykerParser}.
 *
 * @author Akash Manna
 */
class StrykerParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new StrykerParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "stryker";
    }

    @Test
    void shouldCreateParserWithDefaultConstructor() {
        assertThat(new StrykerParser()).isNotNull();
    }

    @Test
    void shouldEnterIfBlockButSkipLoopWhenFilesObjectIsEmpty() {
        assertThatExceptionOfType(ParsingException.class)
                .isThrownBy(() -> readReport("mutation-report-no-files.json"));
    }

    @Test
    void shouldReturnEmptyTreeWhenFilesObjectIsEmptyAndErrorsIgnored() {
        var tree = readReport("mutation-report-no-files.json", ProcessingMode.IGNORE_ERRORS);

        assertThat(tree).hasNoChildren().hasNoValues();
        assertThat(getLog().getErrorMessages())
                .contains("[StrykerParser] The processed file 'mutation-report-no-files.json' does not contain data.");
    }

    @Test
    void shouldSkipIfBlockWhenFilesKeyIsMissing() {
        assertThatExceptionOfType(ParsingException.class)
                .isThrownBy(() -> readReport("mutation-report-missing-files.json"));
    }

    @Test
    void shouldReturnEmptyTreeWhenFilesKeyIsMissingAndErrorsIgnored() {
        var tree = readReport("mutation-report-missing-files.json", ProcessingMode.IGNORE_ERRORS);

        assertThat(tree).hasNoChildren().hasNoValues();
        assertThat(getLog().getErrorMessages())
                .contains("[StrykerParser] The processed file 'mutation-report-missing-files.json' does not contain data.");
    }

    @Test
    void shouldParseAllMutantStatusesInAddFile() {
        var tree = readReport("mutation-report.json");

        var file = findFile(tree, "add.js");
        assertThat(file).hasName("add.js").hasRelativePath("src/math/add.js");
        assertThat(file.getMutations()).hasSize(4)
                .extracting(Mutation::getStatus)
                .containsExactly(
                        MutationStatus.KILLED,
                        MutationStatus.SURVIVED,
                        MutationStatus.NO_COVERAGE,
                        MutationStatus.TIMED_OUT);
    }

    @Test
    void shouldComputeMutationCoverageForAddFile() {
        var tree = readReport("mutation-report.json");

        var file = findFile(tree, "add.js");
        assertThat(file.getValue(MUTATION)).hasValueSatisfying(value ->
                assertThat(value).isInstanceOfSatisfying(Coverage.class,
                        coverage -> assertThat(coverage).hasCovered(1).hasMissed(3)));
        assertThat(getLog().hasErrors()).isFalse();
    }

    @Test
    void shouldReturnEmptyKilledByWhenNodeIsAbsentOrNonArray() {
        var tree = readReport("mutation-report.json");

        var mutation = findMutationByStatus(tree, "add.js", MutationStatus.NO_COVERAGE);
        assertThat(mutation.getKillingTest()).isEmpty();
    }

    @Test
    void shouldReturnEmptyKilledByWhenArrayIsEmpty() {
        var tree = readReport("mutation-report.json");

        var mutation = findMutationByStatus(tree, "add.js", MutationStatus.SURVIVED);
        assertThat(mutation.getKillingTest()).isEmpty();
    }

    @Test
    void shouldJoinSingleKilledByValue() {
        var tree = readReport("mutation-report.json");

        var mutation = findMutationByStatus(tree, "add.js", MutationStatus.KILLED);
        assertThat(mutation.getKillingTest()).isEqualTo("test1");
    }

    @Test
    void shouldJoinMultipleKilledByValues() {
        var tree = readReport("mutation-report.json");

        var mutation = findMutationByStatus(tree, "multiply.js", MutationStatus.KILLED);
        assertThat(mutation.getKillingTest()).isEqualTo("test1,test2");
    }

    @Test
    void shouldParseAllStatusesInMultiplyFile() {
        var tree = readReport("mutation-report.json");

        var file = findFile(tree, "multiply.js");
        assertThat(file.getMutations()).hasSize(5)
                .extracting(Mutation::getStatus)
                .containsExactlyInAnyOrder(
                        MutationStatus.KILLED,
                        MutationStatus.RUN_ERROR,
                        MutationStatus.MEMORY_ERROR,
                        MutationStatus.NON_VIABLE,
                        MutationStatus.NON_VIABLE);
    }

    @Test
    void shouldMapRuntimeErrorToRunError() {
        var tree = readReport("mutation-report.json");

        assertThat(findFile(tree, "multiply.js").getMutations())
                .extracting(Mutation::getStatus)
                .contains(MutationStatus.RUN_ERROR);
    }

    @Test
    void shouldMapCompileErrorToMemoryError() {
        var tree = readReport("mutation-report.json");

        assertThat(findFile(tree, "multiply.js").getMutations())
                .extracting(Mutation::getStatus)
                .contains(MutationStatus.MEMORY_ERROR);
    }

    @Test
    void shouldMapIgnoredAndUnknownStatusToNonViable() {
        var tree = readReport("mutation-report.json");

        var nonViableCount = findFile(tree, "multiply.js").getMutations().stream()
                .map(Mutation::getStatus)
                .filter(MutationStatus.NON_VIABLE::equals)
                .count();
        assertThat(nonViableCount).isEqualTo(2);
    }

    @Test
    void shouldParseAllFilesInReport() {
        var tree = readReport("mutation-report.json");

        var fileNames = tree.getAllFileNodes().stream()
                .map(FileNode::getName)
                .toList();
        assertThat(fileNames).containsExactlyInAnyOrder("add.js", "multiply.js", "", "helper.js");
    }

    @Test
    void shouldHandleRootSlashFileKeyWithNullFileName() {
        var tree = readReport("mutation-report.json");

        var rootFile = findFile(tree, "");
        assertThat(rootFile.getMutations()).hasSize(1);
        assertThat(rootFile.getMutations().getFirst().getStatus()).isEqualTo(MutationStatus.KILLED);
    }

    @Test
    void shouldSkipMutantsWhenMutantsNodeIsNotArray() {
        var tree = readReport("mutation-report.json");

        var helperFile = findFile(tree, "helper.js");
        assertThat(helperFile.getMutations()).isEmpty();
        assertThat(helperFile.getValue(MUTATION)).isEmpty();
    }

    @Test
    void shouldReturnEmptyPackageNameWhenFileIsAtRootLevel() {
        var tree = readReport("mutation-report.json");

        var rootFile = findFile(tree, "");
        assertThat(rootFile.getRelativePath()).doesNotContain(".");
    }

    @Test
    void shouldAssignNonEmptyPackageNodeWhenFileKeyIsRootSlash() {
        var tree = readReport("mutation-report.json");

        var packageNames = tree.getChildren().stream()
                .map(Node::getName)
                .toList();
        assertThat(packageNames).contains("-");
    }

    @Test
    void shouldReturnDotSeparatedPackageNameForNestedFile() {
        var tree = readReport("mutation-report.json");

        var packageNames = tree.getChildren().stream()
                .map(Node::getName)
                .toList();
        assertThat(packageNames).contains("src.math");
    }

    @Test
    void shouldSetDotSeparatedMutatedClassForNestedFile() {
        var tree = readReport("mutation-report.json");

        var mutation = findMutationByStatus(tree, "add.js", MutationStatus.KILLED);
        assertThat(mutation.getMutatedClass()).isEqualTo("src.math.add");
    }

    private static FileNode findFile(
            final edu.hm.hafner.coverage.ModuleNode tree, final String name) {
        return tree.getAllFileNodes().stream()
                .filter(f -> f.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("File not found: " + name));
    }

    private static Mutation findMutationByStatus(
            final edu.hm.hafner.coverage.ModuleNode tree,
            final String fileName,
            final MutationStatus status) {
        return findFile(tree, fileName).getMutations().stream()
                .filter(m -> m.getStatus() == status)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No mutation with status " + status + " in " + fileName));
    }
}
