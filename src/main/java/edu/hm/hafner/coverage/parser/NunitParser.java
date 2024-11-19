package edu.hm.hafner.coverage.parser;

import java.io.Serial;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;

/**
 * Parses reports in the
 * <a href="https://docs.nunit.org/articles/nunit/technical-notes/usage/Test-Result-XML-Format.html">NUnit format</a>
 * into a Java object model.
 *
 * @author Valentin Delaye
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class NunitParser extends AbstractTestParser {
    @Serial
    private static final long serialVersionUID = -5468593789018138107L;

    private static final QName TEST_SUITE = new QName("test-suite");
    private static final QName TEST_CASE = new QName("test-case");
    private static final QName RESULT = new QName("result");
    private static final String PASSED = "Passed";
    private static final String FAILED = "Failed";
    private static final String SKIPPED = "Skipped";

    /**
     * Creates a new instance of {@link NunitParser}.
     */
    public NunitParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link NunitParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public NunitParser(final ProcessingMode processingMode) {
        super(processingMode, TEST_SUITE, TEST_CASE);
    }

    @Override
    TestCase readTestCase(final XMLEventReader reader, final StartElement testCaseElement,
            final String suiteName, final ModuleNode root, final String fileName) throws XMLStreamException {
        var builder = new TestCaseBuilder();

        builder.withTestName(getOptionalValueOf(testCaseElement, NAME).orElse(createId()));

        readStatus(testCaseElement, builder);

        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement() && isFailure(event)) {
                readFailure(reader, builder);
            }
            else if (event.isEndElement() && TEST_CASE.equals(event.asEndElement().getName())) {
                var className = getOptionalValueOf(testCaseElement, CLASS_NAME).orElse(suiteName);
                builder.withClassName(className);
                var packageNode = root.findOrCreatePackageNode(EMPTY);
                var classNode = packageNode.findOrCreateClassNode(className);
                classNode.addTestCase(builder.build());
                return builder.build();
            }
        }
        throw createEofException(fileName);
    }

    private void readStatus(final StartElement testCaseElement, final TestCaseBuilder builder) {
        var status = getValueOf(testCaseElement, RESULT);
        switch (status) {
            case PASSED:
                builder.withStatus(TestCase.TestResult.PASSED);
                break;
            case FAILED:
                builder.withStatus(TestCase.TestResult.FAILED);
                break;
            case SKIPPED:
            default:
                builder.withStatus(TestCase.TestResult.SKIPPED);
                break;
        }
    }

    private boolean isFailure(final XMLEvent event) {
        return FAILURE.equals(getElementName(event));
    }

    private void readFailure(final XMLEventReader reader, final TestCaseBuilder builder)
            throws XMLStreamException {
        builder.withFailure();

        var aggregatedContent = new StringBuilder();
        while (true) {
            var event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            }
            else if (event.isEndElement() && isFailure(event)) {
                return;
            }
            else if (event.isEndElement() && event.asEndElement().getName().equals(MESSAGE)) {
                builder.withDescription(aggregatedContent.toString());
                return;
            }
        }
    }
}
