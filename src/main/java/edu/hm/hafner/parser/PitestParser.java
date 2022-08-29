package edu.hm.hafner.parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.model.ClassNode;
import edu.hm.hafner.model.FileNode;
import edu.hm.hafner.model.MethodNode;
import edu.hm.hafner.model.ModuleNode;
import edu.hm.hafner.model.Node;
import edu.hm.hafner.model.PackageNode;
import edu.hm.hafner.mutation.MutationLeaf;
import edu.hm.hafner.mutation.MutationStatus;
import edu.hm.hafner.mutation.Mutator;

/**
 * A parser which parses reports made by Pitest into a Java Object Model.
 *
 * @author Melissa Bauer
 */
public class PitestParser extends XmlParser {
    private static final long serialVersionUID = 3449160972709724274L;

    /** Required attributes of the XML elements. */
    private static final QName DETECTED = new QName("detected");
    private static final QName STATUS = new QName("status");

    /** Global variables. */
    private ClassNode classNode = null;
    private MutationLeaf mutationLeaf = null;
    private String currentData = null;
    private String currentFileName = null;

    /**
     * Creates a new PitestParser which parses the given file.
     *
     * @param path
     *         path to report file
     */
    public PitestParser(final String path) {
        parseFile(path);
    }


    /**
     * Parses the xml report at given path. Adds an "isCharacters" choice because the main information of this report is
     * located between the xml tags.
     *
     * @param path
     *         path to report file
     */
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

                else if (e.isCharacters()) {
                    currentData = e.asCharacters().getData();
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
     * Gets the document name and creates the root node.
     *
     * @param event
     *         the xml event
     */
    @Override
    protected void startDocument(final XMLEvent event) {
        String systemId = event.getLocation().getSystemId();
        String[] parts = systemId.split("/", 0);
        String filename = parts[parts.length - 1];

        setRootNode(new ModuleNode(filename));
    }

    /**
     * Gets the detected and status information and saves them in a new mutationLeaf.
     *
     * @param element
     *         the current report element
     */
    @Override
    protected void startElement(final StartElement element) {
        String name = element.getName().toString();

        if ("mutation".equals(name)) {
            boolean isDetected = Boolean.parseBoolean(element.getAttributeByName(DETECTED).getValue());
            MutationStatus status = MutationStatus.valueOf(element.getAttributeByName(STATUS).getValue());

            mutationLeaf = new MutationLeaf(isDetected, status);
        }
    }

    /**
     * Handles different end tags of the xml report.
     *
     * @param element
     *         the current report element
     */
    @Override
    protected void endElement(final EndElement element) {
        String name = element.getName().toString();

        switch (name) {
            case "mutation": // end of current mutation, reset all global variables
                classNode = null;
                mutationLeaf = null;
                currentFileName = null;
                currentData = null;
                break;

            case "sourceFile": // save filename for later; need to determine package first
                currentFileName = currentData;
                break;

            case "mutatedClass":
                handleClass();
                break;

            case "mutatedMethod":
                handleMethod();
                break;

            case "lineNumber":
                int lineNumber = Integer.parseInt(currentData);
                mutationLeaf.setLineNumber(lineNumber);
                break;

            case "mutator":
                Mutator mutator = Mutator.fromPath(currentData);
                mutationLeaf.setMutator(mutator);
                break;

            case "killingTest":
                mutationLeaf.setKillingTest(currentData);
                break;

            default:
                break;
        }
    }

    /**
     * Creates a new packageNode, fileNode and classNode if they do not exist yet.
     */
    private void handleClass() {
        String packagePath = StringUtils.substringBeforeLast(currentData, ".");
        String className = StringUtils.substringAfterLast(currentData, ".");

        // Package
        List<Node> packageNodes = getRootNode().getChildren();
        PackageNode currentPackageNode = null;

        for (Node packageNode : packageNodes) {
            if (packageNode.getName().equals(packagePath)) {
                currentPackageNode = (PackageNode) packageNode;
            }
        }

        if (currentPackageNode == null) {
            currentPackageNode = new PackageNode(packagePath);
            getRootNode().add(currentPackageNode);
        }

        // File
        List<Node> fileNodes = currentPackageNode.getChildren();
        FileNode currentFileNode = null;

        for (Node fileNode : fileNodes) {
            if (fileNode.getName().equals(currentFileName)) {
                currentFileNode = (FileNode) fileNode;
            }
        }

        if (currentFileNode == null) {
            currentFileNode = new FileNode(currentFileName);
            currentPackageNode.add(currentFileNode);
        }

        // Method
        List<Node> classNodes = currentFileNode.getChildren();
        ClassNode currentClassNode = null;
        for (Node tmpClassNode : classNodes) {
            if (tmpClassNode.getName().equals(className)) {
                currentClassNode = (ClassNode) tmpClassNode;
            }
        }

        if (currentClassNode == null) {
            currentClassNode = new ClassNode(className);
            currentFileNode.add(currentClassNode);
        }

        classNode = currentClassNode;
    }

    /**
     * Creates a new method node if it does not exist yet.
     */
    private void handleMethod() {
        List<Node> methodNodes = classNode.getChildren();
        MethodNode currentMethodNode = null;
        for (Node method : methodNodes) {
            if (method.getName().equals(currentData)) {
                currentMethodNode = (MethodNode) method;
            }
        }

        if (currentMethodNode == null) {
            currentMethodNode = new MethodNode(currentData, 0);
            classNode.add(currentMethodNode);
        }

        currentMethodNode.add(mutationLeaf);
    }

}
