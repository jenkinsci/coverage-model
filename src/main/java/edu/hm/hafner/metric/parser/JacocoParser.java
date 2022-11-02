package edu.hm.hafner.metric.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CyclomaticComplexity;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
import edu.hm.hafner.metric.Value;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A parser which parses reports made by Jacoco into a Java Object Model.
 *
 * @author Melissa Bauer
 */
public class JacocoParser extends XmlParser {
    private static final long serialVersionUID = -6021749565311262221L;

    /** Attributes of the XML elements. */
    private static final QName NAME = new QName("name");
    private static final QName SOURCEFILENAME = new QName("sourcefilename");
    private static final QName DESC = new QName("desc");
    private static final QName TYPE = new QName("type");
    private static final QName LINE = new QName("line");
    private static final QName MISSED = new QName("missed");
    private static final QName COVERED = new QName("covered");
    private static final QName NR = new QName("nr");
    private static final QName MI = new QName("mi");
    private static final QName CI = new QName("ci");
    private static final QName MB = new QName("mb");
    private static final QName CB = new QName("cb");

    private static final String NO_CURRENT_NODE = "Current node is not set";

    @CheckForNull
    private PackageNode currentPackageNode;
    @CheckForNull
    private Node currentNode;
    private final Map<String, List<ClassNode>> classNodesMap = new HashMap<>();

    /**
     * Creates a node or a leaf depending on the given element type. Ignore group and sessioninfo.
     *
     * @param element
     *         the complete tag element including attributes
     */
    @Override
    protected void startElement(final StartElement element) {
        String name = element.getName().toString();

        switch (name) {
            case "report": // currentNode = null, rootNode after
                setRootNode(new ModuleNode(getValueOf(element, NAME)));
                currentNode = getRootNode();
                break;

            case "package": // currentNode = rootNode, packageNode after
                String packageName = PackageNode.normalizePackageName(getValueOf(element, NAME));
                PackageNode packageNode = new PackageNode(packageName);
                getRootNode().addChild(packageNode);

                currentPackageNode = packageNode; // save for later to be able to add fileNodes
                currentNode = packageNode;
                break;

            case "class": // currentNode = packageNode, classNode after
                handleClassElement(element);
                break;

            case "method": // currentNode = classNode, methodNode after
                String methodName = getValueOf(element, NAME);
                String methodSignature = getValueOf(element, DESC);
                int methodLine = Integer.parseInt(getValueOf(element, LINE));
                MethodNode methodNode = new MethodNode(methodName, methodSignature, methodLine);

                if (currentNode == null) {
                    throw new NoSuchElementException(NO_CURRENT_NODE);
                }
                currentNode.addChild(methodNode);
                currentNode = methodNode;
                break;

            case "counter": // currentNode = methodNode, methodNode after
                handleCounterElement(element);
                break;

            case "sourcefile": // currentNode = packageNode, fileNode after
                String fileName = getValueOf(element, NAME);
                FileNode fileNode = new FileNode(fileName);

                // add all classNodes to current fileNode
                List<ClassNode> classNodeList = classNodesMap.get(fileName);
                classNodeList.forEach(fileNode::addChild);

                if (currentPackageNode == null) {
                    throw new NoSuchElementException("Package node is not set while processing " + fileNode);
                }
                currentPackageNode.addChild(fileNode);
                currentNode = fileNode;
                break;

            case "line": // currentNode = fileNode, fileNode after
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
        ClassNode classNode = new ClassNode(getValueOf(element, NAME));

        String sourcefileName = getValueOf(element, SOURCEFILENAME);

        // Adds current class node as part of a list to the map
        List<ClassNode> classNodesList = classNodesMap.getOrDefault(sourcefileName, new ArrayList<>());

        classNodesList.add(classNode);
        classNodesMap.put(sourcefileName, classNodesList);

        currentNode = classNode;
    }

    /**
     * Creates a leaf with general covered/missed information under a method-node.
     *
     * @param element
     *         the current report element
     */
    private void handleCounterElement(final StartElement element) {
        String currentType = getValueOf(element, TYPE);

        if (currentNode == null) {
            throw new NoSuchElementException(NO_CURRENT_NODE);
        }
        // We only look for data on method layer
        if (!currentNode.getMetric().equals(Metric.METHOD)) {
            return;
        }

        Value value;
        switch (currentType) {
            case "LINE":
            case "INSTRUCTION":
            case "BRANCH":
                var builder = new CoverageBuilder();
                value = builder.setMetric(Metric.valueOf(currentType))
                        // TODO: create String setters in builder
                        .setCovered(Integer.parseInt(getValueOf(element, COVERED)))
                        .setMissed(Integer.parseInt(getValueOf(element, MISSED))).build();
                break;
            case "COMPLEXITY":
                int complexity = Integer.parseInt(getValueOf(element, COVERED))
                        + Integer.parseInt(getValueOf(element, MISSED));
                value = new CyclomaticComplexity(complexity);
                break;
            default:
                return;
        }
        currentNode.addValue(value);
    }

    /**
     * Creates an instruction or branch leaf and add it to the current fileNode.
     *
     * @param element
     *         the current report element
     */
    private void handleLineElement(final StartElement element) {
        int lineNumber = Integer.parseInt(getValueOf(element, NR));
        int missedInstructions = Integer.parseInt(getValueOf(element, MI));
        int coveredInstructions = Integer.parseInt(getValueOf(element, CI));
        int missedBranches = Integer.parseInt(getValueOf(element, MB));
        int coveredBranches = Integer.parseInt(getValueOf(element, CB));

        if (currentNode == null) {
            throw new NoSuchElementException(NO_CURRENT_NODE);
        }

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
        ((FileNode) currentNode).addCounters(lineNumber, covered, missed);
    }

    /**
     * Depending on the tag, either resets the map containing the class objects or sets the current node back to the
     * class node.
     *
     * @param element
     *         the current report element
     */
    @Override
    protected void endElement(final EndElement element) {
        switch (element.getName().toString()) {
            case "package":
                currentNode = getRootNode();
                currentPackageNode = null;
                classNodesMap.clear();
                break;

            case "method":
                if (currentNode == null) {
                    throw new NoSuchElementException(NO_CURRENT_NODE);
                }

                currentNode = currentNode.getParent();
                break;

            default:
                break;
        }
    }
}
