package edu.hm.hafner.metric;

import java.util.List;
import java.util.NoSuchElementException;

import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.Mutation.MutationBuilder;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link Node}.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
@DefaultLocale("en")
class NodeTest {
    private static final String COVERED_FILE = "Covered.java";
    private static final Percentage HUNDERT_PERCENT = Percentage.valueOf(1, 1);
    private static final String MISSED_FILE = "Missed.java";
    private static final String CLASS_WITH_MODIFICATIONS = "classWithModifications";
    private static final String CLASS_WITHOUT_MODIFICATION = "classWithoutModification";

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

        //boundary-interior demonstration (Path "Don't enter loop" is impossible in this case)
        assertThat(child.getParentName()).isEqualTo("Parent"); // boundary -> Enter only once and cover all branches
        assertThat(subSubPackage.getParentName()).isEqualTo(
                "Child.SubPackage"); // interior -> Enter twice and cover all branches

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
    void shouldReturnCorrectPathInBaseClass() {
        var root = new ModuleNode("Root");
        var child = new FileNode("Child");
        var childOfChild = new ClassNode("ChildOfChild");

        root.addChild(child);
        child.addChild(childOfChild);

        assertThat(root).hasPath("");
        assertThat(root.mergePath("-")).isEmpty();
        assertThat(child.mergePath("/local/path")).isEqualTo("/local/path");
        assertThat(childOfChild.mergePath("")).isEqualTo("Child");
    }

    @Test
    void shouldPrintAllMetricsForNodeAndChildNodes() {
        var parent = new ModuleNode("Parent");
        var child1 = new PackageNode("ChildOne");
        var child2 = new PackageNode("ChildTwo");
        var childOfChildOne = new FileNode("ChildOfChildOne");

        parent.addChild(child1);
        parent.addChild(child2);
        child1.addChild(childOfChildOne);

        assertThat(parent.getMetrics().pollFirst()).isEqualTo(MODULE);
        assertThat(parent.getMetrics()).contains(FILE);
    }

    @Test
    void shouldCalculateDistributedMetrics() {
        var builder = new CoverageBuilder();

        var node = new ModuleNode("Node");

        var valueOne = builder.setMetric(LINE).setCovered(1).setMissed(0).build();
        node.addValue(valueOne);
        var valueTwo = builder.setMetric(BRANCH).setCovered(0).setMissed(1).build();
        node.addValue(valueTwo);

        assertThat(node.aggregateValues()).containsExactly(
                builder.setMetric(MODULE).setCovered(1).setMissed(0).build(),
                valueOne,
                valueTwo,
                new LinesOfCode(1));
    }

    @Test
    void shouldHandleLeaves() {
        Node node = new ModuleNode("Node");

        assertThat(node).hasNoValues();

        var builder = new CoverageBuilder();
        var leafOne = builder.setMetric(LINE).setCovered(1).setMissed(0).build();
        node.addValue(leafOne);
        assertThat(node).hasOnlyValues(leafOne);

        var leafTwo = builder.setMetric(BRANCH).setCovered(0).setMissed(1).build();
        node.addValue(leafTwo);
        assertThat(node).hasOnlyValues(leafOne, leafTwo);

        assertThat(getCoverage(node, LINE)).hasCoveredPercentage(HUNDERT_PERCENT);
        assertThat(getCoverage(node, BRANCH)).hasCoveredPercentage(Percentage.ZERO);

        assertThatIllegalArgumentException().isThrownBy(() -> node.addValue(leafOne));
        assertThatIllegalArgumentException().isThrownBy(() -> node.addValue(leafTwo));
    }

