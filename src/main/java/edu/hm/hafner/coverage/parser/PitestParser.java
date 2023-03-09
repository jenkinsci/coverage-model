package edu.hm.hafner.coverage.parser;

import java.io.Reader;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;

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
            root.getAllFileNodes().forEach(this::collectLineCoverage);
            return root;
        }
        catch (XMLStreamException exception) {
            throw new ParsingException(exception);
        }
    }

    private void collectLineCoverage(final FileNode fileNode) {
        var builder = new CoverageBuilder(Metric.LINE);
        var coveredLine = builder.setCovered(1).setMissed(0).build();
        var uncoveredLine = builder.setCovered(0).setMissed(1).build();

        var lineMapping = collectLines(fileNode, Mutation::isCovered).stream()
                .collect(Collectors.toMap(k -> k, v -> coveredLine));
        var covered = lineMapping.size();

        collectLines(fileNode, Mutation::isMissed).forEach(line -> lineMapping.put(line, uncoveredLine));
        var missed = lineMapping.size() - covered;

        lineMapping.forEach((line, coverage) -> fileNode.addCounters(line, coverage.getCovered(), coverage.getMissed()));
        fileNode.addValue(builder.setCovered(covered).setMissed(missed).build());
    }

    private static Set<Integer> collectLines(final FileNode fileNode,
            final Predicate<Mutation> filterPredicate) {
        return fileNode.getMutations().stream()
                .filter(filterPredicate)
                .map(Mutation::getLine)
                .collect(Collectors.toSet());
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
                builder.buildAndAddToModule(root);
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
                    builder.setMutator(content);
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
                    builder.setLine(content);
                }
                return;
            }
        }
    }
}
