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

public class CloverParser extends CoverageParser {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CloverParser.class.getName());

    private static final long serialVersionUID = -1903059983931698657L;

    private static final QName COVERAGE = new QName("coverage");
    private static final QName PACKAGE = new QName("package");
    private static final QName METRICS = new QName("metrics");
    private static final QName FILE = new QName("file");
    private static final QName NAME = new QName("name");
    private static final QName PATH = new QName("path");
    private static final QName STATEMENTS = new QName("statements");
    private static final QName COVERED_STATEMENTS = new QName("coveredstatements");
    private static final QName CONDITIONALS = new QName("conditionals");
    private static final QName COVERED_CONDITIONALS = new QName("coveredconditionals");
    private static final QName LINE = new QName("line");
    private static final QName NUM = new QName("num");
    private static final QName COUNT = new QName("count");

    public CloverParser(ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(Reader reader, String fileName, FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (COVERAGE.equals(tagName)) {
                        ModuleNode root = new ModuleNode("");
                        if (readCoverage(eventReader, root, fileName) == null) {
                            handleEmptyResults(fileName, log);
                        } else {
                            return root;
                        }
                    }
                }
            }
            handleEmptyResults(fileName, log);
            return new ModuleNode("empty");
        } catch (XMLStreamException exception) {
            throw new SecureXmlParserFactory.ParsingException(exception);
        }
    }

    @CanIgnoreReturnValue
    private ModuleNode readCoverage(final XMLEventReader reader, final ModuleNode root, String fileName) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PACKAGE.equals(startElement.getName())) {
                    readPackage(reader, root, startElement, fileName);
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (COVERAGE.equals((endElement.getName()))) {
                    if (root.hasChildren()) {
                        return root;
                    } else {
                        return null;
                    }
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private PackageNode readPackage(final XMLEventReader reader, final ModuleNode root, final StartElement packageElement, String fileName)
            throws XMLStreamException {
        var packageName = getValueOf(packageElement, NAME);
        var packageNode = root.findOrCreatePackageNode(packageName);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (FILE.equals(startElement.getName())) {
                    readFile(reader, packageNode, startElement);
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (PACKAGE.equals((endElement.getName()))) {
                    return packageNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private FileNode readFile(final XMLEventReader reader, final PackageNode packageNode, final StartElement fileElement) throws XMLStreamException {
        String fileName = getValueOf(fileElement, NAME);
        String className = fileName.substring(0, fileName.lastIndexOf("."));
        String filePath = getValueOf(fileElement, PATH);
        // TODO check if filePath is relative or absolute path (latter needs to remove everything except current workspace)
        var fileNode = packageNode.findOrCreateFileNode(fileName, TreeString.valueOf(filePath));
        var classNode = packageNode.findOrCreateClassNode(className);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var e = event.asStartElement();
                if (METRICS.equals(e.getName())) {
                    Integer condTotal = getIntegerValueOf(e, CONDITIONALS);
                    Integer condCovered = getIntegerValueOf(e, COVERED_CONDITIONALS);
                    Integer stmntsTotal = getIntegerValueOf(e, STATEMENTS);
                    Integer stmntsCovered = getIntegerValueOf(e, COVERED_STATEMENTS);
                    classNode.addValue(createValue("CONDITIONAL", condCovered, condTotal - condCovered));
                    classNode.addValue(createValue("INSTRUCTION", stmntsCovered, stmntsTotal - stmntsCovered));

                } else if (LINE.equals(e.getName())) {
                    Integer line =  Integer.parseInt(getValueOf(e, NUM));
                    Integer count = Integer.parseInt(getValueOf(e, COUNT));
                    if (count > 0) {
                        fileNode.addCounters(line, count, 0);
                    } else {
                        fileNode.addCounters(line, 0, 1);
                    }
                } else {
                    LOGGER.warning("Unexpected element in <file> block: " + e.getName());
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (FILE.equals(endElement.getName())) {
                    resolveLines(fileNode);
                    return fileNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    private void resolveLines(final FileNode fileNode) {
        var val = createValue("LINE", fileNode.getCoveredLines().size(), fileNode.getMissedLines().size());
        fileNode.addValue(val);
        for (ClassNode c: fileNode.getAllClassNodes()) {
            c.addValue(val);
        }
    }
}