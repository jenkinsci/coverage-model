package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CyclomaticComplexity;
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

/**
 * Parses Cobertura reports into a hierarchical Java Object Model.
 *
 * @author Melissa Bauer
 * @author Ullrich Hafner
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "PMD.GodClass"})
public class CoberturaParser extends CoverageParser {
    private static final long serialVersionUID = -3625341318291829577L;

    private static final Pattern BRANCH_PATTERN = Pattern.compile(".*\\((?<covered>\\d+)/(?<total>\\d+)\\)");
    private static final PathUtil PATH_UTIL = new PathUtil();

    private static final Coverage DEFAULT_BRANCH_COVERAGE = new CoverageBuilder(Metric.BRANCH).withCovered(2).withMissed(0).build();
    private static final Coverage LINE_COVERED = new CoverageBuilder(Metric.LINE).withCovered(1).withMissed(0).build();
    private static final Coverage LINE_MISSED = new CoverageBuilder(Metric.LINE).withCovered(0).withMissed(1).build();

    /** XML elements. */
    private static final QName SOURCE = new QName("source");
    private static final QName PACKAGE = new QName("package");
    private static final QName CLASS = new QName("class");
    private static final QName METHOD = new QName("method");
    private static final QName LINE = new QName("line");

    /** Required attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName FILE_NAME = new QName("filename");
    private static final QName SIGNATURE = new QName("signature");
    private static final QName HITS = new QName("hits");
    private static final QName COMPLEXITY = new QName("complexity");
    private static final QName NUMBER = new QName("number");

    /** Optional attributes of the XML elements. */
    private static final QName BRANCH = new QName("branch");
    private static final QName CONDITION_COVERAGE = new QName("condition-coverage");

    /**
     * Creates a new instance of {@link CoberturaParser}.
     */
    public CoberturaParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link CoberturaParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public CoberturaParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try {
            var eventReader = new SecureXmlParserFactory().createXmlEventReader(reader);

            var root = new ModuleNode(EMPTY); // Cobertura has no support for module names
            handleEmptyResults(fileName, log, readModule(eventReader, root, fileName, log));
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private boolean readModule(final XMLEventReader eventReader, final ModuleNode root,
            final String fileName, final FilteredLog log) throws XMLStreamException {
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
                    readPackage(eventReader, root, readName(startElement), fileName, log);
                    isEmpty = false;
                }
            }
        }

        return isEmpty;
    }

    private void readPackage(final XMLEventReader reader, final ModuleNode root,
            final String packageName, final String fileName, final FilteredLog log) throws XMLStreamException {
        var packageNode = root.findOrCreatePackageNode(packageName);

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var element = event.asStartElement();
                if (CLASS.equals(element.getName())) {
                    var fileNode = createFileNode(element, packageNode);

                    readClassOrMethod(reader, fileNode, fileNode, element, fileName, log);
                }
            }
            else if (event.isEndElement()) {
                return; // finish processing of package
            }
        }
    }

    private FileNode createFileNode(final StartElement element, final PackageNode packageNode) {
        var fileName = getValueOf(element, FILE_NAME);
        var relativePath = PATH_UTIL.getRelativePath(fileName);
        var actualPath = relativePath.startsWith("/_/") ? 
                         relativePath.replace("/_/", "./") : 
                         relativePath;
        var finalPath = getTreeStringBuilder().intern(actualPath);
        return packageNode.findOrCreateFileNode(getFileName(fileName), finalPath);
    }

    private String getFileName(final String relativePath) {
        var fileName = Paths.get(PATH_UTIL.getAbsolutePath(relativePath)).getFileName();
        var actualFileName = (fileName != null && fileName.toString().startsWith("/_/")) ? 
                              fileName.toString().replace("/_/", "./") : 
                              (fileName != null ? fileName.toString() : relativePath);
        return actualFileName;
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    protected void readClassOrMethod(final XMLEventReader reader, final FileNode fileNode,
            final Node parentNode, final StartElement element, final String fileName, final FilteredLog log)
            throws XMLStreamException {
        var lineCoverage = Coverage.nullObject(Metric.LINE);
        var branchCoverage = Coverage.nullObject(Metric.BRANCH);

        Node node = createNode(parentNode, element, log);
        getOptionalValueOf(element, COMPLEXITY)
                .ifPresent(c -> node.addValue(new CyclomaticComplexity(readComplexity(c))));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (LINE.equals(nextElement.getName())) {
                    Coverage coverage;
                    Coverage currentLineCoverage;
                    if (isBranchCoverage(nextElement)) {
                        coverage = readBranchCoverage(nextElement);
                        currentLineCoverage = computeLineCoverage(coverage.getCovered());

                        branchCoverage = branchCoverage.add(coverage);
                    }
                    else {
                        int lineHits = getIntegerValueOf(nextElement, HITS);
                        currentLineCoverage = computeLineCoverage(lineHits);
                        coverage = currentLineCoverage;
                    }
                    lineCoverage = lineCoverage.add(currentLineCoverage);

                    if (CLASS.equals(element.getName())) { // Use the line counters at the class level for a file
                        int lineNumber = getIntegerValueOf(nextElement, NUMBER);
                        fileNode.addCounters(lineNumber, coverage.getCovered(), coverage.getMissed());
                    }
                }
                else if (METHOD.equals(nextElement.getName())) {
                    readClassOrMethod(reader, fileNode, node, nextElement, fileName, log); // recursive call
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName()) || METHOD.equals(endElement.getName())) {
                    node.addValue(lineCoverage);
                    if (branchCoverage.isSet()) {
                        node.addValue(branchCoverage);
                    }
                    return;
                }
            }
        }
        throw createEofException(fileName);
    }

    protected Coverage computeLineCoverage(final int coverage) {
        return coverage > 0 ? LINE_COVERED : LINE_MISSED;
    }

    protected Node createNode(final Node parentNode, final StartElement element, final FilteredLog log) {
        var name = readName(element);
        if (CLASS.equals(element.getName())) {
            return createClassNode(parentNode, log, name);
        }

        return createMethodNode(parentNode, element, log, name);
    }

    private MethodNode createMethodNode(final Node parentNode, final StartElement element, final FilteredLog log,
            final String name) {
        String methodName = name;
        var signature = getValueOf(element, SIGNATURE);
        if (parentNode.findMethod(methodName, signature).isPresent() && ignoreErrors()) {
            log.logError("Found a duplicate method '%s' with signature '%s' in '%s'",
                    methodName, signature, parentNode.getName());
            methodName = name + "-" + createId();
        }
        return parentNode.createMethodNode(methodName, signature);
    }

    private ClassNode createClassNode(final Node parentNode, final FilteredLog log, final String name) {
        String className = name;
        if (parentNode.hasChild(className) && ignoreErrors()) {
            log.logError("Found a duplicate class '%s' in '%s'", className, parentNode.getName());
            className = name + "-" + createId();
        }
        return parentNode.createClassNode(className);
    }

    private String readName(final StartElement element) {
        return StringUtils.defaultIfBlank(getValueOf(element, NAME), createId());
    }

    private String createId() {
        return UUID.randomUUID().toString();
    }

    protected int readComplexity(final String c) {
        try {
            return Math.round(Float.parseFloat(c)); // some reports use float values
        }
        catch (NumberFormatException ignore) {
            return 0;
        }
    }

    protected boolean isBranchCoverage(final StartElement line) {
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

    protected Coverage readBranchCoverage(final StartElement line) {
        return getOptionalValueOf(line, CONDITION_COVERAGE).map(this::fromConditionCoverage).orElse(DEFAULT_BRANCH_COVERAGE);
    }

    private Coverage fromConditionCoverage(final String conditionCoverageAttribute) {
        var matcher = BRANCH_PATTERN.matcher(conditionCoverageAttribute);
        if (matcher.matches()) {
            return new CoverageBuilder().withMetric(Metric.BRANCH)
                    .withCovered(matcher.group("covered"))
                    .withTotal(matcher.group("total"))
                    .build();
        }
        return Coverage.nullObject(Metric.BRANCH);
    }
}
