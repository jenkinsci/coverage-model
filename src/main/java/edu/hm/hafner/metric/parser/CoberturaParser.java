package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.ClassNode;
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
 * A parser which parses reports made by Cobertura into a Java Object Model.
 *
 * @author Melissa Bauer
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class CoberturaParser extends XmlParser {
    private static final long serialVersionUID = -3625341318291829577L;

    /** Required attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName SOURCEFILENAME = new QName("filename");
    private static final QName SIGNATURE = new QName("signature");
    private static final QName HITS = new QName("hits");
    private static final QName COMPLEXITY = new QName("complexity");
    private static final QName NUMBER = new QName("number");

    /** Not required attributes of the XML elements. */
    private static final QName BRANCH = new QName("branch");
    private static final QName CONDITION_COVERAGE = new QName("condition-coverage");

    private FileNode currentFileNode;
    private Node currentNode;

    private int linesCovered = 0;
    private int linesMissed = 0;
    private int branchesCovered = 0;
    private int branchesMissed = 0;
    private boolean isSource;

    @Override
    public ModuleNode parse(final Reader reader) {
        SecureXmlParserFactory factory = new SecureXmlParserFactory();

        XMLEventReader r;
        try {
            r = factory.createXmlEventReader(reader);
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartElement()) {
                    startElement(e.asStartElement());
                }

                if (isSource && e.isCharacters()) {
                    String source = new PathUtil().getRelativePath(e.asCharacters().getData());
                    getRootNode().addSource(source);
                }

                if (e.isEndElement()) {
                    endElement(e.asEndElement());
                }
            }
        }
        catch (XMLStreamException ex) {
            throw new ParsingException(ex);
        }

        return getRootNode();
    }

    /**
     * Creates a node or a leaf depending on the given element type. Ignore coverage, source and condition
     *
     * @param element
     *         the complete tag element including attributes
     */
    @Override
    protected void startElement(final StartElement element) {
        String name = element.getName().toString();

        switch (name) {
            case "coverage":
                setRootNode(new ModuleNode(""));
                currentNode = getRootNode();
                break;

            case "source":
                isSource = true;
                break;

            case "package":
                String packageName = PackageNode.normalizePackageName(getValueOf(element, NAME));
                PackageNode packageNode = new PackageNode(packageName);
                getRootNode().addChild(packageNode);

                currentNode = packageNode;
                break;

            case "class": // currentNode = packageNode, classNode after
                handleClassElement(element);
                break;

            case "method": // currentNode = classNode, methodNode after
                Node methodNode = new MethodNode(getValueOf(element, NAME), getValueOf(element, SIGNATURE));

                int complexity = Integer.parseInt(getValueOf(element, COMPLEXITY));
                methodNode.addValue(new CyclomaticComplexity(complexity));

                currentNode.addChild(methodNode);
                currentNode = methodNode;
                break;

            case "line": // currentNode = methodNode or classNode
                handleLineElement(element);
                break;

            default:
                break;
        }
    }

    /**
     * Creates a class node and saves it to a map. This is necessary because classes occur before sourcefiles in the
     * report. But in the java model, classes are children of files.
     *
     * @param element
     *         the current report element
     */
    private void handleClassElement(final StartElement element) {
        final String classPath = getValueOf(element, NAME);
        ClassNode classNode = new ClassNode(new PathUtil().getRelativePath(classPath));

        // Gets sourcefilename and adds class to filenode if existing. Creates filenode if not existing
        String sourcefilePath = getValueOf(element, SOURCEFILENAME);
        String[] parts = sourcefilePath.split("/", 0);
        String sourcefileName = parts[parts.length - 1];

        Optional<Node> potentialNode = currentNode.find(Metric.FILE, sourcefileName);
        if (potentialNode.isPresent()) {
            currentFileNode = (FileNode) potentialNode.get();
            currentFileNode.addChild(classNode);
        }
        else {
            FileNode fileNode = new FileNode(sourcefileName);
            fileNode.addChild(classNode);
            currentNode.addChild(fileNode);
            currentFileNode = fileNode;
        }

        currentNode = classNode;
    }

    /**
     * Adds +1 to lines covered/missed and creates a new line or branch leaf for the file node.
     *
     * @param element
     *         the current report element
     */
    private void handleLineElement(final StartElement element) {

        int lineNumber = Integer.parseInt(getValueOf(element, NUMBER));
        int lineHits = Integer.parseInt(getValueOf(element, HITS));

        boolean isBranch = false;
        Attribute branchAttribute = element.getAttributeByName(BRANCH);
        if (branchAttribute != null) {
            isBranch = Boolean.parseBoolean(branchAttribute.getValue());
        }

        // collect linenumber to coverage information in "lines" part
        if (!currentNode.getMetric().equals(Metric.METHOD)) {
            getLineNumberToCoverage(element, lineNumber, lineHits, isBranch);
        }
        // only count lines/branches for method coverage
        else {
            computeMethodCoverage(element, lineHits, isBranch);
        }
    }

    private void getLineNumberToCoverage(final StartElement element, final int lineNumber, final int lineHits,
            final boolean isBranch) {
        var builder = new CoverageBuilder();

        if (isBranch) {
            String[] coveredAllInformation = parseConditionCoverage(element);
            int covered = Integer.parseInt(coveredAllInformation[0].substring(1));

            builder.setMetric(Metric.BRANCH);
            setCoveredVsMissed(builder, covered);

            currentFileNode.addBranchCoverage(lineNumber, builder.build());
        }
        else {
            builder.setMetric(Metric.LINE);
            setCoveredVsMissed(builder, lineHits);

            currentFileNode.addLineCoverage(lineNumber, builder.build());
        }
    }

    private void setCoveredVsMissed(final CoverageBuilder builder, final int coveredItems) {
        if (coveredItems > 0) {
            builder.setCovered(1).setMissed(0);
        }
        else {
            builder.setCovered(0).setMissed(1);
        }
    }

    private void computeMethodCoverage(final StartElement element, final int lineHits, final boolean isBranch) {
        if (!isBranch) {
            if (lineHits > 0) {
                linesCovered++;
            }
            else {
                linesMissed++;
            }
        }
        else {
            String[] coveredAllInformation = parseConditionCoverage(element);
            int covered = Integer.parseInt(coveredAllInformation[0].substring(1));
            int all = Integer.parseInt(StringUtils.chop(coveredAllInformation[1]));

            branchesCovered = branchesCovered + covered;
            branchesMissed = branchesMissed + (all - covered);
        }
    }

    /**
     * Depending on the tag, either resets the map containing the class objects or sets the current node back to the
     * class node.
     *
     * @param element
     *         current xml element
     */
    @Override
    protected void endElement(final EndElement element) {
        switch (element.getName().toString()) {
            case "source":
                isSource = false;
                break;
            case "package": // reset
                currentNode = getRootNode();
                break;

            case "class":
                currentNode = currentNode.getParent();
                break;

            case "method": // currentNode = methodNode, classNode after
                // create leaves
                var builder = new CoverageBuilder();
                builder.setMetric(Metric.LINE).setCovered(linesCovered).setMissed(linesMissed);
                currentNode.addValue(builder.build());

                if (branchesMissed + branchesCovered > 0) {
                    builder.setMetric(Metric.BRANCH).setCovered(branchesCovered).setMissed(branchesMissed);
                    currentNode.addValue(builder.build());
                }

                // reset values
                linesCovered = 0;
                linesMissed = 0;
                branchesCovered = 0;
                branchesMissed = 0;

                currentNode = currentNode.getParent(); // go to class node
                break;
            default:
                break;
        }
    }

    private String[] parseConditionCoverage(final StartElement element) {
        String conditionCoverageAttribute = getValueOf(element, CONDITION_COVERAGE);
        String[] conditionCoverage = conditionCoverageAttribute.split(" ", 0);
        return conditionCoverage[1].split("/", 0);
    }
}
