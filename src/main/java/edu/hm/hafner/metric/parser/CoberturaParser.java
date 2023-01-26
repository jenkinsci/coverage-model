package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * Parses Cobertura report formats into a hierarchical Java Object Model.
 *
 * @author Melissa Bauer
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class CoberturaParser extends CoverageParser {
    private static final long serialVersionUID = -3625341318291829577L;

    private static final QName SOURCE = new QName("source");
    private static final QName PACKAGE = new QName("package");
    private static final QName CLASS = new QName("class");
    private static final QName METHOD = new QName("method");
    private static final QName LINE = new QName("line");

    private static final Pattern BRANCH_PATTERN = Pattern.compile(".*(\\d+)/(\\d+)\\)");

    /** Required attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName FILE_NAME = new QName("filename");
    private static final QName SIGNATURE = new QName("signature");
    private static final QName HITS = new QName("hits");
    private static final QName COMPLEXITY = new QName("complexity");
    private static final QName NUMBER = new QName("number");

    /** Not required attributes of the XML elements. */
    private static final QName BRANCH = new QName("branch");
    private static final QName CONDITION_COVERAGE = new QName("condition-coverage");


    /**
     * Parses the Cobertura report. The report is expected to be in XML format.
     *
     * @param reader
     *         the reader to wrap
     */
    @Override
    public ModuleNode parse(final Reader reader) {
        try {
            SecureXmlParserFactory factory = new SecureXmlParserFactory();
            XMLEventReader eventReader = factory.createXmlEventReader(reader);
            var root = new ModuleNode("-");
            boolean isEmpty = true;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (SOURCE.equals(tagName)) {
                        readSource(eventReader, root);
                    }
                    else if (PACKAGE.equals(startElement.getName())) {
                        readPackage(eventReader, root, startElement);
                        isEmpty = false;
                    }
                }
            }
            if (isEmpty) {
                throw new NoSuchElementException("No coverage information found in the specified file.");
            }
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private void readPackage(final XMLEventReader reader, final ModuleNode root,
            final StartElement startElement) throws XMLStreamException {
        var packageName = getValueOf(startElement, NAME);
        var packageNode = root.findPackage(packageName).orElseGet(() -> createPackage(root, packageName));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var element = event.asStartElement();
                if (CLASS.equals(element.getName())) {
                    var fileName = getValueOf(event.asStartElement(), FILE_NAME);
                    var file = packageNode.findFile(fileName).orElseGet(() -> createFile(packageNode, fileName));

                    readClassOrMethod(reader, file, event.asStartElement());
                }
            }
        }
    }

    private ClassNode createClass(final FileNode fileNode, final String className) {
        // TODO: move to file node
        var classNode = new ClassNode(className);
        fileNode.addChild(classNode);
        return classNode;
    }

    private FileNode createFile(final PackageNode packageNode, final String fileName) {
        // TODO: move to package node
        var fileNode = new FileNode(fileName);
        packageNode.addChild(fileNode);
        return fileNode;
    }

    private PackageNode createPackage(final ModuleNode moduleNode, final String fileName) {
        // TODO: move to module node
        var packageNode = new PackageNode(fileName);
        moduleNode.addChild(packageNode);
        return packageNode;
    }

    private Node readClassOrMethod(final XMLEventReader reader, final FileNode file,
            final StartElement parentElement) throws XMLStreamException {
        var lineCovered = new CoverageBuilder(Metric.LINE).setCovered(1).setMissed(0).build();
        var lineMissed = new CoverageBuilder(Metric.LINE).setCovered(0).setMissed(1).build();

        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);

        Node node;
        if (CLASS.equals(parentElement.getName())) {
            node = createClass(file, getValueOf(parentElement, NAME)); // connect the class with the file
        }
        else {
            node = new MethodNode(getValueOf(parentElement, NAME), getValueOf(parentElement, SIGNATURE));
        }
        getOptionalValueOf(parentElement, COMPLEXITY).ifPresent(
                c -> node.addValue(new CyclomaticComplexity(readComplexity(c))));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (LINE.equals(nextElement.getName())) {
                    int lineNumber = getIntegerOf(nextElement, NUMBER);

                    Coverage coverage;
                    if (isBranchCoverage(nextElement)) {
                        coverage = readBranchCoverage(nextElement);
                        branchCoverage = branchCoverage.add(coverage);
                    }
                    else {
                        int lineHits = getIntegerOf(nextElement, HITS);
                        coverage = lineHits > 0 ? lineCovered : lineMissed;
                        lineCoverage = lineCoverage.add(coverage);
                    }

                    if (CLASS.equals(parentElement.getName())) { // Counters are stored at file level
                        file.addCounters(lineNumber, coverage.getCovered(), coverage.getMissed());
                    }
                }
                else if (METHOD.equals(nextElement.getName())) {
                    Node methodNode = readClassOrMethod(reader, file, nextElement);
                    node.addChild(methodNode);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName()) || METHOD.equals(endElement.getName())) {
                    node.addValue(lineCoverage);
                    if (branchCoverage.isSet()) {
                        node.addValue(branchCoverage);
                    }
                    return node;
                }
            }
        }
        throw new ParsingException("Unexpected end of file");
    }

    private int getIntegerOf(final StartElement nextElement, final QName attributeName) {
        try {
            return parseInteger(getValueOf(nextElement, attributeName));
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    private int readComplexity(final String c) {
        try {
            return Math.round(Float.parseFloat(c)); // some reports use float values
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    private boolean isBranchCoverage(final StartElement line) {
        Attribute branchAttribute = line.getAttributeByName(BRANCH);
        if (branchAttribute != null) {
            return Boolean.parseBoolean(branchAttribute.getValue());
        }
        return false;
    }

    private void readSource(final XMLEventReader reader, final ModuleNode root) throws XMLStreamException {
        var aggregatedContent = new StringBuilder();

        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            }
            else if (event.isEndElement()) {
                root.addSource(new PathUtil().getRelativePath(aggregatedContent.toString()));

                return;
            }
        }
    }

    private Coverage readBranchCoverage(final StartElement line) {
        String conditionCoverageAttribute = getValueOf(line, CONDITION_COVERAGE);
        var matcher = BRANCH_PATTERN.matcher(conditionCoverageAttribute);
        if (matcher.matches()) {
            var builder = new CoverageBuilder();
            return builder.setMetric(Metric.BRANCH)
                    .setCovered(parseInteger(matcher.group(1)))
                    .setTotal(parseInteger(matcher.group(2)))
                    .build();
        }
        return Coverage.nullObject(Metric.BRANCH);

    }

    private int parseInteger(final String value) {
        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }
}
