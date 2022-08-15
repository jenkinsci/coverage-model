package edu.hm.hafner.mutation;

/**
 * Enum which maps the path of a mutator to its short name. Source of path and short names: <a
 * href="https://github.com/hcoles/pitest/tree/master/pitest/src/main/java/org/pitest/mutationtest/engine/gregor/mutators">...</a>
 * and <a
 * href="https://github.com/hcoles/pitest/tree/master/pitest/src/main/java/org/pitest/mutationtest/engine/gregor/mutators/returns">...</a>
 *
 * @author Melissa Buer
 */
public enum Mutator {

    CONDITIONALS_BOUNDARY("org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator"),
    CONSTRUCTOR_CALLS("org.pitest.mutationtest.engine.gregor.mutators.ConstructorCallMutator"),
    INCREMENTS("org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator"),
    INVERT_NEGS("org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator"),
    MATH("org.pitest.mutationtest.engine.gregor.mutators.MathMutator"),
    NEGATE_CONDITIONALS("org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator"),
    NON_VOID_METHOD_CALLS("org.pitest.mutationtest.engine.gregor.mutators.NonVoidMethodCallMutator"),
    RETURN_VALS("org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator"),
    VOID_METHOD_CALLS("org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator"),

    FALSE_RETURNS("org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanFalseReturnValsMutator"),
    TRUE_RETURNS("org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanTrueReturnValsMutator"),
    EMPTY_RETURNS("org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator"),
    NULL_RETURNS("org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator"),
    PRIMITIVE_RETURNS("org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator"),

    NOT_SPECIFIED("");

    private final String path;

    /**
     * Maps the given path to the short name of a mutator.
     *
     * @param mutatorPath
     *         the path given in mutator element in the pitest report
     */
    Mutator(final String mutatorPath) {
        this.path = mutatorPath;
    }

    /**
     * Determines the mutator based on the provided path.
     *
     * @param mutatorPath
     *         the path given in mutator element in the pitest report
     *
     * @return the mutator
     */
    public static Mutator fromPath(final String mutatorPath) {

        for (Mutator mutator : values()) {
            if ((mutator.path).equals(mutatorPath)) {
                return mutator;
            }
        }
        return NOT_SPECIFIED;
    }
}