    @Test
    void shouldReturnAllNodesOfSpecificMetricType() {
        Node parent = new ModuleNode("Parent");
        Node child1 = new PackageNode("ChildOne");
        Node child2 = new PackageNode("ChildTwo");
        Node childOfChildOne = new FileNode("ChildOfChildOne");
        Node childOfChildTwo = new FileNode("ChildOfChildTwo");

        parent.addChild(child1);
        parent.addChild(child2);
        child1.addChild(childOfChildOne);
        child2.addChild(childOfChildTwo);

        assertThat(parent.getAll(FILE))
                .hasSize(2)
                .containsOnly(childOfChildOne, childOfChildTwo);

    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    @Test
    void shouldCalculateCorrectCoverageForModule() {
        Node node = new ModuleNode("Node");
        Value valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();

        node.addValue(valueOne);

        assertThat(getCoverage(node, MODULE)).hasCoveredPercentage(HUNDERT_PERCENT);
    }

    @Test
    void shouldCalculateCorrectCoverageWithNestedStructure() {
        var node = new ModuleNode("Node");
        var missedFile = new FileNode("fileMissed");
        var coveredFile = new FileNode("fileCovered");
        var valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();
        var valueTwo = new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build();

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
        var childNode = new FileNode("childNode");
        var valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();
        var valueTwo = new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build();

        node.addValue(valueOne);
        node.addChild(childNode);
        childNode.addValue(valueTwo);
        Node copiedNode = node.copyTree();

        assertThat(node).isNotSameAs(copiedNode);
        assertThat(node.getChildren().get(0)).isNotSameAs(copiedNode.getChildren().get(0));
    }

    @Test
    void shouldDeepCopyNodeTreeWithSpecifiedNodeAsParent() {
        var node = new ModuleNode("Node");
        var childNode = new FileNode("childNode");
        var valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();
        var valueTwo = new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build();
        var newParent = new ModuleNode("parent");

        node.addValue(valueOne);
        node.addChild(childNode);
        childNode.addValue(valueTwo);
        Node copiedNode = node.copyTree(newParent);

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
        assertThat(node.matches(MODULE, node.getPath().hashCode())).isTrue();
    }

    @Test
    void shouldFindNodeByNameOrHashCode() {
        var node = new ModuleNode("Node");
        var childNode = new FileNode("childNode");
        node.addChild(childNode);

        assertThat(node.find(BRANCH, "NotExisting")).isNotPresent();
        assertThat(node.find(FILE, childNode.getName())).isPresent().get().isEqualTo(childNode);

        assertThat(node.findByHashCode(BRANCH, "NotExisting".hashCode())).isNotPresent();
        assertThat(node.findByHashCode(FILE, childNode.getName().hashCode())).isPresent().get().isEqualTo(childNode);
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
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node sameModule = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node samePackage = new PackageNode("coverage");

        Node fileToKeep = new FileNode("KeepMe");
        Node otherFileToKeep = new FileNode("KeepMeToo");

        pkg.addChild(fileToKeep);
        module.addChild(pkg);
        samePackage.addChild(otherFileToKeep);
        sameModule.addChild(samePackage);
        Node combinedReport = module.merge(sameModule);

        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep, otherFileToKeep);

    }

    @Test
    void shouldKeepChildNodesAfterCombiningMoreComplexReportWithDifferencesOnClassLevel() {
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node sameModule = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node samePackage = new PackageNode("coverage");
        Node fileToKeep = new FileNode("KeepMe");
        Node sameFileToKeep = new FileNode("KeepMe");
        Node classA = new ClassNode("ClassA");
        Node classB = new ClassNode("ClassB");

        module.addChild(pkg);
        pkg.addChild(fileToKeep);
        fileToKeep.addChild(classA);

        sameModule.addChild(samePackage);
        samePackage.addChild(sameFileToKeep);
        sameFileToKeep.addChild(classA);

        Node combinedReport = module.merge(sameModule);
        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep);
        assertThat(combinedReport.getAll(CLASS)).hasSize(1);

