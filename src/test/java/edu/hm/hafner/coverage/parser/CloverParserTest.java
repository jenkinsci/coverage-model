package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.*;
import edu.hm.hafner.coverage.assertions.Assertions;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.Issue;

import java.util.HashSet;
import java.util.Set;

import static edu.hm.hafner.coverage.Metric.*;
import static org.assertj.core.api.Assertions.assertThat;

class CloverParserTest extends AbstractParserTest {
    @Override
    protected String getFolder() {
        return "clover";
    }

    @Override
    CoverageParser createParser(final CoverageParser.ProcessingMode processingMode) {
        return new CloverParser(processingMode);
    }

    @Test
    @SuppressWarnings({"PMD.OptimizableToArrayCall", "PMD.AvoidThrowingRawExceptionTypes"})
    void testBasic() {
        var root = readReport("clover.xml");
        //Verifying package level coverage
        var builder = new Coverage.CoverageBuilder().withMetric(LINE);
        assertThat(root.getValue(LINE)).contains(
                builder.withCovered(143).withTotal(165).build());

        for (Node packageNode : root.getChildren()) {
            if (packageNode.getName().equals("actions")) {
                verifyCoverage(BRANCH, 12, 5, packageNode);
                verifyCoverage(INSTRUCTION, 101, 7, packageNode);
                verifyCoverage(METHOD, 93, 22, packageNode);
                //Verifying package level coverage
                builder = new Coverage.CoverageBuilder().withMetric(LINE);
                assertThat(packageNode.getValue(LINE)).contains(
                        builder.withCovered(101).withTotal(108).build());
            }
            else if (packageNode.getName().equals("components")) {
                //Verifying package level coverage
                builder = new Coverage.CoverageBuilder().withMetric(LINE);
                assertThat(packageNode.getValue(LINE)).contains(
                        builder.withCovered(35).withTotal(46).build());
            }
        }
        verifyFileCoverage(root);
    }

    void verifyFileCoverage(Node root) {
        for (FileNode f : root.getAllFileNodes()) {
            switch (f.getFileName()) {
                case "File1.js":
                    verifyCoverage(BRANCH, 0, 0, f);
                    verifyCoverage(INSTRUCTION, 68, 0, f);
                    verifyCoverage(METHOD, 0, 0, f);
                    Set<Integer> covered = new HashSet<>();
                    addRange(covered, 4, 7);
                    addRange(covered, 12, 22);
                    addRange(covered, 24, 43);
                    addRange(covered, 45, 77);
                    addRange(covered, 79, 77);
                    break;
                case "File2.js":
                    verifyCoverage(BRANCH, 12, 5, f);
                    verifyCoverage(INSTRUCTION, 33, 7, f);
                    verifyCoverage(METHOD, 93, 22, f);
                    Assertions.assertThat(f).hasMissedLines(92, 127, 204, 369, 492, 503, 515).hasCoveredLines(21, 38, 51, 65, 79, 105, 117, 138, 151, 164, 176, 190, 215, 228, 243, 257, 268, 287, 303, 317, 329, 339, 349, 359, 380, 393, 405, 416, 429, 443, 456, 467, 480);
                    break;
                case "File3.jsx":
                    Assertions.assertThat(f).hasMissedLines(45, 46, 78, 104, 105, 106).hasCoveredLines(13, 21, 26, 29, 32, 60, 61, 62, 89, 93, 103);
                    break;
                case "File4.jsx":
                    Assertions.assertThat(f).hasMissedLines(8, 50, 51, 58).hasCoveredLines(4, 11, 14, 30, 38, 43, 46, 49, 57, 61, 68, 72, 77, 81, 85, 89, 90);
                    break;
                case "File5.jsx":
                    Assertions.assertThat(f).hasMissedLines(25).hasCoveredLines(18, 19, 20, 24, 31, 50, 59);
                    break;
                case "File6.jsx":
                    Assertions.assertThat(f).hasMissedLines(32, 33, 35, 92).hasCoveredLines(23, 24, 31, 38, 72, 79, 90);
                    break;
                default:
                    return;
            }
        }
    }

