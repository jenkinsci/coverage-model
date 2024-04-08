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
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * Baseclass for test result parsers.
 *
 * @author Ullrich Hafner
 */
abstract class AbstractTestParser extends CoverageParser {
    private static final long serialVersionUID = 3771784159977766871L;

    static final QName NAME = new QName("name");
    static final QName FAILURE = new QName("failure");
    static final QName MESSAGE = new QName("message");
    static final QName CLASS_NAME = new QName("classname");

    private final QName testSuite;
    private final QName testCase;

    AbstractTestParser(final ProcessingMode processingMode, final QName testSuite, final QName testCase) {
        super(processingMode);
        this.testSuite = testSuite;
        this.testCase = testCase;
    }

    QName getTestCase() {
        return testCase;
    }

    QName getTestSuite() {
        return testSuite;
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try {
            var eventReader = new SecureXmlParserFactory().createXmlEventReader(reader);
            var root = new ModuleNode(fileName);
            var tests = readTestCases(eventReader, root, fileName);
            handleEmptyResults(fileName, log, tests.isEmpty());
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private List<TestCase> readTestCases(final XMLEventReader eventReader,
            final ModuleNode root, final String fileName) throws XMLStreamException {
        String suiteName = EMPTY;
        var tests = new ArrayList<TestCase>();
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartElement() && getTestSuite().equals(event.asStartElement().getName())) {
                suiteName = getOptionalValueOf(event.asStartElement(), NAME).orElse(EMPTY);
            }
            else if (event.isStartElement() && getTestCase().equals(event.asStartElement().getName())) {
                tests.add(readTestCase(eventReader, event.asStartElement(), suiteName, root, fileName));
            }
        }
        return tests;
    }

    abstract TestCase readTestCase(XMLEventReader reader, StartElement testCaseElement,
            String suiteName, ModuleNode root, String fileName) throws XMLStreamException;

    protected String createId() {
        return UUID.randomUUID().toString();
    }
}
