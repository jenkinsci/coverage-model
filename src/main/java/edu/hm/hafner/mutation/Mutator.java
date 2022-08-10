package edu.hm.hafner.mutation;

/**
 * Enum which maps the path of a mutator to its short name. Source of path and short names:
 * <a href="https://github.com/hcoles/pitest/tree/master/pitest/src/main/java/org/pitest/mutationtest/engine/gregor/mutators">...</a> and
 * <a href="https://github.com/hcoles/pitest/tree/master/pitest/src/main/java/org/pitest/mutationtest/engine/gregor/mutators/returns">...</a>
 *
 * @author Melissa Buer
 */
public enum Mutator {

    CONDITIONALS_BOUNDARY,
    CONSTRUCTOR_CALLS,
    INCREMENTS,
    INVERT_NEGS,
    MATH,
    NEGATE_CONDITIONALS,
    NON_VOID_METHOD_CALLS,
    RETURN_VALS,
    VOID_METHOD_CALLS,

    FALSE_RETURNS,
    TRUE_RETURNS,
    EMPTY_RETURNS,
    NULL_RETURNS,
    PRIMITIVE_RETURNS,

    NOT_SPECIFIED;

    /**
     * Maps the given path to the short name of a mutator.
     *
     * @param mutator
     *         the path given in mutator element in the pitest report
     *
     * @return the short name of the mutator
     */
    public static Mutator getMutator(final String mutator) {
        switch (mutator) {
            case "org.pitest.mutationtest.engine.gregor.mutators.ConditionalsBoundaryMutator":
                return CONDITIONALS_BOUNDARY;
            case "org.pitest.mutationtest.engine.gregor.mutators.ConstructorCallMutator":
                return CONSTRUCTOR_CALLS;
            case "org.pitest.mutationtest.engine.gregor.mutators.IncrementsMutator":
                return INCREMENTS;
            case "org.pitest.mutationtest.engine.gregor.mutators.InvertNegsMutator":
                return INVERT_NEGS;
            case "org.pitest.mutationtest.engine.gregor.mutators.MathMutator":
                return MATH;
            case "org.pitest.mutationtest.engine.gregor.mutators.NegateConditionalsMutator":
                return NEGATE_CONDITIONALS;
            case "org.pitest.mutationtest.engine.gregor.mutators.NonVoidMethodCallMutator":
                return NON_VOID_METHOD_CALLS;
            case "org.pitest.mutationtest.engine.gregor.mutators.ReturnValsMutator":
                return RETURN_VALS;
            case "org.pitest.mutationtest.engine.gregor.mutators.VoidMethodCallMutator":
                return VOID_METHOD_CALLS;

            case "org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanFalseReturnValsMutator":
                return FALSE_RETURNS;
            case "org.pitest.mutationtest.engine.gregor.mutators.returns.BooleanTrueReturnValsMutator":
                return TRUE_RETURNS;
            case "org.pitest.mutationtest.engine.gregor.mutators.returns.EmptyObjectReturnValsMutator":
                return EMPTY_RETURNS;
            case "org.pitest.mutationtest.engine.gregor.mutators.returns.NullReturnValsMutator":
                return NULL_RETURNS;
            case "org.pitest.mutationtest.engine.gregor.mutators.returns.PrimitiveReturnsMutator":
                return PRIMITIVE_RETURNS;

            default:
                return NOT_SPECIFIED;
        }
    }

}
