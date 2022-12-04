package edu.hm.hafner.metric;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.math.Fraction;

import edu.hm.hafner.util.Ensure;

/**
 * Leaf which represents a mutation.
 *
 * @author Melissa Bauer
 */
// FIXME: Wouldn't it make sense to separate the value from the details? Same is true for coverage
public final class MutationValue extends Value {
    private static final long serialVersionUID = -7725185756332899065L;

    private final List<Mutation> mutations = new ArrayList<>();
    private final int killed;
    private final int survived;

    /**
     * Creates a new {@link MutationValue} and adds given mutation to the list.
     *
     * @param mutation
     *         the mutation to add
     */
    public MutationValue(final Mutation mutation) {
        super(Metric.MUTATION);

        this.mutations.add(mutation);
        if (mutation.isKilled()) {
            this.killed = 1;
            this.survived = 0;
        }
        else {
            this.survived = 1;
            this.killed = 0;
        }
    }

    /**
     * Creates a new {@link MutationValue} with given mutations, killed and survived amount.
     *
     * @param mutations
     *         the mutations
     * @param killed
     *         the amount of killed mutations
     * @param survived
     *         the amount of survived mutations
     */
    public MutationValue(final List<Mutation> mutations, final int killed, final int survived) {
        super(Metric.MUTATION);

        this.mutations.addAll(mutations);
        this.killed = killed;
        this.survived = survived;
    }

    public List<Mutation> getMutations() {
        return List.copyOf(mutations);
    }

    public int getKilled() {
        return killed;
    }

    public int getSurvived() {
        return survived;
    }

    public int getTotal() {
        return killed + survived;
    }

    /**
     * Returns the covered percentage as a {@link Fraction} in the range of {@code [0, 1]}.
     *
     * @return the covered percentage
     */
    public Fraction getCoveredPercentage() {
        if (getTotal() == 0) {
            return Fraction.ZERO;
        }
        return Fraction.getFraction(getKilled(), getTotal());
    }

    @Override
    public MutationValue add(final Value other) {
        if (hasSameMetric(other) && other instanceof MutationValue) {
            var compositeMutations = new ArrayList<>(getMutations());
            var summand = (MutationValue) other;
            compositeMutations.addAll(summand.getMutations());

            return new MutationValue(compositeMutations,
                    getKilled() + summand.getKilled(),
                    getSurvived() + summand.getSurvived());
        }

        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public Fraction delta(final Value other) {
        if (hasSameMetric(other) && other instanceof MutationValue) {
            return new SafeFraction(getCoveredPercentage()).subtract(((MutationValue) other).getCoveredPercentage());
        }
        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    private MutationValue castAndMap(final Value other, final UnaryOperator<MutationValue> mapper) {
        if (hasSameMetric(other) && other instanceof MutationValue) {
            return mapper.apply((MutationValue) other);
        }

        throw new IllegalArgumentException(String.format("Cannot cast incompatible types: %s and %s", this, other));
    }

    @Override
    public MutationValue max(final Value other) {
        return castAndMap(other, this::computeMax);
    }

    private MutationValue computeMax(final MutationValue otherMutationValue) {
        Ensure.that(getTotal() == otherMutationValue.getTotal())
                .isTrue("Cannot compute maximum of %s and %s since total differs", this, otherMutationValue);
        if (getKilled() >= otherMutationValue.getKilled()) {
            return this;
        }
        return otherMutationValue;
    }

    /**
     * Returns whether this mutation value is below the given threshold. The threshold is a percentage in the range of [0,
     * 100].
     *
     * @param threshold
     *         the threshold in the range of [0, 100]
     *
     * @return {@code true}, if this value is below the specified threshold
     */
    @Override
    public boolean isBelowThreshold(final double threshold) {
        return getCoveredPercentage().doubleValue() * 100 < threshold;
    }

    @Override
    public String serialize() {
        return String.format("%s: %d/%d", getMetric(), getKilled(), getTotal());
    }

    @Override
    public String toString() {
        return String.format("%s: %s (%d/%d)", getMetric(), getCoveredPercentage(), getKilled(), getTotal());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        MutationValue that = (MutationValue) o;
        return killed == that.killed && survived == that.survived && mutations.equals(that.mutations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), mutations, killed, survived);
    }
}
