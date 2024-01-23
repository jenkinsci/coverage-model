package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.TestCase;
import edu.hm.hafner.coverage.TestCase.TestCaseBuilder;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * Parses reports in the NUnit format (https://docs.nunit.org/articles/nunit/technical-notes/usage/Test-Result-XML-Format.html) into a Java object model.
 *
 * @author Valentin Delaye
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class NunitParser extends CoverageParser {
    private static final long serialVersionUID = -5468593789018138107L;

    private static final QName TEST_SUITE = new QName("test-suite");
    private static final QName TEST_CASE = new QName("test-case");
    private static final QName NAME = new QName("name");
    private static final QName CLASS_NAME = new QName("classname");
    private static final QName FAILURE = new QName("failure");
    private static final QName MESSAGE = new QName("message");
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
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            var root = new ModuleNode(EMPTY);
            var tests = readTestCases(eventReader, root);
            handleEmptyResults(log, tests.isEmpty());
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private List<Object> readTestCases(final XMLEventReader eventReader,
            final ModuleNode root) throws XMLStreamException {
        String suiteName = EMPTY;
        var tests = new ArrayList<>();
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement() && TEST_SUITE.equals(event.asStartElement().getName())) {
                suiteName = getOptionalValueOf(event.asStartElement(), NAME).orElse(EMPTY);
            }
            else if (event.isStartElement() && TEST_CASE.equals(event.asStartElement().getName())) {
                tests.add(readTestCase(eventReader, event.asStartElement(), suiteName, root));
            }
        }
        return tests;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private TestCase readTestCase(final XMLEventReader reader, final StartElement testCaseElement,
            final String suiteName, final ModuleNode root)
            throws XMLStreamException {
        var builder = new TestCaseBuilder();

        builder.withTestName(getOptionalValueOf(testCaseElement, NAME).orElse(createId()));
        
        var status = getValueOf(testCaseElement, RESULT);
        if (status.equals(PASSED)) {
            builder.withStatus(TestCase.TestResult.PASSED);
        }
        else if (status.equals(FAILED)) {
            builder.withStatus(TestCase.TestResult.FAILED);
        }
        else if (status.equals(SKIPPED)) {
            builder.withStatus(TestCase.TestResult.SKIPPED);
        }
        else {
            builder.withStatus(TestCase.TestResult.SKIPPED);
        }

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement() && isFailure(event)) {
                readFailure(reader, builder);
            }
            else if (event.isEndElement() && TEST_CASE.equals(event.asEndElement().getName())) {
                var className = getOptionalValueOf(testCaseElement, CLASS_NAME).orElse(suiteName);
                builder.withClassName(className);
                var packageNode = root.findOrCreatePackageNode(EMPTY);
                var classNode = packageNode.findOrCreateClassNode(className);
                classNode.addTestCase(builder.build());
                break;
            }
        }
        return builder.build();
    }

    private boolean isFailure(final XMLEvent event) {
        QName name;
        if (event.isStartElement()) {
            name = event.asStartElement().getName();
        }
        else {
            name = event.asEndElement().getName();
        }

        return FAILURE.equals(name);
    }

    private void readFailure(final XMLEventReader reader, final TestCaseBuilder builder)
            throws XMLStreamException {
        builder.withFailure();
        var aggregatedContent = new StringBuilder();
        while (true) {
            XMLEvent event = reader.nextEvent();
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

    private String createId() {
        return UUID.randomUUID().toString();
    }
}
