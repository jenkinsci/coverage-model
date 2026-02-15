package edu.hm.hafner.coverage.parser;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CoverageParser.ProcessingMode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.Node;

/**
 * Tests for {@link Trace32Parser} - focuses on single file reports.
 */
class Trace32ParserTest extends AbstractParserTest {
    @Override
    CoverageParser createParser(final ProcessingMode processingMode) {
        return new Trace32Parser(processingMode);
    }

    @Override
    protected String getFolder() {
        return "trace32";
    }

    @Test
    void testEmptyReport() {
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("empty.xml"));
    }

    @Test
    void testInvalidReport() {
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> readReport("trace32-invalid.xml"));
    }

    @Test
    void testParseListing() {
        var root = readReport("trace32-call.xml");

        // Check root name
        assertThat(root.getName()).isEqualTo("Trace32 Coverage");

        // Check that class nodes are created
        var classes = root.getAll(Metric.CLASS);
        assertThat(classes).isNotEmpty();

        // Check class node names (anything, but empty)
        var classNames = classes.stream()
                .map(Node::getName)
                .collect(Collectors.toList());
        assertThat(classNames).isNotEmpty();

        // Check that file nodes collector exist, and verify its content
        var filesNode = classes.get(classes.size() - 1);
        assertThat(filesNode.getName()).isEqualTo("Trace32 Coverage Files");
        assertThat(filesNode.getChildren()).extracting(Node::getName).containsExactlyInAnyOrder("coverage.c", "main.c", "gesf2.c", "libgcc2.c", "start.sx", "floatsisf.c");
        assertThat(filesNode.getMetrics()).containsExactlyInAnyOrder(Metric.FILE, Metric.CLASS, Metric.FUNCTION_CALL, Metric.FUNCTION, Metric.BYTES);

        // Check that package nodes are NOT created
        var packages = root.getAll(Metric.PACKAGE);
        assertThat(packages).isEmpty();
    }

    @Test
    void testCallCoverage() {
        var root = readReport("trace32-call.xml");

        assertThat(root.getValue(Metric.FUNCTION)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(47);
            assertThat(cov.getMissed()).isEqualTo(6);
        });

        assertThat(root.getValue(Metric.FUNCTION_CALL)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(131);
            assertThat(cov.getMissed()).isEqualTo(2);
        });

        assertThat(root.getValue(Metric.BYTES)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9132);
            assertThat(cov.getMissed()).isEqualTo(1088);
        });
    }

    @Test
    void testConditionCoverage() {
        var root = readReport("trace32-cond.xml");

        assertThat(root.getValue(Metric.STMT_CC)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(500);
            assertThat(cov.getMissed()).isEqualTo(153);
        });

        assertThat(root.getValue(Metric.BYTES)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9368);
            assertThat(cov.getMissed()).isEqualTo(1136);
        });

        assertThat(root.getValue(Metric.CONDITION)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(197);
            assertThat(cov.getMissed()).isEqualTo(145);
        });
    }

    @Test
    void testDecisionCoverage() {
        var root = readReport("trace32-dec.xml");

        assertThat(root.getValue(Metric.STMT_DC)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(512);
            assertThat(cov.getMissed()).isEqualTo(141);
        });

        assertThat(root.getValue(Metric.BYTES)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9368);
            assertThat(cov.getMissed()).isEqualTo(1136);
        });

        assertThat(root.getValue(Metric.DECISION)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(123);
            assertThat(cov.getMissed()).isEqualTo(103);
        });
    }

    @Test
    void testFunctionCoverage() {
        var root = readReport("trace32-func.xml");

        assertThat(root.getValue(Metric.FUNCTION)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(52);
            assertThat(cov.getMissed()).isEqualTo(1);
        });

        assertThat(root.getValue(Metric.BYTES)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9132);
            assertThat(cov.getMissed()).isEqualTo(1088);
        });
    }

    @Test
    void testMcdcCoverage() {
        var root = readReport("trace32-mcdc.xml");

        assertThat(root.getValue(Metric.DECISION)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(53);
            assertThat(cov.getMissed()).isEqualTo(60);
        });

        assertThat(root.getValue(Metric.CONDITION)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(197);
            assertThat(cov.getMissed()).isEqualTo(145);
        });

        assertThat(root.getValue(Metric.MCDC_PAIR)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(456);
            assertThat(cov.getMissed()).isEqualTo(197);
        });

        assertThat(root.getValue(Metric.BYTES)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9368);
            assertThat(cov.getMissed()).isEqualTo(1136);
        });
    }

    @Test
    void testStatementCoverage() {
        var root = readReport("trace32-stmt.xml");

        assertThat(root.getValue(Metric.STATEMENT)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(618);
            assertThat(cov.getMissed()).isEqualTo(35);
        });

        assertThat(root.getValue(Metric.BYTES)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9780);
            assertThat(cov.getMissed()).isEqualTo(724);
        });
    }

    @Test
    void testObjectCodeCoverage() {
        var root = readReport("trace32-objcode.xml");

        assertThat(root.getValue(Metric.OBJECT_CODE)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(9132);
            assertThat(cov.getMissed()).isEqualTo(1088);
        });

        assertThat(root.getValue(Metric.BRANCH)).isPresent().get().satisfies(coverage -> {
            var cov = (Coverage)coverage;
            assertThat(cov.getCovered()).isEqualTo(375);
            assertThat(cov.getMissed()).isEqualTo(149);
        });
    }
}
