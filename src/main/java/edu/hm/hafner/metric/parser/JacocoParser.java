package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CoverageParser;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * A parser which parses reports made by Jacoco into a Java Object Model.
 *
 * @author Melissa Bauer
 */
public class JacocoParser extends CoverageParser {
    private static final long serialVersionUID = -6021749565311262221L;

    private static final QName REPORT = new QName("report");
    private static final QName PACKAGE = new QName("package");
    private static final QName CLASS = new QName("class");
    private static final QName METHOD = new QName("method");
    private static final QName COUNTER = new QName("counter");
    private static final QName SOURCE_FILE = new QName("sourcefile");

    /** Required attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName SIGNATURE = new QName("desc");
    private static final QName TYPE = new QName("type");
    private static final QName MISSED = new QName("missed");
    private static final QName COVERED = new QName("covered");
    private static final QName LINE_NUMBER = new QName("nr");

    /** Implied attributes of the XML elements. */
    private static final QName SOURCE_FILE_NAME = new QName("sourcefilename");
    private static final QName LINE = new QName("line");
    private static final QName COVERED_INSTRUCTIONS = new QName("ci");
    private static final QName MISSED_BRANCHES = new QName("mb");
    private static final QName COVERED_BRANCHED = new QName("cb");

    /**
     * Parses the JaCoCo report. The report is expected to be in XML format.
     *
     * @param reader
     *         the reader to read the report from
     */
    @Override
    public ModuleNode parse(final Reader reader, final FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (REPORT.equals(tagName)) {
                        var root = new ModuleNode(getValueOf(startElement, NAME));
                        readReport(eventReader, root);
                        return root;
                    }
                }
            }
            throw new NoSuchElementException("No coverage information found in the specified file.");
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private ModuleNode readReport(final XMLEventReader reader, final ModuleNode root)
            throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PACKAGE.equals(startElement.getName())) {
                    readPackage(reader, root, startElement);
                }
                else if (COUNTER.equals(startElement.getName())) {
                    readValueCounter(root, startElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (REPORT.equals(endElement.getName())) {
                    return root;
                }
            }
        }
        throw createEofException();
    }

    private PackageNode readPackage(final XMLEventReader reader,
            final ModuleNode root, final StartElement startElement) throws XMLStreamException {
        var packageNode = root.findOrCreatePackageNode(getValueOf(startElement, NAME));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS.equals(nextElement.getName())) {
                    readClass(reader, packageNode, nextElement);
                }
                else if (SOURCE_FILE.equals(nextElement.getName())) {
                    readSourceFile(reader, packageNode, nextElement);
                }
                else if (COUNTER.equals(startElement.getName())) {
                    readValueCounter(packageNode, startElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (PACKAGE.equals(endElement.getName())) {
                    return packageNode;
                }
            }
        }
        throw createEofException();
    }

    private Node readClass(final XMLEventReader reader, final PackageNode packageNode,
            final StartElement startElement) throws XMLStreamException {
        String fileName = getValueOf(startElement, SOURCE_FILE_NAME);
        var fileNode = packageNode.findOrCreateFileNode(fileName);
        var classNode = fileNode.findOrCreateClassNode(getValueOf(startElement, NAME));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (METHOD.equals(nextElement.getName())) {
                    readMethod(reader, classNode, nextElement);
                }
                else if (COUNTER.equals(nextElement.getName())) {
                    readValueCounter(classNode, nextElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName())) {
                    return classNode;
                }
            }
        }
        throw createEofException();
    }

    private Node readSourceFile(final XMLEventReader reader, final PackageNode packageNode,
            final StartElement startElement) throws XMLStreamException {
        String fileName = getValueOf(startElement, NAME);
        var fileNode = packageNode.findOrCreateFileNode(fileName);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (LINE.equals(nextElement.getName())) {
                    readLine(fileNode, nextElement);
                }
                else if (COUNTER.equals(nextElement.getName())) {
                    readValueCounter(fileNode, nextElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (SOURCE_FILE.equals(endElement.getName())) {
                    return fileNode;
                }
            }
        }
        throw createEofException();
    }

    private void readLine(final FileNode fileNode, final StartElement startElement)  {
        int lineNumber = getIntegerValueOf(startElement, LINE_NUMBER);
        int coveredInstructions = getIntegerValueOf(startElement, COVERED_INSTRUCTIONS);
        int coveredBranches = getIntegerValueOf(startElement, COVERED_BRANCHED);
        int missedBranches = getIntegerValueOf(startElement, MISSED_BRANCHES);

        int missed;
        int covered;
        if (missedBranches + coveredBranches == 0) { // only instruction coverage found
            covered = coveredInstructions > 0 ? 1 : 0;
            missed = covered > 0 ? 0 : 1;
        }
        else {
            covered = coveredBranches;
            missed = missedBranches;
        }
        fileNode.addCounters(lineNumber, covered, missed);
    }

    private Node readMethod(final XMLEventReader reader, final ClassNode classNode,
            final StartElement startElement) throws XMLStreamException {
        String methodName = getValueOf(startElement, NAME);
        String methodSignature = getValueOf(startElement, SIGNATURE);

        MethodNode methodNode = createMethod(startElement, methodName, methodSignature);
        classNode.addChild(methodNode);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (COUNTER.equals(nextElement.getName())) {
                    readValueCounter(methodNode, nextElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (METHOD.equals(endElement.getName())) {
                    return methodNode;
                }
            }
        }
        throw createEofException();
    }

    private MethodNode createMethod(final StartElement startElement, final String methodName,
            final String methodSignature) {
        return getOptionalValueOf(startElement, LINE)
                .map(CoverageParser::parseInteger)
                .map(line -> new MethodNode(methodName, methodSignature, line))
                .orElseGet(() -> new MethodNode(methodName, methodSignature));
    }

    private void readValueCounter(final Node node, final StartElement startElement) {
        String currentType = getValueOf(startElement, TYPE);

        if (StringUtils.containsAny(currentType, "LINE", "INSTRUCTION", "BRANCH", "COMPLEXITY")) {
            var covered = getIntegerValueOf(startElement, COVERED);
            var missed = getIntegerValueOf(startElement, MISSED);

            node.addValue(createValue(currentType, covered, missed));
        }
    }

    private Value createValue(final String currentType, final int covered, final int missed) {
        if ("COMPLEXITY".equals(currentType)) {
            return new CyclomaticComplexity(covered + missed);
        }
        else {
            var builder = new CoverageBuilder();
            return builder.setMetric(Metric.valueOf(currentType))
                        .setCovered(covered)
                        .setMissed(missed).build();
        }
    }
}
