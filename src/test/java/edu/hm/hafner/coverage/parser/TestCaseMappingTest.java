package edu.hm.hafner.coverage.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.util.FilteredLog;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests mapping of test classes to the tested files.
 *
 * @author Ullrich Hafner
 */
class TestCaseMappingTest {
    @Test
    void shouldMapTestCasesToClasses() {
        var coverage = readReport("jacoco.xml", new JacocoParser());
        assertThat(coverage.getFiles()).hasSize(26);

        var tests = readReport("junit.xml", new JunitParser());
        assertThat(tests.getTestCases()).hasSize(257);

        var testCases = tests.getTestCases();
        coverage.mapTests(tests.getAllClassNodes());

        testCases.removeAll(coverage.getTestCases());
        assertThat(testCases).hasSize(10)
                .extracting(TestCase::getClassName)
                .containsOnly("ArchitectureTest", "PackageArchitectureTest");
    }

    @SuppressFBWarnings("OBL")
    private ModuleNode readReport(final String fileName, final CoverageParser parser) {
        try {
            try (InputStream stream = Objects.requireNonNull(TestCaseMappingTest.class.getResourceAsStream(fileName),
                    "File not found: " + fileName);
                    Reader reader = new InputStreamReader(Objects.requireNonNull(stream), StandardCharsets.UTF_8)) {
                return parser.parse(reader, new FilteredLog("Errors"));
            }
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

}
