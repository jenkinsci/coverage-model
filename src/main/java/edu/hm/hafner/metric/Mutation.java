package edu.hm.hafner.metric;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

/**
 * Class which represents a mutation of the PIT Mutation Testing tool.
 *
 * @author Melissa Bauer
 */
@SuppressWarnings("PMD.DataClass")
public final class Mutation implements Serializable {
    private static final long serialVersionUID = -7725185756332899065L;

    private final boolean detected;
    private final MutationStatus status;
    private int lineNumber;
    private String mutator;
    private String killingTest;
    private final String mutatedClass;
    private final String method;
    private final String signature;
    private final String description;

    @SuppressWarnings("checkstyle:ParameterNumber")
    private Mutation(final boolean detected, final MutationStatus status, final int lineNumber, final String mutator,
            final String killingTest, final String mutatedClass,
            final String method, final String signature, final String description) {
        this.detected = detected;
        this.status = status;
        this.lineNumber = lineNumber;
        this.mutator = mutator;
        this.killingTest = killingTest;
        this.mutatedClass = mutatedClass;
        this.method = method;
        this.signature = signature;
        this.description = description;
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

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(final int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getMutator() {
        return mutator;
    }

    public void setMutator(final String mutator) {
        this.mutator = mutator;
    }

    public void setKillingTest(final String killingTest) {
        this.killingTest = killingTest;
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
                + ", lineNumber=" + lineNumber
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
        return detected == mutation.detected && lineNumber == mutation.lineNumber && status == mutation.status
                && Objects.equals(mutator, mutation.mutator) && Objects.equals(killingTest,
                mutation.killingTest) && Objects.equals(mutatedClass, mutation.mutatedClass)
                && Objects.equals(method, mutation.method) && Objects.equals(signature,
                mutation.signature) && Objects.equals(description, mutation.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(detected, status, lineNumber, mutator, killingTest, mutatedClass, method, signature,
                description);
    }

    /**
     * Builder to create new {@link Mutation} instances.
     */
    public static class MutationBuilder {
        private boolean isDetected;
        private MutationStatus status = MutationStatus.NO_COVERAGE;
        private int lineNumber;
        private String mutator = StringUtils.EMPTY;
        private String killingTest = StringUtils.EMPTY;
        private String description = StringUtils.EMPTY;
        private String sourceFile = StringUtils.EMPTY;
        private String mutatedClass = StringUtils.EMPTY;
        private String mutatedMethod = StringUtils.EMPTY;
        private String mutatedMethodSignature = StringUtils.EMPTY;

        public void setIsDetected(final boolean isDetected) {
            this.isDetected = isDetected;
        }

        public void setStatus(final MutationStatus status) {
            this.status = status;
        }

        public void setLineNumber(final String lineNumber) {
            this.lineNumber = CoverageParser.parseInteger(lineNumber);
        }

        public void setMutator(final String mutator) {
            this.mutator = mutator;
        }

        public void setKillingTest(final String killingTest) {
            this.killingTest = killingTest;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public void setSourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;
        }

        public void setMutatedClass(final String mutatedClass) {
            this.mutatedClass = mutatedClass;
        }

        public void setMutatedMethod(final String mutatedMethod) {
            this.mutatedMethod = mutatedMethod;
        }

        public void setMutatedMethodSignature(final String mutatedMethodSignature) {
            this.mutatedMethodSignature = mutatedMethodSignature;
        }

        /**
         * Builds a new mutation and adds it to the root of the tree.
         *
         * @param root
         *         the module root to add the mutations to
         */
        public void buildAndAddToModule(final ModuleNode root) {
            String packageName = StringUtils.substringBeforeLast(mutatedClass, ".");
            String className = StringUtils.substringAfterLast(mutatedClass, ".");
            var packageNode = root.findPackage(packageName).orElseGet(() -> root.createPackageNode(packageName));
            var fileNode = packageNode.findFile(sourceFile).orElseGet(() -> packageNode.createFileNode(sourceFile));
            var classNode = fileNode.findClass(className).orElseGet(() -> fileNode.createClassNode(className));
            var methodNode = classNode.findMethod(mutatedMethod, mutatedMethodSignature)
                    .orElseGet(() -> classNode.createMethodNode(mutatedMethod, mutatedMethodSignature));

            var coverage = methodNode.getValue(Metric.MUTATION)
                    .map(Coverage.class::cast)
                    .orElse(Coverage.nullObject(Metric.MUTATION));
            var builder = new CoverageBuilder(coverage);
            if (isDetected) {
                builder.incrementCovered();
            }
            else {
                builder.incrementMissed();
            }
            methodNode.replaceValue(builder.build());
            methodNode.addMutation(new Mutation(isDetected, status, lineNumber, mutator, killingTest,
                    mutatedClass, mutatedMethod, mutatedMethodSignature, description));
        }
    }
}
