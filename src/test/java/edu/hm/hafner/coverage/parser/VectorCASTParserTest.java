package edu.hm.hafner.coverage.parser;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.FractionValue;
import edu.hm.hafner.coverage.LinesOfCode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.Percentage;

import static edu.hm.hafner.coverage.Metric.CLASS;
import static edu.hm.hafner.coverage.Metric.FILE;
import static edu.hm.hafner.coverage.Metric.*;
import static edu.hm.hafner.coverage.assertions.Assertions.*;

@DefaultLocale("en")
class VectorCASTParserTest extends AbstractParserTest {
    @Override
    VectorCASTParser createParser(final ProcessingMode processingMode) {
        return new VectorCASTParser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "vectorcast";
    }
    
    @Test
    void shouldConvertVectorCASTStatementBranchToTree() {
        Node root = readReport("vectorcast-statement-branch.xml");

        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(3);
        assertThat(root.getAll(FILE)).hasSize(3);
        assertThat(root.getAll(CLASS)).hasSize(3);
        assertThat(root.getAll(METHOD)).hasSize(0);

        assertThat(root).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, LINE, BRANCH, COMPLEXITY, COMPLEXITY_MAXIMUM, COMPLEXITY_DENSITY, LOC);

        var files = root.getAllFileNodes();
        assertThat(files).hasSize(3).extracting(FileNode::getFileName)
                .containsExactlyInAnyOrder("database.c", "manager.c", "whitebox.c");

        var builder = new CoverageBuilder();
        assertThat(root.find(FILE, "tutorial/c/database/database.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(10, 9, 19, 17, 20, 23)
                                .hasCoveredLines(7, 12)));
                                
