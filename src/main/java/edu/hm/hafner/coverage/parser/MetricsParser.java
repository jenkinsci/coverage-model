package edu.hm.hafner.coverage.parser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.MethodNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.TreeString;

import java.io.Reader;
import java.io.Serial;
import java.nio.file.Path;

/**
 * Parses Metrics reports into a hierarchical Java Object Model.
 *
 * @author Maximilian Waidelich
 */
public class MetricsParser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = -4461747681863455621L;

    /** XML elements. */
    private static final QName METRICS = new QName("metrics");
    private static final QName PACKAGE = new QName("package");
    private static final QName CLASS = new QName("class");
    private static final QName METHOD = new QName("method");
    private static final QName METRIC = new QName("metric");
    private static final QName FILE = new QName("file");

    /** Attributes of the XML elements. */
    private static final QName PROJECT_NAME = new QName("projectName");
    private static final QName NAME = new QName("name");
    private static final QName BEGIN_LINE = new QName("beginline");
    private static final QName VALUE = new QName("value");

    private static final PathUtil PATH_UTIL = new PathUtil();

    /**
     * Creates a new instance of {@link MetricsParser}.
     */
    public MetricsParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link MetricsParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public MetricsParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            var root = new ModuleNode("");

            while (eventReader.hasNext()) {
                var event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (METRICS.equals(tagName)) {
                        root = new ModuleNode(getOptionalValueOf(startElement, PROJECT_NAME).orElse(""));
                    }
                    else if (PACKAGE.equals(tagName)) {
                        readPackage(eventReader, root, startElement, fileName);
                    }
                }
            }
            if (root.hasChildren()) {
                return root;
            }
            else {
                handleEmptyResults(fileName, log);
                return new ModuleNode("empty");
            }
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    @CanIgnoreReturnValue
    private PackageNode readPackage(final XMLEventReader reader, final ModuleNode root,
            final StartElement startElement, final String fileName) throws XMLStreamException {
        var packageName = getValueOf(startElement, NAME);
        var packageNode = root.findOrCreatePackageNode(packageName);
        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (FILE.equals(nextElement.getName())) {
                    readSourceFile(reader, packageNode, nextElement, fileName);
                }
                else if (METRIC.equals(nextElement.getName())) {
                    readValueCounter(packageNode, nextElement);
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
    private Node readClass(final XMLEventReader reader, final FileNode fileNode, final StartElement startElement,
            final String fileName, final PackageNode packageNode) throws XMLStreamException {
        var classNode = fileNode.findOrCreateClassNode(packageNode.getName() + "." + getValueOf(startElement, NAME));
        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (METHOD.equals(nextElement.getName())) {
                    readMethod(reader, classNode, nextElement, fileName);
                }
                else if (METRIC.equals(nextElement.getName())) {
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
        throw createEofException(fileName);
    }

    private TreeString internPath(final String filePath) {
        return getTreeStringBuilder().intern(PATH_UTIL.getRelativePath(Path.of(filePath)));
    }

    @CanIgnoreReturnValue
    private Node readSourceFile(final XMLEventReader reader, final PackageNode packageNode,
            final StartElement startElement, final String fileName)
            throws XMLStreamException {
        var sourceFileName = getSourceFileName(startElement);
        var fileNode = packageNode.findOrCreateFileNode(sourceFileName,
                internPath(getValueOf(startElement, NAME)));

        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS.equals(nextElement.getName())) {
                    readClass(reader, fileNode, nextElement, fileName, packageNode);
                }
                else if (METRIC.equals(nextElement.getName())) {
                    readValueCounter(fileNode, nextElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (FILE.equals(endElement.getName())) {
                    return fileNode;
                }
            }
        }
        throw createEofException(fileName);
    }

    private String getSourceFileName(final StartElement startSourceFileElement) {
        var sourceFilePath = Path.of(getValueOf(startSourceFileElement, NAME)).getFileName();
        if (sourceFilePath == null) {
            return getValueOf(startSourceFileElement, NAME);
        }
        else {
            return sourceFilePath.toString();
        }
    }

    @CanIgnoreReturnValue
    private Node readMethod(final XMLEventReader reader, final ClassNode classNode,
            final StartElement startElement, final String fileName) throws XMLStreamException {
        var methodName = getValueOf(startElement, NAME) + "#" + getValueOf(startElement, BEGIN_LINE);

        var methodNode = createMethod(startElement, methodName);
        classNode.addChild(methodNode);

        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (METRIC.equals(nextElement.getName())) {
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
        throw createEofException(fileName);
    }

    private MethodNode createMethod(final StartElement startElement, final String methodName) {
        return new MethodNode(methodName, "", parseInteger(getValueOf(startElement, BEGIN_LINE)));
    }

    private void readValueCounter(final Node node, final StartElement startElement) {
        var currentType = getValueOf(startElement, NAME);
        var metric = Metric.fromName(currentType);
        var value = metric.parseValue(getValueOf(startElement, VALUE));
        node.addValue(value);
    }
}
