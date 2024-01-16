package edu.hm.hafner.coverage;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.util.TreeStringBuilder;

/**
 * Represents a mutation of the PIT Mutation Testing tool.
 *
 * @author Melissa Bauer
 */
@SuppressWarnings("PMD.DataClass")
public final class Mutation implements Serializable {
    private static final long serialVersionUID = -7725185756332899065L;

    private final boolean detected;
    private final MutationStatus status;
    private final int line;
    private final String mutator;
    private final String killingTest;
    private final String mutatedClass;
    private final String method;
    private final String signature;
    private final String description;

    @SuppressWarnings("checkstyle:ParameterNumber")
    private Mutation(final boolean detected, final MutationStatus status, final int line, final String mutator,
            final String killingTest, final String mutatedClass,
            final String method, final String signature, final String description) {
        this.detected = detected;
        this.status = status;
        this.line = line;

        // Intern all these values as the content is repeated very often across all mutations
        this.mutator = mutator.intern();
        this.killingTest = killingTest.intern();
        this.mutatedClass = mutatedClass.intern();
        this.method = method.intern();
        this.signature = signature.intern();
        this.description = description.intern();
    }

    public String getMutatedClass() {
        return mutatedClass;
    }

    public String getMethod() {
        return method;
    }

    public String getSignature() {
        return signature;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDetected() {
        return detected;
    }

    public MutationStatus getStatus() {
        return status;
    }

    public boolean isValid() {
        return isCovered() || isMissed();
    }

    public boolean isCovered() {
        return status.isCovered();
    }

    public boolean isMissed() {
        return status.isMissed();
    }

    public int getLine() {
        return line;
    }

    public String getMutator() {
        return mutator;
    }

    public String getKillingTest() {
        return killingTest;
    }

    /**
     * Returns if the mutation was killed.
     *
     * @return if the mutation was killed
     */
    public boolean isKilled() {
        return status.equals(MutationStatus.KILLED);
    }

    /**
     * Returns if the mutation has survived.
     *
     * @return if the mutation has survived
     */
    public boolean hasSurvived() {
        return status.equals(MutationStatus.SURVIVED);
    }

    @Override
    public String toString() {
        return "[Mutation]:"
                + " isDetected=" + detected
                + ", status=" + status
                + ", lineNumber=" + line
                + ", mutator=" + mutator
                + ", killingTest='" + killingTest + "'";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Mutation mutation = (Mutation) o;
        return detected == mutation.detected
                && line == mutation.line
                && status == mutation.status
                && Objects.equals(mutator, mutation.mutator)
                && Objects.equals(killingTest, mutation.killingTest)
                && Objects.equals(mutatedClass, mutation.mutatedClass)
                && Objects.equals(method, mutation.method)
                && Objects.equals(signature, mutation.signature)
                && Objects.equals(description, mutation.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detected, status, line, mutator, killingTest, mutatedClass, method, signature,
                description);
    }

    /**
     * Builder to create new {@link Mutation} instances.
     */
    @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class MutationBuilder {
        private boolean isDetected;
        private MutationStatus status = MutationStatus.NO_COVERAGE;
        private int line;
        private String mutator = StringUtils.EMPTY;
        private String killingTest = StringUtils.EMPTY;
        private String description = StringUtils.EMPTY;
        private String sourceFile = StringUtils.EMPTY;
        private String mutatedClass = StringUtils.EMPTY;
        private String mutatedMethod = StringUtils.EMPTY;
        private String mutatedMethodSignature = StringUtils.EMPTY;

        @CanIgnoreReturnValue
        public MutationBuilder withIsDetected(final boolean isDetected) {
            this.isDetected = isDetected;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withStatus(final MutationStatus status) {
            this.status = status;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withLine(final String line) {
            this.line = CoverageParser.parseInteger(line);

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withLine(final int line) {
            this.line = line;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withMutator(final String mutator) {
            this.mutator = mutator;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withKillingTest(final String killingTest) {
            this.killingTest = killingTest;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withDescription(final String description) {
            this.description = description;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withSourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withMutatedClass(final String mutatedClass) {
            this.mutatedClass = mutatedClass;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withMutatedMethod(final String mutatedMethod) {
            this.mutatedMethod = mutatedMethod;

            return this;
        }

        @CanIgnoreReturnValue
        public MutationBuilder withMutatedMethodSignature(final String mutatedMethodSignature) {
            this.mutatedMethodSignature = mutatedMethodSignature;

            return this;
        }

        /**
         * Builds a new mutation and adds it to the root of the tree.
         *
         * @param root
         *         the module root to add the mutations to
         * @param treeStringBuilder
         *         the tree string builder to create the file names
         */
        public void buildAndAddToModule(final ModuleNode root, final TreeStringBuilder treeStringBuilder) {
            var packageName = StringUtils.substringBeforeLast(mutatedClass, ".");
            var packageNode = root.findOrCreatePackageNode(packageName);
            var relativePath = packageName.replace('.', '/') + '/' + sourceFile;
            var fileNode = packageNode.findOrCreateFileNode(sourceFile, treeStringBuilder.intern(relativePath));
            var className = StringUtils.substringAfterLast(mutatedClass, ".");
            var classNode = fileNode.findOrCreateClassNode(className);
            var methodNode = classNode.findMethod(mutatedMethod, mutatedMethodSignature)
                    .orElseGet(() -> classNode.createMethodNode(mutatedMethod, mutatedMethodSignature));

            updateMetricForMethod(Metric.MUTATION, methodNode);
            if (status != MutationStatus.NO_COVERAGE) {
                updateMetricForMethod(Metric.TEST_STRENGTH, methodNode);
            }
            fileNode.addMutation(build());
        }

        private void updateMetricForMethod(final Metric metric, final MethodNode methodNode) {
            var existingCoverage = methodNode.getValue(metric)
                    .map(Coverage.class::cast)
                    .orElse(Coverage.nullObject(metric));
            var builder = new CoverageBuilder(existingCoverage);
            if (isDetected) {
                builder.incrementCovered();
            }
            else {
                builder.incrementMissed();
            }
            methodNode.replaceValue(builder.build());
        }

        public Mutation build() {
            return new Mutation(isDetected, status, line, mutator, killingTest,
                    mutatedClass, mutatedMethod, mutatedMethodSignature, description);
        }
    }
}
