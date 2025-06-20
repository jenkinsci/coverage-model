package edu.hm.hafner.coverage.parser;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
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
import java.util.Optional;

/**
 * Clover parser that parses coverage clover generated coverage files.
 */
@SuppressWarnings("PMD.GodClass")
public class CloverParser extends CoverageParser {
    @Serial
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
    private static final QName METHODS = new QName("methods");
    private static final QName COVERED_METHODS = new QName("coveredmethods");
    private static final QName LINE = new QName("line");
    private static final QName NUM = new QName("num");
    private static final QName COUNT = new QName("count");
    private static final QName TURE_COUNT = new QName("truecount");
    private static final QName FALSE_COUNT = new QName("falsecount");
    private static final QName TYPE = new QName("type");

    private static final String COND = "cond";
    private static final String STMT = "stmt";

    private static final PathUtil PATH_UTIL = new PathUtil();

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
                var event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (COVERAGE.equals(tagName)) {
                        var root = readCoverage(fileName, eventReader, log);
                        if (root.hasChildren()) {
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
    private ModuleNode readCoverage(final String fileName, final XMLEventReader reader,
                                    final FilteredLog log) throws XMLStreamException {
        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (PROJECT.equals(startElement.getName())) {
                    var projectName = getOptionalValueOf(startElement, NAME).orElse(EMPTY);
                    var root = new ModuleNode(projectName);

                    readProject(fileName, reader, root);

                    return root;
                }
            }
        }
        handleEmptyResults(fileName, log);

        return new ModuleNode("empty");
    }

    @CanIgnoreReturnValue
    private ModuleNode readProject(final String fileName, final XMLEventReader reader, final ModuleNode root) throws XMLStreamException {
        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (METRICS.equals(startElement.getName())) {
                    readCoverageMetrics(root, startElement);
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
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                var startElement = event.asStartElement();
                if (METRICS.equals(startElement.getName())) {
                    readCoverageMetrics(packageNode, startElement);
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
    private FileNode readFile(final String parserFileName, final XMLEventReader reader,
                              final PackageNode packageNode, final StartElement fileElement) throws XMLStreamException {
        String fileName = getValueOf(fileElement, NAME);
        var fileNode = packageNode.findOrCreateFileNode(fileName, constructPathForFile(fileElement, packageNode.getName(), fileName));

        while (reader.hasNext()) {
            var event = reader.nextEvent();
            if (event.isStartElement()) {
                var e = event.asStartElement();
                if (CLASS.equals(e.getName())) {
                    readClass(parserFileName, reader, e, fileNode);
                }
                else if (METRICS.equals(e.getName())) {
                    readCoverageMetrics(fileNode, e);
                }
                else if (LINE.equals(e.getName())) {
                    addLineCoverage(e, fileNode);
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

    private void addLineCoverage(final StartElement e, final FileNode fileNode) {
        String type = getValueOf(e, TYPE);
        int line = getIntegerValueOf(e, NUM);
        if (STMT.equals(type)) {
            int count = getIntegerValueOf(e, COUNT);
            addCountersToFile(count, fileNode, line);
        }
        else if (COND.equals(type)) {
            Optional<String> countVal = getOptionalValueOf(e, COUNT);
            if (countVal.isPresent()) {
                // If count exists, using it to decide the line coverage
                int count = parseInteger(countVal.get());
                addCountersToFile(count, fileNode, line);
            }
            else {
                // If no count, then using trueCount or falseCount to decide on the line coverage
                addCountersUsingConditional(fileNode, line, e);
            }
        }
    }

    private void addCountersToFile(final int count, final FileNode fileNode, final int line) {
        if (count > 0) {
            fileNode.addCounters(line, 1, 0);
        }
        else {
            fileNode.addCounters(line, 0, 1);
        }
    }

    private void addCountersUsingConditional(final FileNode fileNode, final int line, final StartElement e) {
        int trueCount = getIntegerValueOf(e, TURE_COUNT);
        int falseCount = getIntegerValueOf(e, FALSE_COUNT);
        if (trueCount > 0 || falseCount > 0) {
            fileNode.addCounters(line, 1, 0);
        }
        else {
            fileNode.addCounters(line, 0, 1);
        }
    }

    private TreeString constructPathForFile(final StartElement fileElement, final String packageName, final String fileName) {
        return getOptionalValueOf(fileElement, PATH)
                .map(TreeString::valueOf)
                .orElseGet(() -> getTreeStringBuilder().intern(getPath(packageName, fileName)));
    }

    private String getPath(final String packageName, final String fileName) {
        if (PATH_UTIL.isAbsolute(fileName)) {
            return fileName;
        }
        var relativePath = Path.of(packageName.replace(".", "/"), fileName);
        return PATH_UTIL.getRelativePath(relativePath);
    }

    private void readClass(final String parserFileName, final XMLEventReader reader, final StartElement fileElement,
                           final Node fileNode) throws XMLStreamException {
        String className = getValueOf(fileElement, NAME);
        var classNode = fileNode.findOrCreateClassNode(className);
        while (reader.hasNext()) {
            var event = reader.nextEvent();
            if (event.isStartElement()) {
                var e = event.asStartElement();
                if (METRICS.equals(e.getName())) {
                    readCoverageMetrics(classNode, e);
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName())) {
                    return;
                }
            }
        }
        throw createEofException(parserFileName);
    }

    private void resolveLines(final FileNode fileNode) {
        var builder = new CoverageBuilder();
        var lineCoverage = builder.withMetric(Metric.LINE)
                .withCovered(fileNode.getCoveredLines().size())
                .withMissed(fileNode.getMissedLines().size()).build();
        fileNode.addValue(lineCoverage);
        for (ClassNode c : fileNode.getAllClassNodes()) {
            c.addValue(lineCoverage);
        }
    }

    private void readCoverageMetrics(final Node node, final StartElement e) {
        addCoverage(node, Metric.BRANCH, CONDITIONALS, COVERED_CONDITIONALS, e);
        addCoverage(node, Metric.INSTRUCTION, STATEMENTS, COVERED_STATEMENTS, e);
        addCoverage(node, Metric.METHOD, METHODS, COVERED_METHODS, e);
    }

    private void addCoverage(final Node node, final Metric metric,
                             final QName coveredElement, final QName totalElement, final StartElement e) {
        int condTotal = getIntegerValueOf(e, coveredElement);
        int condCovered = getIntegerValueOf(e, totalElement);
        var builder = new CoverageBuilder();
        node.addValue(builder.withMetric(metric)
                .withCovered(condCovered)
                .withMissed(condTotal - condCovered).build());
    }
}
