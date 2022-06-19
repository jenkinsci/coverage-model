package edu.hm.hafner.parser;

import java.util.ArrayList;
import java.util.HashMap;
import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.coverage.CoverageLeaf;
import edu.hm.hafner.coverage.CoverageMetric;
import edu.hm.hafner.coverage.CoverageNode;
import edu.hm.hafner.coverage.FileCoverageNode;
import edu.hm.hafner.coverage.PackageCoverageNode;

/**
 * A parser which parses reports made by Jacoco into a Java Object Model.
 *
 * @author Melissa Bauer
 */
public class JacocoParser extends XmlParser {

    /** Attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName SOURCEFILENAME = new QName("sourcefilename");
    private static final QName TYPE = new QName("type");
    private static final QName MISSED = new QName("missed");
    private static final QName COVERED = new QName("covered");
    private static final QName NR = new QName("nr");
    private static final QName MI = new QName("mi");
    private static final QName CI = new QName("ci");
    private static final QName MB = new QName("mb");
    private static final QName CB = new QName("cb");

    private CoverageNode currentPackageNode;
    private CoverageNode currentNode;
    private String filename;
    private HashMap<String, ArrayList<CoverageNode>> classNodesMap = new HashMap<>();

    /**
     * Creates a new JacocoParser which parses the given Jacoco xml report into a java data model.
     *
     * @param path path to report file
     */
    public JacocoParser(final String path) {
        parseFile(path);
    }

    @Override
    protected void startDocument(final XMLEvent event) {
        String systemId = event.getLocation().getSystemId();
        String[] parts = systemId.split("/");
        filename = parts[parts.length - 1];
    }

    /**
     * Creates a node or a leaf depending on the given element type.
     * Ignore group and sessioninfo.
     *
     * @param element the complete tag element including attributes
     */
    @Override
    protected void startElement(final StartElement element) {
        String name = element.getName().toString();

        switch (name) {
            case "report": // currentNode = null, rootNode after
                // module name consists of name attribute of the element together with the filename, divided with :
                String moduleName = element.getAttributeByName(NAME).getValue() + ": " + filename;

                setRootNode(new CoverageNode(CoverageMetric.MODULE, moduleName));
                currentNode = getRootNode();
                break;

            case "package": // currentNode = rootNode, packageNode after
                String packageName = element.getAttributeByName(NAME).getValue();
                CoverageNode packageNode = new PackageCoverageNode(packageName.replaceAll("/", "."));
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

                currentNode.add(methodNode);
                currentNode = methodNode;
                break;

            case "counter": // currentNode = methodNode, methodNode after
                handleCounterElement(element);
                break;

            case "sourcefile": // currentNode = packageNode, fileNode after
                String fileName = element.getAttributeByName(NAME).getValue();
                CoverageNode fileNode = new FileCoverageNode(fileName);

                // add all classNodes to current fileNode
                ArrayList<CoverageNode> classNodeList = classNodesMap.get(fileName);
                for (CoverageNode classNode : classNodeList) {
                    fileNode.add(classNode);
                }

                currentPackageNode.add(fileNode);
                currentNode = fileNode;
                break;

            case "line": // currentNode = fileNode, fileNode after
                handleLineElement(element);
                break;
        }
    }

    /**
     * Creates a class node and saves it to a map.
     * This is necessary because classes occur before sourcefiles in the report.
     * But in the java model, classes are children of files.
     *
     * @param element the current report element
     */
    private void handleClassElement(final StartElement element) {
        CoverageNode classNode = new CoverageNode(CoverageMetric.CLASS, element.getAttributeByName(NAME).getValue());

        String sourcefileName = element.getAttributeByName(SOURCEFILENAME).getValue();

        // Adds current class node as part of a list to the map
        ArrayList<CoverageNode> classNodesList;
        if (classNodesMap.containsKey(sourcefileName)) {
            classNodesList = classNodesMap.get(sourcefileName);
        }
        else {
            classNodesList = new ArrayList<>();
        }
        classNodesList.add(classNode);
        classNodesMap.put(sourcefileName, classNodesList);

        currentNode = classNode;
    }

    /**
     * Creates a leaf with general covered/missed information under a method-node.
     *
     * @param element the current report element
     */
    private void handleCounterElement(final StartElement element) {
        String currentType = element.getAttributeByName(TYPE).getValue();

        // We only look for data on method layer
        if (currentNode.getMetric() != CoverageMetric.METHOD) {
            return;
        }

        CoverageMetric coverageMetric;
        switch (currentType) {
            case "LINE":
                coverageMetric = CoverageMetric.LINE;
                break;
            case "INSTRUCTION":
                coverageMetric = CoverageMetric.INSTRUCTION;
                break;
            case "BRANCH":
                coverageMetric = CoverageMetric.BRANCH;
                break;
            case "COMPLEXITY":
                coverageMetric = CoverageMetric.COMPLEXITY;
                break;
            default:
                return;
        }

        CoverageLeaf coverageLeaf = new CoverageLeaf(coverageMetric,
                new Coverage(Integer.parseInt(element.getAttributeByName(COVERED).getValue()),
                        Integer.parseInt(element.getAttributeByName(MISSED).getValue())));

        currentNode.add(coverageLeaf);
    }

    /**
     * Creates an instruction or branch leaf and add it to the current fileNode.
     *
     * @param element the current report element
     */
    private void handleLineElement(final StartElement element) {
        int lineNumber = Integer.parseInt(element.getAttributeByName(NR).getValue());
        int missedInstructions = Integer.parseInt(element.getAttributeByName(MI).getValue());
        int coveredInstructions = Integer.parseInt(element.getAttributeByName(CI).getValue());
        int missedBranches = Integer.parseInt(element.getAttributeByName(MB).getValue());
        int coveredBranches = Integer.parseInt(element.getAttributeByName(CB).getValue());

        if (coveredInstructions > 0 || missedInstructions > 0) {
            CoverageLeaf instructionLeaf = new CoverageLeaf(CoverageMetric.INSTRUCTION,
                    new Coverage(coveredInstructions, missedInstructions));
            ((FileCoverageNode) currentNode).getLineNumberToInstructionCoverage().put(lineNumber, instructionLeaf);
        }

        if (coveredBranches > 0 || missedBranches > 0) {
            CoverageLeaf branchLeaf = new CoverageLeaf(CoverageMetric.BRANCH,
                    new Coverage(coveredBranches, missedBranches));
            ((FileCoverageNode) currentNode).getLineNumberToBranchCoverage().put(lineNumber, branchLeaf);
        }
    }

    /**
     * Depending on the tag, either resets the map containing the class objects
     * or sets the current node back to the class node.
     *
     * @param element the current report element
     */
    @Override
    protected void endElement(final EndElement element) {
        switch (element.getName().toString()) {
            case "package":
                currentNode = getRootNode();
                currentPackageNode = null;
                classNodesMap = new HashMap<>();
                break;

            case "method":
                currentNode = currentNode.getParent();
                break;
        }
    }
}