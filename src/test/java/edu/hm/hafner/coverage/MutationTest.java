package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

import edu.hm.hafner.util.TreeStringBuilder;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Additional test-cases for interacting with objects of class {@link Mutation} and its attributes. These might also be
 * tested by adding additional asserts in the tests for the {@link Node} class --> {@link NodeTest}.
 */
class MutationTest {
    private Mutation createDummyMutation(final String identifier, final MutationStatus status) {
        return new Mutation.MutationBuilder()
                .withMutatedClass("Class%s.class".formatted(identifier))
                .withSourceFile("Class%s.java".formatted(identifier))
                .withMutatedMethod("Method%s".formatted(identifier))
                .withLine("1")
                .withStatus(status)
                .withMutator("Mutator%s".formatted(identifier))
                .withKillingTest("Test%s".formatted(identifier))
                .withMutatedMethodSignature("Signature%s".formatted(identifier))
                .withDescription("Description%s".formatted(identifier))
                .build();
    }

    @Test
    void shouldReturnCorrectAttributeValues() {
        var mutationA = createDummyMutation("A", MutationStatus.NO_COVERAGE);
        var mutationB = createDummyMutation("B", MutationStatus.KILLED);
        var mutationC = createDummyMutation("C", MutationStatus.RUN_ERROR);

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
        assertThat(mutationA.getStatus()).isNotDetected();
        assertThat(mutationB.getStatus()).isDetected();
    }

    @Test
    void shouldBuildAndAddToModule() {
        var mutationBuilder = new Mutation.MutationBuilder()
                .withMutatedClass("module.package.file.Class")
                .withSourceFile("Class.java")
                .withMutatedMethod("Method")
                .withLine("1")
                .withStatus(MutationStatus.NO_COVERAGE)
                .withMutator("Mutator")
                .withKillingTest("Test")
                .withMutatedMethodSignature("Signature")
                .withDescription("Description");
        var moduleNode = new ModuleNode("module");

        mutationBuilder.buildAndAddToModule(moduleNode, new TreeStringBuilder());

        assertThat(moduleNode.getAllFileNodes()).hasSize(1);
        assertThat(moduleNode.getAllFileNodes().getFirst())
                .hasName("Class.java")
                .hasMutations(mutationBuilder.build());
        assertThat(moduleNode.getAll(Metric.PACKAGE)).extracting(Node::getName).containsExactly("module.package.file");
        assertThat(moduleNode.getAll(Metric.CLASS))
                .extracting(Node::getName)
                .containsExactly("module.package.file.Class");
    }

    /**
     * Verifies that the detection state, the validity, and the relevance for the test strength are derived from the
     * status of the mutation.
     */
    @Test
    void shouldDeriveDetectionFromStatus() {
        assertThat(createDummyMutation("K", MutationStatus.KILLED))
                .isDetected()
                .isValid()
                .isRelevantForTestStrength();
        assertThat(createDummyMutation("T", MutationStatus.TIMED_OUT))
                .isDetected()
                .isValid()
                .isRelevantForTestStrength();
        assertThat(createDummyMutation("S", MutationStatus.SURVIVED))
                .isNotDetected()
                .isValid()
                .isRelevantForTestStrength();
        assertThat(createDummyMutation("N", MutationStatus.NO_COVERAGE))
                .isNotDetected()
                .isValid()
                .isNotRelevantForTestStrength();
        for (MutationStatus status :
                List.of(MutationStatus.NON_VIABLE, MutationStatus.MEMORY_ERROR, MutationStatus.RUN_ERROR)) {
            assertThat(createDummyMutation("I", status))
                    .isNotDetected()
                    .isNotValid()
                    .isNotRelevantForTestStrength();
        }
    }

    /** Verifies that invalid mutations are neither part of the mutation coverage nor of the test strength. */
    @Test
    void shouldIgnoreInvalidMutationsInMetricsOfMethod() {
        var moduleNode = new ModuleNode("module");
        var builder = new Mutation.MutationBuilder()
                .withMutatedClass("module.package.file.Class")
                .withSourceFile("Class.java")
                .withMutatedMethod("Method")
                .withMutatedMethodSignature("Signature");
        var treeStringBuilder = new TreeStringBuilder();

        builder.withLine("1").withStatus(MutationStatus.KILLED).buildAndAddToModule(moduleNode, treeStringBuilder);
        builder.withLine("2").withStatus(MutationStatus.TIMED_OUT).buildAndAddToModule(moduleNode, treeStringBuilder);
        builder.withLine("3").withStatus(MutationStatus.SURVIVED).buildAndAddToModule(moduleNode, treeStringBuilder);
        builder.withLine("4").withStatus(MutationStatus.NO_COVERAGE).buildAndAddToModule(moduleNode, treeStringBuilder);
        builder.withLine("5").withStatus(MutationStatus.RUN_ERROR).buildAndAddToModule(moduleNode, treeStringBuilder);
        builder.withLine("6").withStatus(MutationStatus.NON_VIABLE).buildAndAddToModule(moduleNode, treeStringBuilder);

        assertThat(moduleNode.getValue(Metric.MUTATION)).contains(Coverage.valueOf("MUTATION: 2/4"));
        assertThat(moduleNode.getValue(Metric.TEST_STRENGTH)).contains(Coverage.valueOf("TEST_STRENGTH: 2/3"));
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(Mutation.class).verify();
    }
}
