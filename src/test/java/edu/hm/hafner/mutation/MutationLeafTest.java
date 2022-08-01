package edu.hm.hafner.mutation;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;
import edu.hm.hafner.model.Metric;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class MutationLeafTest {

    @Test
    void shouldCreateMutation() {
        MutationLeaf mutationLeaf = new MutationLeaf(true, MutationStatus.KILLED);
        mutationLeaf.setMutator(Mutator.VOID_METHOD_CALLS);
        mutationLeaf.setKillingTest("shouldKillMutation");
        mutationLeaf.setLineNumber(1);

        assertThat(mutationLeaf)
                .isDetected()
                .hasStatus(MutationStatus.KILLED)
                .hasLineNumber(1)
                .hasMutator(Mutator.VOID_METHOD_CALLS)
                .hasKillingTest("shouldKillMutation")
                .hasToString("[Mutation]: Killed");
    }

    /*@Test
    void shouldCreateLeaf() {
        CoverageLeaf coverageLeaf = new CoverageLeaf(Metric.LINE, COVERED);

        assertThat(coverageLeaf)
                .hasMetric(Metric.LINE)
                .hasCoverage(COVERED)
                .hasToString("[Line]: 100.00% (1/1)");
        assertThat(coverageLeaf.getCoverage(Metric.LINE)).isEqualTo(COVERED);
        assertThat(coverageLeaf.getCoverage(Metric.MODULE)).isEqualTo(Coverage.NO_COVERAGE);
    }*/

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(MutationLeaf.class).withNonnullFields("metric").verify();
    }
}