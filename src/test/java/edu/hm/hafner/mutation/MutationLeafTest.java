package edu.hm.hafner.mutation;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Tests the class {@link MutationLeaf}.
 *
 * @author Melissa Bauer
 */
@DefaultLocale("en")
class MutationLeafTest {

    @Test
    void shouldCreateKilledMutationLeaf() {
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
                .hasToString("[Mutation]: isDetected=true, status=KILLED, lineNumber=1, mutator=VOID_METHOD_CALLS, killingTest='shouldKillMutation'");

        assertThat(mutationLeaf.getResult()).hasKilled(1);
        assertThat(mutationLeaf.getResult()).hasSurvived(0);
    }

    @Test
    void shouldCreateSurvivedMutationLeaf() {
        MutationLeaf mutationLeaf = new MutationLeaf(true, MutationStatus.SURVIVED);
        mutationLeaf.setMutator(Mutator.VOID_METHOD_CALLS);
        mutationLeaf.setKillingTest("shouldNotKillMutation");
        mutationLeaf.setLineNumber(1);

        assertThat(mutationLeaf)
                .isDetected()
                .hasStatus(MutationStatus.SURVIVED)
                .hasLineNumber(1)
                .hasMutator(Mutator.VOID_METHOD_CALLS)
                .hasKillingTest("shouldNotKillMutation")
                .hasToString("[Mutation]: isDetected=true, status=SURVIVED, lineNumber=1, mutator=VOID_METHOD_CALLS, killingTest='shouldNotKillMutation'");

        assertThat(mutationLeaf.getResult()).hasKilled(0);
        assertThat(mutationLeaf.getResult()).hasSurvived(1);

    }

    /**
     * Tests equals() method.
     */
    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(MutationLeaf.class).withNonnullFields("metric", "isDetected", "status", "lineNumber", "mutator", "killingTest").verify();
    }
}