    @Test
    void testCloverWithClasses() {
        var root = readReport("clover-java.xml");
        //Verifying package level coverage
        var builder = new Coverage.CoverageBuilder().withMetric(LINE);
        assertThat(root.getValue(LINE)).contains(
                builder.withCovered(1).withTotal(17).build());
        verifyCoverage(BRANCH, 1, 1, root);
        verifyCoverage(INSTRUCTION, 2, 16, root);
        verifyCoverage(METHOD, 1, 9, root);
        for (FileNode f : root.getAllFileNodes()) {
            switch (f.getFileName()) {
                case "CloverPublisher.java":
                    Assertions.assertThat(f).hasMissedLines(28, 33, 37, 38, 43, 59, 64, 69, 70, 71, 76);
                    break;
                case "PluginImpl.java":
                    Assertions.assertThat(f).hasMissedLines(21);
                    break;
                case "CloverCoverageParser.java":
                    Assertions.assertThat(f)
                            .hasMissedLines(18, 19, 20, 22)
                            .hasCoveredLines(17, 18);
                    break;
                default:
                    return;
            }
        }
    }

    @Test
    void testRelativePathFromPackage() {
        var root = readReport("clover-declarative.xml");

        assertThat(root.getAllFileNodes()).hasSize(1)
                .satisfiesExactlyInAnyOrder(
                        file -> Assertions.assertThat(file).hasName("CloverPublisher.java").hasRelativePath("hudson/plugins/clover/CloverPublisher.java"));
    }

    @Test
    void testPathFromFilePath() {
        var root = readReport("clover.xml");

        assertThat(root.getAllFileNodes()).hasSize(6)
                .satisfiesExactlyInAnyOrder(
                        file -> Assertions.assertThat(file).hasName("File1.js").hasRelativePath("/home/jenkins/agent/workspace/dir/src/js/actions/File1.js"),
                        file -> Assertions.assertThat(file).hasName("File2.js").hasRelativePath("/home/jenkins/agent/workspace/dir/src/js/actions/File2.js"),
                        file -> Assertions.assertThat(file).hasName("File3.jsx").hasRelativePath("/home/jenkins/agent/workspace/dir/src/js/components/File3.jsx"),
                        file -> Assertions.assertThat(file).hasName("File4.jsx").hasRelativePath("/home/jenkins/agent/workspace/dir/src/js/components/File4.jsx"),
                        file -> Assertions.assertThat(file).hasName("File5.jsx").hasRelativePath("/home/jenkins/agent/workspace/dir/src/js/components/File5.jsx"),
                        file -> Assertions.assertThat(file).hasName("File6.jsx").hasRelativePath("/home/jenkins/agent/workspace/dir/src/js/components/AddEditCategories/File6.jsx"));
    }