        sameFileToKeep.addChild(classB);
        Node combinedReport2Classes = module.merge(sameModule);
        assertThat(combinedReport2Classes.getAll(CLASS)).hasSize(2);
        assertThat(combinedReport2Classes.getChildren().get(0).getChildren().get(0)).hasOnlyChildren(classA, classB);
    }

    private static Node setUpNodeTree() {
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node file = new FileNode("Node.java");
        Node covNodeClass = new ClassNode("Node.class");
        Node combineWithMethod = new MethodNode("combineWith", "(Ljava/util/Map;)V", 10);

        module.addChild(pkg);
        pkg.addChild(file);
        file.addChild(covNodeClass);
        covNodeClass.addChild(combineWithMethod);

        return module;
    }

    @Test
    void shouldComputeCorrectCoverageAfterCombiningMethods() {
        Node module = new ModuleNode("edu.hm.hafner.module");
        Node pkg = new PackageNode("edu.hm.hafner.package");
        Node file = new FileNode("Node.java");
        Node covNodeClass = new ClassNode("Node.class");
        Node combineWithMethod = new MethodNode("combineWith", "(Ljava/util/Map;)V", 10);

        module.addChild(pkg);
        pkg.addChild(file);
        file.addChild(covNodeClass);
        covNodeClass.addChild(combineWithMethod);
        combineWithMethod.addValue(new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build());

        Node otherNode = module.copyTree();
        Node addMethod = new MethodNode("add", "(Ljava/util/Map;)V", 1);
        otherNode.getAll(CLASS).get(0).addChild(addMethod); // the same class node in the copied tree
        addMethod.addValue(new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build());

        Node combinedReport = module.merge(otherNode);
        assertThat(combinedReport.getAll(METHOD)).hasSize(2);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(1).hasMissed(1);
    }

    @Test
    void shouldTakeMaxCoverageIfTwoLineCoverageValuesForSameMethodExist() {
        Node module = setUpNodeTree();
        Node sameProject = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        method.addValue(new CoverageBuilder().setMetric(LINE).setCovered(2).setMissed(8).build());
        methodOtherCov.addValue(new CoverageBuilder().setMetric(LINE).setCovered(5).setMissed(5).build());

        Node combinedReport = module.merge(sameProject);
        assertThat(combinedReport.getAll(METHOD)).hasSize(1);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(5).hasMissed(5);
    }

    @Test
    void shouldThrowErrorIfCoveredPlusMissedLinesDifferInReports() {
        Node module = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);

        Node sameProject = setUpNodeTree();
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        assertThat(module.merge(sameProject)).isEqualTo(
                module); // should not throw error if no line coverage exists for method

        method.addValue(new CoverageBuilder().setMetric(LINE).setCovered(5).setMissed(5).build());
        methodOtherCov.addValue(new CoverageBuilder().setMetric(LINE).setCovered(2).setMissed(7).build());
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> module.merge(sameProject))
                .withMessageContaining("Cannot compute maximum of coverages", "(5/10)", "(2/9)");
    }

    @Test
    void shouldTakeMaxCoverageIfDifferentCoverageValuesOfDifferentMetricsExistForSameMethod() {
        Node module = setUpNodeTree();
        Node sameProject = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        method.addValue(new CoverageBuilder().setMetric(LINE).setCovered(2).setMissed(8).build());
        methodOtherCov.addValue(new CoverageBuilder().setMetric(LINE).setCovered(5).setMissed(5).build());
        method.addValue(new CoverageBuilder().setMetric(BRANCH).setCovered(10).setMissed(5).build());
        methodOtherCov.addValue(new CoverageBuilder().setMetric(BRANCH).setCovered(12).setMissed(3).build());
        method.addValue(new CoverageBuilder().setMetric(INSTRUCTION).setCovered(7).setMissed(8).build());
        methodOtherCov.addValue(new CoverageBuilder().setMetric(INSTRUCTION).setCovered(5).setMissed(10).build());

        Node combinedReport = module.merge(sameProject);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(5).hasMissed(5);
        assertThat(getCoverage(combinedReport, BRANCH)).hasCovered(12).hasMissed(3);
        assertThat(getCoverage(combinedReport, INSTRUCTION)).hasCovered(7).hasMissed(8);
    }

    @Test
    void shouldCorrectlyCombineTwoComplexReports() {
        var report = setUpNodeTree();
        var otherReport = setUpNodeTree();

        // Difference on package level
        var autograding = new PackageNode("autograding");
        var file = new FileNode("Main.java");
        var mainClass = new ClassNode("Main.class");
        var mainMethod = new MethodNode("main", "(Ljava/util/Map;)V", 10);

        otherReport.addChild(autograding);
        autograding.addChild(file);
        file.addChild(mainClass);
        mainClass.addChild(mainMethod);
        mainMethod.addValue(new CoverageBuilder().setMetric(LINE).setCovered(8).setMissed(2).build());

        // Difference on file level
        var leaf = new FileNode("Leaf");
        var pkgCovFile = new FileNode("HelloWorld");
        leaf.addChild(mainClass.copyTree());

        report.getAll(PACKAGE).get(0).addChild(pkgCovFile);
        otherReport.getAll(PACKAGE).get(0).addChild(leaf);

        var combinedReport = report.merge(otherReport);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);
        assertThat(combinedReport.getAll(FILE)).hasSize(4);
        assertThat(combinedReport.getAll(CLASS)).hasSize(3);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(16).hasMissed(4);
        assertThat(combinedReport.getValue(BRANCH)).isEmpty();
    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithInRelatedProjects() {
        var project = new ModuleNode("edu.hm.hafner.module1");
        var sameProject = project.copyTree();
        var coveragePkg = new PackageNode("coverage");
        var autogradingPkg = new PackageNode("autograding");

        project.addChild(coveragePkg);
        sameProject.addChild(autogradingPkg);
        Node combinedReport = project.merge(sameProject);

        assertThat(combinedReport.find(coveragePkg.getMetric(), coveragePkg.getName()).get())
                .isNotSameAs(coveragePkg);
        assertThat(combinedReport.find(autogradingPkg.getMetric(), autogradingPkg.getName()).get())
                .isNotSameAs(autogradingPkg);
    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelThanMethod() {
        Node report = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node file = new FileNode("Node.java");

        report.addChild(pkg);
        pkg.addChild(file);
        Node otherReport = report.copyTree();

        otherReport.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(90).setMissed(10).build());
        report.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(80).setMissed(20).build());

        Node combined = report.merge(otherReport);
        assertThat(getCoverage(combined, LINE)).hasMissed(10).hasCovered(90);
    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelAndOtherReportHasHigherCoverage() {
        Node report = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node file = new FileNode("Node.java");

        report.addChild(pkg);
        pkg.addChild(file);
        Node otherReport = report.copyTree();
        otherReport.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(70).setMissed(30).build());
        report.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(80).setMissed(20).build());

        Node combined = report.merge(otherReport);
        assertThat(getCoverage(combined, LINE)).hasMissed(20).hasCovered(80);
    }

    @Test
    void shouldCreateEmptyModifiedLinesCoverageTreeWithoutChanges() {
        Node tree = createTreeWithoutCoverage();

        verifyEmptyTree(tree, tree.filterByModifiedLines());
    }

    @Test
    void shouldCreateModifiedLinesCoverageTree() {
        Node tree = createTreeWithoutCoverage();

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
                .hasPath(tree.getPath())
                .hasMetric(tree.getMetric())
                .hasOnlyFiles("coverage/" + COVERED_FILE)
                .hasModifiedLines()
                .satisfies(treeVerification);
    }

    private void verifyModifiedLines(final Node root) {
        assertThat(root.getAll(FILE)).extracting(Node::getName).containsExactly(COVERED_FILE);

        var builder = new CoverageBuilder();
        assertThat(root.getValue(LINE)).isNotEmpty().contains(
                builder.setMetric(LINE).setCovered(4).setMissed(3).build());
        assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                builder.setMetric(BRANCH).setCovered(6).setMissed(6).build());
        assertThat(root.getValue(MUTATION)).isNotEmpty().contains(
                builder.setMetric(MUTATION).setCovered(1).setMissed(2).build());

        assertThat(root.findFile(COVERED_FILE)).isPresent().get().satisfies(file -> {
            verifyCountersOfCoveredClass(file);
            assertThat(file.getCoveredCounters()).containsExactly(1, 0, 1, 0, 0, 4, 2);
            assertThat(file.getMissedCounters()).containsExactly(0, 1, 0, 1, 4, 0, 2);
            assertThat(file.getMissedLines()).containsExactly(11, 13);
            assertThat(file.getPartiallyCoveredLines()).containsExactly(entry(14, 4), entry(16, 2));
            assertThat(file.getMutations()).hasSize(3)
                    .extracting(Mutation::getLine).containsExactlyInAnyOrder(17, 18, 19);
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
        Node tree = createTreeWithoutCoverage();

        var filteredTree = tree.filterByModifiedFiles();
        verifyEmptyTree(tree, filteredTree);
    }

    private void verifyEmptyTree(final Node tree, final Node filteredTree) {
        assertThat(filteredTree)
                .isNotSameAs(tree)
                .hasName(tree.getName())
                .hasPath(tree.getPath())
                .hasMetric(tree.getMetric())
                .hasNoChildren()
                .hasNoValues();
    }

    @Test
    void shouldCreateModifiedFilesCoverageTree() {
        Node tree = createTreeWithoutCoverage();

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
                builder.setMetric(LINE).setCovered(8).setMissed(6).build());
        assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                builder.setMetric(BRANCH).setCovered(12).setMissed(12).build());
        assertThat(root.getValue(MUTATION)).isNotEmpty().contains(
                builder.setMetric(MUTATION).setCovered(2).setMissed(4).build());

        assertThat(root.findFile(COVERED_FILE)).isPresent().get().satisfies(file -> {
            verifyCountersOfCoveredClass(file);
            assertThat(file.getCoveredCounters()).containsExactly(1, 0, 1, 0, 0, 4, 2, 1, 0, 1, 0, 0, 4, 2);
            assertThat(file.getMissedCounters()).containsExactly(0, 1, 0, 1, 4, 0, 2, 0, 1, 0, 1, 4, 0, 2);
            assertThat(file.getMissedLines()).containsExactly(11, 13, 21, 23);
            assertThat(file.getPartiallyCoveredLines()).containsExactly(entry(14, 4), entry(16, 2), entry(24, 4), entry(26, 2));
            assertThat(file.getMutations()).hasSize(6)
                    .extracting(Mutation::getLine).containsExactlyInAnyOrder(17, 18, 19, 27, 28, 29);
        });
    }

    @Test
    void shouldCreateEmptyIndirectCoverageChangesTreeWithoutChanges() {
        Node tree = createTreeWithoutCoverage();
        verifyEmptyTree(tree, tree.filterByIndirectChanges());
    }

    @Test
    void shouldCreateIndirectCoverageChangesTree() {
        Node tree = createTreeWithoutCoverage();

        var node = tree.findFile(COVERED_FILE);
        assertThat(node).isPresent();
        registerIndirectCoverageChanges(node.get());

        assertThat(tree.filterByIndirectChanges())
                .isNotSameAs(tree)
                .hasName(tree.getName())
                .hasPath(tree.getPath())
                .hasMetric(tree.getMetric())
                .hasFiles("coverage/" + COVERED_FILE)
                .satisfies(this::verifyIndirectChanges);
    }

    private void verifyIndirectChanges(final Node root) {
        assertThat(root.getAll(FILE)).extracting(Node::getName).containsExactly(COVERED_FILE);

        var builder = new CoverageBuilder();
        assertThat(root.getValue(LINE)).isNotEmpty().contains(
                builder.setMetric(LINE).setCovered(2).setMissed(2).build());
        assertThat(root.getValue(BRANCH)).isNotEmpty().contains(
                builder.setMetric(BRANCH).setCovered(4).setMissed(4).build());
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
        classNode.addValue(builder.setMetric(LINE).setCovered(4).setMissed(3).build());
        classNode.addValue(builder.setMetric(BRANCH).setCovered(6).setMissed(6).build());
        classNode.addValue(builder.setMetric(MUTATION).setCovered(2).setMissed(4).build());
    }

    private void addCounters(final FileNode fileNode, final ClassNode classNode, final int offset) {
        fileNode.addCounters(10 + offset, 1, 0);
        fileNode.addCounters(11 + offset, 0, 1);
        fileNode.addCounters(12 + offset, 1, 0);
        fileNode.addCounters(13 + offset, 0, 1);

        fileNode.addCounters(14 + offset, 0, 4);
        fileNode.addCounters(15 + offset, 4, 0);
        fileNode.addCounters(16 + offset, 2, 2);

        MutationBuilder builder = new MutationBuilder().setMutatedClass(classNode.getName()).setMutatedMethod("method");

        fileNode.addMutation(builder.setLine(17 + offset).setStatus(MutationStatus.KILLED).setIsDetected(true).build());
        fileNode.addMutation(builder.setLine(18 + offset).setStatus(MutationStatus.SURVIVED).setIsDetected(false).build());
        fileNode.addMutation(builder.setLine(19 + offset).setStatus(MutationStatus.NO_COVERAGE).setIsDetected(false).build());
    }

    private void registerCoverageWithoutChange(final FileNode file) {
        var classNode = file.createClassNode(CLASS_WITHOUT_MODIFICATION);

        addCounters(file, classNode, 10);

        var builder = new CoverageBuilder();
        classNode.addValue(builder.setMetric(LINE).setCovered(4).setMissed(3).build());
        classNode.addValue(builder.setMetric(BRANCH).setCovered(6).setMissed(6).build());
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
        Node moduleNode = new ModuleNode("edu.hm.hafner.module1");
        Node packageNode = new PackageNode("coverage");
        Node coveredFileNode = new FileNode(COVERED_FILE);
        Node missedFileNode = new FileNode(MISSED_FILE);

        moduleNode.addChild(packageNode);

        packageNode.addChild(missedFileNode);
        packageNode.addChild(coveredFileNode);

        coveredFileNode.addChild(new ClassNode("CoveredClass.class"));
        missedFileNode.addChild(new ClassNode("MissedClass.class"));

        return moduleNode;
    }
}
