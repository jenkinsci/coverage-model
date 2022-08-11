package edu.hm.hafner.coverage;

import java.util.Locale;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link CoverageNode}.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
class CoverageNodeTest {

    @BeforeAll
    static void beforeAll() {
        Locale.setDefault(Locale.ENGLISH);
    }

    @Test
    void shouldSplitPackagesWithoutPackageNodes() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.FILE, "file.c"));
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
    }

    @Test
    void shouldSplitPackagesWithoutName() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        CoverageNode packageNode = new CoverageNode(CoverageMetric.PACKAGE, "");
        root.add(packageNode);
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);

        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
        assertThat(root).hasOnlyChildren(packageNode);
    }

    @Test
    void shouldNotSplitPackagesIfNotExecutedOnModuleNode() {
        CoverageNode packageNode = new CoverageNode(CoverageMetric.PACKAGE, "edu.hm.hanfer");

        packageNode.splitPackages();
        assertThat(packageNode.getAll(CoverageMetric.PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesWithSingleDot() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, "."));
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
    }

    @Test
    void shouldSplitPackagesIntoHierarchy() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(0);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, "edu.hm.hafner"));
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(1);
        root.splitPackages();
        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(3).satisfiesExactly(
                s -> assertThat(s).hasName("hafner"),
                s -> assertThat(s).hasName("hm"),
                s -> assertThat(s).hasName("edu")
        );
    }

    @Test
    void shouldDetectExistingPackagesOnSplit() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode eduPackage = new CoverageNode(CoverageMetric.PACKAGE, "edu");
        CoverageNode differentPackage = new CoverageNode(CoverageMetric.PACKAGE, "org");

        root.add(differentPackage);
        root.add(eduPackage);

        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(2);

        root.add(new CoverageNode(CoverageMetric.PACKAGE, "edu.hm.hafner"));
        root.splitPackages();

        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(4);
    }

    @Test
    void shouldNotRecreateExistingChildOnSplitPackages() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode eduPackage = new CoverageNode(CoverageMetric.PACKAGE, "edu");
        CoverageNode unsplittedPackage = new CoverageNode(CoverageMetric.PACKAGE, "edu.hm.hafner");

        root.add(eduPackage);
        root.add(unsplittedPackage);

        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(2);
        root.splitPackages();

        assertThat(root).hasOnlyChildren(eduPackage);
    }

    @Test
    void shouldKeepNodesAfterSplitting() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "edu.hm.hafner");
        CoverageNode file = new CoverageNode(CoverageMetric.FILE, "HelloWorld.java");

        root.add(pkg);
        pkg.add(file);
        root.splitPackages();

        assertThat(root.getAll(CoverageMetric.PACKAGE)).hasSize(3);
        assertThat(root.getAll(CoverageMetric.FILE)).hasSize(1);

    }

    @Test
    void shouldHandleNonExistingParent() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");

        assertThat(root).doesNotHaveParent();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent)
                .withMessage("Parent is not set");
        assertThat(root).hasParentName(CoverageNode.ROOT);
    }

    @Test
    void shouldReturnParentOfNodeAndItsName() {
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, "Parent");
        CoverageNode child = new CoverageNode(CoverageMetric.PACKAGE, "Child");
        CoverageNode subPackage = new CoverageNode(CoverageMetric.PACKAGE, "SubPackage");
        CoverageNode subSubPackage = new CoverageNode(CoverageMetric.PACKAGE, "SubSubPackage");

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
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, "Parent");
        CoverageNode child1 = new CoverageNode(CoverageMetric.PACKAGE, "ChildOne");
        CoverageNode child2 = new CoverageNode(CoverageMetric.PACKAGE, "ChildTwo");

        assertThat(parent).hasNoChildren();

        parent.add(child1);
        assertThat(parent).hasOnlyChildren(child1);
        assertThat(parent).doesNotHaveChildren(child2);

        parent.add(child2);
        assertThat(parent).hasOnlyChildren(child1, child2);
    }

    @Test
    void shouldReturnCorrectPathInBaseClass() {
        CoverageNode root = new CoverageNode(CoverageMetric.MODULE, "Root");
        FileCoverageNode child = new FileCoverageNode("Child");
        CoverageNode childOfChild = new CoverageNode(CoverageMetric.LINE, "ChildOfChild");

        root.add(child);
        child.add(childOfChild);

        assertThat(root).hasPath("");
        assertThat(root.mergePath("-")).isEqualTo("");
        assertThat(child.mergePath("/local/path")).isEqualTo("/local/path");
        assertThat(childOfChild.mergePath("")).isEqualTo("Child");

    }

    @Test
    void shouldPrintAllMetricsForNodeAndChildNodes() {
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, "Parent");
        CoverageNode child1 = new CoverageNode(CoverageMetric.PACKAGE, "ChildOne");
        CoverageNode child2 = new CoverageNode(CoverageMetric.PACKAGE, "ChildTwo");
        CoverageNode childOfChildOne = new CoverageNode(CoverageMetric.FILE, "ChildOfChildOne");

        parent.add(child1);
        parent.add(child2);
        child1.add(childOfChildOne);

        assertThat(parent.getMetrics().pollFirst()).isEqualTo(CoverageMetric.MODULE);
        assertThat(parent.getMetrics()).contains(CoverageMetric.FILE);
    }

    @Test
    void shouldCalculateDistributedMetrics() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(leafTwo);

        assertThat(node.getMetricsDistribution()).hasSize(2);
        assertThat(node.getMetricsDistribution().get(CoverageMetric.LINE)).isEqualTo(new Coverage(1, 1));

        assertThat(node.getMetricsPercentages()).hasSize(2);
        assertThat(node.getMetricsPercentages().get(CoverageMetric.LINE)).isEqualTo(Fraction.ONE_HALF);

    }

    @Test
    void shouldCalculateDeltaBetweenCoverageNodes() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(leafTwo);
        CoverageNode deltaNode = node.copyTree();
        node.getLeaves().add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0)));

        assertThat(node.computeDelta(deltaNode).get(CoverageMetric.LINE)).isEqualTo(Fraction.getFraction(1, 6));

    }

    @Test
    void shouldHandlePotentialOverflowOnDeltaComputation() {
        CoverageNode nodeOne = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageNode nodeTwo = new CoverageNode(CoverageMetric.MODULE, "NodeTwo");
        Coverage coverageOne = new Coverage(Integer.MAX_VALUE, Integer.MIN_VALUE);
        Coverage coverageTwo = new Coverage(Integer.MAX_VALUE, Integer.MAX_VALUE);
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, coverageOne);
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, coverageTwo);

        assertThatExceptionOfType(ArithmeticException.class).isThrownBy(() -> coverageOne.getCoveredPercentage().subtract(coverageTwo.getCoveredPercentage()));

        nodeOne.add(leafOne);
        nodeTwo.add(leafTwo);

        assertThat(nodeOne.computeDelta(nodeTwo).get(CoverageMetric.LINE)).isNotNull(); // works if no error is thrown.
    }

    @Test
    void shouldHandleLeaves() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        assertThat(node).hasNoLeaves();

        node.add(leafOne);
        assertThat(node).hasOnlyLeaves(leafOne);

        node.add(leafTwo);
        assertThat(node).hasOnlyLeaves(leafOne, leafTwo);

    }

    @Test
    void shouldReturnAllNodesOfSpecificMetricType() {
        CoverageNode parent = new CoverageNode(CoverageMetric.MODULE, "Parent");
        CoverageNode child1 = new CoverageNode(CoverageMetric.PACKAGE, "ChildOne");
        CoverageNode child2 = new CoverageNode(CoverageMetric.PACKAGE, "ChildTwo");
        CoverageNode childOfChildOne = new CoverageNode(CoverageMetric.FILE, "ChildOfChildOne");
        CoverageNode childOfChildTwo = new CoverageNode(CoverageMetric.FILE, "ChildOfChildTwo");

        parent.add(child1);
        parent.add(child2);
        child1.add(childOfChildOne);
        child2.add(childOfChildTwo);

        assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> parent.getAll(CoverageMetric.INSTRUCTION));
        assertThat(parent.getAll(CoverageMetric.FILE))
                .hasSize(2)
                .containsOnly(childOfChildOne, childOfChildTwo);

    }

    @Test
    void shouldCalculateCorrectCoverageWithLeafOnlyStructure() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        node.add(leafOne);
        assertThat(node.getCoverage(CoverageMetric.LINE)).hasCoveredPercentage(Fraction.ONE);

        node.add(leafTwo);
        assertThat(node.getCoverage(CoverageMetric.LINE)).hasCoveredPercentage(Fraction.ONE_HALF);

    }

    @Test
    void shouldCalculateCorrectCoverageForModule() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));

        node.add(leafOne);

        assertThat(node.getCoverage(CoverageMetric.MODULE)).hasCoveredPercentage(Fraction.ONE);
    }

    @Test
    void shouldCalculateCorrectCoverageWithNestedStructure() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageNode missedFile = new CoverageNode(CoverageMetric.FILE, "fileMissed");
        CoverageNode coveredFile = new CoverageNode(CoverageMetric.FILE, "fileCovered");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        node.add(missedFile);
        node.add(coveredFile);
        coveredFile.add(leafOne);
        missedFile.add(leafTwo);

        assertThat(node.getCoverage(CoverageMetric.LINE)).hasCoveredPercentage(Fraction.ONE_HALF);
        assertThat(node.getCoverage(CoverageMetric.FILE)).hasCoveredPercentage(Fraction.ONE_HALF);

    }

    @Test
    void shouldPrintCoverageWithDefaultOrSpecifiedLocale() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(leafTwo);

        assertThat(node.printCoverageFor(CoverageMetric.LINE)).isEqualTo("50.00%");
        assertThat(node.printCoverageFor(CoverageMetric.LINE, Locale.GERMAN)).isEqualTo("50,00%");
    }

    @Test
    void shouldDeepCopyCoverageNodeTree() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageNode childNode = new CoverageNode(CoverageMetric.FILE, "childNode");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));

        node.add(leafOne);
        node.add(childNode);
        childNode.add(leafTwo);
        CoverageNode copiedNode = node.copyTree();

        assertThat(node).isNotSameAs(copiedNode);
        assertThat(node.getChildren().get(0)).isNotSameAs(copiedNode.getChildren().get(0));
    }

    @Test
    void shouldDeepCopyCoverageNodeTreeWithSpecifiedNodeAsParent() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageNode childNode = new CoverageNode(CoverageMetric.FILE, "childNode");
        CoverageLeaf leafOne = new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0));
        CoverageLeaf leafTwo = new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1));
        CoverageNode newParent = new CoverageNode(CoverageMetric.MODULE, "parent");

        node.add(leafOne);
        node.add(childNode);
        childNode.add(leafTwo);
        CoverageNode copiedNode = node.copyTree(newParent);

        assertThat(copiedNode).hasParent(newParent);
    }

    @Test
    void shouldDetectMatchingOfMetricTypeAndNameOrHashCode() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");

        assertThat(node.matches(CoverageMetric.MODULE, "WrongName")).isFalse();
        assertThat(node.matches(CoverageMetric.PACKAGE, "Node")).isFalse();
        assertThat(node.matches(node.getMetric(), node.getName())).isTrue();

        assertThat(node.matches(CoverageMetric.MODULE, node.getName().hashCode())).isTrue();
        assertThat(node.matches(CoverageMetric.MODULE, "WrongName".hashCode())).isFalse();
        assertThat(node.matches(CoverageMetric.MODULE, node.getPath().hashCode())).isTrue();
    }

    @Test
    void shouldFindNodeByNameOrHashCode() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");
        CoverageNode childNode = new CoverageNode(CoverageMetric.FILE, "childNode");
        node.add(childNode);

        assertThat(node.find(CoverageMetric.BRANCH, "NotExisting")).isNotPresent();
        assertThat(node.find(CoverageMetric.FILE, childNode.getName())).isPresent().get().isEqualTo(childNode);

        assertThat(node.findByHashCode(CoverageMetric.BRANCH, "NotExisting".hashCode())).isNotPresent();
        assertThat(node.findByHashCode(CoverageMetric.FILE, childNode.getName().hashCode())).isPresent().get().isEqualTo(childNode);
    }

    @Test
    void shouldPrintObjectHumanReadableInToString() {
        CoverageNode node = new CoverageNode(CoverageMetric.MODULE, "Node");

        assertThat(node).hasToString(String.format("[%s] Node", CoverageMetric.MODULE));
    }

    @Test
    void shouldAdhereToEquals() {
        CoverageNode parentForTestOne = new CoverageNode(CoverageMetric.PACKAGE, "edu");
        CoverageNode parentForTestTwo = new CoverageNode(CoverageMetric.PACKAGE, "hm");

        EqualsVerifier.simple().forClass(CoverageNode.class)
                .withIgnoredFields("parent")
                .withPrefabValues(CoverageNode.class, parentForTestOne, parentForTestTwo)
                .verify();
    }

    // TDD PART
    @Test
    void shouldCombineTwoSimpleCoverageReports() {
        CoverageNode moduleOne = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode moduleTwo = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module2");

        CoverageNode combinedWithItself = moduleOne.combineWith(moduleOne);
        CoverageNode combinedWithModuleTwo = moduleOne.combineWith(moduleTwo);

        assertThat(combinedWithItself.getAll(CoverageMetric.MODULE))
                .as("Combined report should still have only one module")
                .hasSize(1);
        assertThat(combinedWithModuleTwo.getAll(CoverageMetric.MODULE))
                .as("Combined report should consist of two modules")
                .hasSize(2);
        assertThat(combinedWithModuleTwo)
                .as("Combined report root should be of MetricType Group")
                .hasMetric(CoverageMetric.GROUP);
    }

    @Test
    void shouldThrowExceptionIfNodesAreNotOfTypeModule() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "edu.hm.hafner.pkg");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .as("Should not accept non-module node")
                .isThrownBy(() -> module.combineWith(pkg));
        assertThatExceptionOfType(IllegalStateException.class)
                .as("Should combine if executed on non-module node")
                .isThrownBy(() -> pkg.combineWith(module));
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingDifferentPackages() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameModule = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkgOne = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode pkgTwo = new CoverageNode(CoverageMetric.PACKAGE, "autograding");

        module.add(pkgOne);
        sameModule.add(pkgTwo);
        CoverageNode combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport).hasMetric(CoverageMetric.MODULE);
        assertThat(combinedReport.getAll(CoverageMetric.MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(CoverageMetric.PACKAGE)).hasSize(2);

    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSamePackage() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameModule = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode samePackage = new CoverageNode(CoverageMetric.PACKAGE, "coverage");

        module.add(pkg);
        sameModule.add(samePackage);
        CoverageNode combinedReport = module.combineWith(sameModule);
        assertThat(combinedReport).hasMetric(CoverageMetric.MODULE);
        assertThat(combinedReport.getAll(CoverageMetric.MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(CoverageMetric.PACKAGE)).hasSize(1);
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSameAndDifferentPackages() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameModule = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode pkgTwo = new CoverageNode(CoverageMetric.PACKAGE, "autograding");

        module.add(pkg);
        sameModule.add(pkgTwo);
        sameModule.add(pkg.copyEmpty());
        CoverageNode combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport).hasMetric(CoverageMetric.MODULE);
        assertThat(combinedReport.getAll(CoverageMetric.MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(CoverageMetric.PACKAGE)).hasSize(2);
        assertThat(combinedReport.getAll(CoverageMetric.PACKAGE)).satisfiesExactlyInAnyOrder(
                p -> assertThat(p.getName()).isEqualTo(pkg.getName()),
                p -> assertThat(p.getName()).isEqualTo(pkgTwo.getName())
        );
    }

    @Test
    void shouldKeepChildNodesAfterCombiningReportWithSamePackage() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameModule = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode samePackage = new CoverageNode(CoverageMetric.PACKAGE, "coverage");

        CoverageNode fileToKeep = new CoverageNode(CoverageMetric.FILE, "KeepMe");
        CoverageNode otherFileToKeep = new CoverageNode(CoverageMetric.FILE, "KeepMeToo");

        pkg.add(fileToKeep);
        module.add(pkg);
        samePackage.add(otherFileToKeep);
        sameModule.add(samePackage);
        CoverageNode combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep, otherFileToKeep);

    }

    @Test
    void shouldKeepChildNodesAfterCombiningMoreComplexReportWithDifferencesOnClassLevel() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameModule = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode samePackage = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode fileToKeep = new CoverageNode(CoverageMetric.FILE, "KeepMe");
        CoverageNode sameFileToKeep = new CoverageNode(CoverageMetric.FILE, "KeepMe");
        CoverageNode classA = new CoverageNode(CoverageMetric.CLASS, "ClassA");
        CoverageNode classB = new CoverageNode(CoverageMetric.CLASS, "ClassB");

        module.add(pkg);
        pkg.add(fileToKeep);
        fileToKeep.add(classA);

        sameModule.add(samePackage);
        samePackage.add(sameFileToKeep);
        sameFileToKeep.add(classA);

        CoverageNode combinedReport = module.combineWith(sameModule);
        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep);
        assertThat(combinedReport.getAll(CoverageMetric.CLASS)).hasSize(1);

        sameFileToKeep.add(classB);
        CoverageNode combinedReport2Classes = module.combineWith(sameModule);
        assertThat(combinedReport2Classes.getAll(CoverageMetric.CLASS)).hasSize(2);
        assertThat(combinedReport2Classes.getChildren().get(0).getChildren().get(0)).hasOnlyChildren(classA, classB);
    }

    private static CoverageNode setUpCoverageNodeTree() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode file = new CoverageNode(CoverageMetric.FILE, "CoverageNode.java");
        CoverageNode covNodeClass = new CoverageNode(CoverageMetric.CLASS, "CoverageNode.class");
        CoverageNode combineWithMethod = new MethodCoverageNode("combineWith", 10);

        module.add(pkg);
        pkg.add(file);
        file.add(covNodeClass);
        covNodeClass.add(combineWithMethod);

        return module;
    }

    @Test
    void shouldComputeCorrectCoverageAfterCombiningMethods() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode file = new CoverageNode(CoverageMetric.FILE, "CoverageNode.java");
        CoverageNode covNodeClass = new CoverageNode(CoverageMetric.CLASS, "CoverageNode.class");
        CoverageNode combineWithMethod = new MethodCoverageNode("combineWith", 10);
        CoverageNode addMethod = new MethodCoverageNode("add", 1);

        module.add(pkg);
        pkg.add(file);
        file.add(covNodeClass);
        combineWithMethod.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(1, 0)));
        addMethod.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(0, 1)));

        CoverageNode otherNode = module.copyTree();
        covNodeClass.add(combineWithMethod);
        otherNode.getAll(CoverageMetric.CLASS).get(0).add(addMethod);

        CoverageNode combinedReport = module.combineWith(otherNode);
        assertThat(combinedReport.getAll(CoverageMetric.METHOD)).hasSize(2);
        assertThat(combinedReport.getCoverage(CoverageMetric.LINE)).hasCovered(1).hasMissed(1);

    }

    @Test
    void shouldTakeMaxCoverageIfTwoLineCoverageValuesForSameMethodExist() {
        CoverageNode module = setUpCoverageNodeTree();
        CoverageNode sameProject = setUpCoverageNodeTree();
        CoverageNode method = module.getAll(CoverageMetric.METHOD).get(0);
        CoverageNode methodOtherCov = sameProject.getAll(CoverageMetric.METHOD).get(0);

        method.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(2, 8)));
        methodOtherCov.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(5, 5)));

        CoverageNode combinedReport = module.combineWith(sameProject);
        assertThat(combinedReport.getAll(CoverageMetric.METHOD)).hasSize(1);
        assertThat(combinedReport.getCoverage(CoverageMetric.LINE)).hasCovered(5).hasMissed(5);

    }

    @Test
    void shouldThrowErrorIfCoveredPlusMissedLinesDifferInReports() {
        CoverageNode module = setUpCoverageNodeTree();
        CoverageNode sameProject = setUpCoverageNodeTree();
        CoverageNode method = module.getAll(CoverageMetric.METHOD).get(0);
        CoverageNode methodOtherCov = sameProject.getAll(CoverageMetric.METHOD).get(0);

        module.combineWith(sameProject); // should not throw error if no line coverage exists for method

        method.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(5, 5)));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> module.combineWith(sameProject))
                .withMessageContaining("mismatch of leaves");

        methodOtherCov.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(2, 7)));
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> module.combineWith(sameProject))
                .withMessageContaining("mismatch")
                .withMessageContaining(method.getName());

    }

    @Test
    void shouldTakeMaxCoverageIfDifferentCoverageValuesOfDifferentMetricsExistForSameMethod() {
        CoverageNode module = setUpCoverageNodeTree();
        CoverageNode sameProject = setUpCoverageNodeTree();
        CoverageNode method = module.getAll(CoverageMetric.METHOD).get(0);
        CoverageNode methodOtherCov = sameProject.getAll(CoverageMetric.METHOD).get(0);

        method.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(2, 8)));
        methodOtherCov.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(5, 5)));
        method.add(new CoverageLeaf(CoverageMetric.BRANCH, new Coverage(10, 5)));
        methodOtherCov.add(new CoverageLeaf(CoverageMetric.BRANCH, new Coverage(12, 3)));
        method.add(new CoverageLeaf(CoverageMetric.INSTRUCTION, new Coverage(7, 8)));
        methodOtherCov.add(new CoverageLeaf(CoverageMetric.INSTRUCTION, new Coverage(5, 5)));
        methodOtherCov.add(new CoverageLeaf(CoverageMetric.INSTRUCTION, new Coverage(3, 2)));

        CoverageNode combinedReport = module.combineWith(sameProject);
        assertThat(combinedReport.getCoverage(CoverageMetric.LINE)).hasCovered(5).hasMissed(5);
        assertThat(combinedReport.getCoverage(CoverageMetric.BRANCH)).hasCovered(12).hasMissed(3);
        assertThat(combinedReport.getCoverage(CoverageMetric.INSTRUCTION)).hasCovered(8).hasMissed(7);

    }

    @Test
    void shouldCorrectlyCombineTwoComplexReports() {
        CoverageNode report = setUpCoverageNodeTree();
        CoverageNode otherReport = setUpCoverageNodeTree();

        // Difference on Package Level
        PackageCoverageNode autograding = new PackageCoverageNode("autograding");
        FileCoverageNode file = new FileCoverageNode("Main.java");
        CoverageNode mainClass = new CoverageNode(CoverageMetric.CLASS, "Main.class");
        MethodCoverageNode mainMethod = new MethodCoverageNode("main", 10);

        otherReport.add(autograding);
        autograding.add(file);
        file.add(mainClass);
        mainClass.add(mainMethod);
        mainMethod.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(8, 2)));

        // Difference on File Level
        FileCoverageNode covLeavefile = new FileCoverageNode("CoverageLeaf");
        FileCoverageNode pkgCovFile = new FileCoverageNode("HelloWorld");
        covLeavefile.add(mainClass.copyTree());

        report.getAll(CoverageMetric.PACKAGE).get(0).add(pkgCovFile);
        otherReport.getAll(CoverageMetric.PACKAGE).get(0).add(covLeavefile);

        CoverageNode combinedReport = report.combineWith(otherReport);
        assertThat(combinedReport.getAll(CoverageMetric.PACKAGE)).hasSize(2);
        assertThat(combinedReport.getAll(CoverageMetric.FILE)).hasSize(4);
        assertThat(combinedReport.getAll(CoverageMetric.CLASS)).hasSize(3);
        assertThat(combinedReport.getCoverage(CoverageMetric.LINE)).hasCovered(16).hasMissed(4);
        assertThat(combinedReport.getCoverage(CoverageMetric.BRANCH)).isNotSet();

    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithIfProjectsAreUnrelated() {
        CoverageNode module = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameModule = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module2");

        CoverageNode combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport.find(CoverageMetric.MODULE, module.getName()).get()).isNotSameAs(module);
        assertThat(combinedReport.find(CoverageMetric.MODULE, sameModule.getName()).get()).isNotSameAs(sameModule);
    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithInRelatedProjects() {
        CoverageNode project = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode sameProject = project.copyTree();
        PackageCoverageNode coveragePkg = new PackageCoverageNode("coverage");
        PackageCoverageNode autogradingPkg = new PackageCoverageNode("autograding");

        project.add(coveragePkg);
        sameProject.add(autogradingPkg);
        CoverageNode combinedReport = project.combineWith(sameProject);

        assertThat(combinedReport.find(coveragePkg.getMetric(), coveragePkg.getName()).get())
                .isNotSameAs(coveragePkg);
        assertThat(combinedReport.find(autogradingPkg.getMetric(), autogradingPkg.getName()).get())
                .isNotSameAs(autogradingPkg);

    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelThanMethod() {
        CoverageNode report = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode file = new CoverageNode(CoverageMetric.FILE, "CoverageNode.java");
        CoverageNode otherReport;

        report.add(pkg);
        pkg.add(file);
        otherReport = report.copyTree();

        otherReport.find(CoverageMetric.FILE, file.getName()).get().add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(90, 10)));
        report.find(CoverageMetric.FILE, file.getName()).get().add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(80, 20)));

        assertThat(report.combineWith(otherReport).getCoverage(CoverageMetric.LINE)).hasMissed(10).hasCovered(90);
    }

    @Test
    void shouldAlsoHandleReportsThatStopAtHigherLevelAndOtherReportHasHigherCoverage() {
        CoverageNode report = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode file = new CoverageNode(CoverageMetric.FILE, "CoverageNode.java");

        report.add(pkg);
        pkg.add(file);
        CoverageNode otherReport = report.copyTree();
        otherReport.find(CoverageMetric.FILE, file.getName()).get().add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(70, 30)));
        report.find(CoverageMetric.FILE, file.getName()).get().add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(80, 20)));

        assertThat(report.combineWith(otherReport).getCoverage(CoverageMetric.LINE)).hasMissed(20).hasCovered(80);
    }

    /**
     * If one report stops e.g. at file level and other report goes down to class level,
     * results of the report with higher depth should be used.
     */
    @Test
    void shouldHandleReportsOfDifferentDepth() {
        CoverageNode report = new CoverageNode(CoverageMetric.MODULE, "edu.hm.hafner.module1");
        CoverageNode pkg = new CoverageNode(CoverageMetric.PACKAGE, "coverage");
        CoverageNode file = new CoverageNode(CoverageMetric.FILE, "CoverageNode.java");
        CoverageNode covNodeClass = new CoverageNode(CoverageMetric.CLASS, "CoverageNode.class");
        CoverageNode otherReport;

        report.add(pkg);
        pkg.add(file);

        otherReport = report.copyTree();
        otherReport.find(file.getMetric(), file.getName()).get().add(covNodeClass);
        covNodeClass.add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(90, 10)));

        report.find(CoverageMetric.FILE, file.getName()).get().add(new CoverageLeaf(CoverageMetric.LINE, new Coverage(80, 20)));

        assertThat(report.combineWith(otherReport).getCoverage(CoverageMetric.LINE)).hasMissed(10).hasCovered(90);
        assertThat(otherReport.combineWith(report).getCoverage(CoverageMetric.LINE)).hasMissed(10).hasCovered(90);
        assertThat(report.combineWith(otherReport).find(covNodeClass.getMetric(), covNodeClass.getName()).get())
                .isNotSameAs(covNodeClass);

    }

}
