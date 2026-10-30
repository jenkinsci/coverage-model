package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.Mutation.MutationBuilder;
import edu.hm.hafner.coverage.MutationStatus;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import java.io.Reader;
import java.io.Serial;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import org.apache.commons.lang3.StringUtils;

/**
 * Parses reports created by PITest into a Java object model.
 *
 * @author Melissa Bauer
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class PitestParser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = 3449160972709724274L;

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

    /** Creates a new instance of {@link PitestParser}. */
    public PitestParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link PitestParser}.
     *
     * @param processingMode determines whether to ignore errors
     */
    public PitestParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try {
            var factory = new SecureXmlParserFactory();
            var eventReader = factory.createXmlEventReader(reader);

            var root = new ModuleNode(EMPTY); // PIT has no support for module names
            boolean isEmpty = true;
            while (eventReader.hasNext()) {
                var event = eventReader.nextEvent();

                if (event.isStartElement()
                        && MUTATION.equals(event.asStartElement().getName())) {
                    readMutation(eventReader, root, event.asStartElement());
                    isEmpty = false;
                }
            }
            handleEmptyResults(fileName, log, isEmpty);
            root.getAllFileNodes().forEach(this::collectLineCoverage);
            return root;
        } catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    /**
     * Derives the line coverage from the mutations of the specified file. A line is covered if at least one of its
     * mutations has been executed by the tests (killed, survived, timed out). A line is missed only if none of its
     * mutations has been executed, i.e., all of its mutations have the status {@link MutationStatus#NO_COVERAGE}.
     *
     * @param fileNode the file to derive the line coverage for
     */
    private void collectLineCoverage(final FileNode fileNode) {
        var coveredLines = collectLines(fileNode, Mutation::isCovered);
        var missedLines = collectLines(fileNode, Mutation::isMissed);
        missedLines.removeAll(coveredLines);

        coveredLines.forEach(line -> fileNode.addCounters(line, 1, 0));
        missedLines.forEach(line -> fileNode.addCounters(line, 0, 1));

        fileNode.addValue(new CoverageBuilder(Metric.LINE)
                .withCovered(coveredLines.size())
                .withMissed(missedLines.size())
                .build());
    }

    private static NavigableSet<Integer> collectLines(
            final FileNode fileNode, final Predicate<Mutation> filterPredicate) {
        return fileNode.getMutations().stream()
                .filter(filterPredicate)
                .map(Mutation::getLine)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private void readMutation(final XMLEventReader reader, final ModuleNode root, final StartElement mutationElement)
            throws XMLStreamException {
        var builder = new MutationBuilder();

        builder.withStatus(MutationStatus.valueOf(getValueOf(mutationElement, STATUS)));
        builder.withIsDetected(Boolean.parseBoolean(getValueOf(mutationElement, DETECTED)));

        while (reader.hasNext()) {
            var event = reader.nextEvent();

            if (event.isStartElement()) {
                readProperty(reader, builder);
            } else if (event.isEndElement()) {
                builder.buildAndAddToModule(root, getTreeStringBuilder());
                return;
            }
        }
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"}) // There are a lot of properties to read
    private void readProperty(final XMLEventReader reader, final MutationBuilder builder) throws XMLStreamException {
        var aggregatedContent = new StringBuilder();

        while (true) {
            var event = reader.nextEvent();
            if (event.isCharacters()) {
                aggregatedContent.append(event.asCharacters().getData());
            } else if (event.isStartElement()) {
                readProperty(reader, builder); // sometimes properties are wrapped by another container element
            } else if (event.isEndElement()) {
                var content = StringUtils.defaultString(StringUtils.strip(aggregatedContent.toString()));
                var name = event.asEndElement().getName();
                if (name.equals(MUTATOR)) {
                    builder.withMutator(content);
                } else if (name.equals(KILLING_TEST)) {
                    builder.withKillingTest(content);
                } else if (name.equals(DESCRIPTION)) {
                    builder.withDescription(content);
                } else if (name.equals(SOURCE_FILE)) {
                    builder.withSourceFile(content);
                } else if (name.equals(MUTATED_CLASS)) {
                    builder.withMutatedClass(content);
                } else if (name.equals(MUTATED_METHOD)) {
                    builder.withMutatedMethod(content);
                } else if (name.equals(MUTATED_METHOD_SIGNATURE)) {
                    builder.withMutatedMethodSignature(content);
                } else if (name.equals(LINE_NUMBER)) {
                    builder.withLine(content);
                }
                return;
            }
        }
    }
}
