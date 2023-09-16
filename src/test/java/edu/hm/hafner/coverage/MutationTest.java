package edu.hm.hafner.coverage;

import edu.hm.hafner.util.TreeStringBuilder;
import org.junit.jupiter.api.Test;

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
                .setMutatedClass(String.format("Class%s.class", identifier))
                .setSourceFile(String.format("Class%s.java", identifier))
                .setMutatedMethod(String.format("Method%s", identifier))
                .setLine("1")
                .setStatus(status)
                .setIsDetected(detected)
                .setMutator(String.format("Mutator%s", identifier))
                .setKillingTest(String.format("Test%s", identifier))
                .setMutatedMethodSignature(String.format("Signature%s", identifier))
                .setDescription(String.format("Description%s", identifier))
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
                .setMutatedClass("module.package.file.Class")
                .setSourceFile("Class.java")
                .setMutatedMethod("Method")
                .setLine("1")
                .setStatus(MutationStatus.NO_COVERAGE)
                .setIsDetected(false)
                .setMutator("Mutator")
                .setKillingTest("Test")
                .setMutatedMethodSignature("Signature")
                .setDescription("Description");
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