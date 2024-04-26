package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.nio.file.Paths;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.MethodNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;
import edu.hm.hafner.util.TreeString;

import static edu.hm.hafner.coverage.parser.ParseUtil.createValue;

/**
 * Parses JaCoCo reports into a hierarchical Java Object Model.
 *
 * @author Melissa Bauer
 */
@SuppressWarnings("PMD.GodClass")
public class JacocoParser extends CoverageParser {
    private static final long serialVersionUID = -6021749565311262221L;

    /** XML elements. */
    private static final QName REPORT = new QName("report");
    private static final QName PACKAGE = new QName("package");
    private static final QName GROUP = new QName("group");
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

    /** Optional attributes of the XML elements. */
    private static final QName SOURCE_FILE_NAME = new QName("sourcefilename");
    private static final QName LINE = new QName("line");
    private static final QName COVERED_INSTRUCTIONS = new QName("ci");
    private static final QName MISSED_BRANCHES = new QName("mb");
    private static final QName COVERED_BRANCHED = new QName("cb");

    private static final String VALUE_COMPLEXITY = "COMPLEXITY";

    private static final PathUtil PATH_UTIL = new PathUtil();
    private static final String VALUE_BRANCH = "BRANCH";
    private static final String VALUE_INSTRUCTION = "INSTRUCTION";
    private static final String VALUE_LINE = "LINE";

    /**
     * Creates a new instance of {@link JacocoParser}.
     */
    public JacocoParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link JacocoParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public JacocoParser(final ProcessingMode processingMode) {
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
                    if (REPORT.equals(tagName)) {
                        var root = new ModuleNode(getValueOf(startElement, NAME));
                        readModule(eventReader, root, fileName);
                        return root;
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
    private ModuleNode readModule(final XMLEventReader reader, final ModuleNode module, final String fileName)
            throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PACKAGE.equals(startElement.getName())) {
                    readPackage(reader, module, startElement, fileName);
                }
                else if (GROUP.equals(startElement.getName())) {
                    var subModule = new ModuleNode(getValueOf(startElement, NAME));
                    readModule(reader, subModule, fileName);
                    module.addChild(subModule);
                }
                else if (COUNTER.equals(startElement.getName())) {
                    readValueCounter(module, startElement);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (isModuleEnd(endElement)) {
                    return module;
                }
            }
        }
        throw createEofException(fileName);
    }

    private boolean isModuleEnd(final EndElement endElement) {
        return REPORT.equals(endElement.getName()) || GROUP.equals(endElement.getName());
    }

    @CanIgnoreReturnValue
    private PackageNode readPackage(final XMLEventReader reader, final ModuleNode root,
            final StartElement startElement, final String fileName) throws XMLStreamException {
        var packageName = getValueOf(startElement, NAME);
        var packageNode = root.findOrCreatePackageNode(packageName);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS.equals(nextElement.getName())) {
                    readClass(reader, packageNode, packageName, nextElement, fileName);
                }
                else if (SOURCE_FILE.equals(nextElement.getName())) {
                    readSourceFile(reader, packageNode, packageName, nextElement, fileName);
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
        throw createEofException(fileName);
    }

    @CanIgnoreReturnValue
    private Node readClass(final XMLEventReader reader, final PackageNode packageNode,
            final String packageName, final StartElement startElement, final String fileName) throws XMLStreamException {
        Optional<String> possibleFileName = getOptionalValueOf(startElement, SOURCE_FILE_NAME);
        ClassNode classNode;
        if (possibleFileName.isPresent()) {
            var classFileName = possibleFileName.get();
            var fileNode = packageNode.findOrCreateFileNode(classFileName, internPath(packageName, classFileName));

            classNode = fileNode.findOrCreateClassNode(getValueOf(startElement, NAME));
        }
        else {
            // Class nodes without files might not be inserted into the tree structure correctly
            classNode = packageNode.findOrCreateClassNode(getValueOf(startElement, NAME));
        }
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (METHOD.equals(nextElement.getName())) {
                    readMethod(reader, classNode, nextElement, fileName);
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
        throw createEofException(fileName);
    }

    private TreeString internPath(final String packageName, final String fileName) {
        return getTreeStringBuilder().intern(PATH_UTIL.getRelativePath(Paths.get(packageName, fileName)));
    }

    @CanIgnoreReturnValue
    private Node readSourceFile(final XMLEventReader reader, final PackageNode packageNode,
            final String packageName, final StartElement startElement, final String fileName) throws XMLStreamException {
        String sourceFilefileName = getValueOf(startElement, NAME);
        var fileNode = packageNode.findOrCreateFileNode(sourceFilefileName, internPath(packageName, sourceFilefileName));

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
        throw createEofException(fileName);
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

    @CanIgnoreReturnValue
    private Node readMethod(final XMLEventReader reader, final ClassNode classNode,
            final StartElement startElement, final String fileName) throws XMLStreamException {
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
        throw createEofException(fileName);
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

        if (StringUtils.containsAny(currentType, VALUE_LINE, VALUE_INSTRUCTION, VALUE_BRANCH, VALUE_COMPLEXITY)) {
            var covered = getIntegerValueOf(startElement, COVERED);
            var missed = getIntegerValueOf(startElement, MISSED);

            if (!node.isAggregation()) {
                node.addValue(createValue(currentType, covered, missed));
            }
        }
    }

}
