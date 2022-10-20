package edu.hm.hafner.metric;

import java.util.NoSuchElementException;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

import static edu.hm.hafner.metric.Metric.CLASS;
import static edu.hm.hafner.metric.Metric.FILE;
import static edu.hm.hafner.metric.Metric.*;
import static edu.hm.hafner.metric.assertions.Assertions.assertThat;
import static edu.hm.hafner.metric.assertions.Assertions.assertThatExceptionOfType;
import static edu.hm.hafner.metric.assertions.Assertions.entry;
import static org.assertj.core.api.Assertions.*;

/**
 * Tests the class {@link Node}.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("PMD.GodClass")
@DefaultLocale("en")
class NodeTest {
    @Test
    void shouldHandleNonExistingParent() {
        ModuleNode root = new ModuleNode("Root");

        assertThat(root).doesNotHaveParent();
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(root::getParent)
                .withMessage("Parent is not set");
        assertThat(root).hasParentName(Node.ROOT);
    }

    @Test
    void shouldReturnParentOfNodeAndItsName() {
        ModuleNode parent = new ModuleNode("Parent");
        Node child = new PackageNode("Child");
        Node subPackage = new PackageNode("SubPackage");
        Node subSubPackage = new PackageNode("SubSubPackage");

        parent.addChild(child);
        child.addChild(subPackage);
        subPackage.addChild(subSubPackage);

        assertThat(child.getParent()).isEqualTo(parent);

        //boundary-interior demonstration (Path "Don't enter loop" is impossible in this case)
        assertThat(child.getParentName()).isEqualTo("Parent"); // boundary -> Enter only once and cover all branches
        assertThat(subSubPackage.getParentName()).isEqualTo("Child.SubPackage"); // interior -> Enter twice and cover all branches

    }

    @Test
    void shouldReturnCorrectChildNodes() {
        ModuleNode parent = new ModuleNode("Parent");
        Node child1 = new PackageNode("ChildOne");
        Node child2 = new PackageNode("ChildTwo");

        assertThat(parent).hasNoChildren();

        parent.addChild(child1);
        assertThat(parent).hasOnlyChildren(child1);
        assertThat(parent).doesNotHaveChildren(child2);

        parent.addChild(child2);
        assertThat(parent).hasOnlyChildren(child1, child2);
    }

    @Test
    void shouldReturnCorrectPathInBaseClass() {
        ModuleNode root = new ModuleNode("Root");
        FileNode child = new FileNode("Child");
        ClassNode childOfChild = new ClassNode("ChildOfChild");

        root.addChild(child);
        child.addChild(childOfChild);

        assertThat(root).hasPath("");
        assertThat(root.mergePath("-")).isEmpty();
        assertThat(child.mergePath("/local/path")).isEqualTo("/local/path");
        assertThat(childOfChild.mergePath("")).isEqualTo("Child");
    }

    @Test
    void shouldPrintAllMetricsForNodeAndChildNodes() {
        Node parent = new ModuleNode("Parent");
        Node child1 = new PackageNode("ChildOne");
        Node child2 = new PackageNode("ChildTwo");
        Node childOfChildOne = new FileNode("ChildOfChildOne");

        parent.addChild(child1);
        parent.addChild(child2);
        child1.addChild(childOfChildOne);

        assertThat(parent.getMetrics().pollFirst()).isEqualTo(MODULE);
        assertThat(parent.getMetrics()).contains(FILE);
    }

    @Test
    void shouldCalculateDistributedMetrics() {
        var builder = new CoverageBuilder();

        Node node = new ModuleNode("Node");

        Value valueOne = builder.setMetric(LINE).setCovered(1).setMissed(0).build();
        node.addValue(valueOne);
        Value valueTwo = builder.setMetric(BRANCH).setCovered(0).setMissed(1).build();
        node.addValue(valueTwo);

        assertThat(node.getMetricsDistribution()).containsExactly(
                entry(MODULE, builder.setMetric(MODULE).setCovered(1).setMissed(0).build()),
                entry(LINE, valueOne),
                entry(BRANCH, valueTwo),
                entry(LOC, new LinesOfCode(1)));
    }

    @Test
    void shouldHandleLeaves() {
        Node node = new ModuleNode("Node");

        assertThat(node).hasNoValues();

        var builder = new CoverageBuilder();
        Coverage leafOne = builder.setMetric(LINE).setCovered(1).setMissed(0).build();
        node.addValue(leafOne);
        assertThat(node).hasOnlyValues(leafOne);

        Coverage leafTwo = builder.setMetric(BRANCH).setCovered(0).setMissed(1).build();
        node.addValue(leafTwo);
        assertThat(node).hasOnlyValues(leafOne, leafTwo);

        assertThat(getCoverage(node, LINE)).hasCoveredPercentage(Fraction.ONE);
        assertThat(getCoverage(node, BRANCH)).hasCoveredPercentage(Fraction.ZERO);

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
        return (Coverage)node.getValue(metric).get();
    }

    @Test
    void shouldCalculateCorrectCoverageForModule() {
        Node node = new ModuleNode("Node");
        Value valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();

        node.addValue(valueOne);

        assertThat(getCoverage(node, MODULE)).hasCoveredPercentage(Fraction.ONE);
    }

    @Test
    void shouldCalculateCorrectCoverageWithNestedStructure() {
        Node node = new ModuleNode("Node");
        Node missedFile = new FileNode("fileMissed");
        Node coveredFile = new FileNode("fileCovered");
        Value valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();
        Value valueTwo = new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build();

        node.addChild(missedFile);
        node.addChild(coveredFile);
        coveredFile.addValue(valueOne);
        missedFile.addValue(valueTwo);

        assertThat(getCoverage(node, LINE)).hasCoveredPercentage(Fraction.ONE_HALF);
        assertThat(getCoverage(node, FILE)).hasCoveredPercentage(Fraction.ONE_HALF);
    }

    @Test
    void shouldDeepCopyNodeTree() {
        Node node = new ModuleNode("Node");
        Node childNode = new FileNode("childNode");
        Value valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();
        Value valueTwo = new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build();

        node.addValue(valueOne);
        node.addChild(childNode);
        childNode.addValue(valueTwo);
        Node copiedNode = node.copyTree();

        assertThat(node).isNotSameAs(copiedNode);
        assertThat(node.getChildren().get(0)).isNotSameAs(copiedNode.getChildren().get(0));
    }

    @Test
    void shouldDeepCopyNodeTreeWithSpecifiedNodeAsParent() {
        Node node = new ModuleNode("Node");
        Node childNode = new FileNode("childNode");
        Value valueOne = new CoverageBuilder().setMetric(LINE).setCovered(1).setMissed(0).build();
        Value valueTwo = new CoverageBuilder().setMetric(LINE).setCovered(0).setMissed(1).build();
        Node newParent = new ModuleNode("parent");

        node.addValue(valueOne);
        node.addChild(childNode);
        childNode.addValue(valueTwo);
        Node copiedNode = node.copyTree(newParent);

        assertThat(copiedNode).hasParent(newParent);
    }

    @Test
    void shouldDetectMatchingOfMetricTypeAndNameOrHashCode() {
        Node node = new ModuleNode("Node");

        assertThat(node.matches(MODULE, "WrongName")).isFalse();
        assertThat(node.matches(PACKAGE, "Node")).isFalse();
        assertThat(node.matches(node.getMetric(), node.getName())).isTrue();

        assertThat(node.matches(MODULE, node.getName().hashCode())).isTrue();
        assertThat(node.matches(MODULE, "WrongName".hashCode())).isFalse();
        assertThat(node.matches(MODULE, node.getPath().hashCode())).isTrue();
    }

    @Test
    void shouldFindNodeByNameOrHashCode() {
        Node node = new ModuleNode("Node");
        Node childNode = new FileNode("childNode");
        node.addChild(childNode);

        assertThat(node.find(BRANCH, "NotExisting")).isNotPresent();
        assertThat(node.find(FILE, childNode.getName())).isPresent().get().isEqualTo(childNode);

        assertThat(node.findByHashCode(BRANCH, "NotExisting".hashCode())).isNotPresent();
        assertThat(node.findByHashCode(FILE, childNode.getName().hashCode())).isPresent().get().isEqualTo(childNode);
    }

    @Test
    void shouldNotAcceptIncompatibleNodes() {
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("edu.hm.hafner.pkg");
        Node moduleTwo = new ModuleNode("edu.hm.hafner.module2");

        assertThatIllegalArgumentException()
                .as("Should not accept incompatible nodes (different metric)")
                .isThrownBy(() -> module.combineWith(pkg));
        assertThatIllegalArgumentException()
                .as("Should not accept incompatible nodes (different name)")
                .isThrownBy(() -> module.combineWith(moduleTwo));
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingDifferentPackages() {
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node sameModule = new ModuleNode("edu.hm.hafner.module1");
        Node pkgOne = new PackageNode("coverage");
        Node pkgTwo = new PackageNode("autograding");

        module.addChild(pkgOne);
        sameModule.addChild(pkgTwo);
        Node combinedReport = module.combineWith(sameModule);

        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSamePackage() {
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node sameModule = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node samePackage = new PackageNode("coverage");

        module.addChild(pkg);
        sameModule.addChild(samePackage);
        Node combinedReport = module.combineWith(sameModule);
        assertThat(combinedReport).hasMetric(MODULE);
        assertThat(combinedReport.getAll(MODULE)).hasSize(1);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(1);
    }

    @Test
    void shouldCombineReportsOfSameModuleContainingSameAndDifferentPackages() {
        Node module = new ModuleNode("edu.hm.hafner.module1");
        Node sameModule = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node pkgTwo = new PackageNode("autograding");

        module.addChild(pkg);
        sameModule.addChild(pkgTwo);
        sameModule.addChild(pkg.copy());
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
        Node combinedReport = module.combineWith(sameModule);

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

        Node combinedReport = module.combineWith(sameModule);
        assertThat(combinedReport.getChildren().get(0)).hasOnlyChildren(fileToKeep);
        assertThat(combinedReport.getAll(CLASS)).hasSize(1);

        sameFileToKeep.addChild(classB);
        Node combinedReport2Classes = module.combineWith(sameModule);
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

        Node combinedReport = module.combineWith(otherNode);
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

        Node combinedReport = module.combineWith(sameProject);
        assertThat(combinedReport.getAll(METHOD)).hasSize(1);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(5).hasMissed(5);
    }

    @Test
    void shouldThrowErrorIfCoveredPlusMissedLinesDifferInReports() {
        Node module = setUpNodeTree();
        Node method = module.getAll(METHOD).get(0);

        Node sameProject = setUpNodeTree();
        Node methodOtherCov = sameProject.getAll(METHOD).get(0);

        assertThat(module.combineWith(sameProject)).isEqualTo(module); // should not throw error if no line coverage exists for method

        method.addValue(new CoverageBuilder().setMetric(LINE).setCovered(5).setMissed(5).build());
        methodOtherCov.addValue(new CoverageBuilder().setMetric(LINE).setCovered(2).setMissed(7).build());
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> module.combineWith(sameProject))
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

        Node combinedReport = module.combineWith(sameProject);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(5).hasMissed(5);
        assertThat(getCoverage(combinedReport, BRANCH)).hasCovered(12).hasMissed(3);
        assertThat(getCoverage(combinedReport, INSTRUCTION)).hasCovered(7).hasMissed(8);
    }

    @Test
    void shouldCorrectlyCombineTwoComplexReports() {
        Node report = setUpNodeTree();
        Node otherReport = setUpNodeTree();

        // Difference on Package Level
        PackageNode autograding = new PackageNode("autograding");
        FileNode file = new FileNode("Main.java");
        Node mainClass = new ClassNode("Main.class");
        MethodNode mainMethod = new MethodNode("main", "(Ljava/util/Map;)V", 10);

        otherReport.addChild(autograding);
        autograding.addChild(file);
        file.addChild(mainClass);
        mainClass.addChild(mainMethod);
        mainMethod.addValue(new CoverageBuilder().setMetric(LINE).setCovered(8).setMissed(2).build());

        // Difference on File Level
        FileNode covLeavefile = new FileNode("Leaf");
        FileNode pkgCovFile = new FileNode("HelloWorld");
        covLeavefile.addChild(mainClass.copyTree());

        report.getAll(PACKAGE).get(0).addChild(pkgCovFile);
        otherReport.getAll(PACKAGE).get(0).addChild(covLeavefile);

        Node combinedReport = report.combineWith(otherReport);
        assertThat(combinedReport.getAll(PACKAGE)).hasSize(2);
        assertThat(combinedReport.getAll(FILE)).hasSize(4);
        assertThat(combinedReport.getAll(CLASS)).hasSize(3);
        assertThat(getCoverage(combinedReport, LINE)).hasCovered(16).hasMissed(4);
        assertThat(combinedReport.getValue(BRANCH)).isEmpty();
    }

    @Test
    void shouldUseDeepCopiedNodesInCombineWithInRelatedProjects() {
        Node project = new ModuleNode("edu.hm.hafner.module1");
        Node sameProject = project.copyTree();
        PackageNode coveragePkg = new PackageNode("coverage");
        PackageNode autogradingPkg = new PackageNode("autograding");

        project.addChild(coveragePkg);
        sameProject.addChild(autogradingPkg);
        Node combinedReport = project.combineWith(sameProject);

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
        Node otherReport;

        report.addChild(pkg);
        pkg.addChild(file);
        otherReport = report.copyTree();

        otherReport.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(90).setMissed(10).build());
        report.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(80).setMissed(20).build());

        Node combined = report.combineWith(otherReport);
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

        Node combined = report.combineWith(otherReport);
        assertThat(getCoverage(combined, LINE)).hasMissed(20).hasCovered(80);
    }

    /**
     * If one report stops e.g. at file level and other report goes down to class level,
     * results of the report with higher depth should be used.
     */
    @Test @Disabled("Does it make sense to provide that functionality?")
    void shouldHandleReportsOfDifferentDepth() {
        Node report = new ModuleNode("edu.hm.hafner.module1");
        Node pkg = new PackageNode("coverage");
        Node file = new FileNode("Node.java");
        Node covNodeClass = new ClassNode("Node.class");

        report.addChild(pkg);
        pkg.addChild(file);

        Node otherReport;
        otherReport = report.copyTree();
        otherReport.find(file.getMetric(), file.getName()).get().addChild(covNodeClass);
        covNodeClass.addValue(new CoverageBuilder().setMetric(LINE).setCovered(90).setMissed(10).build());

        report.find(FILE, file.getName()).get().addValue(
                new CoverageBuilder().setMetric(LINE).setCovered(80).setMissed(20).build());

        assertThat(getCoverage(report.combineWith(otherReport), LINE)).hasMissed(10).hasCovered(90);
        assertThat(getCoverage(otherReport.combineWith(report), LINE)).hasMissed(10).hasCovered(90);
        assertThat(report.combineWith(otherReport).find(covNodeClass.getMetric(), covNodeClass.getName()).get())
                .isNotSameAs(covNodeClass);
    }
}
