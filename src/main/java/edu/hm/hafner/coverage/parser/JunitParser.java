package edu.hm.hafner.coverage.parser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;

/**
 * Parses reports in the JUnit format into a Java object model.
 *
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class JunitParser extends AbstractTestParser {
    private static final long serialVersionUID = -5468593789018138107L;

    private static final QName TEST_SUITE = new QName("testsuite");
    private static final QName TEST_CASE = new QName("testcase");
    private static final QName FAILURE_TYPE = new QName("type");
    private static final QName ERROR = new QName("error");
    private static final QName SKIPPED = new QName("skipped");

    /**
     * Creates a new instance of {@link JunitParser}.
     */
    public JunitParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link JunitParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public JunitParser(final ProcessingMode processingMode) {
        super(processingMode, TEST_SUITE, TEST_CASE);
    }

    @Override
    TestCase readTestCase(final XMLEventReader reader, final StartElement testCaseElement,
            final String suiteName, final ModuleNode root, final String fileName) throws XMLStreamException {
        var builder = new TestCaseBuilder();

        builder.withTestName(getOptionalValueOf(testCaseElement, NAME).orElse(createId()));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement() && isFailure(event)) {
                readFailure(reader, event.asStartElement(), builder);
            }
            else if (event.isStartElement() && SKIPPED.equals(event.asStartElement().getName())) {
                builder.withStatus(TestCase.TestResult.SKIPPED);
            }
            else if (event.isEndElement() && TEST_CASE.equals(event.asEndElement().getName())) {
                var className = getOptionalValueOf(testCaseElement, CLASS_NAME).orElse(suiteName);
                builder.withClassName(className);
                var packageName = createPackageForClass(className);
                var packageNode = root.findOrCreatePackageNode(packageName);
                var classNode = packageNode.findOrCreateClassNode(className);
                classNode.addTestCase(builder.build());
                return builder.build();
            }
        }
        throw createEofException(fileName);
    }

    private boolean isFailure(final XMLEvent event) {
        var name = getElementName(event);

        return FAILURE.equals(name) || ERROR.equals(name);
    }

    private String createPackageForClass(final String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        }
        return "-";
    }

    private void readFailure(final XMLEventReader reader, final StartElement startElement,
            final TestCaseBuilder builder)
            throws XMLStreamException {
        builder.withFailure();

        getOptionalValueOf(startElement, FAILURE_TYPE).ifPresent(builder::withType);
        getOptionalValueOf(startElement, MESSAGE).ifPresent(builder::withMessage);

        var aggregatedContent = new StringBuilder();

        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            }
            else if (event.isEndElement() && isFailure(event)) {
                builder.withDescription(aggregatedContent.toString());
                return;
            }
        }
    }
}
