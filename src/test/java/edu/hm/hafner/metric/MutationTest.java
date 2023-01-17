package edu.hm.hafner.metric;

import org.junitpioneer.jupiter.DefaultLocale;

/**
 * Tests the class {@link Mutation}.
 *
 * @author Melissa Bauer
 */
@DefaultLocale("en")
class MutationTest {
//
//    @Test
//    void shouldCreateKilledMutationLeaf() {
//        Mutation mutationLeaf = new MutationBuilder().setIsDetected(true)
//                .setStatus(MutationStatus.KILLED)
//                .setLineNumber(1)
//                .setMutator(Mutator.VOID_METHOD_CALLS)
//                .setKillingTest("shouldKillMutation")
//                .createMutation();
//
//        assertThat(mutationLeaf)
//                .isDetected()
//                .hasStatus(MutationStatus.KILLED)
//                .hasLineNumber(1)
//                .hasMutator(Mutator.VOID_METHOD_CALLS)
//                .hasKillingTest("shouldKillMutation")
//                .hasToString(
//                        "[Mutation]: isDetected=true, status=KILLED, lineNumber=1, mutator=VOID_METHOD_CALLS, killingTest='shouldKillMutation'");
//    }
//
//    @Test
//    void shouldCreateSurvivedMutationLeaf() {
//        Mutation mutationLeaf = new MutationBuilder().setIsDetected(true)
//                .setStatus(MutationStatus.SURVIVED)
//                .setLineNumber(1)
//                .setMutator(Mutator.VOID_METHOD_CALLS)
//                .setKillingTest(null)
//                .createMutation();
//
//        assertThat(mutationLeaf)
//                .isDetected()
//                .hasStatus(MutationStatus.SURVIVED)
//                .hasLineNumber(1)
//                .hasMutator(Mutator.VOID_METHOD_CALLS)
//                .hasKillingTest(null)
//                .hasToString(
//                        "[Mutation]: isDetected=true, status=SURVIVED, lineNumber=1, mutator=VOID_METHOD_CALLS, killingTest='null'");
//
//    }
//
//    @Test
//    void shouldAdhereToEquals() {
//        EqualsVerifier.simple().forClass(Mutation.class).verify();
//    }
}
