package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import java.io.Serial;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Parses reports in the JUnit format into a Java object model.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class JunitParser extends AbstractTestParser {
    @Serial
    private static final long serialVersionUID = -5468593789018138107L;

    private static final QName TEST_SUITE = new QName("testsuite");
    private static final QName TEST_CASE = new QName("testcase");
    private static final QName FAILURE_TYPE = new QName("type");
    private static final QName ERROR = new QName("error");
    private static final QName SKIPPED = new QName("skipped");

    /** Creates a new instance of {@link JunitParser}. */
    public JunitParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link JunitParser}.
     *
     * @param processingMode determines whether to ignore errors
     */
    public JunitParser(final ProcessingMode processingMode) {
        super(processingMode, TEST_SUITE, TEST_CASE);
    }

    @Override
    TestCase readTestCase(
            final XMLEventReader reader,
            final StartElement testCaseElement,
            final String suiteName,
            final ModuleNode root,
            final String fileName)
            throws XMLStreamException {
        var builder = new TestCaseBuilder();

        builder.withTestName(getOptionalValueOf(testCaseElement, NAME).orElse(createId()));

        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement() && isFailure(event)) {
                readFailure(reader, event.asStartElement(), builder);
            } else if (event.isStartElement()
                    && SKIPPED.equals(event.asStartElement().getName())) {
                builder.withStatus(TestCase.TestResult.SKIPPED);
            } else if (event.isEndElement()
                    && TEST_CASE.equals(event.asEndElement().getName())) {
                var className = createClassName(
                        getOptionalValueOf(testCaseElement, CLASS_NAME).orElse(suiteName), suiteName);
                builder.withClassName(className);
                var packageName = createPackageForClass(className);
                var packageNode = root.findOrCreatePackageNode(packageName);
                var classNode = packageNode.findOrCreateClassNode(className);
                addTestCase(classNode, builder.build());
                return builder.build();
            }
        }
        throw createEofException(fileName);
    }

    private boolean isFailure(final XMLEvent event) {
        var name = getElementName(event);

        return FAILURE.equals(name) || ERROR.equals(name);
    }

    /**
     * Some tools (e.g., ArchUnit) report the simple class name in the attribute {@code className} only, while the suite
     * name contains the fully qualified class name. In this case, the suite name is used as class name so that the
     * package of the test class is available.
     *
     * @param className the class name of the test case
     * @param suiteName the name of the test suite
     * @return the fully qualified class name if available, the original class name otherwise
     */
    private String createClassName(final String className, final String suiteName) {
        if (className.indexOf('.') < 0 && suiteName.endsWith("." + className)) {
            return suiteName;
        }
        return className;
    }

    private String createPackageForClass(final String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        }
        return "-";
    }

    private void readFailure(
            final XMLEventReader reader, final StartElement startElement, final TestCaseBuilder builder)
            throws XMLStreamException {
        builder.withFailure();

        getOptionalValueOf(startElement, FAILURE_TYPE).ifPresent(builder::withType);
        getOptionalValueOf(startElement, MESSAGE).ifPresent(builder::withMessage);

        var aggregatedContent = new StringBuilder();

        while (true) {
            var event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            } else if (event.isEndElement() && isFailure(event)) {
                builder.withDescription(aggregatedContent.toString());
                return;
            }
        }
    }
}