    @Test
    void testPathFromFileNameIfAbsolute() {
        var root = readReport("clover-java.xml");

        assertThat(root.getAllFileNodes()).hasSize(4)
                .satisfiesExactlyInAnyOrder(
                        file -> Assertions.assertThat(file).hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverPublisher.java")
                                .hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverPublisher.java"),
                        file -> Assertions.assertThat(file).hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\PluginImpl.java").
                                hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\PluginImpl.java"),
                        file -> Assertions.assertThat(file).hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverBuildAction.java").hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverBuildAction.java"),
                        file -> Assertions.assertThat(file).hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverCoverageParser.java").hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverCoverageParser.java"));
    }

    @Test
    void testCloverWithDeclarative() {
        var root = readReport("clover-declarative.xml");
        for (FileNode f : root.getAllFileNodes()) {
            if (f.getFileName().equals("CloverPublisher.java")) {
                verifyCoverage(BRANCH, 0, 0, f);
                verifyCoverage(INSTRUCTION, 0, 11, f);
                verifyCoverage(METHOD, 0, 8, f);
                for (ClassNode classNode : f.getAllClassNodes()) {
                    switch (classNode.getName()) {
                        case "CloverPublisher":
                            verifyCoverage(BRANCH, 0, 0, classNode);
                            verifyCoverage(INSTRUCTION, 0, 5, classNode);
                            verifyCoverage(METHOD, 0, 4, classNode);
                            break;
                        case "CloverPublisher.DescriptorImpl":
                            verifyCoverage(BRANCH, 0, 0, classNode);
                            verifyCoverage(INSTRUCTION, 0, 6, classNode);
                            verifyCoverage(METHOD, 0, 4, classNode);
                            break;
                        default:
                            break;
                    }
                }
            }
            return;
        }
    }

    @Test
    void testCloverWithMultiplePackages() {
        var root = readReport("clover-two-packages.xml");
        for (Node pacageNode : root.getChildren()) {
            switch (pacageNode.getName()) {
                case "hudson.plugins.clover":
                    for (Node f : pacageNode.getChildren()) {
                        switch (((FileNode)f).getFileName()) {
                            case "CloverCoverageParser.java":
                                Assertions.assertThat((FileNode) f)
                                        .hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverCoverageParser.java")
                                        .hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverCoverageParser.java");
                                verifyCoverage(BRANCH, 2, 0, f);
                                verifyCoverage(METHOD, 1, 0, f);
                                verifyCoverage(INSTRUCTION, 12, 1, f);
                                verifyCoverage(LINE, 11, 1, f);
                                Assertions.assertThat((FileNode) f)
                                        .hasCoveredLines(20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30)
                                        .hasMissedLines(32);
                                break;
                            case "PluginImpl.java":
                                Assertions.assertThat((FileNode) f)
                                        .hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\PluginImpl.java")
                                        .hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\PluginImpl.java");
                                verifyCoverage(LINE, 0, 1, f);
                                Assertions.assertThat((FileNode) f)
                                        .hasMissedLines(21);
                                break;
                            case "CloverPublisher.java":
                                Assertions.assertThat((FileNode) f)
                                        .hasName("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverPublisher.java")
                                        .hasRelativePath("C:\\local\\maven\\helpers\\hudson\\clover\\src\\main\\java\\hudson\\plugins\\clover\\CloverPublisher.java");
                                verifyCoverage(LINE, 0, 11, f);
                                Assertions.assertThat((FileNode) f)
                                        .hasMissedLines(28, 33, 37, 38, 43, 59, 64, 69, 70, 71, 76);
                                break;
                            default:
                                break;
                        }
                    }
                    //Verifying package level coverage
                    var builder = new Coverage.CoverageBuilder().withMetric(LINE);
                    assertThat(pacageNode.getValue(LINE)).contains(
                                    builder.withCovered(11).withTotal(24).build());

                    //verifyCoverage(LINE, 11, 13, pacageNode);
                    verifyCoverage(BRANCH, 2, 0, pacageNode);
                    verifyCoverage(INSTRUCTION, 12, 13, pacageNode);
                    verifyCoverage(METHOD, 1, 9, pacageNode);
                    break;
                default:
                    break;
            }
        }
    }

    private void verifyCoverage(Metric metric, final int covered, final int missed, Node node) {
        Assertions.assertThat(node).hasValueMetrics(metric);
        Assertions.assertThat(node).hasValues(createCoverage(metric, covered, missed));
    }

    private static void addRange(final Set<Integer> collection, final int start, final int end) {
        // generate a range of integers from start to end (inclusive)
        for (int i = start; i <= end; i++) {
            collection.add(i);
        }
    }

    @Test
    void shouldCreateEmptyPackage() {
        var root = readReport("clover-empty-package.xml");
        var line = new Coverage.CoverageBuilder().withMetric(Metric.LINE);
        assertThat(root.getAllFileNodes()).satisfiesExactlyInAnyOrder(
                file -> Assertions.assertThat(file)
                        .hasFileName("HelloWidget.jsx")
                        .hasNoMissedLines()
                        .hasCoveredLines(15, 19, 36, 37, 38, 46, 47, 48, 59, 71, 80, 81)
                        .hasValues(line.withCovered(12).withTotal(12).build()),
                file -> Assertions.assertThat(file)
                        .hasFileName("HelpInfo.jsx")
                        .hasNoMissedLines()
                        .hasCoveredLines(7, 12, 16, 24, 29, 36, 37, 41, 55, 56, 59, 64, 78, 79, 150)
                        .hasValues(line.withCovered(15).withTotal(15).build()),
                file -> Assertions.assertThat(file)
                        .hasFileName("UserInfoForm.jsx")
                        .hasNoMissedLines()
                        .hasCoveredLines(9, 13, 17, 22, 29, 30, 31, 32, 33, 34, 35, 36, 42, 45, 49, 58, 62, 65, 66, 68, 72, 79, 83, 86, 87, 95, 96, 98, 99, 103, 104, 105, 106, 109, 110, 111, 115, 116, 124, 131, 133, 166)
                        .hasValues(line.withCovered(42).withTotal(42).build())
        );
    }

    private Coverage createCoverage(Metric metric, final int covered, final int missed) {
        return new Coverage.CoverageBuilder().withMetric(metric).withCovered(covered).withMissed(missed).build();
    }
}
