package edu.hm.hafner.metric;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

import static edu.hm.hafner.metric.assertions.Assertions.*;

/**
 * Tests the class {@link Mutation}.
 *
 * @author Melissa Bauer
 */
@DefaultLocale("en")
class MutationValueTest {

    @Test
    void shouldCreateMutationValueWithMutation() {
        Mutation mutationLeaf = new Mutation(true, MutationStatus.KILLED);
        MutationValue mutationValue = new MutationValue(mutationLeaf);

        assertThat(mutationValue)
                .hasMetric(Metric.MUTATION)
                .hasKilled(1)
                .hasSurvived(0)
                .hasMutations(mutationLeaf);
    }

    @Test
    void shouldCreateMutationValueWithMutationsAndKilledAndSurvived() {
        List<Mutation> mutations = createMutations();

        MutationValue mutationValue = new MutationValue(mutations, 3, 2);

        assertThat(mutationValue)
                .hasKilled(3)
                .hasSurvived(2)
                .hasMutations(mutations);
    }

    @Test
    void shouldAddAnotherMutationValue() {
        List<Mutation> mutations = createMutations();
        MutationValue mutationValue = new MutationValue(mutations, 3, 3);
        Mutation mutationToAdd = new Mutation(false, MutationStatus.SURVIVED);
        MutationValue mutationValueToAdd = new MutationValue(mutationToAdd);

        mutations.add(mutationToAdd);
        MutationValue addedValues = mutationValue.add(mutationValueToAdd);

        assertThat(addedValues)
                .hasKilled(3)
                .hasSurvived(4)
                .hasMutations(mutations);
    }

    @Test
    void shouldGetMax() {
        MutationValue firstMutationValue = new MutationValue(createMutations(), 3, 3);
        MutationValue secondMutationValue = new MutationValue(createMutations(), 4, 2);

        MutationValue maxValue = firstMutationValue.max(secondMutationValue);

        assertThat(maxValue).isEqualTo(secondMutationValue);
    }

    private List<Mutation> createMutations() {
        List<Mutation> mutations = new ArrayList<>();
        mutations.add(new Mutation(true, MutationStatus.KILLED, 1, Mutator.VOID_METHOD_CALLS,
                "shouldKillMutation"));
        mutations.add(new Mutation(true, MutationStatus.KILLED, 1, Mutator.VOID_METHOD_CALLS,
                "shouldKillMutation"));
        mutations.add(new Mutation(true, MutationStatus.SURVIVED, 1, Mutator.VOID_METHOD_CALLS,
                null));
        mutations.add(new Mutation(true, MutationStatus.SURVIVED, 1, Mutator.VOID_METHOD_CALLS,
                null));
        mutations.add(new Mutation(true, MutationStatus.KILLED, 1, Mutator.VOID_METHOD_CALLS,
                "shouldKillMutation"));
        return mutations;
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(MutationValue.class).withRedefinedSuperclass().suppress(Warning.NONFINAL_FIELDS).verify();
    }
}
