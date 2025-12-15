package edu.hm.hafner.coverage;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.util.Generated;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a test case that has been executed.
 *
 * @author Ullrich Hafner
 */
public final class TestCase implements Serializable {
    @Serial
    private static final long serialVersionUID = -2181204291759959155L;

    private final String testName;
    private final String className;
    private final TestResult result;
    private final String type;
    private final String message;
    private final String description;

    private TestCase(final String testName, final String className, final TestResult result,
            final String type, final String message, final String description) {
        this.testName = testName;
        this.className = className.intern();
        this.result = result;
        this.type = type;
        this.message = message;
        this.description = description;
    }

    public String getTestName() {
        return testName;
    }

    public String getClassName() {
        return className;
    }

    public TestResult getResult() {
        return result;
    }

    public boolean isPassed() {
        return result == TestResult.PASSED;
    }

    public boolean isFailed() {
        return result == TestResult.FAILED;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    @Override @Generated
    public String toString() {
        return "TestCase{testName='" + testName + '\'' + ", className='" + className + '\'' + ", status=" + result
                + ", type='" + type + '\'' + ", message='" + message + '\'' + ", description='" + description + '\''
                + '}';
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var testCase = (TestCase) o;
        return Objects.equals(testName, testCase.testName)
                && Objects.equals(className, testCase.className)
                && result == testCase.result
                && Objects.equals(type, testCase.type)
                && Objects.equals(message, testCase.message)
                && Objects.equals(description, testCase.description);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(testName, className, result, type, message, description);
    }

    /**
     * Builder to create new {@link TestCase} instances.
     */
    @SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:HiddenField", "ParameterHidesMemberVariable"})
    public static class TestCaseBuilder {
        private TestResult status = TestResult.PASSED;
        private String testName = StringUtils.EMPTY;
        private String className = StringUtils.EMPTY;
        private String type = StringUtils.EMPTY;
        private String message = StringUtils.EMPTY;
        private String description = StringUtils.EMPTY;

        @CanIgnoreReturnValue
        public TestCaseBuilder withStatus(final TestResult status) {
            this.status = status;

            return this;
        }

        @CanIgnoreReturnValue
        public TestCaseBuilder withTestName(final String testName) {
            this.testName = testName;

            return this;
        }

        @CanIgnoreReturnValue
        public TestCaseBuilder withClassName(final String className) {
            this.className = className;

            return this;
        }

        @CanIgnoreReturnValue
        public TestCaseBuilder withType(final String type) {
            this.type = type;

            return this;
        }

        @CanIgnoreReturnValue
        public TestCaseBuilder withMessage(final String message) {
            this.message = message;

            return this;
        }

        @CanIgnoreReturnValue
        public TestCaseBuilder withDescription(final String description) {
            this.description = description;

            return this;
        }

        @CanIgnoreReturnValue
        public TestCaseBuilder withFailure() {
            status = TestResult.FAILED;

            return this;
        }

        public TestCase build() {
            return new TestCase(testName, className, status, type, message, description);
        }
    }

    /**
     * The result of a test case.
     */
    public enum TestResult {
        PASSED,
        FAILED,
        SKIPPED,
        ABORTED
    }
}
