package edu.hm.hafner.metric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

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
    void shouldCompareWithThreshold() {
        var builder = new CoverageBuilder().setMetric(Metric.LINE);

        MutationValue zero = new MutationValue(Collections.emptyList(), 0, 2);
        MutationValue fifty = new MutationValue(Collections.emptyList(), 2, 2);
        MutationValue hundred = new MutationValue(Collections.emptyList(), 2, 0);

        assertThat(zero.isBelowThreshold(0)).isFalse();
        assertThat(zero.isBelowThreshold(0.1)).isTrue();
        assertThat(fifty.isBelowThreshold(50)).isFalse();
        assertThat(fifty.isBelowThreshold(50.1)).isTrue();
        assertThat(hundred.isBelowThreshold(100)).isFalse();
        assertThat(hundred.isBelowThreshold(100.1)).isTrue();
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

        assertThat(mutationValue.getMutations()).hasSize(5).doesNotContain(mutationToAdd);
    }

    @Test
    void shouldGetMax() {
        MutationValue firstMutationValue = new MutationValue(createMutations(), 3, 3);
        MutationValue secondMutationValue = new MutationValue(createMutations(), 4, 2);

        MutationValue maxValue = firstMutationValue.max(secondMutationValue);

        assertThat(maxValue).isEqualTo(secondMutationValue);
    }

    @Test
    void shouldComputeDelta() {
        MutationValue worse = new MutationValue(createMutations(), 0, 2);
        MutationValue ok = new MutationValue(createMutations(), 1, 1);
        MutationValue better = new MutationValue(createMutations(), 2, 0);

        assertThat(worse.delta(better).doubleValue()).isEqualTo(getDelta("-1/1"));
        assertThat(better.delta(worse).doubleValue()).isEqualTo(getDelta("1/1"));
        assertThat(worse.delta(ok).doubleValue()).isEqualTo(getDelta("-1/2"));
        assertThat(ok.delta(worse).doubleValue()).isEqualTo(getDelta("1/2"));
    }

    private static double getDelta(final String value) {
        return Fraction.getFraction(value).doubleValue();
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
