package edu.hm.hafner.model;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;
import static edu.hm.hafner.model.Metric.CLASS;
import static edu.hm.hafner.model.Metric.FILE;
import static edu.hm.hafner.model.Metric.*;

/**
 * Tests the class {@link Node}.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
class NodeTest {

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.add(new Node(FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        Node packageNode = new Node(PACKAGE, "");
        root.add(packageNode);
        assertThat(root.getAll(PACKAGE)).hasSize(1);

        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        assertThat(root).hasOnlyChildren(packageNode);
    }

    @Test
    void shouldNotSplitPackagesIfNotExecutedOnModuleNode() {
        Node packageNode = new Node(PACKAGE, "edu.hm.hafer");

        packageNode.splitPackages();
        assertThat(packageNode.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.add(new Node(PACKAGE, "."));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldNotBreakEquals() {
        // Given
        Node root = new Node(MODULE, "Root");
        Node child = new Node(PACKAGE, ".");
        root.add(child);

        // When
        Node actualCopy = root.copyTree();

        // Then
        assertThat(actualCopy).isEqualTo(root);
    }

    @Test
    void shouldSplitPackagesIntoHierarchy() {
        Node root = new Node(MODULE, "Root");
        assertThat(root.getAll(PACKAGE)).isEmpty();
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).isEmpty();

        root.add(new Node(PACKAGE, "edu.hm.hafner"));
        assertThat(root.getAll(PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(PACKAGE)).hasSize(3).satisfiesExactly(
                s -> assertThat(s).hasName("hafner"),
                s -> assertThat(s).hasName("hm"),
                s -> assertThat(s).hasName("edu")
        );
    }

    @Test
    void shouldDetectExistingPackagesOnSplit() {
        Node root = new Node(MODULE, "Root");
        Node eduPackage = new Node(PACKAGE, "edu");
        Node differentPackage = new Node(PACKAGE, "org");

        root.add(differentPackage);
        root.add(eduPackage);

        assertThat(root.getAll(PACKAGE)).hasSize(2);

        root.add(new Node(PACKAGE, "edu.hm.hafner"));
        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).hasSize(4);
    }

    @Test
    void shouldNotRecreateExistingChildOnSplitPackages() {
        Node root = new Node(MODULE, "Root");
        Node eduPackage = new Node(PACKAGE, "edu");
        Node unsplittedPackage = new Node(PACKAGE, "edu.hm.hafner");

        root.add(eduPackage);
        root.add(unsplittedPackage);

        assertThat(root.getAll(PACKAGE)).hasSize(2);
        root.splitPackages();

        assertThat(root).hasOnlyChildren(eduPackage);
    }

    @Test
    void shouldKeepNodesAfterSplitting() {
        Node root = new Node(MODULE, "Root");
        Node pkg = new Node(PACKAGE, "edu.hm.hafner");
        Node file = new Node(FILE, "HelloWorld.java");

        root.add(pkg);
        pkg.add(file);
        root.splitPackages();

        assertThat(root.getAll(PACKAGE)).hasSize(3);
        assertThat(root.getAll(FILE)).hasSize(1);

    }

    @Test
    void shouldHandleNonExistingParent() {
        Node root = new Node(MODULE, "Root");

        assertThat(root).doesNotHaveParent();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent)
                .withMessage("Parent is not set");
        assertThat(root).hasParentName(Node.ROOT);
    }

    @Test
    void shouldReturnParentOfNodeAndItsName() {
        Node parent = new Node(MODULE, "Parent");
        Node child = new Node(PACKAGE, "Child");
        Node subPackage = new Node(PACKAGE, "SubPackage");
        Node subSubPackage = new Node(PACKAGE, "SubSubPackage");

        parent.add(child);
        child.add(subPackage);
        subPackage.add(subSubPackage);

        assertThat(child.getParent()).isEqualTo(parent);

        //boundary-interior demonstration (Path "Don't enter loop" is impossible in this case)
        assertThat(child.getParentName()).isEqualTo("Parent"); // boundary -> Enter only once and cover all branches
        assertThat(subSubPackage.getParentName()).isEqualTo("Child.SubPackage"); // interior -> Enter twice and cover all branches

    }

    @Test
    void shouldReturnCorrectChildNodes() {
        Node parent = new Node(MODULE, "Parent");
        Node child1 = new Node(PACKAGE, "ChildOne");
        Node child2 = new Node(PACKAGE, "ChildTwo");

        assertThat(parent).hasNoChildren();

        parent.add(child1);
        assertThat(parent).hasOnlyChildren(child1);
        assertThat(parent).doesNotHaveChildren(child2);

        parent.add(child2);
        assertThat(parent).hasOnlyChildren(child1, child2);
    }

    @Test
    void shouldReturnCorrectPathInBaseClass() {
        Node root = new Node(MODULE, "Root");
        FileNode child = new FileNode("Child");
        Node childOfChild = new Node(LINE, "ChildOfChild");

        root.add(child);
        child.add(childOfChild);

        assertThat(root).hasPath("");
        assertThat(root.mergePath("-")).isEmpty();
        assertThat(child.mergePath("/local/path")).isEqualTo("/local/path");
        assertThat(childOfChild.mergePath("")).isEqualTo("Child");

    }

    @Test
    void shouldPrintAllMetricsForNodeAndChildNodes() {
        Node parent = new Node(MODULE, "Parent");
        Node child1 = new Node(PACKAGE, "ChildOne");
        Node child2 = new Node(PACKAGE, "ChildTwo");
        Node childOfChildOne = new Node(FILE, "ChildOfChildOne");

        parent.add(child1);
        parent.add(child2);
        child1.add(childOfChildOne);

        assertThat(parent.getMetrics().pollFirst()).isEqualTo(MODULE);
        assertThat(parent.getMetrics()).contains(FILE);
    }

    @Test
    void shouldCalculateDistributedMetrics() {
        Node node = new Node(MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(leafTwo);

        assertThat(node.getMetricsDistribution()).hasSize(2);
        assertThat(node.getMetricsDistribution().get(LINE)).isEqualTo(new Coverage(1, 1));

        assertThat(node.getMetricsPercentages()).hasSize(2);
        assertThat(node.getMetricsPercentages().get(LINE)).isEqualTo(Fraction.ONE_HALF);

    }

    @Test
    void shouldCalculateDeltaBetweenNodes() {
        Node node = new Node(MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(leafTwo);
        Node deltaNode = node.copyTree();
        node.getLeaves().add(new CoverageLeaf(LINE, new Coverage(1, 0)));

        assertThat(node.computeDelta(deltaNode).get(LINE)).isEqualTo(Fraction.getFraction(1, 6));

    }

    @Test
    void shouldHandlePotentialOverflowOnDeltaComputation() {
        Node nodeOne = new Node(MODULE, "Node");
        Node nodeTwo = new Node(MODULE, "NodeTwo");
        Coverage coverageOne = new Coverage(Integer.MAX_VALUE, Integer.MIN_VALUE);
        Coverage coverageTwo = new Coverage(Integer.MAX_VALUE, Integer.MAX_VALUE);
        CoverageLeaf leafOne = new CoverageLeaf(LINE, coverageOne);
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, coverageTwo);

        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> coverageOne.getCoveredPercentage().subtract(coverageTwo.getCoveredPercentage()));

        nodeOne.add(leafOne);
        nodeTwo.add(leafTwo);

        assertThat(nodeOne.computeDelta(nodeTwo).get(LINE)).isNotNull(); // works if no error is thrown.
    }

    @Test
    void shouldHandleLeaves() {
        Node node = new Node(MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        assertThat(node).hasNoLeaves();

        node.add(leafOne);
        assertThat(node).hasOnlyLeaves(leafOne);

        node.add(leafTwo);
        assertThat(node).hasOnlyLeaves(leafOne, leafTwo);

    }

    @Test
    void shouldReturnAllNodesOfSpecificMetricType() {
        Node parent = new Node(MODULE, "Parent");
        Node child1 = new Node(PACKAGE, "ChildOne");
        Node child2 = new Node(PACKAGE, "ChildTwo");
        Node childOfChildOne = new Node(FILE, "ChildOfChildOne");
        Node childOfChildTwo = new Node(FILE, "ChildOfChildTwo");

        parent.add(child1);
        parent.add(child2);
        child1.add(childOfChildOne);
        child2.add(childOfChildTwo);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> parent.getAll(INSTRUCTION));
        assertThat(parent.getAll(FILE))
                .hasSize(2)
                .containsOnly(childOfChildOne, childOfChildTwo);

    }

    @Test
    void shouldCalculateCorrectCoverageWithLeafOnlyStructure() {
        Node node = new Node(MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        node.add(leafOne);
        assertThat(node.getCoverage(LINE)).hasCoveredPercentage(Fraction.ONE);

        node.add(leafTwo);
        assertThat(node.getCoverage(LINE)).hasCoveredPercentage(Fraction.ONE_HALF);

    }

    @Test
    void shouldCalculateCorrectCoverageForModule() {
        Node node = new Node(MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));

        node.add(leafOne);

        assertThat(node.getCoverage(MODULE)).hasCoveredPercentage(Fraction.ONE);
    }

    @Test
    void shouldCalculateCorrectCoverageWithNestedStructure() {
        Node node = new Node(MODULE, "Node");
        Node missedFile = new Node(FILE, "fileMissed");
        Node coveredFile = new Node(FILE, "fileCovered");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        node.add(missedFile);
        node.add(coveredFile);
        coveredFile.add(leafOne);
        missedFile.add(leafTwo);

        assertThat(node.getCoverage(LINE)).hasCoveredPercentage(Fraction.ONE_HALF);
        assertThat(node.getCoverage(FILE)).hasCoveredPercentage(Fraction.ONE_HALF);

    }

    @Test
    void shouldPrintCoverageWithDefaultOrSpecifiedLocale() {
        Node node = new Node(MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(leafTwo);

        assertThat(node.printCoverageFor(LINE)).isEqualTo("50.00%");
        assertThat(node.printCoverageFor(LINE, Locale.GERMAN)).isEqualTo("50,00%");
    }

    @Test
    void shouldDeepCopyNodeTree() {
        Node node = new Node(MODULE, "Node");
        Node childNode = new Node(FILE, "childNode");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(childNode);
        childNode.add(leafTwo);
        Node copiedNode = node.copyTree();

        assertThat(node).isNotSameAs(copiedNode);
        assertThat(node.getChildren().get(0)).isNotSameAs(copiedNode.getChildren().get(0));
    }

    @Test
    void shouldDeepCopyNodeTreeWithSpecifiedNodeAsParent() {
        Node node = new Node(MODULE, "Node");
        Node childNode = new Node(FILE, "childNode");
        CoverageLeaf leafOne = new CoverageLeaf(LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(LINE, new Coverage(0, 1));
        Node newParent = new Node(MODULE, "parent");

        node.add(leafOne);
        node.add(childNode);
        childNode.add(leafTwo);
        Node copiedNode = node.copyTree(newParent);

        assertThat(copiedNode).hasParent(newParent);
    }

    @Test
    void shouldDetectMatchingOfMetricTypeAndNameOrHashCode() {
        Node node = new Node(MODULE, "Node");

        assertThat(node.matches(MODULE, "WrongName")).isFalse();
        assertThat(node.matches(PACKAGE, "Node")).isFalse();
        assertThat(node.matches(node.getMetric(), node.getName())).isTrue();

        assertThat(node.matches(MODULE, node.getName().hashCode())).isTrue();
        assertThat(node.matches(MODULE, "WrongName".hashCode())).isFalse();
        assertThat(node.matches(MODULE, node.getPath().hashCode())).isTrue();
    }

    @Test
    void shouldFindNodeByNameOrHashCode() {
        Node node = new Node(MODULE, "Node");
        Node childNode = new Node(FILE, "childNode");
        node.add(childNode);

        assertThat(node.find(BRANCH, "NotExisting")).isNotPresent();
        assertThat(node.find(FILE, childNode.getName())).isPresent().get().isEqualTo(childNode);

        assertThat(node.findByHashCode(BRANCH, "NotExisting".hashCode())).isNotPresent();
        assertThat(node.findByHashCode(FILE, childNode.getName().hashCode())).isPresent().get().isEqualTo(childNode);
    }

    @Test
    void shouldPrintObjectHumanReadableInToString() {
        Node node = new Node(MODULE, "Node");

        assertThat(node).hasToString(String.format("[%s] Node", MODULE));
    }

    @Test
    void shouldAdhereToEquals() {
        Node parentForTestOne = new Node(PACKAGE, "edu");
        Node parentForTestTwo = new Node(PACKAGE, "hm");

        EqualsVerifier.simple().forClass(Node.class)
                .withIgnoredFields("parent")
                .withPrefabValues(Node.class, parentForTestOne, parentForTestTwo)
                .verify();
    }

    // TDD PART
    @Test
    void shouldCombineTwoSimpleCoverageReports() {
        Node moduleOne = new Node(MODULE, "edu.hm.hafner.module1");
        Node moduleTwo = new Node(MODULE, "edu.hm.hafner.module2");

        Node combinedWithItself = moduleOne.combineWith(moduleOne);
        Node combinedWithModuleTwo = moduleOne.combineWith(moduleTwo);

        assertThat(combinedWithItself.getAll(MODULE))
                .as("Combined report should still have only one module")
                .hasSize(1);
        assertThat(combinedWithModuleTwo.getAll(MODULE))
                .as("Combined report should consist of two modules")
                .hasSize(2);
        assertThat(combinedWithModuleTwo)
                .as("Combined report root should be of MetricType Group")
                .hasMetric(GROUP);
    }

    @Test
    void shouldThrowExceptionIfNodesAreNotOfTypeModule() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "edu.hm.hafner.pkg");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .as("Should not accept non-module node")
                .isThrownBy(() -> module.combineWith(pkg));
        assertThatExceptionOfType(IllegalStateException.class)
                .as("Should combine if executed on non-module node")
                .isThrownBy(() -> pkg.combineWith(module));
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingDifferentPackages() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameModule = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkgOne = new Node(PACKAGE, "coverage");
        Node pkgTwo = new Node(PACKAGE, "autograding");

        module.add(pkgOne);
        sameModule.add(pkgTwo);
        Node combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);

    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSamePackage() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameModule = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node samePackage = new Node(PACKAGE, "coverage");

        module.add(pkg);
        sameModule.add(samePackage);
        Node combinedReport = module.combineWith(sameModule);
        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSameAndDifferentPackages() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameModule = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node pkgTwo = new Node(PACKAGE, "autograding");

        module.add(pkg);
        sameModule.add(pkgTwo);
        sameModule.add(pkg.copyEmpty());
        Node combinedReport = module.combineWith(sameModule);

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
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameModule = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node samePackage = new Node(PACKAGE, "coverage");

        Node fileToKeep = new Node(FILE, "KeepMe");
        Node otherFileToKeep = new Node(FILE, "KeepMeToo");

        pkg.add(fileToKeep);
        module.add(pkg);
        samePackage.add(otherFileToKeep);
        sameModule.add(samePackage);
        Node combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep, otherFileToKeep);

    }

    @Test
    void shouldKeepChildNodesAfterCombiningMoreComplexReportWithDifferencesOnClassLevel() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameModule = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node samePackage = new Node(PACKAGE, "coverage");
        Node fileToKeep = new Node(FILE, "KeepMe");
        Node sameFileToKeep = new Node(FILE, "KeepMe");
        Node classA = new Node(CLASS, "ClassA");
        Node classB = new Node(CLASS, "ClassB");

        module.add(pkg);
        pkg.add(fileToKeep);
        fileToKeep.add(classA);

        sameModule.add(samePackage);
        samePackage.add(sameFileToKeep);
        sameFileToKeep.add(classA);

        Node combinedReport = module.combineWith(sameModule);
        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep);
        assertThat(combinedReport.getAll(CLASS)).hasSize(1);

        sameFileToKeep.add(classB);
        Node combinedReport2Classes = module.combineWith(sameModule);
        assertThat(combinedReport2Classes.getAll(CLASS)).hasSize(2);
        assertThat(combinedReport2Classes.getChildren().get(0).getChildren().get(0)).hasOnlyChildren(classA, classB);
    }

    private static Node setUpNodeTree() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node file = new Node(FILE, "Node.java");
        Node covNodeClass = new Node(CLASS, "Node.class");
        Node combineWithMethod = new MethodNode("combineWith", 10);

        module.add(pkg);
        pkg.add(file);
        file.add(covNodeClass);
        covNodeClass.add(combineWithMethod);

        return module;
    }

    @Test
    void shouldComputeCorrectCoverageAfterCombiningMethods() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node file = new Node(FILE, "Node.java");
        Node covNodeClass = new Node(CLASS, "Node.class");
        Node combineWithMethod = new MethodNode("combineWith", 10);
        Node addMethod = new MethodNode("add", 1);

        module.add(pkg);
        pkg.add(file);
        file.add(covNodeClass);
        combineWithMethod.add(new CoverageLeaf(LINE, new Coverage(1, 0)));
        addMethod.add(new CoverageLeaf(LINE, new Coverage(0, 1)));

        Node otherNode = module.copyTree();
        covNodeClass.add(combineWithMethod);
        otherNode.getAll(CLASS).get(0).add(addMethod);

        Node combinedReport = module.combineWith(otherNode);
        assertThat(combinedReport.getAll(METHOD)).hasSize(2);
        assertThat(combinedReport.getCoverage(LINE)).hasCovered(1).hasMissed(1);

    }

    @Test
    void shouldTakeMaxCoverageIfTwoLineCoverageValuesForSameMethodExist() {
        Node module = setUpNodeTree();
        Node sameProject = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        method.add(new CoverageLeaf(LINE, new Coverage(2, 8)));
        methodOtherCov.add(new CoverageLeaf(LINE, new Coverage(5, 5)));

        Node combinedReport = module.combineWith(sameProject);
        assertThat(combinedReport.getAll(METHOD)).hasSize(1);
        assertThat(combinedReport.getCoverage(LINE)).hasCovered(5).hasMissed(5);

    }

    @Test
    void shouldThrowErrorIfCoveredPlusMissedLinesDifferInReports() {
        Node module = setUpNodeTree();
        Node sameProject = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        module.combineWith(sameProject); // should not throw error if no line coverage exists for method

        method.add(new CoverageLeaf(LINE, new Coverage(5, 5)));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> module.combineWith(sameProject))
                .withMessageContaining("mismatch of leaves");

        methodOtherCov.add(new CoverageLeaf(LINE, new Coverage(2, 7)));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> module.combineWith(sameProject))
                .withMessageContaining("mismatch")
                .withMessageContaining(method.getName());

    }

    @Test
    void shouldTakeMaxCoverageIfDifferentCoverageValuesOfDifferentMetricsExistForSameMethod() {
        Node module = setUpNodeTree();
        Node sameProject = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        method.add(new CoverageLeaf(LINE, new Coverage(2, 8)));
        methodOtherCov.add(new CoverageLeaf(LINE, new Coverage(5, 5)));
        method.add(new CoverageLeaf(BRANCH, new Coverage(10, 5)));
        methodOtherCov.add(new CoverageLeaf(BRANCH, new Coverage(12, 3)));
        method.add(new CoverageLeaf(INSTRUCTION, new Coverage(7, 8)));
        methodOtherCov.add(new CoverageLeaf(INSTRUCTION, new Coverage(5, 5)));
        methodOtherCov.add(new CoverageLeaf(INSTRUCTION, new Coverage(3, 2)));

        Node combinedReport = module.combineWith(sameProject);
        assertThat(combinedReport.getCoverage(LINE)).hasCovered(5).hasMissed(5);
        assertThat(combinedReport.getCoverage(BRANCH)).hasCovered(12).hasMissed(3);
        assertThat(combinedReport.getCoverage(INSTRUCTION)).hasCovered(8).hasMissed(7);

    }

    @Test
    void shouldCorrectlyCombineTwoComplexReports() {
        Node report = setUpNodeTree();
        Node otherReport = setUpNodeTree();

        // Difference on Package Level
        PackageNode autograding = new PackageNode("autograding");
        FileNode file = new FileNode("Main.java");
        Node mainClass = new Node(CLASS, "Main.class");
        MethodNode mainMethod = new MethodNode("main", 10);

        otherReport.add(autograding);
        autograding.add(file);
        file.add(mainClass);
        mainClass.add(mainMethod);
        mainMethod.add(new CoverageLeaf(LINE, new Coverage(8, 2)));

        // Difference on File Level
        FileNode covLeavefile = new FileNode("CoverageLeaf");
        FileNode pkgCovFile = new FileNode("HelloWorld");
        covLeavefile.add(mainClass.copyTree());

        report.getAll(PACKAGE).get(0).add(pkgCovFile);
        otherReport.getAll(PACKAGE).get(0).add(covLeavefile);

        Node combinedReport = report.combineWith(otherReport);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);
        assertThat(combinedReport.getAll(FILE)).hasSize(4);
        assertThat(combinedReport.getAll(CLASS)).hasSize(3);
        assertThat(combinedReport.getCoverage(LINE)).hasCovered(16).hasMissed(4);
        assertThat(combinedReport.getCoverage(BRANCH)).isNotSet();

    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithIfProjectsAreUnrelated() {
        Node module = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameModule = new Node(MODULE, "edu.hm.hafner.module2");

        Node combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport.find(MODULE, module.getName()).get()).isNotSameAs(module);
        assertThat(combinedReport.find(MODULE, sameModule.getName()).get()).isNotSameAs(sameModule);
    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithInRelatedProjects() {
        Node project = new Node(MODULE, "edu.hm.hafner.module1");
        Node sameProject = project.copyTree();
        PackageNode coveragePkg = new PackageNode("coverage");
        PackageNode autogradingPkg = new PackageNode("autograding");

        project.add(coveragePkg);
        sameProject.add(autogradingPkg);
        Node combinedReport = project.combineWith(sameProject);

        assertThat(combinedReport.find(coveragePkg.getMetric(), coveragePkg.getName()).get())
                .isNotSameAs(coveragePkg);
        assertThat(combinedReport.find(autogradingPkg.getMetric(), autogradingPkg.getName()).get())
                .isNotSameAs(autogradingPkg);

    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelThanMethod() {
        Node report = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node file = new Node(FILE, "Node.java");
        Node otherReport;

        report.add(pkg);
        pkg.add(file);
        otherReport = report.copyTree();

        otherReport.find(FILE, file.getName()).get().add(new CoverageLeaf(LINE, new Coverage(90, 10)));
        report.find(FILE, file.getName()).get().add(new CoverageLeaf(LINE, new Coverage(80, 20)));

        assertThat(report.combineWith(otherReport).getCoverage(LINE)).hasMissed(10).hasCovered(90);
    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelAndOtherReportHasHigherCoverage() {
        Node report = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node file = new Node(FILE, "Node.java");

        report.add(pkg);
        pkg.add(file);
        Node otherReport = report.copyTree();
        otherReport.find(FILE, file.getName()).get().add(new CoverageLeaf(LINE, new Coverage(70, 30)));
        report.find(FILE, file.getName()).get().add(new CoverageLeaf(LINE, new Coverage(80, 20)));

        assertThat(report.combineWith(otherReport).getCoverage(LINE)).hasMissed(20).hasCovered(80);
    }

    /**
     * If one report stops e.g. at file level and other report goes down to class level,
     * results of the report with higher depth should be used.
     */
    @Test
    void shouldHandleReportsOfDifferentDepth() {
        Node report = new Node(MODULE, "edu.hm.hafner.module1");
        Node pkg = new Node(PACKAGE, "coverage");
        Node file = new Node(FILE, "Node.java");
        Node covNodeClass = new Node(CLASS, "Node.class");
        Node otherReport;

        report.add(pkg);
        pkg.add(file);

        otherReport = report.copyTree();
        otherReport.find(file.getMetric(), file.getName()).get().add(covNodeClass);
        covNodeClass.add(new CoverageLeaf(LINE, new Coverage(90, 10)));

        report.find(FILE, file.getName()).get().add(new CoverageLeaf(LINE, new Coverage(80, 20)));

        assertThat(report.combineWith(otherReport).getCoverage(LINE)).hasMissed(10).hasCovered(90);
        assertThat(otherReport.combineWith(report).getCoverage(LINE)).hasMissed(10).hasCovered(90);
        assertThat(report.combineWith(otherReport).find(covNodeClass.getMetric(), covNodeClass.getName()).get())
                .isNotSameAs(covNodeClass);

    }

}