        assertThat(root.find(CLASS, "database")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(4),
                                           builder.withMetric(LINE).withCovered(3).withMissed(7).build(),
                                           builder.withMetric(BRANCH).withCovered(2).withMissed(4).build()
                                           )));
           
        assertThat(root.find(FILE, "tutorial/c/order_entry/manager.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(80, 89, 78, 81, 82, 88, 76, 75, 83, 86, 74, 79)
                                .hasCoveredLines(46, 60, 39, 42, 43, 62, 53, 63, 56, 66, 59, 51, 
                                    48, 67, 41, 54, 57, 23, 19, 20, 21, 17, 25, 26, 27, 29, 95, 96, 
                                    103, 101, 106, 107, 109, 105, 102, 116, 114, 115)));
                                    
        assertThat(root.find(CLASS, "manager")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(18),
                                           builder.withMetric(LINE).withCovered(48).withMissed(13).build(),
                                           builder.withMetric(BRANCH).withCovered(21).withMissed(4).build()
                                           )));

        assertThat(root.find(FILE, "tutorial/c/utils/whitebox.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(17, 22, 27, 29, 28)
                                .hasCoveredLines(30)));
                                    
        assertThat(root.find(CLASS, "whitebox")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(3),
                                           builder.withMetric(LINE).withCovered(1).withMissed(8).build(),
                                           builder.withMetric(BRANCH).withCovered(0).withMissed(3).build()
                                           )));

        assertThat(root.aggregateValues()).containsExactly(
                builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
                builder.withMetric(PACKAGE).withCovered(3).withMissed(0).build(),
                builder.withMetric(FILE).withCovered(3).withMissed(0).build(),
                builder.withMetric(CLASS).withCovered(3).withMissed(0).build(),
                builder.withMetric(LINE).withCovered(52).withMissed(28).build(),
                builder.withMetric(BRANCH).withCovered(23).withMissed(11).build(),
                new CyclomaticComplexity(25),
                new FractionValue(COMPLEXITY_DENSITY, 25, 80),
                new LinesOfCode(80));

        verifyCoverageMetrics(root);

        List<Node> nodes = root.getAll(FILE);

        long missedLines = 0;
        long coveredLines = 0;
        for (Node node : nodes) {
            var lineCoverage = (Coverage) node.getValue(LINE).get();
            missedLines = missedLines + lineCoverage.getMissed();
            coveredLines = coveredLines + lineCoverage.getCovered();
        }

        assertThat(missedLines).isEqualTo(28);
        assertThat(coveredLines).isEqualTo(52);
    }

    @Test
    void shouldHaveOneSource() {
        ModuleNode tree = readExampleReport();

        assertThat(tree.getSourceFolders())
                .hasSize(1)
                .containsExactly("");
    }

    private static Coverage getCoverage(final Node node, final Metric metric) {
        return (Coverage) node.getValue(metric).get();
    }

    private void verifyCoverageMetrics(final Node tree) {
        assertThat(getCoverage(tree, LINE))
                .hasCovered(52)
                .hasCoveredPercentage(Percentage.valueOf(52, 52 + 28))
                .hasMissed(28)
                .hasTotal(52 + 28);

        assertThat(getCoverage(tree, BRANCH))
                .hasCovered(23)
                .hasCoveredPercentage(Percentage.valueOf(23, 23 + 11))
                .hasMissed(11)
                .hasTotal(23 + 11);

        assertThat(getCoverage(tree, MODULE))
                .hasCovered(1)
                .hasCoveredPercentage(Percentage.valueOf(1, 1))
                .hasMissed(0)
                .hasTotal(1);

        assertThat(tree).hasName("-")
                .doesNotHaveParent()
                .isRoot()
                .hasMetric(MODULE).hasParentName("^");
    }
    
    void verifyMcdcFccEncryptC(Node root) {
        CoverageBuilder builder = new CoverageBuilder();
        assertThat(root.find(FILE, "CurrentRelease/encrypt/src/encrypt.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(74, 77, 80, 83, 89)
                                .hasCoveredLines(61, 63, 67, 70, 87, 91, 96, 99, 102, 105, 
                                    110, 112, 129, 135, 139, 142, 145, 146, 149, 152, 155, 
                                    161, 179, 181, 182, 185, 188, 191, 195, 214, 216, 219, 
                                    222, 225, 228, 231, 251, 253, 259, 261)));
                                
        assertThat(root.find(CLASS, "encrypt")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(17),
                                           builder.withMetric(LINE).withCovered(40).withMissed(5).build(),
                                           builder.withMetric(BRANCH).withCovered(52).withMissed(17).build(),
                                           builder.withMetric(MCDC_PAIR).withCovered(9).withMissed(9).build(),
                                           builder.withMetric(FUNCTION_CALL).withCovered(14).withMissed(4).build(),
                                           builder.withMetric(FUNCTION).withCovered(5).withMissed(0).build()
                                           )));
    }

    void verifyMcdcFccManagerC(Node root) {
        CoverageBuilder builder = new CoverageBuilder();
        assertThat(root.find(FILE, "CurrentRelease/order_entry/src/manager.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(142, 143, 178, 228, 276)
                                .hasCoveredLines(71, 73, 76, 77, 78, 80, 84, 85, 86, 88, 110, 112,
                                    115, 116, 117, 120, 123, 126, 128, 129, 130, 131, 132, 133,
                                    134, 135, 136, 137, 138, 139, 140, 141, 147, 150, 151, 167,
                                    169, 170, 173, 176, 182, 183, 184, 186, 187, 188, 189, 190,
                                    191, 194, 196, 199, 203, 219, 220, 223, 226, 231, 232, 235,
                                    255, 257, 260, 263, 266, 269, 272)));
                                    
        assertThat(root.find(CLASS, "manager")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(17),
                                           builder.withMetric(LINE).withCovered(67).withMissed(5).build(),
                                           builder.withMetric(BRANCH).withCovered(38).withMissed(9).build(),
                                           builder.withMetric(MCDC_PAIR).withCovered(6).withMissed(5).build(),
                                           builder.withMetric(FUNCTION_CALL).withCovered(10).withMissed(0).build(),
                                           builder.withMetric(FUNCTION).withCovered(5).withMissed(0).build()
                                           )));
    }

    void verifyMcdcFccWhiteboxC(Node root) {
        CoverageBuilder builder = new CoverageBuilder();
        assertThat(root.find(FILE, "CurrentRelease/utils/src/whitebox.c")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(FileNode.class,
                        f -> assertThat(f)
                                .hasMissedLines(67, 69, 85, 87, 104, 105, 121, 123, 126, 129, 130)));
                                    
        assertThat(root.find(CLASS, "whitebox")).isNotEmpty()
                .hasValueSatisfying(n -> assertThat(n).isInstanceOfSatisfying(ClassNode.class,
                        f -> assertThat(f)
                                .hasValues(new CyclomaticComplexity(4),
                                           builder.withMetric(LINE).withCovered(0).withMissed(11).build(),
                                           builder.withMetric(BRANCH).withCovered(0).withMissed(4).build(),
                                           builder.withMetric(FUNCTION_CALL).withCovered(0).withMissed(2).build(),
                                           builder.withMetric(FUNCTION).withCovered(0).withMissed(4).build()
                                           )));
    }

    void verifyMcdcFccProjectMetrics(List<Node> nodes) {
        long missedLines = 0;
        long coveredLines = 0;

        long missedBranches = 0;
        long coveredBranches = 0;

        long missedMcdcPairs = 0;
        long coveredMcdcPairs = 0;

        long missedFunctions = 0;
        long coveredFunctions = 0;

        long missedFunctionCalls = 0;
        long coveredFunctionCalls = 0;

        for (Node node : nodes) {
            if (node.getValue(LINE).isPresent()) {
                var lineCoverage = (Coverage) node.getValue(LINE).get();
                missedLines = missedLines + lineCoverage.getMissed();
                coveredLines = coveredLines + lineCoverage.getCovered();
            }

            if (node.getValue(BRANCH).isPresent()) {
                var branchCoverage = (Coverage) node.getValue(BRANCH).get();
                missedBranches = missedBranches + branchCoverage.getMissed();
                coveredBranches = coveredBranches + branchCoverage.getCovered();
            }

            if (node.getValue(MCDC_PAIR).isPresent()) {
                var mcdcCoverage = (Coverage) node.getValue(MCDC_PAIR).get();
                missedMcdcPairs = missedMcdcPairs + mcdcCoverage.getMissed();
                coveredMcdcPairs = coveredMcdcPairs + mcdcCoverage.getCovered();
            }

            if (node.getValue(FUNCTION).isPresent()) {
                var functionCoverage = (Coverage) node.getValue(FUNCTION).get();
                missedFunctions = missedFunctions + functionCoverage.getMissed();
                coveredFunctions = coveredFunctions + functionCoverage.getCovered();
            }

            if (node.getValue(FUNCTION_CALL).isPresent()) {
                var functionCallCoverage = (Coverage) node.getValue(FUNCTION_CALL).get();
                missedFunctionCalls = missedFunctionCalls + functionCallCoverage.getMissed();
                coveredFunctionCalls = coveredFunctionCalls + functionCallCoverage.getCovered();
            }
        }

        assertThat(coveredLines).isEqualTo(235);
        assertThat(missedLines).isEqualTo(59);
        
        assertThat(coveredBranches).isEqualTo(180);
        assertThat(missedBranches).isEqualTo(92);
        
        assertThat(coveredMcdcPairs).isEqualTo(24);
        assertThat(missedMcdcPairs).isEqualTo(35);
        
        assertThat(coveredFunctions).isEqualTo(21);
        assertThat(missedFunctions).isEqualTo(9);
        
        assertThat(coveredFunctionCalls).isEqualTo(62);
        assertThat(missedFunctionCalls).isEqualTo(17);
        
    }
                
    void verifyMcdcFccProject(Node root) {
        CoverageBuilder builder = new CoverageBuilder();
        assertThat(root.aggregateValues()).containsExactly(
            builder.withMetric(MODULE).withCovered(1).withMissed(0).build(),
            builder.withMetric(PACKAGE).withCovered(5).withMissed(0).build(),
            builder.withMetric(FILE).withCovered(6).withMissed(2).build(),
            builder.withMetric(CLASS).withCovered(6).withMissed(2).build(),
            builder.withMetric(METHOD).withCovered(0).withMissed(30).build(),
            builder.withMetric(LINE).withCovered(235).withMissed(59).build(),
            builder.withMetric(BRANCH).withCovered(180).withMissed(92).build(),
            builder.withMetric(MCDC_PAIR).withCovered(24).withMissed(35).build(),
            builder.withMetric(FUNCTION).withCovered(21).withMissed(9).build(),
            builder.withMetric(FUNCTION_CALL).withCovered(62).withMissed(17).build(),
            new CyclomaticComplexity(100),
            new CyclomaticComplexity(26, COMPLEXITY_MAXIMUM),
            new FractionValue(COMPLEXITY_DENSITY, 100, 294),
            new LinesOfCode(294));
    }

    @Test
    void verifyMcdcFunctionCallCoverage() {
        Node root = readReport("vectorcast-statement-mcdc-fcc.xml");

        assertThat(root.getAll(MODULE)).hasSize(1);
        assertThat(root.getAll(PACKAGE)).hasSize(5);
        assertThat(root.getAll(FILE)).hasSize(8);
        assertThat(root.getAll(CLASS)).hasSize(8);
        assertThat(root.getAll(METHOD)).hasSize(30);

        assertThat(root).hasOnlyMetrics(MODULE, PACKAGE, FILE, CLASS, LINE, BRANCH, COMPLEXITY, COMPLEXITY_MAXIMUM, COMPLEXITY_DENSITY, LOC, MCDC_PAIR, FUNCTION_CALL, METHOD, FUNCTION);

        var files = root.getAllFileNodes();
        assertThat(files).hasSize(8).extracting(FileNode::getFileName)
                .containsExactlyInAnyOrder("database.c", "manager.c", "whitebox.c", "matrix_multiply.c", "linked_list.c", "encrypt.c", "pos_driver.c", "waiting_list.c");
                
        verifyMcdcFccEncryptC(root);
        verifyMcdcFccManagerC(root);
        verifyMcdcFccWhiteboxC(root);
        verifyMcdcFccProject(root);

        List<Node> nodes = root.getAll(FILE);
        verifyMcdcFccProjectMetrics(nodes);
        
        assertThat(root.getAllFileNodes())
            .hasSize(8)
            .extracting(FileNode::getRelativePath)
            .containsOnly(
                "CurrentRelease/database/src/database.c",
                "CurrentRelease/encrypt/src/encrypt.c",
                "CurrentRelease/encrypt/src/matrix_multiply.c",
                "CurrentRelease/main/pos_driver.c",
                "CurrentRelease/order_entry/src/manager.c",
                "CurrentRelease/order_entry/src/waiting_list.c",
                "CurrentRelease/utils/src/linked_list.c",
                "CurrentRelease/utils/src/whitebox.c"
                );
    }

    private ModuleNode readExampleReport() {
        return readReport("vectorcast-statement-branch.xml");
    }
}
