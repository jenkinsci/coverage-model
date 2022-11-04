package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Mutation;
import edu.hm.hafner.metric.MutationStatus;
import edu.hm.hafner.metric.MutationValue;
import edu.hm.hafner.metric.Mutator;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;
import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A parser which parses reports made by Pitest into a Java Object Model.
 *
 * @author Melissa Bauer
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class PitestParser extends XmlParser {
    private static final long serialVersionUID = 3449160972709724274L;

    /** Required attributes of the XML elements. */
    private static final QName DETECTED = new QName("detected");
    private static final QName STATUS = new QName("status");

    private static final String MUTATION_ERROR_MESSAGE = "No mutation value available";

    /** Global variables. */
    @CheckForNull
    private ClassNode classNode = null;
    @CheckForNull
    private Mutation mutation = null;
    @CheckForNull
    private String currentData = null;
    @CheckForNull
    private String currentFileName = null;
    @CheckForNull
    private String currentMethodName = null;

    /**
     * Parses the xml report at given path. Adds an "isCharacters" choice because the main information of this report is
     * located between the xml tags.
     *
     * @param reader
     *         the reader to wrap
     */
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

                if (e.isCharacters()) {
                    currentData = e.asCharacters().getData();
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
     * Creates a new {@link ModuleNode} or gets the detected and status information and saves them in a new
     * mutationLeaf.
     *
     * @param element
     *         the current report element
     */
    @Override
    protected void startElement(final StartElement element) {
        String name = element.getName().toString();

        if ("mutations".equals(name)) {
            setRootNode(new ModuleNode(""));
        }
        else if ("mutation".equals(name)) {
            boolean isDetected = Boolean.parseBoolean(getValueOf(element, DETECTED));
            MutationStatus status = MutationStatus.valueOf(getValueOf(element, STATUS));

            mutation = new Mutation(isDetected, status);
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
            case "sourceFile": // save filename for later; need to determine package first
                currentFileName = currentData;
                break;

            case "mutatedClass":
                handleClass();
                break;

            case "mutatedMethod":
                currentMethodName = currentData;
                break;

            case "methodDescription":
                handleMethod();
                break;

            case "lineNumber":
                if (mutation == null) {
                    throw new NoSuchElementException(MUTATION_ERROR_MESSAGE);
                }
                if (currentData == null) {
                    throw new NoSuchElementException("CurrentData is not set");
                }

                mutation.setLineNumber(Integer.parseInt(currentData));
                break;

            case "mutator":
                if (mutation == null) {
                    throw new NoSuchElementException(MUTATION_ERROR_MESSAGE);
                }

                mutation.setMutator(Mutator.fromPath(currentData));
                break;

            case "killingTest":
                if (mutation == null) {
                    throw new NoSuchElementException(MUTATION_ERROR_MESSAGE);
                }

                mutation.setKillingTest(currentData);
                break;

            case "mutation": // end of current mutation, reset all global variables
                classNode = null;
                mutation = null;
                currentFileName = null;
                currentMethodName = null;
                currentData = null;
                break;

            default:
                break;
        }
    }

    // Pitest has no optional attributes
    @Override
    boolean isOptional(final String attribute) {
        return false;
    }

    /**
     * Creates a new packageNode, fileNode and classNode if they do not exist yet.
     */
    private void handleClass() {
        String packagePath = StringUtils.substringBeforeLast(currentData, ".");
        String normalizedPackagePath = PackageNode.normalizePackageName(packagePath);
        String className = StringUtils.substringAfterLast(currentData, ".");

        // Package
        PackageNode currentPackageNode;
        Optional<Node> optionalPackage = getRootNode().find(Metric.PACKAGE, normalizedPackagePath);
        if (optionalPackage.isPresent()) {
            currentPackageNode = (PackageNode) optionalPackage.get();
        }
        else {
            currentPackageNode = new PackageNode(normalizedPackagePath);
            getRootNode().addChild(currentPackageNode);
        }

        // File
        FileNode currentFileNode;
        Optional<Node> optionalFile = currentPackageNode.find(Metric.FILE, currentFileName);
        if (optionalFile.isPresent()) {
            currentFileNode = (FileNode) optionalFile.get();
        }
        else {
            currentFileNode = new FileNode(currentFileName);
            currentPackageNode.addChild(currentFileNode);
        }

        // Class
        ClassNode currentClassNode;
        Optional<Node> optionalClass = currentFileNode.find(Metric.CLASS, className);
        if (optionalClass.isPresent()) {
            currentClassNode = (ClassNode) optionalClass.get();
        }
        else {
            currentClassNode = new ClassNode(className);
            currentFileNode.addChild(currentClassNode);
        }

        classNode = currentClassNode;
    }

    /**
     * Creates a new method node if it does not exist yet.
     */
    private void handleMethod() {
        if (classNode == null) {
            throw new NoSuchElementException("Class node not set");
        }

        MethodNode currentMethodNode;
        MutationValue newValue = new MutationValue(mutation);
        Optional<MethodNode> potentialMethodNode = classNode.findMethodNode(currentMethodName, currentData);

        // already exists a node and has the same signature?
        if (potentialMethodNode.isPresent()) {
            currentMethodNode = potentialMethodNode.get();
            Optional<Value> potentialValue = currentMethodNode.getValue(Metric.MUTATION);
            // value must be present as creating a new method node is accompanied by creating a mutation value
            potentialValue.ifPresent(value -> currentMethodNode.replaceMutationValue(value.add(newValue)));

        }
        else {
            currentMethodNode = new MethodNode(currentMethodName, currentData, 0);
            currentMethodNode.addValue(newValue);
            classNode.addChild(currentMethodNode);
        }
    }
}
