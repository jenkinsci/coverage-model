package edu.hm.hafner.coverage.parser;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.hm.hafner.coverage.*;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.TreeString;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.Reader;

/**
 * Clover parser that parses coverage clover generated coverage files.
 */
public class CloverParser extends CoverageParser {
    private static final long serialVersionUID = -1903059983931698657L;

    private static final QName COVERAGE = new QName("coverage");
    private static final QName PROJECT = new QName("project");
    private static final QName PACKAGE = new QName("package");
    private static final QName METRICS = new QName("metrics");
    private static final QName FILE = new QName("file");
    private static final QName CLASS = new QName("class");
    private static final QName NAME = new QName("name");
    private static final QName PATH = new QName("path");
    private static final QName STATEMENTS = new QName("statements");
    private static final QName COVERED_STATEMENTS = new QName("coveredstatements");
    private static final QName CONDITIONALS = new QName("conditionals");
    private static final QName COVERED_CONDITIONALS = new QName("coveredconditionals");
    private static final QName LINE = new QName("line");
    private static final QName NUM = new QName("num");
    private static final QName COUNT = new QName("count");

    /**
     * Creates a new instance of {@link CloverParser}.
     *
     * @param processingMode
     *          determines whether to ignore errors
     */
    public CloverParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (COVERAGE.equals(tagName)) {
                        ModuleNode root = readCoverage(fileName, eventReader);
                        if(root.hasChildren()) {
                            return root;
                        }
                        else {
                            handleEmptyResults(fileName, log);
                        }
                    }
                }
            }
            handleEmptyResults(fileName, log);
            return new ModuleNode("empty");
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    @CanIgnoreReturnValue
    private ModuleNode readCoverage(final String fileName, final XMLEventReader reader) throws XMLStreamException {
        ModuleNode root = new ModuleNode("");

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PROJECT.equals(startElement.getName())) {
                    //Initializing the project node
                    String projectName = getValueOf(startElement, NAME);
                    root = new ModuleNode(projectName);
                    readProject(fileName, reader, root);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (COVERAGE.equals(endElement.getName())) {
                    return root;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private ModuleNode readProject(final String fileName, final XMLEventReader reader, final ModuleNode root) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (METRICS.equals(startElement.getName())) {
                    addBranchCoverage(root, startElement);
                    addInstructionCoverage(root, startElement);
                }
                else if (PACKAGE.equals(startElement.getName())) {
                    readPackage(fileName, reader, root, startElement);
                }
                else if (FILE.equals(startElement.getName())) {
                    readFile(fileName, reader, root.findOrCreatePackageNode(""), startElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (PROJECT.equals(endElement.getName())) {
                    return root;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private PackageNode readPackage(final String fileName, final XMLEventReader reader, final ModuleNode root, final StartElement packageElement)
            throws XMLStreamException {
        var packageName = getValueOf(packageElement, NAME);
        var packageNode = root.findOrCreatePackageNode(packageName);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (METRICS.equals(startElement.getName())) {
                    addBranchCoverage(packageNode, startElement);
                    addInstructionCoverage(packageNode, startElement);
                }
                else if (FILE.equals(startElement.getName())) {
                    readFile(fileName, reader, packageNode, startElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (PACKAGE.equals(endElement.getName())) {
                    return packageNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    @SuppressWarnings("PMD.CognitiveComplexity")
    private FileNode readFile(final String parserFileName, final XMLEventReader reader, final PackageNode packageNode, final StartElement fileElement) throws XMLStreamException {
        String fileName = getValueOf(fileElement, NAME);
        String filePath = getValueOf(fileElement, PATH);
        var fileNode = packageNode.findOrCreateFileNode(fileName, TreeString.valueOf(filePath));
        var classNode = readClass(reader, fileElement, fileName, fileNode);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var e = event.asStartElement();
                if (METRICS.equals(e.getName())) {
                    addBranchCoverage(classNode, e);
                    addInstructionCoverage(classNode, e);
                }
                else if (LINE.equals(e.getName())) {
                    int line = getIntegerValueOf(e, NUM);
                    int count = getIntegerValueOf(e, COUNT);
                    if (count > 0) {
                        fileNode.addCounters(line, 1, 0);
                    }
                    else {
                        fileNode.addCounters(line, 0, 1);
                    }
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (FILE.equals(endElement.getName())) {
                    resolveLines(fileNode);
                    return fileNode;
                }
            }
        }
        throw createEofException(parserFileName);
    }

    private ClassNode readClass(final XMLEventReader reader, final StartElement fileElement,
                                final String fileName, final Node fileNode) throws XMLStreamException {
        String className = fileName.substring(0, fileName.lastIndexOf('.'));
        var classNode = fileNode.findOrCreateClassNode(className);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var e = event.asStartElement();
                if (CLASS.equals(e.getName())) {
                    className = getValueOf(fileElement, NAME);
                    classNode = fileNode.findOrCreateClassNode(className);
                }
                //Exit the loop after finding class elemenet in the first place
                break;
            }
        }
        return classNode;
    }

    private void resolveLines(final FileNode fileNode) {
        var val = createValue("LINE", fileNode.getCoveredLines().size(), fileNode.getMissedLines().size());
        fileNode.addValue(val);
        for (ClassNode c : fileNode.getAllClassNodes()) {
            c.addValue(val);
        }
    }

    private void addBranchCoverage(final Node node, final StartElement e) {
        int condTotal = getIntegerValueOf(e, CONDITIONALS);
        int condCovered = getIntegerValueOf(e, COVERED_CONDITIONALS);
        addCoverage(node, "BRANCH", condCovered, condTotal);
    }

    private void addInstructionCoverage(final Node node, final StartElement e) {
        int stmntsTotal = getIntegerValueOf(e, STATEMENTS);
        int stmntsCovered = getIntegerValueOf(e, COVERED_STATEMENTS);
        addCoverage(node, "INSTRUCTION", stmntsCovered, stmntsTotal);
    }

    private void addCoverage(final Node node, final String metricName, final int covered, final int total) {
        var builder = new Coverage.CoverageBuilder();
        node.addValue(builder.withMetric(Metric.valueOf(metricName))
                .withCovered(covered)
                .withMissed(total - covered).build());
    }
}
