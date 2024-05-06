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

public class PythonParser extends CoverageParser {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(PythonParser.class.getName());

    private static final long serialVersionUID = -2343357235229921679L;

    private static final QName COVERAGE = new QName("coverage");
    private static final QName LINES_VALID = new QName("lines-valid");
    private static final QName LINES_COVERED = new QName("lines-covered");
    private static final QName PACKAGES = new QName("packages");
    private static final QName PACKAGE = new QName("package");
    private static final QName FILENAME = new QName("filename");
    private static final QName CLASSES = new QName("classes");
    private static final QName CLASS = new QName("class");
    private static final QName METHODS = new QName("methods");
    private static final QName LINES = new QName("lines");
    private static final QName LINE = new QName("line");
    private static final QName NUMBER = new QName("number");
    private static final QName HITS = new QName("hits");
    private static final QName NAME = new QName("name");


    public PythonParser(ProcessingMode processingMode) {
        super(processingMode);
    }


    @Override
    protected ModuleNode parseReport(Reader reader, String fileName, FilteredLog log) {
        ModuleNode root = null;
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (COVERAGE.equals(tagName)) {
                        root = new ModuleNode("");
                        var linesValid = getIntegerValueOf(startElement, LINES_VALID);
                        var linesCovered = getIntegerValueOf(startElement, LINES_COVERED);
                        root.addValue(createValue("LINE", linesCovered, linesValid - linesCovered));
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
        }
        catch (XMLStreamException exception) {
            throw new SecureXmlParserFactory.ParsingException(exception);
        }
    }

    @CanIgnoreReturnValue
    private ModuleNode readCoverage(final XMLEventReader reader, final ModuleNode module, String fileName)
            throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PACKAGES.equals(startElement.getName())) {
                    readPackages(reader, module, fileName);
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (COVERAGE.equals((endElement.getName()))) {
                    if (module.hasChildren()) {
                        return module;
                    } else {
                        return null;
                    }
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private Node readPackages(final XMLEventReader reader, final ModuleNode root, final String fileName) throws XMLStreamException {
        while (reader.hasNext()) {
            var event = reader.nextEvent();
            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PACKAGE.equals(startElement.getName())) {
                    readPackage(reader, root, startElement, fileName);
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (PACKAGES.equals(endElement.getName())) {
                    return root;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private PackageNode readPackage(final XMLEventReader reader, final ModuleNode root, final StartElement startElement, String fileName)
            throws XMLStreamException {
        var packageName = getValueOf(startElement, NAME);
        var packageNode = root.findOrCreatePackageNode(packageName);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASSES.equals(nextElement.getName())) {
                    readClasses(reader, packageNode, fileName);
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
    private Node readClasses(final XMLEventReader reader, final PackageNode packageNode, String fileName) throws XMLStreamException {
        while (reader.hasNext()) {
            var event = reader.nextEvent();
            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (CLASS.equals(startElement.getName())) {
                    readClass(reader, packageNode, startElement, fileName);
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASSES.equals(endElement.getName())) {
                    return packageNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private ClassNode readClass(final XMLEventReader reader, final PackageNode packageNode, final StartElement startElement, String fileName) throws XMLStreamException {
        String className = getValueOf(startElement, NAME);
        String filePath = getValueOf(startElement, FILENAME);
        if (!filePath.contains("/")) {
            filePath = "./" + filePath; // Workaround for Nodes needing unique names where ClassName=FileName, and prepending the current working dir to filepath if file is located in current dir
        }
        FileNode fileNode = packageNode.findOrCreateFileNode(className, TreeString.valueOf(filePath));
        ClassNode classNode = fileNode.findOrCreateClassNode(className);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var e = event.asStartElement();
                // TODO methods(?) seems to be empty block
                if (LINES.equals(e.getName())) {
                    readLines(reader, fileNode, fileName);
                } else {
                    LOGGER.warning("Unexpected element in <class> block: " + e.getName());
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName())) {
                    resolveLines(fileNode);
                    return classNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private Node readLines(final XMLEventReader reader, final FileNode fileNode, String fileName) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (LINE.equals(nextElement.getName())) {
                    readLine(fileNode, nextElement);
                }
            } else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (LINES.equals(endElement.getName())) {
                    return fileNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    private void readLine(final FileNode fileNode, StartElement element) {
        int lineNumber = getIntegerValueOf(element, NUMBER);
        int hits = getIntegerValueOf(element, HITS);
        if (hits > 0) {
            fileNode.addCounters(lineNumber, hits, 0);
        } else {
            fileNode.addCounters(lineNumber, 0, 1);
        }
    }

    private void resolveLines(final FileNode fileNode) {
        var val = createValue("LINE", fileNode.getCoveredLines().size(), fileNode.getMissedLines().size());
        fileNode.addValue(val);
        for (ClassNode c: fileNode.getAllClassNodes()) {
            c.addValue(val);
        }
    }
}