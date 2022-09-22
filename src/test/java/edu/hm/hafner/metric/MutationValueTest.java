package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link MutationValue}.
 *
 * @author Melissa Bauer
 */
@DefaultLocale("en")
class MutationValueTest {

    @Test
    void shouldCreateKilledMutationLeaf() {
        MutationValue mutationLeaf = new MutationValue(true, MutationStatus.KILLED);
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
        MutationValue mutationLeaf = new MutationValue(true, MutationStatus.SURVIVED);
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

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(MutationValue.class).withRedefinedSuperclass().suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
