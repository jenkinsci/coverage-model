package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import edu.hm.hafner.coverage.ClassNode;
import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.CyclomaticComplexity;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.MethodNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * A parser which parses reports made by OpenCover into a Java Object Model.
 *
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling", "PMD.GodClass"})
public class OpenCoverParser extends CoverageParser {
    private static final long serialVersionUID = 1L;

    private static final PathUtil PATH_UTIL = new PathUtil();

    /** XML elements. */
    private static final QName MODULE = new QName("Module");
    private static final QName CLASS = new QName("Class");
    private static final QName METHOD = new QName("Method");
    private static final QName CLASS_NAME = new QName("FullName");
    private static final QName METHOD_NAME = new QName("Name");
    private static final QName MODULE_NAME = new QName("ModuleName");
    private static final QName FILE = new QName("File");
    private static final QName FILE_REF = new QName("FileRef");
    private static final QName SUMMARY = new QName("Summary");
    private static final QName SEQUENCE_POINTS = new QName("SequencePoints");
    private static final QName SEQUENCE_POINT = new QName("SequencePoint");
    private static final QName BRANCH_POINTS = new QName("BranchPoints");
    private static final QName BRANCH_POINT = new QName("BranchPoint");

    private static final QName SOURCE_LINE_NUMBER = new QName("sl");
    private static final QName SOURCE_LINE_HINT = new QName("vc");
    private static final QName MODULE_SKIPPED = new QName("skippedDueTo");
    private static final QName METHOD_VISITED = new QName("visited");
    private static final QName UID = new QName("uid");
    private static final QName FULL_PATH = new QName("fullPath");
    private static final QName METHOD_INTRUCTION_COVERED = new QName("visitedSequencePoints");
    private static final QName METHOD_INTRUCTION_TOTAL = new QName("numSequencePoints");
    private static final QName METHOD_BRANCH_COVERED = new QName("visitedBranchPoints");
    private static final QName METHOD_BRANCH_TOTAL = new QName("numBranchPoints");
    private static final QName METHOD_CYCLOMATIC_COMPLEXITY = new QName("cyclomaticComplexity");

