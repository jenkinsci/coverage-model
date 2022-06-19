package edu.hm.hafner.parser;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;
import edu.hm.hafner.coverage.CoverageMetric;
import edu.hm.hafner.coverage.CoverageNode;
import edu.hm.hafner.coverage.FileCoverageNode;
import edu.hm.hafner.coverage.PackageCoverageNode;

/**
 * A parser which parses reports made by Cobertura into a Java Object Model.
 *
 * @author Melissa Bauer
 */
public class CoberturaParser extends XmlParser {

    /** Required attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName SOURCEFILENAME = new QName("filename");
    private static final QName HITS = new QName("hits");
    private static final QName COMPLEXITY = new QName("complexity");
    private static final QName NUMBER = new QName("number");

    /** Not required attributes of the XML elements. */
    private static final QName BRANCH = new QName("branch");
    private static final QName CONDITION_COVERAGE = new QName("condition-coverage");

    private PackageCoverageNode currentPackageNode;
    private FileCoverageNode currentFileNode;
    private CoverageNode currentNode;

    private int linesCovered = 0;
    private int linesMissed = 0;
    private int branchesCovered = 0;
    private int branchesMissed = 0;

    /**
     * Creates a new JacocoParser which parses the given Jacoco xml report into a java data model.
     *
     * @param path
     *         path to report file
     */
    public CoberturaParser(final String path) {
        super.parseFile(path);
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

        setRootNode(new CoverageNode(CoverageMetric.MODULE, filename));
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
            case "package":
                String packageName = element.getAttributeByName(NAME).getValue();
                PackageCoverageNode packageNode = new PackageCoverageNode(packageName);
                getRootNode().add(packageNode);

                currentPackageNode = packageNode; // save for later to be able to add fileNodes
                currentNode = packageNode;
                break;

            case "class": // currentNode = packageNode, classNode after
                handleClassElement(element);
                break;

            case "method": // currentNode = classNode, methodNode after
                CoverageNode methodNode = new CoverageNode(CoverageMetric.METHOD,
                        element.getAttributeByName(NAME).getValue());

                long complexity = Long.parseLong(element.getAttributeByName(COMPLEXITY).getValue());
                CoverageLeaf complexityLeaf = new CoverageLeaf(CoverageMetric.COMPLEXITY,
                        new Coverage((int) complexity, 0));

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
        CoverageNode classNode = new CoverageNode(CoverageMetric.CLASS, element.getAttributeByName(NAME).getValue());

        // Gets sourcefilename and adds class to filenode if existing. Creates filenode if not existing
        String sourcefilePath = element.getAttributeByName(SOURCEFILENAME).getValue();
        String[] parts = sourcefilePath.split("/", 0);
        String sourcefileName = parts[parts.length - 1];

        List<CoverageNode> fileNodes = currentPackageNode.getChildren();

        // add class node to file node if found
        AtomicBoolean found = new AtomicBoolean(false);
        if (!fileNodes.isEmpty()) {
            fileNodes.forEach(fileNode -> {
                if (fileNode.getName().equals(sourcefileName)) {
                    fileNode.add(classNode);
                    found.set(true);
                    currentFileNode = (FileCoverageNode) fileNode;
                }
            });
        }

        // create new file node if not found/not existing
        if (!found.get()) {
            FileCoverageNode fileNode = new FileCoverageNode(sourcefileName);
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

        // collect linenumber to coverage information
        if (!currentNode.getMetric().equals(CoverageMetric.METHOD)) {
            Coverage coverage;
            CoverageLeaf coverageLeaf;

            if (!isBranch) {
                if (lineHits > 0) {
                    coverage = new Coverage(1, 0);
                }
                else {
                    coverage = new Coverage(0, 1);
                }

                coverageLeaf = new CoverageLeaf(CoverageMetric.LINE, coverage);
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

                coverageLeaf = new CoverageLeaf(CoverageMetric.BRANCH, coverage);
                currentFileNode.getLineNumberToBranchCoverage().put(lineNumber, coverageLeaf);
            }
        }
        // only count lines/branches for method coverage
        else {
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
            case "package": // reset
                currentNode = getRootNode();
                break;

            case "method": // currentNode = methodNode, classNode after
                // create leaves
                Coverage lineCoverage = new Coverage(linesCovered, linesMissed);
                CoverageLeaf lines = new CoverageLeaf(CoverageMetric.LINE, lineCoverage);

                Coverage branchCoverage = new Coverage(branchesCovered, branchesMissed);
                CoverageLeaf branches = new CoverageLeaf(CoverageMetric.BRANCH, branchCoverage);

                currentNode.add(lines);
                currentNode.add(branches);

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