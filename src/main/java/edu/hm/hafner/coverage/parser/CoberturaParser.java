package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.MethodNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
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

    private static final PathUtil PATH_UTIL = new PathUtil();
    private static final QName SOURCE = new QName("source");
    private static final QName PACKAGE = new QName("package");
    private static final QName CLASS = new QName("class");
    private static final QName METHOD = new QName("method");
    private static final QName LINE = new QName("line");

    private static final Pattern BRANCH_PATTERN = Pattern.compile(".*\\((?<covered>\\d+)/(?<total>\\d+)\\)");

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
    private static final Coverage LINE_COVERED = new CoverageBuilder(Metric.LINE).setCovered(1).setMissed(0).build();
    private static final Coverage LINE_MISSED = new CoverageBuilder(Metric.LINE).setCovered(0).setMissed(1).build();

    /**
     * Parses the Cobertura report. The report is expected to be in XML format.
     *
     * @param reader
     *         the reader to read the report from
     */
    @Override
    protected ModuleNode parseReport(final Reader reader, final FilteredLog log) {
        try {
            var eventReader = new SecureXmlParserFactory().createXmlEventReader(reader);

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
                    else if (PACKAGE.equals(tagName)) {
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
            final StartElement currentStartElement) throws XMLStreamException {
        var packageNode = root.findOrCreatePackageNode(getValueOf(currentStartElement, NAME));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS.equals(nextElement.getName())) {
                    var fileName = getValueOf(nextElement, FILE_NAME);
                    var relativePath = PATH_UTIL.getRelativePath(fileName);
                    var fileNode = packageNode.findOrCreateFileNode(getFileName(fileName),
                            getTreeStringBuilder().intern(relativePath));
                    readClassOrMethod(reader, fileNode, nextElement);
                }
            }
            else if (event.isEndElement()) {
                return; // finish processing of package
            }
        }
    }

    private String getFileName(final String relativePath) {
        var path = Paths.get(PATH_UTIL.getAbsolutePath(relativePath)).getFileName();
        if (path == null) {
            return relativePath;
        }
        return path.toString();
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private Node readClassOrMethod(final XMLEventReader reader, final FileNode fileNode,
            final StartElement parentElement) throws XMLStreamException {
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);

        Node node = createNode(fileNode, parentElement);
        getOptionalValueOf(parentElement, COMPLEXITY)
                .ifPresent(c -> node.addValue(new CyclomaticComplexity(readComplexity(c))));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (LINE.equals(nextElement.getName())) {
                    Coverage coverage;
                    if (isBranchCoverage(nextElement)) {
                        coverage = readBranchCoverage(nextElement);
                        branchCoverage = branchCoverage.add(coverage);
                    }
                    else {
                        int lineHits = getIntegerValueOf(nextElement, HITS);
                        coverage = lineHits > 0 ? LINE_COVERED : LINE_MISSED;
                        lineCoverage = lineCoverage.add(coverage);
                    }

                    if (CLASS.equals(parentElement.getName())) { // Counters are stored at file level
                        int lineNumber = getIntegerValueOf(nextElement, NUMBER);
                        fileNode.addCounters(lineNumber, coverage.getCovered(), coverage.getMissed());
                    }
                }
                else if (METHOD.equals(nextElement.getName())) {
                    Node methodNode = readClassOrMethod(reader, fileNode, nextElement);
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
        throw createEofException();
    }

    private Node createNode(final FileNode file, final StartElement parentElement) {
        var name = getValueOf(parentElement, NAME);
        if (CLASS.equals(parentElement.getName())) {
            return file.createClassNode(name); // connect the class with the file
        }
        else {
            return new MethodNode(name, getValueOf(parentElement, SIGNATURE));
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
        return getOptionalValueOf(line, BRANCH)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    private void readSource(final XMLEventReader reader, final ModuleNode root) throws XMLStreamException {
        var aggregatedContent = new StringBuilder();

        while (reader.hasNext()) {
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
                    .setCovered(matcher.group("covered"))
                    .setTotal(matcher.group("total"))
                    .build();
        }
        return Coverage.nullObject(Metric.BRANCH);
    }
}