    /**
     * Creates a new instance of {@link OpenCoverParser}.
     */
    public OpenCoverParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link OpenCoverParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public OpenCoverParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    @SuppressWarnings("PMD.CyclomaticComplexity")
    protected ModuleNode parseReport(final Reader reader, final FilteredLog log) {
        try {
            var eventReader = new SecureXmlParserFactory().createXmlEventReader(reader);
            var root = new ModuleNode(EMPTY);
            var isEmpty = true;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    var startElement = event.asStartElement();
                    var tagName = startElement.getName();
                    if (MODULE.equals(tagName) && startElement.getAttributeByName(MODULE_SKIPPED) == null) {
                        isEmpty = readModule(eventReader, root);
                        if (!isEmpty) {
                            return root;
                        }
                    }
                }
            }
            handleEmptyResults(log, isEmpty);
            return new ModuleNode("empty");
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private boolean readModule(final XMLEventReader reader, final ModuleNode root) throws XMLStreamException {
        Map<String, String> files = new LinkedHashMap<>();
        List<CoverageClassHolder> classes = new LinkedList<>();
        boolean isEmpty = true;
        PackageNode packageNode = new PackageNode(EMPTY);
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS.equals(nextElement.getName())) {
                    classes.add(readClass(reader));
                }
                else if (FILE.equals(nextElement.getName())) {
                    var fileName = getValueOf(nextElement, FULL_PATH);
                    var uid = getValueOf(nextElement, UID);
                    var relativePath = PATH_UTIL.getRelativePath(fileName);
                    files.put(uid, relativePath);
                }
                else if (MODULE_NAME.equals(nextElement.getName())) {
                    String packageName = reader.nextEvent().asCharacters().getData();
                    packageNode = root.findOrCreatePackageNode(packageName);
                    isEmpty = false;
                }
            }
            else if (event.isEndElement()) {
                var nextElement = event.asEndElement();
                if (MODULE.equals(nextElement.getName())) {
                    break;
                }
            }
        }

        if (isEmpty) {
            return true;
        }

        // Creating all nodes
        for (var file : files.entrySet()) {
            FileNode fileNode = packageNode.findOrCreateFileNode(getFileName(file.getValue()), getTreeStringBuilder().intern(file.getValue()));
            for (CoverageClassHolder clazz : classes) {
                if (clazz.hasMethods() && clazz.getFileId() != null && clazz.getFileId().equals(file.getKey())) {
                    ClassNode classNode = fileNode.createClassNode(clazz.getClassName());
                    for (var method : clazz.getMethods()) {
                        if (classNode.findMethod(method.getMethodName(), method.getMethodName()).isEmpty()) {
                            createPoints(fileNode, classNode, method);
                        }
                    }
                }
            }
        }
        return false;
    }

    private void createPoints(final FileNode fileNode, final ClassNode classNode, final CoverageMethod method) {
        MethodNode methodNode = classNode.createMethodNode(method.getMethodName(), method.getMethodName());
        var builder = new CoverageBuilder();
        var branchCoverage = builder.withMetric(Metric.BRANCH)
                    .withCovered(method.getBranchCovered())
                    .withMissed(method.getBranchMissed()).build();
        var instructionCoverage = builder.withMetric(Metric.INSTRUCTION)
                    .withCovered(method.getInstructionCovered())
                    .withMissed(method.getInstructionMissed()).build();
        var lineCoverage = builder.withMetric(Metric.LINE)
                    .withCovered(method.getInstructionCovered())
                    .withMissed(method.getInstructionMissed()).build();
        methodNode.addValue(lineCoverage);
        methodNode.addValue(branchCoverage);
        methodNode.addValue(instructionCoverage);
        methodNode.addValue(new CyclomaticComplexity(method.getComplexity(), Metric.COMPLEXITY));
        Map<Integer, Pair<Integer, Integer>> points = new LinkedHashMap<>();

        // Line coverage only
        for (var sequencePoint : method.getSequencePoints()) {
            points.put(sequencePoint.getLineNumber(), Pair.of(sequencePoint.getHint(), 0));
        }

        // Branch coverage (update existing line coverage)
        for (var branchPoint : method.getBranchPoints()) {
            if (points.containsKey(branchPoint.getLineNumber())) {
                points.put(branchPoint.getLineNumber(), Pair.of(points.get(branchPoint.getLineNumber()).getLeft(), branchPoint.getRight()));
            }
        }

        // Create all counters for each point
        for (var point : points.entrySet()) {
            addCounters(fileNode, point.getKey(), point.getValue().getLeft(), point.getValue().getRight());
        }
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private CoverageClassHolder readClass(final XMLEventReader reader) throws XMLStreamException {
        String className = StringUtils.EMPTY;
        List<CoverageMethod> methods = new LinkedList<>();
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (CLASS_NAME.equals(nextElement.getName())) {
                    className = reader.nextEvent().asCharacters().getData();
                }
                // Only add visited methods
                var visited = nextElement.getAttributeByName(METHOD_VISITED);
                if (METHOD.equals(nextElement.getName()) && (visited == null || visited.getValue().equals("true"))) {
                    methods.add(readMethod(reader, nextElement));
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (CLASS.equals(endElement.getName())) {
                    return new CoverageClassHolder(className, methods);
                }
            }
        }

        throw new NoSuchElementException("Unable to parse class " + className);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private CoverageMethod readMethod(final XMLEventReader reader, final StartElement parentElement) throws XMLStreamException {
        CoverageMethod coverageMethod = new CoverageMethod();
        coverageMethod.setComplexity(getIntegerValueOf(parentElement, METHOD_CYCLOMATIC_COMPLEXITY));
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (METHOD_NAME.equals(nextElement.getName())) {
                    coverageMethod.setMethodName(reader.nextEvent().asCharacters().getData());
                }
                if (SUMMARY.equals(nextElement.getName())) {
                    readMethodSummary(coverageMethod, nextElement);
                }
                if (BRANCH_POINTS.equals(nextElement.getName())) {
                    readBranchPoints(reader, coverageMethod);
                }
                if (SEQUENCE_POINTS.equals(nextElement.getName())) {
                    readSequencePoints(reader, coverageMethod);
                }
                if (FILE_REF.equals(nextElement.getName())) {
                    coverageMethod.setFileId(getValueOf(nextElement, UID));
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (METHOD.equals(endElement.getName())) {
                    return coverageMethod;
                }
            }
        }

        throw new NoSuchElementException("Unable to parse method " + coverageMethod.getMethodName());
    }

    private void readMethodSummary(final CoverageMethod coverageMethod, final StartElement startElement) {
        coverageMethod.setBranchCovered(getIntegerValueOf(startElement, METHOD_BRANCH_COVERED));
        coverageMethod.setBranchMissed(getIntegerValueOf(startElement, METHOD_BRANCH_TOTAL) - coverageMethod.getBranchCovered());
        coverageMethod.setInstructionCovered(getIntegerValueOf(startElement, METHOD_INTRUCTION_COVERED));
        coverageMethod.setInstructionMissed(getIntegerValueOf(startElement, METHOD_INTRUCTION_TOTAL) - coverageMethod.getInstructionCovered());
    }

    private void readSequencePoints(final XMLEventReader reader, final CoverageMethod coverageMethod) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (SEQUENCE_POINT.equals(nextElement.getName()) && nextElement.getAttributeByName(SOURCE_LINE_NUMBER) != null) {
                    coverageMethod.getSequencePoints().add(new CoverageHint(
                            getIntegerValueOf(nextElement, SOURCE_LINE_NUMBER),
                            getIntegerValueOf(nextElement, SOURCE_LINE_HINT)
                    ));
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (SEQUENCE_POINT.equals(endElement.getName()) || SEQUENCE_POINTS.equals(endElement.getName())) {
                    return;
                }
            }
        }
    }

    private void readBranchPoints(final XMLEventReader reader, final CoverageMethod coverageMethod) throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();
            if (event.isStartElement()) {
                var nextElement = event.asStartElement();
                if (BRANCH_POINT.equals(nextElement.getName()) && nextElement.getAttributeByName(SOURCE_LINE_NUMBER) != null) {
                    coverageMethod.getBranchPoints().add(new CoverageHint(
                            getIntegerValueOf(nextElement, SOURCE_LINE_NUMBER),
                            getIntegerValueOf(nextElement, SOURCE_LINE_HINT)
                    ));
                }
            }
            else if (event.isEndElement()) {
                var endElement = event.asEndElement();
                if (BRANCH_POINT.equals(endElement.getName()) || BRANCH_POINTS.equals(endElement.getName())) {
                    return;
                }
            }
        }
    }

    private void addCounters(final FileNode fileNode, final int lineNumber, final int coveredInstructions, final int coveredBranches) {
        int missed;
        int covered;
        if (coveredBranches == 0) { // only instruction coverage found
            covered = coveredInstructions > 0 ? 1 : 0;
            missed = covered > 0 ? 0 : 1;
        }
        else {
            covered = coveredBranches;
            missed = coveredBranches - coveredInstructions;
        }
        fileNode.addCounters(lineNumber, covered, missed);
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private String getFileName(final String relativePath) {
        return Paths.get(PATH_UTIL.getAbsolutePath(relativePath)).getFileName().toString();
    }

    private static class CoverageClassHolder extends MutablePair<String, List<CoverageMethod>> {
        private static final long serialVersionUID = 1L;

        CoverageClassHolder(final String className, final List<CoverageMethod> methods) {
            super(className, methods);
        }

        boolean hasMethods() {
            return !getMethods().isEmpty();
        }

        List<CoverageMethod> getMethods() {
            return getRight();
        }

        String getClassName() {
            return getLeft();
        }

        String getFileId() {
            return getMethods().get(0).getFileId();
        }
    }

    private static class CoverageHint extends MutablePair<Integer, Integer> {
        private static final long serialVersionUID = 1L;

        CoverageHint(final Integer lineNumber, final Integer hint) {
            super(lineNumber, hint);
        }

        Integer getLineNumber() {
            return getLeft();
        }

        Integer getHint() {
            return getRight();
        }
    }

    private static class CoverageMethod {
        private String methodName;
        private String fileId;
        private int instructionCovered;
        private int instructionMissed;
        private int branchCovered;
        private int branchMissed;
        private int complexity;
        private final List<CoverageHint> sequencePoints = new ArrayList<>();
        private final List<CoverageHint> branchPoints = new ArrayList<>();

        String getMethodName() {
            return methodName;
        }

        void setMethodName(final String methodName) {
            this.methodName = methodName;
        }

        String getFileId() {
            return fileId;
        }

        void setFileId(final String fileId) {
            this.fileId = fileId;
        }

        int getInstructionCovered() {
            return instructionCovered;
        }

        void setInstructionCovered(final int instructionCovered) {
            this.instructionCovered = instructionCovered;
        }

        int getInstructionMissed() {
            return instructionMissed;
        }

        void setInstructionMissed(final int instructionMissed) {
            this.instructionMissed = instructionMissed;
        }

        int getBranchCovered() {
            return branchCovered;
        }

        void setBranchCovered(final int branchCovered) {
            this.branchCovered = branchCovered;
        }

        int getBranchMissed() {
            return branchMissed;
        }

        void setBranchMissed(final int branchMissed) {
            this.branchMissed = branchMissed;
        }

        int getComplexity() {
            return complexity;
        }

        void setComplexity(final int complexity) {
            this.complexity = complexity;
        }

        List<CoverageHint> getSequencePoints() {
            return sequencePoints;
        }

        List<CoverageHint> getBranchPoints() {
            return branchPoints;
        }
    }
}
