package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.TreeStringBuilder;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

/**
 * Additional test-cases for interacting with objects of
 * class {@link Mutation} and its attributes.
 * These might also be tested by adding additional asserts
 * in the tests for the {@link Node} class --> {@link NodeTest}.
 */
class MutationTest {
    private Mutation createDummyMutation(final String identifier, final boolean detected,
                                         final MutationStatus status) {
        return new Mutation.MutationBuilder()
                .withMutatedClass("Class%s.class".formatted(identifier))
                .withSourceFile("Class%s.java".formatted(identifier))
                .withMutatedMethod("Method%s".formatted(identifier))
                .withLine("1")
                .withStatus(status)
                .withIsDetected(detected)
                .withMutator("Mutator%s".formatted(identifier))
                .withKillingTest("Test%s".formatted(identifier))
                .withMutatedMethodSignature("Signature%s".formatted(identifier))
                .withDescription("Description%s".formatted(identifier))
                .build();
    }

    @Test
    void shouldReturnCorrectAttributeValues() {
        var mutationA = createDummyMutation("A", false, MutationStatus.NO_COVERAGE);
        var mutationB = createDummyMutation("B", true, MutationStatus.KILLED);
        var mutationC = createDummyMutation("C", false, MutationStatus.RUN_ERROR);

        assertThat(mutationA)
                .hasMutatedClass("ClassA.class")
                .hasMethod("MethodA")
                .hasLine(1)
                .hasStatus(MutationStatus.NO_COVERAGE)
                .isNotDetected()
                .hasMutator("MutatorA")
                .hasKillingTest("TestA")
                .hasSignature("SignatureA")
                .hasDescription("DescriptionA")
                .isValid()
                .isNotKilled();
        assertThat(mutationB)
                .hasMutatedClass("ClassB.class")
                .hasMethod("MethodB")
                .hasLine(1)
                .hasStatus(MutationStatus.KILLED)
                .isDetected()
                .hasMutator("MutatorB")
                .hasKillingTest("TestB")
                .hasSignature("SignatureB")
                .hasDescription("DescriptionB")
                .isValid()
                .isKilled();
        assertThat(mutationC)
                .hasMutatedClass("ClassC.class")
                .hasMethod("MethodC")
                .hasLine(1)
                .hasStatus(MutationStatus.RUN_ERROR)
                .isNotDetected()
                .hasMutator("MutatorC")
                .hasKillingTest("TestC")
                .hasSignature("SignatureC")
                .hasDescription("DescriptionC")
                .isNotValid()
                .isNotKilled();
        assertThat(mutationA.getStatus())
                .isNotDetected();
        assertThat(mutationB.getStatus())
                .isDetected();
    }

    @Test
    void shouldBuildAndAddToModule() {
        var mutationBuilder = new Mutation.MutationBuilder()
                .withMutatedClass("module.package.file.Class")
                .withSourceFile("Class.java")
                .withMutatedMethod("Method")
                .withLine("1")
                .withStatus(MutationStatus.NO_COVERAGE)
                .withIsDetected(false)
                .withMutator("Mutator")
                .withKillingTest("Test")
                .withMutatedMethodSignature("Signature")
                .withDescription("Description");
        var moduleNode = new ModuleNode("module");

        mutationBuilder.buildAndAddToModule(moduleNode, new TreeStringBuilder());

        assertThat(moduleNode.getAllFileNodes())
                .hasSize(1);
        assertThat(moduleNode.getAllFileNodes().get(0))
                .hasName("Class.java")
                .hasMutations(mutationBuilder.build());
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Mutation.class).verify();
    }
}
