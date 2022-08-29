package edu.hm.hafner.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.complexity.ComplexityLeaf;
import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;
import edu.hm.hafner.model.ClassNode;
import edu.hm.hafner.model.FileNode;
import edu.hm.hafner.model.MethodNode;
import edu.hm.hafner.model.Metric;
import edu.hm.hafner.model.ModuleNode;
import edu.hm.hafner.model.Node;
import edu.hm.hafner.model.PackageNode;
import edu.hm.hafner.util.PathUtil;

/**
 * A parser which parses reports made by Cobertura into a Java Object Model.
 *
 * @author Melissa Bauer
 */
public class CoberturaParser extends XmlParser {
    private static final long serialVersionUID = -3625341318291829577L;

    /** Required attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName SOURCEFILENAME = new QName("filename");
    private static final QName HITS = new QName("hits");
    private static final QName COMPLEXITY = new QName("complexity");
    private static final QName NUMBER = new QName("number");

    /** Not required attributes of the XML elements. */
    private static final QName BRANCH = new QName("branch");
    private static final QName CONDITION_COVERAGE = new QName("condition-coverage");

    private PackageNode currentPackageNode;
    private FileNode currentFileNode;
    private Node currentNode;

    private int linesCovered = 0;
    private int linesMissed = 0;
    private int branchesCovered = 0;
    private int branchesMissed = 0;
    private boolean isSource;

    /**
     * Creates a new JacocoParser which parses the given Jacoco xml report into a java data model.
     *
     * @param path
     *         path to report file
     */
    public CoberturaParser(final String path) {
        parseFile(path);
    }

