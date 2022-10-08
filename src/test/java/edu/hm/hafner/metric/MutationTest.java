package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link Mutation}.
 *
 * @author Melissa Bauer
 */
@DefaultLocale("en")
class MutationTest {

    @Test
    void shouldCreateKilledMutationLeaf() {
        Mutation mutationLeaf = new Mutation(true, MutationStatus.KILLED, 1, Mutator.VOID_METHOD_CALLS,
                "shouldKillMutation");

        assertThat(mutationLeaf)
                .isDetected()
                .hasStatus(MutationStatus.KILLED)
                .hasLineNumber(1)
                .hasMutator(Mutator.VOID_METHOD_CALLS)
                .hasKillingTest("shouldKillMutation")
                .hasToString(
                        "[Mutation]: isDetected=true, status=KILLED, lineNumber=1, mutator=VOID_METHOD_CALLS, killingTest='shouldKillMutation'");
    }

    @Test
    void shouldCreateSurvivedMutationLeaf() {
        Mutation mutationLeaf = new Mutation(true, MutationStatus.SURVIVED, 1, Mutator.VOID_METHOD_CALLS,
                null);

        assertThat(mutationLeaf)
                .isDetected()
                .hasStatus(MutationStatus.SURVIVED)
                .hasLineNumber(1)
                .hasMutator(Mutator.VOID_METHOD_CALLS)
                .hasKillingTest(null)
                .hasToString(
                        "[Mutation]: isDetected=true, status=SURVIVED, lineNumber=1, mutator=VOID_METHOD_CALLS, killingTest='null'");

    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(Mutation.class).verify();
    }
}
