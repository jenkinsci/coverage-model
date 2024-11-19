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
 * <a href="https://xunit.net/docs/format-xml-v2">XUnit format</a>
 * into a Java object model.
 *
 * @author Valentin Delaye
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class XunitParser extends AbstractTestParser {
    @Serial
    private static final long serialVersionUID = -5468593789018138107L;

    private static final QName COLLECTION = new QName("collection");
    private static final QName TEST = new QName("test");
    private static final QName RESULT = new QName("result");
    private static final QName TYPE = new QName("type");
    private static final String PASS = "Pass";
    private static final String FAIL = "Fail";
    private static final String SKIP = "Skip";

    /**
     * Creates a new instance of {@link XunitParser}.
     */
    public XunitParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link XunitParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public XunitParser(final ProcessingMode processingMode) {
        super(processingMode, COLLECTION, TEST);
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
            else if (event.isEndElement() && TEST.equals(event.asEndElement().getName())) {
                var className = getOptionalValueOf(testCaseElement, TYPE).orElse(suiteName);
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
            case PASS:
                builder.withStatus(TestCase.TestResult.PASSED);
                break;
            case FAIL:
                builder.withStatus(TestCase.TestResult.FAILED);
                break;
            case SKIP:
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
