package edu.hm.hafner.coverage.parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests mapping of test classes to the tested files.
 *
 * @author Ullrich Hafner
 */
class TestCaseMappingTest {
    private static final String[] METRIC_TESTS = {"shouldGetCoverageMetrics",
            "shouldCorrectlyComputeDensityEvaluator",
            "shouldCorrectlyImplementIsContainer",
            "shouldConvertToTags(Metric)[1]",
            "shouldConvertToTags(Metric)[2]",
            "shouldConvertToTags(Metric)[3]",
            "shouldConvertToTags(Metric)[4]",
            "shouldConvertToTags(Metric)[5]",
            "shouldConvertToTags(Metric)[6]",
            "shouldConvertToTags(Metric)[7]",
            "shouldConvertToTags(Metric)[8]",
            "shouldConvertToTags(Metric)[9]",
            "shouldConvertToTags(Metric)[10]",
            "shouldConvertToTags(Metric)[11]",
            "shouldConvertToTags(Metric)[12]",
            "shouldConvertToTags(Metric)[13]",
            "shouldConvertToTags(Metric)[14]",
            "shouldConvertToTags(Metric)[15]",
            "shouldReturnEmptyOptionalOnComputeDensityEvaluator"};
    private static final String[] TEST_CLASSES = {"ArchitectureTest",
            "PackageArchitectureTest",
            "edu.hm.hafner.coverage.ClassNodeTest",
            "edu.hm.hafner.coverage.ContainerNodeTest",
            "edu.hm.hafner.coverage.CoverageParserTest",
            "edu.hm.hafner.coverage.CoverageTest",
            "edu.hm.hafner.coverage.CyclomaticComplexityTest",
            "edu.hm.hafner.coverage.FileNodeTest",
            "edu.hm.hafner.coverage.FractionValueTest",
            "edu.hm.hafner.coverage.LinesOfCodeTest",
            "edu.hm.hafner.coverage.MethodNodeTest",
            "edu.hm.hafner.coverage.MetricTest",
            "edu.hm.hafner.coverage.ModuleNodeTest",
            "edu.hm.hafner.coverage.MutationTest",
            "edu.hm.hafner.coverage.NodeTest",
            "edu.hm.hafner.coverage.PackageNodeTest",
            "edu.hm.hafner.coverage.PercentageTest",
            "edu.hm.hafner.coverage.SafeFractionTest",
            "edu.hm.hafner.coverage.TestCaseTest",
            "edu.hm.hafner.coverage.TestCountTest",
            "edu.hm.hafner.coverage.ValueTest",
            "edu.hm.hafner.coverage.parser.CoberturaParserTest",
            "edu.hm.hafner.coverage.parser.JacocoParserTest",
            "edu.hm.hafner.coverage.parser.JunitParserTest",
            "edu.hm.hafner.coverage.parser.PitestParserTest",
            "edu.hm.hafner.coverage.registry.ParserRegistryTest"};

    @Test
    void shouldMapTestCasesToClasses() {
        var coverage = readReport("jacoco.xml", new JacocoParser());
        assertThat(coverage.getFiles()).hasSize(26);

        var tests = readReport("junit.xml", new JunitParser());
        var numberOfTests = 257;
        assertThat(tests.getTestCases()).hasSize(numberOfTests);
        assertThat(tests.getAllClassNodes()).extracting(Node::getName).containsExactly(TEST_CLASSES);
        assertThat(tests.getValue(Metric.TESTS)).contains(new Value(Metric.TESTS, numberOfTests));

        var unmappedTests = coverage.mergeTests(tests.getAllClassNodes());
        assertThat(unmappedTests).flatMap(Node::getTestCases).hasSize(10);
        assertThat(coverage.getTestCases()).hasSize(247);
        assertThat(coverage.getValue(Metric.TESTS)).contains(new Value(Metric.TESTS, numberOfTests));

        assertThat(unmappedTests)
                .extracting(Node::getName)
                .containsOnly("ArchitectureTest", "PackageArchitectureTest");

        assertThat(coverage.findFile("Metric.java")).hasValueSatisfying(
                file -> assertThat(file.getTestCases()).hasSize(19)
                        .extracting(TestCase::getTestName)
                        .containsOnly(METRIC_TESTS));
    }

    @SuppressFBWarnings("OBL")
    private ModuleNode readReport(final String fileName, final CoverageParser parser) {
        try {
            try (var stream = Objects.requireNonNull(TestCaseMappingTest.class.getResourceAsStream(fileName),
                    "File not found: " + fileName);
                    var reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
                return parser.parse(reader, fileName, new FilteredLog("Errors"));
            }
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
