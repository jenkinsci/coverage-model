package edu.hm.hafner.coverage.parser;

import java.io.File;
import java.io.Reader;
import java.io.Serial;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.HashMap;
import java.util.Set;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Node;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SecureXmlParserFactory;
import edu.hm.hafner.util.TreeString;

/**
 * A parser for TRACE32 coverage reports.
 *
 * @author Igor Miksza
 */
@SuppressWarnings({"PMD.GodClass", "PMD.CyclomaticComplexity"})
public class Trace32Parser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final QName LIST_FUNC = new QName("COVerage.EXPORT.ListFunc");
    private static final QName LIST_MODULE = new QName("COVerage.EXPORT.ListModule");
    private static final QName LIST_EXPORT = new QName("List.EXPORT");
    private static final QName MODULE = new QName("module");
    private static final QName FUNCTION = new QName("function");
    private static final QName TREE = new QName("tree");
    private static final QName MIXED = new QName("mixed");
    private static final QName SRCPATH = new QName("srcpath");
    private static final QName METRIC_ATTR = new QName("metric");

    @SuppressWarnings("PMD.LooseCoupling")
    private final HashMap<String, String> filesToProcess = new HashMap<>();

    private enum Fields {
        BYTES, BYTESOK,
        CALLS, CALLSOK,
        LINES, LINESOK,
        FUNCTIONS, FUNCTIONSOK,
        DECISIONS, DECISIONSOK,
        CONDITIONS, TRUE, FALSE,
        OK, TAKEN, NOTTAKEN, NEVER;

        static Fields fromTag(final String tag) throws IllegalArgumentException {
            return Fields.valueOf(tag.toUpperCase(Locale.getDefault()));
        }
    }

    /**
     * Creates a new instance of Trace32Parser.
     */
    public Trace32Parser() {
        super(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of Trace32Parser.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public Trace32Parser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        var root = new ModuleNode("TRACE32 Coverage");

        try {
            parseFile(root, reader);
        }
        catch (XMLStreamException e) {
            throw new ParsingException(e);
        }

        if (root.isEmpty()) {
            handleEmptyResults(fileName, log);
            return new ModuleNode("empty");
        }

        // Add files and copy metrics
        var rootFiles = root.createClassNode("TRACE32 Files");

        for (var entry : filesToProcess.entrySet()) {
            var filePath = entry.getValue(); // for clarity

            var fileNodePath = Paths.get(filePath).getFileName();
            var fileNodeName = (fileNodePath != null) ? fileNodePath.toString() : "missing_file";
            if (rootFiles.hasChild(filePath + fileNodeName)) {
                log.logInfo("[TRACE32] Found duplicate file module: \"%s\", source: \"%s\"", entry.getKey(), filePath);
                continue;
            }
            log.logInfo("[TRACE32] Found file module: \"%s\", source: \"%s\"", entry.getKey(), filePath);

            root.addSource(filePath);
            var fileNode = rootFiles.createFileNode(fileNodeName, TreeString.valueOf(filePath));

            // Renaming module name with a file name
            var moduleNode = getNodeTree(root, entry.getKey()).orElse(null);
            if (moduleNode != null) {
                var newNode = root.createClassNode(fileNodeName);
                newNode.addAllChildren(moduleNode.getChildren());
                moduleNode.getValues().forEach(value -> {
                    newNode.addValue(value);
                    fileNode.addValue(value);
                });

                // Hide original node
                moduleNode.getMetrics().forEach(metric -> {
                    moduleNode.replaceValue(new CoverageBuilder(metric).withTotal(0).withCovered(0).build());
                });
            }
        }

        // Hide file nodes
        for (var metric : rootFiles.getMetrics()) {
            if (metric != Metric.FILE) {
                rootFiles.addValue(new CoverageBuilder(metric).withTotal(0).withCovered(0).build());
            }
        }

        return root;
    }

    private boolean startElement(final XMLEvent event, final QName name) {
        return event.isStartElement() && event.asStartElement().getName().equals(name);
    }

    private boolean endElement(final XMLEvent event, final QName name) {
        return event.isEndElement() && event.asEndElement().getName().equals(name);
    }

    @SuppressWarnings("StringSplitter")
    private Optional<Node> getNodeTree(final Node root, final String treeName) {
        var lastNode = root;
        for (var name : treeName.split("[\\/\\\\]")) {
            lastNode = lastNode.findClass(name).orElse(null);
            if (lastNode == null) {
                return Optional.empty();
            }
        }
        return Optional.of(lastNode);
    }

    @SuppressWarnings("StringSplitter")
    private Node makeNodeTree(final Node root, final String treeName) {
        var lastNode = root;
        for (var name : treeName.split("[\\/\\\\]")) {
            lastNode = lastNode.findOrCreateClassNode(name);
        }
        return lastNode;
    }

    private void addOrReplaceMetric(final Map<Fields, Integer> map, final Node node, final Metric metric, final Fields total, final Fields covered) {
        node.replaceValue(new CoverageBuilder(metric)
                .withTotal(map.getOrDefault(total, 0))
                .withCovered(map.getOrDefault(covered, 0)).build());
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private String readMetric(final XMLEventReader xml, final Node root, final String metric, final QName element, final boolean readFunction) throws XMLStreamException {
        var event = xml.nextEvent();
        var treeName = "";
        var map = new EnumMap<Fields, Integer>(Fields.class);

        while (!endElement(event, element)) {
            if (!event.isStartElement()) {
                event = xml.nextEvent();
                continue;
            }
            var data = xml.nextEvent().asCharacters().getData().trim();
            var tag = event.asStartElement().getName();
            if (tag.equals(TREE)) {
                treeName = data.replace("\\\\", "").replace('\\', File.separatorChar);
            }
            else if (tag.equals(FUNCTION)) {
                if (!readFunction) {
                    break;
                }
                readMetric(xml, root, metric, FUNCTION, false);
            }
            else {
                try {
                    map.put(Fields.fromTag(tag.toString()), parseInteger(data));
                }
                catch (IllegalArgumentException e) {
                    // Not a metric field, just continue
                }
            }
            event = xml.nextEvent();
        }

        if (treeName.isEmpty()) {
            return treeName;
        }

        var node = makeNodeTree(root, treeName);
        addOrReplaceMetric(map, node, "object".equals(metric) ? Metric.OBJECT_CODE : Metric.BYTES, Fields.BYTES, Fields.BYTESOK);

        switch (metric) {
            case "func" -> {
                addOrReplaceMetric(map, node, Metric.FUNCTION, Fields.FUNCTIONS, Fields.FUNCTIONSOK);
            }
            case "stmt" -> {
                addOrReplaceMetric(map, node, Metric.STATEMENT, Fields.LINES, Fields.LINESOK);
            }
            case "mcdc" -> {
                addOrReplaceMetric(map, node, Metric.MCDC_PAIR, Fields.LINES, Fields.LINESOK);
                addOrReplaceMetric(map, node, Metric.DECISION, Fields.DECISIONS, Fields.DECISIONSOK);
                node.replaceValue(new CoverageBuilder(Metric.CONDITION)
                        .withTotal(2 * map.getOrDefault(Fields.CONDITIONS, 0))
                        .withCovered(map.getOrDefault(Fields.TRUE, 0) + map.getOrDefault(Fields.FALSE, 0)).build());
            }
            case "call" -> {
                addOrReplaceMetric(map, node, Metric.FUNCTION_CALL, Fields.CALLS, Fields.CALLSOK);
                addOrReplaceMetric(map, node, Metric.FUNCTION, Fields.FUNCTIONS, Fields.FUNCTIONSOK);
            }
            case "cond" -> {
                addOrReplaceMetric(map, node, Metric.STMT_CC, Fields.LINES, Fields.LINESOK);
                node.replaceValue(new CoverageBuilder(Metric.CONDITION)
                        .withTotal(2 * map.getOrDefault(Fields.CONDITIONS, 0))
                        .withCovered(map.getOrDefault(Fields.TRUE, 0) + map.getOrDefault(Fields.FALSE, 0)).build());
            }
            case "dec" -> {
                addOrReplaceMetric(map, node, Metric.STMT_DC, Fields.LINES, Fields.LINESOK);
                node.replaceValue(new CoverageBuilder(Metric.DECISION)
                        .withTotal(2 * map.getOrDefault(Fields.DECISIONS, 0))
                        .withCovered(map.getOrDefault(Fields.TRUE, 0) + map.getOrDefault(Fields.FALSE, 0)).build());
            }
            default -> { // "object"
                var total = 2 * (map.getOrDefault(Fields.OK, 0) + map.getOrDefault(Fields.TAKEN, 0) + map.getOrDefault(Fields.NOTTAKEN, 0) + map.getOrDefault(Fields.NEVER, 0));
                var covered = (2 * map.getOrDefault(Fields.OK, 0)) + map.getOrDefault(Fields.TAKEN, 0) + map.getOrDefault(Fields.NOTTAKEN, 0);
                node.replaceValue(new CoverageBuilder(Metric.BRANCH)
                        .withTotal(total)
                        .withCovered(covered).build());
            }
        }

        return treeName;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private void parseFile(final ModuleNode root, final Reader reader) throws XMLStreamException {
        final var xml = new SecureXmlParserFactory().createXmlEventReader(reader);

        while (xml.hasNext()) {
            var event = xml.nextEvent();
            if (startElement(event, LIST_MODULE) || startElement(event, LIST_FUNC)) {
                var metric = event.asStartElement().getAttributeByName(METRIC_ATTR).getValue();

                event = xml.nextEvent();
                while (!(endElement(event, LIST_MODULE) || endElement(event, LIST_FUNC))) {
                    if (startElement(event, MODULE)) {
                        readMetric(xml, root, metric, MODULE, true);
                    }
                    event = xml.nextEvent();
                }
            }
            else if (startElement(event, LIST_EXPORT)) {
                // <listing><List.EXPORT>
                event = xml.nextEvent();
                while (!endElement(event, LIST_EXPORT)) {
                    if (!startElement(event, MIXED)) {
                        event = xml.nextEvent();
                        continue;
                    }
                    var moduleAttr = event.asStartElement().getAttributeByName(MODULE);
                    var pathAttr = event.asStartElement().getAttributeByName(SRCPATH);
                    if (moduleAttr == null || pathAttr == null) {
                        event = xml.nextEvent();
                        continue;
                    }
                    var wrongSeparator = File.separatorChar == '\\' ? '/' : '\\';
                    var moduleName = moduleAttr.getValue()
                            .replace("\\\\", "")
                            .replace(wrongSeparator, File.separatorChar);
                    var filePath = pathAttr.getValue().replace(wrongSeparator, File.separatorChar);
                    filesToProcess.putIfAbsent(moduleName, filePath);

                    event = xml.nextEvent();
                }
                // </List.EXPORT></listing>
            }
        }
        xml.close();
    }
}
