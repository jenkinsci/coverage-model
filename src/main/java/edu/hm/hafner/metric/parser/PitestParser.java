package edu.hm.hafner.metric.parser;

import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.CoverageParser;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.MethodNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Mutation;
import edu.hm.hafner.metric.MutationStatus;
import edu.hm.hafner.metric.Mutator;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.SecureXmlParserFactory.ParsingException;

/**
 * A parser which parses reports created by PITest into a Java object model.
 *
 * @author Melissa Bauer
 * @author Ullrich Hafner
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

    /**
     * Parses the PIT report. The report is expected to be in XML format.
     *
     * @param reader
     *         the reader to read the report from
     */
    @Override
    public ModuleNode parse(final Reader reader, final FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            var root = new ModuleNode("-");
            boolean isEmpty = true;
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement() && MUTATION.equals(event.asStartElement().getName())) {
                    readMutation(eventReader, root, event.asStartElement());
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
        root.getAllMethodNodes().forEach(this::collectLineCoverage);
        root.getAllFileNodes().forEach(this::collectLineCoverageForFiles);
    }

    private void collectLineCoverageForFiles(final FileNode fileNode) {
        var methodNodes = fileNode.getAllMethodNodes();
        var builder = new CoverageBuilder(Metric.LINE);
        var killed = builder.setCovered(1).setMissed(0).build();
        var survived = builder.setCovered(0).setMissed(1).build();

        var lineMapping = collectLines(methodNodes, Mutation::isDetected).stream()
                .collect(Collectors.toMap(k -> k, v -> killed, Coverage::add));
        collectLines(methodNodes, Predicate.not(Mutation::isDetected))
                .forEach(line -> lineMapping.merge(line, survived, Coverage::add));

        lineMapping.forEach((line, coverage) -> fileNode.addCounters(line, coverage.getCovered(), coverage.getMissed()));
    }

    private static List<Integer> collectLines(final List<MethodNode> methodNodes,
            final Predicate<Mutation> filterPredicate) {
        return methodNodes.stream()
                .map(MethodNode::getMutations)
                .flatMap(Collection::stream)
                .filter(filterPredicate)
                .map(Mutation::getLineNumber)
                .collect(Collectors.toList());
    }

    private void collectLineCoverage(final MethodNode methodNode) {
        var coveredLines = collectLines(List.of(methodNode), Mutation::isCovered);
        var missedLines = collectLines(List.of(methodNode), Mutation::isMissed);
        if (new HashSet<>(coveredLines).removeAll(missedLines)) {
            throw new IllegalStateException("Line coverage is not exclusive: " + coveredLines); // FIXME: should we remove that check before going live?
        }
        var builder = new CoverageBuilder(Metric.LINE);
        methodNode.addValue(builder.setCovered(coveredLines.size()).setMissed(missedLines.size()).build());
    }

    private void readMutation(final XMLEventReader reader, final ModuleNode root, final StartElement mutationElement)
            throws XMLStreamException {
        var builder = new MutationBuilder();

        builder.setStatus(MutationStatus.valueOf(getValueOf(mutationElement, STATUS)));
        builder.setIsDetected(Boolean.parseBoolean(getValueOf(mutationElement, DETECTED)));

        while (reader.hasNext()) {
            XMLEvent event = reader.nextEvent();

            if (event.isStartElement()) {
                readProperty(reader, builder);
            }
            else if (event.isEndElement()) {
                builder.build(root);
                return;
            }
        }
    }

    @SuppressWarnings("PMD.CyclomaticComplexity") // There are a lot of properties to read
    private void readProperty(final XMLEventReader reader, final MutationBuilder builder)
            throws XMLStreamException {
        var aggregatedContent = new StringBuilder();

        while (true) {
            XMLEvent event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            }
            else if (event.isEndElement()) {
                var content = StringUtils.defaultString(StringUtils.strip(aggregatedContent.toString()));
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
        private MutationStatus status = MutationStatus.NO_COVERAGE;
        private int lineNumber;
        private Mutator mutator = Mutator.NOT_SPECIFIED;
        private String killingTest = StringUtils.EMPTY;
        private String description = StringUtils.EMPTY;
        private String sourceFile = StringUtils.EMPTY;
        private String mutatedClass = StringUtils.EMPTY;
        private String mutatedMethod = StringUtils.EMPTY;
        private String mutatedMethodSignature = StringUtils.EMPTY;

        private void setIsDetected(final boolean isDetected) {
            this.isDetected = isDetected;
        }

        private void setStatus(final MutationStatus status) {
            this.status = status;
        }

        private void setLineNumber(final String lineNumber) {
            this.lineNumber = parseInteger(lineNumber);
        }

        private void setMutator(final Mutator mutator) {
            this.mutator = mutator;
        }

        private void setKillingTest(final String killingTest) {
            this.killingTest = killingTest;
        }

        private void setDescription(final String description) {
            this.description = description;
        }

        private void setSourceFile(final String sourceFile) {
            this.sourceFile = sourceFile;
        }

        private void setMutatedClass(final String mutatedClass) {
            this.mutatedClass = mutatedClass;
        }

        private void setMutatedMethod(final String mutatedMethod) {
            this.mutatedMethod = mutatedMethod;
        }

        private void setMutatedMethodSignature(final String mutatedMethodSignature) {
            this.mutatedMethodSignature = mutatedMethodSignature;
        }

        private void build(final ModuleNode root) {
            String packageName = StringUtils.substringBeforeLast(mutatedClass, ".");
            String className = StringUtils.substringAfterLast(mutatedClass, ".");
            var packageNode = root.findPackage(packageName).orElseGet(() -> root.createPackageNode(packageName));
            var fileNode = packageNode.findFile(sourceFile).orElseGet(() -> packageNode.createFileNode(sourceFile));
            var classNode = fileNode.findClass(className).orElseGet(() -> fileNode.createClassNode(className));
            var methodNode = classNode.findMethod(mutatedMethod, mutatedMethodSignature)
                    .orElseGet(() -> classNode.createMethodNode(mutatedMethod, mutatedMethodSignature));

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
            methodNode.addMutation(new Mutation(isDetected, status, lineNumber, mutator, killingTest,
                    mutatedClass, mutatedMethod, mutatedMethodSignature, description));
        }
    }
}
