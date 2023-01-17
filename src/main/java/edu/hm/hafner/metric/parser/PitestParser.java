package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.ClassNode;
import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Mutation;
import edu.hm.hafner.metric.MutationStatus;
import edu.hm.hafner.metric.Mutator;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.PackageNode;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * A parser which parses reports created by PITest into a Java object model.
 *
 * @author Melissa Bauer
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class PitestParser extends CoverageParser {
    private static final long serialVersionUID = 3449160972709724274L;

    private static final QName MUTATIONS = new QName("mutations");
    private static final QName MUTATION = new QName("mutation");
    private static final QName SOURCE_FILE = new QName("sourceFile");
    private static final QName MUTATED_CLASS = new QName("mutatedClass");
    private static final QName KILLING_TEST = new QName("killingTest");
    private static final QName MUTATED_METHOD = new QName("mutatedMethod");
    private static final QName MUTATED_METHOD_SIGNATURE = new QName("methodDescription");
    private static final QName MUTATOR = new QName("mutator");
    private static final QName DESCRIPTION = new QName("description");
    private static final QName LINE_NUMBER = new QName("lineNumber");
    private static final QName DETECTED = new QName("detected");
    private static final QName STATUS = new QName("status");

    private static final QName INDEX = new QName("index"); // TODO: not used yet
    private static final QName BLOCK = new QName("block"); // TODO: not used yet

    /**
     * Parses the PIT report. The report is expected to be in XML format.
     *
     * @param reader
     *         the reader to wrap
     */
    @Override
    public ModuleNode parse(final Reader reader) {
        try {
            SecureXmlParserFactory factory = new SecureXmlParserFactory();
            XMLEventReader eventReader = factory.createXmlEventReader(reader);
            var root = new ModuleNode("-");
            boolean isEmpty = true;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement() && MUTATIONS.equals(event.asStartElement().getName())) {
                    readMutations(eventReader, root);
                    isEmpty = false;
                }
            }
            if (isEmpty) {
                throw new NoSuchElementException("No mutations found in the specified file.");
            }
            aggregateLineCoverage(root);
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private void aggregateLineCoverage(final ModuleNode root) {
        root.getAllFileNodes().forEach(this::collectLineCoverage);
    }

    private void collectLineCoverage(final FileNode fileNode) {
        var coveredLines = fileNode.getMutations()
                .stream()
                .filter(mutation -> mutation.getStatus().isCovered())
                .map(Mutation::getLineNumber)
                .collect(Collectors.toSet());
        var missedLines = fileNode.getMutations()
                .stream()
                .filter(mutation -> mutation.getStatus().isMissed())
                .map(Mutation::getLineNumber)
                .collect(Collectors.toSet());
        if (new HashSet<>(coveredLines).removeAll(missedLines)) {
            throw new IllegalStateException("Line coverage is not exclusive: " + coveredLines);
        }
        var builder = new CoverageBuilder(Metric.LINE);
        fileNode.addValue(builder.setCovered(coveredLines.size()).setMissed(missedLines.size()).build());
    }

    private void readMutations(final XMLEventReader reader, final ModuleNode root)
            throws XMLStreamException {
        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement() && MUTATION.equals(event.asStartElement().getName())) {
                readMutation(reader, root, event.asStartElement());
            }
        }
    }

    private void readMutation(final XMLEventReader reader, final ModuleNode root, final StartElement mutationElement)
            throws XMLStreamException {
        var builder = new MutationBuilder();

        builder.setStatus(MutationStatus.valueOf(getValueOf(mutationElement, STATUS)));
        builder.setIsDetected(Boolean.parseBoolean(getValueOf(mutationElement, DETECTED)));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                readProperty(reader, builder, event.asStartElement());
            }
            else if (event.isEndElement()) {
                builder.build(root);
                return;
            }
        }
    }

    private void readProperty(final XMLEventReader reader, final MutationBuilder builder, final StartElement property)
            throws XMLStreamException {
        var aggregatedContent = new StringBuilder();

        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            }
            else if (event.isEndElement()) {
                var content = StringUtils.strip(aggregatedContent.toString());
                var name = event.asEndElement().getName();
                if (name.equals(MUTATOR)) {
                    builder.setMutator(Mutator.fromPath(content));
                }
                else if (name.equals(KILLING_TEST)) {
                    builder.setKillingTest(content);
                }
                else if (name.equals(DESCRIPTION)) {
                    builder.setDescription(content);
                }
                else if (name.equals(SOURCE_FILE)) {
                    builder.setSourceFile(content);
                }
                else if (name.equals(MUTATED_CLASS)) {
                    builder.setMutatedClass(content);
                }
                else if (name.equals(MUTATED_METHOD)) {
                    builder.setMutatedMethod(content);
                }
                else if (name.equals(MUTATED_METHOD_SIGNATURE)) {
                    builder.setMutatedMethodSignature(content);
                }
                else if (name.equals(LINE_NUMBER)) {
                    builder.setLineNumber(content);
                }
                return;
            }
        }
    }

    private static class MutationBuilder {
        private boolean isDetected;
        private MutationStatus status;
        private int lineNumber;
        private Mutator mutator;
        private String killingTest;
        private String description;
        private String sourceFile;
        private String mutatedClass;
        private String mutatedMethod;
        private String mutatedMethodSignature;

        public void setIsDetected(final boolean isDetected) {
            this.isDetected = isDetected;
        }

        public void setStatus(final MutationStatus status) {
            this.status = status;
        }

        public void setLineNumber(final String lineNumber) {
            this.lineNumber = Integer.parseInt(lineNumber);
        }

        public void setMutator(final Mutator mutator) {
            this.mutator = mutator;
        }

        public void setKillingTest(final String killingTest) {
            this.killingTest = killingTest;
        }

        public void setDescription(final String description) {
            this.description = description;
        }

        public void setSourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;
        }

        public void setMutatedClass(final String mutatedClass) {
            this.mutatedClass = mutatedClass;
        }

        public void setMutatedMethod(final String mutatedMethod) {
            this.mutatedMethod = mutatedMethod;
        }

        public void setMutatedMethodSignature(final String mutatedMethodSignature) {
            this.mutatedMethodSignature = mutatedMethodSignature;
        }

        public void build(final Node root) {
            String packageName = StringUtils.substringBeforeLast(mutatedClass, ".");
            String className = StringUtils.substringAfterLast(mutatedClass, ".");
            var packageNode = root.findPackage(packageName).orElseGet(() -> createPackageNode(root, packageName));
            var fileNode = packageNode.findFile(sourceFile).orElseGet(() -> createFileNode(packageNode, sourceFile));
            var classNode = fileNode.findClass(className).orElseGet(() -> createClassNode(fileNode, className));
            var methodNode = classNode.findMethod(mutatedMethod, mutatedMethodSignature)
                    .orElseGet(() -> createMethodNode(classNode, mutatedMethod, mutatedMethodSignature));

            var coverage = methodNode.getValue(Metric.MUTATION)
                    .map(Coverage.class::cast)
                    .orElse(Coverage.nullObject(Metric.MUTATION));
            var builder = new CoverageBuilder(coverage);
            if (isDetected) {
                builder.incrementCovered();
            }
            else {
                builder.incrementMissed();
            }
            methodNode.replaceValue(builder.build());

            fileNode.addMutation(new Mutation(isDetected, status, lineNumber, mutator, killingTest,
                    mutatedClass, mutatedMethod, mutatedMethodSignature, description));
        }

        private PackageNode createPackageNode(final Node root, final String packageName) {
            var fileNode = new PackageNode(packageName);
            root.addChild(fileNode);
            return fileNode;
        }

        private FileNode createFileNode(final Node root, final String fileName) {
            var fileNode = new FileNode(fileName);
            root.addChild(fileNode);
            return fileNode;
        }

        private ClassNode createClassNode(final Node root, final String className) {
            var fileNode = new ClassNode(className);
            root.addChild(fileNode);
            return fileNode;
        }

        private MethodNode createMethodNode(final Node root, final String methodName, final String signature) {
            var fileNode = new MethodNode(methodName, signature);
            root.addChild(fileNode);
            return fileNode;
        }
    }
}
