package edu.hm.hafner.coverage;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;
import org.junitpioneer.jupiter.Issue;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.Mutation.MutationBuilder;
import edu.hm.hafner.util.TreeString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * TestCount the class {@link Node}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class NodeTest {
    private static final String COVERED_FILE = "Covered.java";
    private static final Percentage HUNDERT_PERCENT = Percentage.valueOf(1, 1);
    private static final String MISSED_FILE = "Missed.java";
    private static final String CLASS_WITH_MODIFICATIONS = "classWithModifications";
    private static final String CLASS_WITHOUT_MODIFICATION = "classWithoutModification";
    private static final String COVERED_CLASS = "CoveredClass.class";
    private static final String MISSED_CLASS = "MissedClass.class";

    @Test
    void shouldMapPackageAndFileNameOfWarnings() {
        var node = new PackageNode("edu.hm.hafner.grading");

        var logHandler = new FileNode("LogHandler.java", "src/main/java/edu/hm/hafner/grading/LogHandler.java");
        node.addChild(logHandler);

        var value = new Value(WARNINGS, 1);
        logHandler.addValue(value);

        assertThat(logHandler)
                .hasFileName("LogHandler.java")
                .hasRelativePath("src/main/java/edu/hm/hafner/grading/LogHandler.java")
                .hasName("LogHandler.java")
                .hasValues(value);

        assertThat(node).hasName("edu.hm.hafner.grading");
        assertThat(node.aggregateValues()).containsExactly(value);
    }

    @Test
    void shouldFixFileNameAndRelativePath() {
        var root = new ModuleNode("Root");

        var fileNode = root.findOrCreateFileNode("File.java", TreeString.valueOf("relative/path/to/File.java"));
        assertThat(fileNode).hasFileName("File.java")
                .hasRelativePath("relative/path/to/File.java")
                .hasName("File.java");

        var other = root.findOrCreateFileNode("path/to/Other.java", TreeString.valueOf("relative/path/to/Other.java"));
        assertThat(other).hasFileName("Other.java")
                .hasRelativePath("relative/path/to/Other.java")
                .hasName("Other.java");

        var another = root.findOrCreateFileNode("another/path/to/Other.java", TreeString.valueOf("another/path/to/Other.java"));
        assertThat(another).hasFileName("Other.java")
                .hasRelativePath("another/path/to/Other.java")
                .hasName("Other.java");

        var wrongName = new FileNode("path/to/WrongName.java", "path/to/WrongName.java");
        assertThat(wrongName).hasFileName("WrongName.java")
                .hasRelativePath("path/to/WrongName.java")
                .hasName("path/to/WrongName.java"); // Note: This is the original name, not fixed
        root.addChild(wrongName);

        assertThat(root.findOrCreateFileNode("WrongName.java", TreeString.valueOf("path/to/WrongName.java")))
                .isSameAs(wrongName);
        assertThat(root.findOrCreateFileNode("path/to/WrongName.java", TreeString.valueOf("path/to/WrongName.java")))
                .isSameAs(wrongName);
    }

    @Test
    void shouldHandleNonExistingParent() {
        var root = new ModuleNode("Root");

        assertThat(root).doesNotHaveParent();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent)
                .withMessage("Parent is not set");
        assertThat(root).hasParentName(Node.ROOT);
    }

    @Test
    void shouldReturnParentOfNodeAndItsName() {
        var parent = new ModuleNode("Parent");
        var child = new PackageNode("Child");
        var subPackage = new PackageNode("SubPackage");
        var subSubPackage = new PackageNode("SubSubPackage");

        parent.addChild(child);
        child.addChild(subPackage);
        subPackage.addChild(subSubPackage);

        assertThat(child.getParent()).isEqualTo(parent);

        assertThat(child.getParentName()).isEqualTo("Parent");
        assertThat(subSubPackage.getParentName()).isEqualTo("Child.SubPackage");

        parent.setName("NewParent");
        assertThat(child.getParentName()).isEqualTo("NewParent");
    }

    @Test
    void shouldReturnCorrectChildNodes() {
        var parent = new ModuleNode("Parent");
        var child1 = new PackageNode("ChildOne");
        var child2 = new PackageNode("ChildTwo");

        assertThat(parent).hasNoChildren();

        parent.addChild(child1);
        assertThat(parent).hasOnlyChildren(child1);
        assertThat(parent).doesNotHaveChildren(child2);

        parent.addChild(child2);
        assertThat(parent).hasOnlyChildren(child1, child2);
    }

    @Test
    void shouldPrintAllMetricsForNodeAndChildNodes() {
        var parent = new ModuleNode("Parent");
        var child1 = new PackageNode("ChildOne");
        var child2 = new PackageNode("ChildTwo");
        var childOfChildOne = new FileNode("ChildOfChildOne", "path");
        var builder = new CoverageBuilder();
        childOfChildOne.addValue(builder.withMetric(LINE).withCovered(1).withMissed(0).build());
        parent.addChild(child1);
        parent.addChild(child2);
        child1.addChild(childOfChildOne);

        assertThat(parent.getMetrics()).containsOnly(MODULE, PACKAGE, FILE, LINE, LOC);
    }

    @Test
    void shouldCalculateDistributedMetrics() {
        var builder = new CoverageBuilder();

        var node = new ModuleNode("Node");

        var valueOne = builder.withMetric(LINE).withCovered(1).withMissed(0).build();
        node.addValue(valueOne);
        var valueTwo = builder.withMetric(BRANCH).withCovered(0).withMissed(1).build();
        node.addValue(valueTwo);

        assertThat(node.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                valueOne,
                valueTwo,
                new Value(LOC, 1));
    }

    @Test
    void shouldHandleLeaves() {
        var node = new ModuleNode("Node");

        assertThat(node).hasNoValues();

        var builder = new CoverageBuilder();
        var leafOne = builder.withMetric(LINE).withCovered(1).withMissed(0).build();
        node.addValue(leafOne);
        assertThat(node).hasOnlyValues(leafOne);

        var leafTwo = builder.withMetric(BRANCH).withCovered(0).withMissed(1).build();
        node.addValue(leafTwo);
        assertThat(node).hasOnlyValues(leafOne, leafTwo);

        assertThat(getCoverage(node, LINE)).hasCoveredPercentage(HUNDERT_PERCENT);
        assertThat(getCoverage(node, BRANCH)).hasCoveredPercentage(Percentage.ZERO);

        assertThatIllegalArgumentException().isThrownBy(() -> node.addValue(leafOne));
        assertThatIllegalArgumentException().isThrownBy(() -> node.addValue(leafTwo));
    }

    @Test
    void shouldReturnAllNodesOfSpecificMetricType() {
        var parent = new ModuleNode("Parent");
        var child1 = new PackageNode("ChildOne");
        var child2 = new PackageNode("ChildTwo");
        var childOfChildOne = new FileNode("ChildOfChildOne", "path");
        var childOfChildTwo = new FileNode("ChildOfChildTwo", "path");

        parent.addChild(child1);
        parent.addChild(child2);
        child1.addChild(childOfChildOne);
        child2.addChild(childOfChildTwo);

        assertThat(parent.getAll(FILE))
                .hasSize(2)
                .containsOnly(childOfChildOne, childOfChildTwo);
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).orElseThrow();
    }

    @Test
    void shouldCalculateCorrectCoverageForModule() {
        var node = new ModuleNode("Node");
        var valueOne = new CoverageBuilder().withMetric(LINE).withCovered(1).withMissed(0).build();

        node.addValue(valueOne);

        assertThat(getCoverage(node, MODULE)).hasCoveredPercentage(HUNDERT_PERCENT);
    }

    @Test
    void shouldCalculateCorrectCoverageWithNestedStructure() {
        var node = new ModuleNode("Node");
        var missedFile = new FileNode("fileMissed", "path");
        var coveredFile = new FileNode("fileCovered", "path");
        var valueOne = Coverage.valueOf(LINE, "1/1");
        var valueTwo = Coverage.valueOf(LINE, "0/1");

        node.addChild(missedFile);
        node.addChild(coveredFile);
        coveredFile.addValue(valueOne);
        missedFile.addValue(valueTwo);

        var oneHalf = Percentage.valueOf(1, 2);
        assertThat(getCoverage(node, LINE)).hasCoveredPercentage(oneHalf);
        assertThat(getCoverage(node, FILE)).hasCoveredPercentage(oneHalf);
    }

    @Test
    void shouldDeepCopyNodeTree() {
        var node = new ModuleNode("Node");
        var childNode = new FileNode("childNode", "path");
        var valueOne = new CoverageBuilder().withMetric(LINE).withCovered(1).withMissed(0).build();
        var valueTwo = new CoverageBuilder().withMetric(LINE).withCovered(0).withMissed(1).build();

        node.addValue(valueOne);
        node.addChild(childNode);
        childNode.addValue(valueTwo);
        var copiedNode = node.copyTree();

        assertThat(node).isNotSameAs(copiedNode);
        assertThat(node.getChildren().getFirst()).isNotSameAs(copiedNode.getChildren().getFirst());
    }

    @Test
    void shouldDeepCopyNodeTreeWithSpecifiedNodeAsParent() {
        var node = new ModuleNode("Node");
        var childNode = new FileNode("childNode", "path");
        var valueOne = new CoverageBuilder().withMetric(LINE).withCovered(1).withMissed(0).build();
        var valueTwo = new CoverageBuilder().withMetric(LINE).withCovered(0).withMissed(1).build();
        var newParent = new ModuleNode("parent");

        node.addValue(valueOne);
        node.addChild(childNode);
        childNode.addValue(valueTwo);
        var copiedNode = node.copyTree(newParent);

        assertThat(copiedNode).hasParent(newParent);
    }

    @Test
    void shouldDetectMatchingOfMetricTypeAndNameOrHashCode() {
        var node = new ModuleNode("Node");

        assertThat(node.matches(MODULE, "WrongName")).isFalse();
        assertThat(node.matches(PACKAGE, "Node")).isFalse();
        assertThat(node.matches(node.getMetric(), node.getName())).isTrue();

        assertThat(node.matches(MODULE, node.getName().hashCode())).isTrue();
        assertThat(node.matches(MODULE, "WrongName".hashCode())).isFalse();
    }

    @Test
    void shouldFindNodeByNameOrHashCode() {
        var node = new ModuleNode("Node");
        var childNode = new FileNode("childNode", "path");
        node.addChild(childNode);

        assertThat(node.find(BRANCH, "NotExisting")).isNotPresent();
        assertThat(node.find(FILE, childNode.getName())).isPresent().contains(childNode);

        assertThat(node.findByHashCode(BRANCH, "NotExisting".hashCode())).isNotPresent();
        assertThat(node.findByHashCode(FILE, childNode.getName().hashCode())).isPresent().contains(childNode);
    }

    @Test
    void shouldNotAcceptIncompatibleNodes() {
        var module = new ModuleNode("edu.hm.hafner.module1");
        var pkg = new PackageNode("edu.hm.hafner.pkg");
        var moduleTwo = new ModuleNode("edu.hm.hafner.module2");

        assertThatIllegalArgumentException()
                .as("Should not accept incompatible nodes (different metric)")
                .isThrownBy(() -> module.merge(pkg));
        assertThatIllegalArgumentException()
                .as("Should not accept incompatible nodes (different name)")
                .isThrownBy(() -> module.merge(moduleTwo));
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingDifferentPackages() {
        var module = new ModuleNode("edu.hm.hafner.module1");
        var sameModule = new ModuleNode("edu.hm.hafner.module1");
        var pkgOne = new PackageNode("coverage");
        var pkgTwo = new PackageNode("autograding");

        module.addChild(pkgOne);
        sameModule.addChild(pkgTwo);
        var combinedReport = module.merge(sameModule);

        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSamePackage() {
        var module = new ModuleNode("edu.hm.hafner.module1");
        var sameModule = new ModuleNode("edu.hm.hafner.module1");
        var pkg = new PackageNode("coverage");
        var samePackage = new PackageNode("coverage");

        module.addChild(pkg);
        sameModule.addChild(samePackage);
        var combinedReport = module.merge(sameModule);
        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSameAndDifferentPackages() {
        var module = new ModuleNode("edu.hm.hafner.module1");
        var sameModule = new ModuleNode("edu.hm.hafner.module1");
        var pkg = new PackageNode("coverage");
        var pkgTwo = new PackageNode("autograding");

        module.addChild(pkg);
        sameModule.addChild(pkgTwo);
        sameModule.addChild(pkg.copy());
        var combinedReport = module.merge(sameModule);

        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);
        assertThat(combinedReport.getAll(PACKAGE)).satisfiesExactlyInAnyOrder(
                p -> assertThat(p.getName()).isEqualTo(pkg.getName()),
                p -> assertThat(p.getName()).isEqualTo(pkgTwo.getName())
        );
    }

    @Test
    void shouldKeepChildNodesAfterCombiningReportWithSamePackage() {
        var module = new ModuleNode("edu.hm.hafner.module1");
        var sameModule = new ModuleNode("edu.hm.hafner.module1");
        var pkg = new PackageNode("coverage");
        var samePackage = new PackageNode("coverage");

        var fileToKeep = new FileNode("KeepMe", "path");
        var otherFileToKeep = new FileNode("KeepMeToo", "path");

        pkg.addChild(fileToKeep);
        module.addChild(pkg);
        samePackage.addChild(otherFileToKeep);
        sameModule.addChild(samePackage);
        var combinedReport = module.merge(sameModule);

        assertThat(combinedReport.getChildren().getFirst()).hasOnlyChildren(fileToKeep, otherFileToKeep);
    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithInRelatedProjects() {
        var project = new ModuleNode("edu.hm.hafner.module1");
        var sameProject = project.copyTree();
        var coveragePkg = new PackageNode("coverage");
        var autogradingPkg = new PackageNode("autograding");

        project.addChild(coveragePkg);
        sameProject.addChild(autogradingPkg);
        var combinedReport = project.merge(sameProject);

        assertThat(combinedReport.find(coveragePkg.getMetric(), coveragePkg.getName()).orElseThrow())
                .isNotSameAs(coveragePkg);
        assertThat(combinedReport.find(autogradingPkg.getMetric(), autogradingPkg.getName()).orElseThrow())
                .isNotSameAs(autogradingPkg);
    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelAndOtherReportHasHigherCoverage() {
        var report = new ModuleNode("edu.hm.hafner.module1");
        var pkg = new PackageNode("coverage");
        var file = new FileNode("Node.java", "path");

        report.addChild(pkg);
        pkg.addChild(file);
        var otherReport = report.copyTree();

        otherReport.getAllFileNodes().getFirst()
                .addCounters(1, 1, 0)
                .addCounters(2, 1, 0)
                .addCounters(3, 0, 1);
        report.getAllFileNodes().getFirst()
                .addCounters(1, 1, 0)
                .addCounters(2, 0, 1)
                .addCounters(3, 1, 0);

        var combined = report.merge(otherReport);
        assertThat(getCoverage(combined, LINE)).hasMissed(0).hasCovered(3);
    }

    @Test
    void shouldFilterByFileName() {
        var tree = createTreeWithoutCoverage();

        var files = tree.getFiles();
        var single = List.of(new ArrayList<>(files).getFirst());
        var filtered = tree.filterByFileNames(single);

        assertThat(filtered.getFiles()).hasSize(1).first().asString().contains(COVERED_FILE);
    }

    @Test
    void shouldFilterByFile() {
        var tree = createTreeWithoutCoverage();

        var filtered = tree.copyTree(null, this::file);

        assertThat(filtered.getFiles()).hasSize(1).first().asString().contains(COVERED_FILE);
    }

    private boolean file(final Node node) {
        return node.getMetric() != FILE || node.getName().contains(COVERED_FILE);
    }

    @Test
    void shouldCreateEmptyModifiedLinesCoverageTreeWithoutChanges() {
        var tree = createTreeWithoutCoverage();

        verifyEmptyTree(tree, tree.filterByModifiedLines());
    }

    @Test
    void shouldCreateModifiedLinesCoverageTree() {
        var tree = createTreeWithoutCoverage();

        var file = tree.findFile(COVERED_FILE);
        assertThat(file).isPresent();

        registerCodeChangesAndCoverage(file.get());

        verifyFilteredTree(tree, tree.filterByModifiedLines(), this::verifyModifiedLines);
    }

    private void verifyFilteredTree(final Node tree, final Node filteredTree,
            final ThrowingConsumer<Node> treeVerification) {
        assertThat(filteredTree)
                .isNotSameAs(tree)
                .hasName(tree.getName())
                .hasMetric(tree.getMetric())
                .hasOnlyFiles("path/to/" + COVERED_FILE)
                .hasModifiedLines()
                .satisfies(treeVerification);
    }

    private void verifyModifiedLines(final Node root) {
        assertThat(root.getAll(FILE)).extracting(Node::getName).containsExactly(COVERED_FILE);

        var builder = new CoverageBuilder();
        assertThat(root.getValue(LINE)).isNotEmpty().contains(
                builder.withMetric(LINE).withCovered(4).withMissed(3).build());
        assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                builder.withMetric(BRANCH).withCovered(6).withMissed(6).build());
        assertThat(root.getValue(MUTATION)).isNotEmpty().contains(
                builder.withMetric(MUTATION).withCovered(1).withMissed(2).build());

        assertThat(root.findFile(COVERED_FILE)).isPresent().get().satisfies(file -> {
            verifyCountersOfCoveredClass(file);
            assertThat(file.getCoveredCounters()).containsExactly(1, 0, 1, 0, 0, 4, 2);
            assertThat(file.getMissedCounters()).containsExactly(0, 1, 0, 1, 4, 0, 2);
            assertThat(file.getMissedLines()).containsExactly(11, 13, 14);
            assertThat(file.getPartiallyCoveredLines()).containsExactly(entry(16, 2));
            assertThat(file.getMutations()).extracting(Mutation::getLine)
                    .containsExactlyInAnyOrder(17, 18, 19);
        });
    }

    private void verifyCountersOfCoveredClass(final FileNode file) {
        assertThat(file).hasOnlyModifiedLines(10, 11, 12, 13, 14, 15, 16, 17, 18, 19);
        assertThat(file.getIndirectCoverageChanges()).isEmpty();
        List.of(10, 11, 12, 13, 14, 15, 16).forEach(line -> {
            assertThat(file.hasModifiedLine(line)).isTrue();
            assertThat(file.hasCoverageForLine(line)).isTrue();
        });
    }

    @Test
    void shouldCreateEmptyModifiedFilesCoverageTreeWithoutChanges() {
        var tree = createTreeWithoutCoverage();

        var filteredTree = tree.filterByModifiedFiles();
        verifyEmptyTree(tree, filteredTree);
    }

    private void verifyEmptyTree(final Node tree, final Node filteredTree) {
        assertThat(filteredTree)
                .isNotSameAs(tree)
                .hasName(tree.getName())
                .hasMetric(tree.getMetric())
                .hasNoChildren()
                .hasNoValues();
    }

    @Test
    void shouldCreateModifiedFilesCoverageTree() {
        var tree = createTreeWithoutCoverage();

        var node = tree.findFile(COVERED_FILE);
        assertThat(node).isPresent();
        var fileNode = node.get();

        registerCoverageWithoutChange(fileNode);
        registerCodeChangesAndCoverage(fileNode);

        var filteredTree = tree.filterByModifiedFiles();
        verifyFilteredTree(tree, filteredTree, this::verifyModifiedFiles);
    }

    private void verifyModifiedFiles(final Node root) {
        assertThat(root.getAll(FILE)).extracting(Node::getName).containsExactly(COVERED_FILE);

        var builder = new CoverageBuilder();
        assertThat(root.getValue(LINE)).isNotEmpty().contains(
                builder.withMetric(LINE).withCovered(8).withMissed(6).build());
        assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                builder.withMetric(BRANCH).withCovered(12).withMissed(12).build());
        assertThat(root.getValue(MUTATION)).isNotEmpty().contains(
                builder.withMetric(MUTATION).withCovered(2).withMissed(4).build());

        assertThat(root.findFile(COVERED_FILE)).isPresent().get().satisfies(file -> {
            verifyCountersOfCoveredClass(file);
            assertThat(file.getCoveredCounters()).containsExactly(1, 0, 1, 0, 0, 4, 2, 1, 0, 1, 0, 0, 4, 2);
            assertThat(file.getMissedCounters()).containsExactly(0, 1, 0, 1, 4, 0, 2, 0, 1, 0, 1, 4, 0, 2);
            assertThat(file.getMissedLines()).containsExactly(11, 13, 14, 21, 23, 24);
            assertThat(file.getPartiallyCoveredLines()).containsExactly(entry(16, 2), entry(26, 2));
            assertThat(file.getMutations()).extracting(Mutation::getLine)
                    .containsExactlyInAnyOrder(17, 18, 19, 27, 28, 29);
        });
    }

    @Test
    void shouldCreateEmptyIndirectCoverageChangesTreeWithoutChanges() {
        var tree = createTreeWithoutCoverage();
        verifyEmptyTree(tree, tree.filterByIndirectChanges());
    }

    @Test
    void shouldCreateIndirectCoverageChangesTree() {
        var tree = createTreeWithoutCoverage();

        var node = tree.findFile(COVERED_FILE);
        assertThat(node).isPresent();
        registerIndirectCoverageChanges(node.get());

        assertThat(tree.filterByIndirectChanges())
                .isNotSameAs(tree)
                .hasName(tree.getName())
                .hasMetric(tree.getMetric())
                .hasFiles("path/to/" + COVERED_FILE)
                .satisfies(this::verifyIndirectChanges);
    }

    private void verifyIndirectChanges(final Node root) {
        assertThat(root.getAll(FILE)).extracting(Node::getName).containsExactly(COVERED_FILE);

        var builder = new CoverageBuilder();
        assertThat(root.getValue(LINE)).isNotEmpty().contains(
                builder.withMetric(LINE).withCovered(2).withMissed(2).build());
        assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                builder.withMetric(BRANCH).withCovered(4).withMissed(4).build());
    }

    private void registerCodeChangesAndCoverage(final FileNode file) {
        file.addModifiedLines(
                10, 11, 12, 13, // line
                14, 15, 16, // branch
                17, 18, 19 // mutation
        );

        var classNode = file.createClassNode(CLASS_WITH_MODIFICATIONS);
        addCounters(file, classNode, 0);

        var builder = new CoverageBuilder();
        classNode.addValue(builder.withMetric(LINE).withCovered(4).withMissed(3).build());
        classNode.addValue(builder.withMetric(BRANCH).withCovered(6).withMissed(6).build());
        classNode.addValue(builder.withMetric(MUTATION).withCovered(2).withMissed(4).build());
    }

    private void addCounters(final FileNode fileNode, final ClassNode classNode, final int offset) {
        fileNode.addCounters(10 + offset, 1, 0);
        fileNode.addCounters(11 + offset, 0, 1);
        fileNode.addCounters(12 + offset, 1, 0);
        fileNode.addCounters(13 + offset, 0, 1);

        fileNode.addCounters(14 + offset, 0, 4);
        fileNode.addCounters(15 + offset, 4, 0);
        fileNode.addCounters(16 + offset, 2, 2);

        var builder = new MutationBuilder().withMutatedClass(classNode.getName()).withMutatedMethod("method");

        fileNode.addMutation(builder.withLine(17 + offset).withStatus(MutationStatus.KILLED).withIsDetected(true).build());
        fileNode.addMutation(builder.withLine(18 + offset).withStatus(MutationStatus.SURVIVED).withIsDetected(false).build());
        fileNode.addMutation(builder.withLine(19 + offset).withStatus(MutationStatus.NO_COVERAGE).withIsDetected(false).build());
    }

    private void registerCoverageWithoutChange(final FileNode file) {
        var classNode = file.createClassNode(CLASS_WITHOUT_MODIFICATION);

        addCounters(file, classNode, 10);

        var builder = new CoverageBuilder();
        classNode.addValue(builder.withMetric(LINE).withCovered(4).withMissed(3).build());
        classNode.addValue(builder.withMetric(BRANCH).withCovered(6).withMissed(6).build());
    }

    private void registerIndirectCoverageChanges(final FileNode file) {
        registerCodeChangesAndCoverage(file);
        registerCoverageWithoutChange(file);

        file.addIndirectCoverageChange(20, 1);
        file.addIndirectCoverageChange(21, -1);
        file.addIndirectCoverageChange(24, -4);
        file.addIndirectCoverageChange(25, 4);
    }

    private Node createTreeWithoutCoverage() {
        var moduleNode = new ModuleNode("edu.hm.hafner.module1");
        var packageNode = new PackageNode("coverage");
        var coveredFileNode = new FileNode(COVERED_FILE, "path/to/" + COVERED_FILE);
        var missedFileNode = new FileNode(MISSED_FILE, "path/to/" + MISSED_FILE);

        moduleNode.addChild(packageNode);

        packageNode.addChild(missedFileNode);
        packageNode.addChild(coveredFileNode);

        coveredFileNode.addChild(new ClassNode(COVERED_CLASS));
        missedFileNode.addChild(new ClassNode(MISSED_CLASS));

        return moduleNode;
    }

    @Test
    void shouldCreateEmptyNodes() {
        var fullyEmpty = new PackageNode("Empty Node");

        assertThat(fullyEmpty).isEmpty();
    }

    @Test
    void shouldCreateNonEmptyNodes() {
        var noChildrenButValues = new PackageNode("No Children");
        noChildrenButValues.addValue(new CoverageBuilder().withMetric(LINE).withCovered(10).withMissed(0).build());
        var noValuesButChildren = new PackageNode("No Values");
        noValuesButChildren.addChild(new FileNode("child", "."));

        assertThat(noChildrenButValues).isNotEmpty();
        assertThat(noValuesButChildren).isNotEmpty();
    }

    @Test
    void shouldMergeSingleNodeInList() {
        var parent = new PackageNode("package");
        var child = new FileNode("file", ".");
        parent.addChild(child);

        var merged = Node.merge(List.of(parent));

        assertThat(merged).isEqualTo(parent);
    }

    @Test
    void shouldMergeEmptyNodes() {
        assertThat(Node.merge(List.of())).isEmpty();
    }

    @Test
    void shouldMergeMultipleNodesWithSameNameInList() {
        var parentA = new PackageNode("package");
        var childA = new FileNode("fileA", ".");
        parentA.addChild(childA);
        var parentB = new PackageNode("package");
        var childB = new FileNode("fileB", ".");
        parentB.addChild(childB);
        var parentC = new PackageNode("package");
        var childC = new FileNode("fileC", ".");
        parentC.addChild(childC);

        var merged = Node.merge(List.of(parentA, parentB, parentC));

        assertThat(merged).hasOnlyChildren(childA, childB, childC);
    }

    @Test
    void shouldMergeMultipleNodesWithDifferentNameInList() {
        var parentA = new PackageNode("packageA");
        var childA = new FileNode("fileA", ".");
        parentA.addChild(childA);
        var parentB = new PackageNode("packageB");
        var childB = new FileNode("fileB", ".");
        parentB.addChild(childB);
        var parentC = new PackageNode("packageC");
        var childC = new FileNode("fileC", ".");
        parentC.addChild(childC);

        var merged = Node.merge(List.of(parentA, parentB, parentC));

        assertThat(merged)
                .hasName("Container")
                .hasMetric(CONTAINER)
                .hasOnlyChildren(parentA, parentB, parentC);
    }

    @Test @Issue("JENKINS-72310")
    void shouldMergeWithDuplicateAndDifferentNames() {
        var parentA = new PackageNode("packageA");
        var childA = new FileNode("fileA", ".");
        parentA.addChild(childA);
        var parentB = new PackageNode("packageA");
        var childB = new FileNode("fileB", ".");
        parentB.addChild(childB);
        var parentC = new PackageNode("packageC");
        var childC = new FileNode("fileC", ".");
        parentC.addChild(childC);
        var parentD = new PackageNode("packageC");
        var childD = new FileNode("fileD", ".");
        parentD.addChild(childD);

        var merged = Node.merge(List.of(parentA, parentB, parentC, parentD));

        assertThat(merged)
                .hasName("Container")
                .hasMetric(CONTAINER);

        assertThat(merged.getChildren()).hasSize(2);
    }

    @Test
    void shouldMergeMultipleNodesWithDifferentMetricInList() {
        var parentA = new ModuleNode("M");
        var parentB = new PackageNode("P");
        var parentC = new FileNode("F", ".");

        var merged = Node.merge(List.of(parentA, parentB, parentC));

        assertThat(merged)
                .hasName("Container")
                .hasMetric(CONTAINER)
                .hasOnlyChildren(parentA, parentB, parentC);
    }

    @Test
    void shouldGetAllNodesOfTypeInTree() {
        var tree = createTreeWithoutCoverage();
        var coveredMethod = new MethodNode("coveredMethod", "signature");
        var missedMethod = new MethodNode("missedMethod", "signature");

        tree.findClass(COVERED_CLASS).orElseThrow().addChild(coveredMethod);
        tree.findClass(MISSED_CLASS).orElseThrow().addChild(missedMethod);

        assertThat(tree.getAllMethodNodes()).containsExactlyInAnyOrder(coveredMethod, missedMethod);
        assertThat(tree.getAllFileNodes()).containsExactlyInAnyOrder(
                tree.findFile("Covered.java").orElseThrow(),
                tree.findFile("Missed.java").orElseThrow());
        assertThat(tree.getAllClassNodes()).containsExactlyInAnyOrder(
                tree.findClass("CoveredClass.class").orElseThrow(),
                tree.findClass("MissedClass.class").orElseThrow());
    }

    @Test
    void shouldComputeDelta() {
        var coverageBuilder = new CoverageBuilder();
        var fileA = new FileNode("FileA.java", ".");
        fileA.addAllValues(Arrays.asList(
                coverageBuilder.withMetric(LINE).withCovered(10).withMissed(0).build(),
                coverageBuilder.withMetric(BRANCH).withCovered(2).withMissed(0).build(),
                coverageBuilder.withMetric(MUTATION).withCovered(2).withMissed(0).build()
        ));
        Node fileB = new FileNode("FileB.java", ".");
        fileB.addAllValues(Arrays.asList(
                coverageBuilder.withMetric(LINE).withCovered(0).withMissed(10).build(),
                coverageBuilder.withMetric(BRANCH).withCovered(1).withMissed(1).build()
        ));

        List<Difference> delta = fileA.computeDelta(fileB);

        assertThat(delta).map(Difference::getMetric)
                .containsExactly(FILE, LINE, BRANCH, LOC).doesNotContain(MUTATION);
        assertThat(delta.get(1).asDouble()).isEqualTo(100);
        assertThat(delta.get(2).asDouble()).isEqualTo(50);
    }

    @Test
    void shouldGetAllValueMetrics() {
        var coverageBuilder = new CoverageBuilder();
        var fileA = new FileNode("FileA.java", ".");
        fileA.addChild(new ClassNode("ClassA.java"));
        fileA.addAllValues(Arrays.asList(
                coverageBuilder.withMetric(LINE).withCovered(10).withMissed(0).build(),
                coverageBuilder.withMetric(BRANCH).withCovered(2).withMissed(0).build(),
                coverageBuilder.withMetric(MUTATION).withCovered(2).withMissed(0).build()
        ));
        assertThat(fileA.getValueMetrics())
                .containsExactlyInAnyOrder(LINE, BRANCH, MUTATION);
    }

    @Test
    void shouldContainMetric() {
        var fileA = new FileNode("FileA.java", ".");
        fileA.addChild(new ClassNode("ClassA.java"));
        fileA.addValue(new CoverageBuilder().withMetric(LINE).withCovered(10).withMissed(0).build());

        assertThat(fileA.containsMetric(CLASS)).isFalse();
        assertThat(fileA.containsMetric(FILE)).isTrue();
        assertThat(fileA.containsMetric(LINE)).isTrue();
        assertThat(fileA.containsMetric(BRANCH)).isFalse();
    }

    @Test
    void shouldGetCoverageValueByMetricWithDefault() {
        var coverageBuilder = new CoverageBuilder();
        var fileACoverage = coverageBuilder.withMetric(LINE).withCovered(10).withMissed(0).build();
        var defaultCoverage = coverageBuilder.withMetric(BRANCH).withCovered(1).withMissed(0).build();
        var fileA = new FileNode("FileA.java", ".");
        fileA.addValue(fileACoverage);

        assertThat(fileA.getTypedValue(LINE, defaultCoverage)).isEqualTo(fileACoverage);
        assertThat(fileA.getTypedValue(BRANCH, defaultCoverage)).isEqualTo(defaultCoverage);
    }

    @Test
    void shouldThrowExceptionWhenTryingToRemoveNodeThatIsNotAChild() {
        var moduleNode = new ModuleNode("module");
        var packageNode = new PackageNode("package");

        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> moduleNode.removeChild(packageNode))
                .withMessageContaining("The node %s is not a child of this node %s".formatted(packageNode, moduleNode));
    }

    @Test
    void shouldReturnCorrectHasModifiedLines() {
        var packageNode = new PackageNode("package");
        var fileNode = new FileNode("file", ".");
        packageNode.addChild(fileNode);

        assertThat(packageNode).doesNotHaveModifiedLines();

        fileNode.addModifiedLines(1);
        assertThat(packageNode).hasModifiedLines();
    }
}