    @Override
    void parseFile(final String path) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLEventReader r;
        try (FileInputStream fip = new FileInputStream(path)) {
            r = factory.createXMLEventReader(path, fip);
            while (r.hasNext()) {
                XMLEvent e = r.nextEvent();

                if (e.isStartDocument()) {
                    startDocument(e);
                }

                else if (e.isStartElement()) {
                    startElement(e.asStartElement());
                }

                else if (isSource && e.isCharacters()) {
                    String source = new PathUtil().getRelativePath(e.asCharacters().getData());
                    getRootNode().addSource(source);
                }

                else if (e.isEndElement()) {
                    endElement(e.asEndElement());
                }
            }
        }
        catch (XMLStreamException | IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Gets the document name and creates the root node of the data tree.
     *
     * @param event
     *         the xml element
     */
    @Override
    protected void startDocument(final XMLEvent event) {
        String systemId = event.getLocation().getSystemId();
        String[] parts = systemId.split("/", 0);
        String filename = parts[parts.length - 1];

        setRootNode(new ModuleNode(filename));
        currentNode = getRootNode();
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
            case "source":
                isSource = true;
                break;
            case "package":
                String packageName = element.getAttributeByName(NAME).getValue();
                PackageNode packageNode = new PackageNode(packageName.replace("/", "."));
                getRootNode().add(packageNode);

                currentPackageNode = packageNode; // save for later to be able to add fileNodes
                currentNode = packageNode;
                break;

            case "class": // currentNode = packageNode, classNode after
                handleClassElement(element);
                break;

            case "method": // currentNode = classNode, methodNode after
                Node methodNode = new MethodNode(element.getAttributeByName(NAME).getValue());

                long complexity = Long.parseLong(element.getAttributeByName(COMPLEXITY).getValue());
                ComplexityLeaf complexityLeaf = new ComplexityLeaf((int) complexity);

                methodNode.add(complexityLeaf);

                currentNode.add(methodNode);
                currentNode = methodNode;
                break;

            case "line": // currentNode = methodNode or classNode
                handleLineElement(element);
                break;
            default: break;
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
        final String classPath = element.getAttributeByName(NAME).getValue();
        ClassNode classNode = new ClassNode(new PathUtil().getRelativePath(classPath));

        // Gets sourcefilename and adds class to filenode if existing. Creates filenode if not existing
        String sourcefilePath = element.getAttributeByName(SOURCEFILENAME).getValue();
        String[] parts = sourcefilePath.split("/", 0);
        String sourcefileName = parts[parts.length - 1];

        List<Node> fileNodes = currentPackageNode.getChildren();

        // add class node to file node if found
        AtomicBoolean found = new AtomicBoolean(false);
        if (!fileNodes.isEmpty()) {
            fileNodes.forEach(fileNode -> {
                if (fileNode.getName().equals(sourcefileName)) {
                    fileNode.add(classNode);
                    found.set(true);
                    currentFileNode = (FileNode) fileNode;
                }
            });
        }

        // create new file node if not found/not existing
        if (!found.get()) {
            FileNode fileNode = new FileNode(sourcefileName);
            fileNode.add(classNode);
            currentPackageNode.add(fileNode);
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

        int lineNumber = Integer.parseInt(element.getAttributeByName(NUMBER).getValue());
        int lineHits = Integer.parseInt(element.getAttributeByName(HITS).getValue());

        boolean isBranch = false;
        Attribute branchAttribute = element.getAttributeByName(BRANCH);
        if (branchAttribute != null) {
            isBranch = Boolean.parseBoolean(branchAttribute.getValue());
        }

        // collect linenumber to coverage information in "lines" part
        if (!currentNode.getMetric().equals(Metric.METHOD)) {
            getLinenumberToCoverage(element, lineNumber, lineHits, isBranch);
        }
        // only count lines/branches for method coverage
        else {
            computeMethodCoverage(element, lineHits, isBranch);
        }
    }

    private void getLinenumberToCoverage(final StartElement element, final int lineNumber, final int lineHits,
            final boolean isBranch) {
        Coverage coverage;
        CoverageLeaf coverageLeaf;

        if (!isBranch) {
            if (lineHits > 0) {
                coverage = new Coverage(1, 0);
            }
            else {
                coverage = new Coverage(0, 1);
            }

            coverageLeaf = new CoverageLeaf(Metric.LINE, coverage);
            currentFileNode.getLineNumberToInstructionCoverage().put(lineNumber, coverageLeaf);
        }
        else {
            String[] coveredAllInformation = parseConditionCoverage(element);
            int covered = Integer.parseInt(coveredAllInformation[0].substring(1));

            if (covered > 0) {
                coverage = new Coverage(1, 0);
            }
            else {
                coverage = new Coverage(0, 1);
            }

            coverageLeaf = new CoverageLeaf(Metric.BRANCH, coverage);
            currentFileNode.getLineNumberToBranchCoverage().put(lineNumber, coverageLeaf);
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

            if (covered > 0) {
                branchesCovered = branchesCovered + covered;
            }
            else {
                branchesMissed = branchesMissed + (all - covered);
            }
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

            case "method": // currentNode = methodNode, classNode after
                // create leaves
                Coverage lineCoverage = new Coverage(linesCovered, linesMissed);
                CoverageLeaf lines = new CoverageLeaf(Metric.LINE, lineCoverage);
                currentNode.add(lines);

                if (branchesMissed + branchesCovered > 0) {
                    Coverage branchCoverage = new Coverage(branchesCovered, branchesMissed);
                    CoverageLeaf branches = new CoverageLeaf(Metric.BRANCH, branchCoverage);
                    currentNode.add(branches);
                }

                // reset values
                linesCovered = 0;
                linesMissed = 0;
                branchesCovered = 0;
                branchesMissed = 0;

                currentNode = currentNode.getParent(); // go to class node
                break;
            default: break;
        }
    }

    private String[] parseConditionCoverage(final StartElement element) {
        String conditionCoverageAttribute = element.getAttributeByName(CONDITION_COVERAGE).getValue();
        String[] conditionCoverage = conditionCoverageAttribute.split(" ", 0);
        return conditionCoverage[1].split("/", 0);
    }
}