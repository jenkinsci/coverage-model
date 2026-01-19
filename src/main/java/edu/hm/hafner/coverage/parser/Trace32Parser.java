package edu.hm.hafner.coverage.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.io.StringReader;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.HashMap;
import java.util.Set;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;

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

    private static final QName TRACE32 = new QName("TRACE32");
    private static final QName COVERAGE = new QName("coverage");
    private static final QName LIST_FUNC = new QName("COVerage.EXPORT.ListFunc");
    private static final QName LIST_MODULE = new QName("COVerage.EXPORT.ListModule");
    private static final QName LIST_EXPORT = new QName("List.EXPORT");
    private static final QName MODULE = new QName("module");
    private static final QName FUNCTION = new QName("function");
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
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        var root = new ModuleNode("Trace32 Coverage");

        // Find and link module names to file paths
        log.logInfo("[TRACE32] Parsing files for listing...");
        var fileParent = Paths.get(fileName).getParent();
        if (fileParent == null) {
            fileParent = Paths.get(".");
        }
        try (var paths = Files.walk(fileParent)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                if (path.toString().endsWith(".xml")) {
                    try {
                        parseListing(path.toString(), log);
                    }
                    catch (XMLStreamException unused) {
                        // Ignore that file, we're only collecting file and module names
                    }
                }
            });
        }
        catch (IOException e) {
            throw new ParsingException(e);
        }

        // Parse main file
        try {
            parseFile(root, '.' + File.separatorChar + fileName, Optional.of(reader), log);
        }
        catch (XMLStreamException e) {
            throw new ParsingException(e);
        }

        if (root.isEmpty()) {
            handleEmptyResults(fileName, log);
            return new ModuleNode("empty");
        }

        // Add files and copy metrics
        var rootFiles = root.createClassNode("Trace32 Coverage Files");

        for (var entry : filesToProcess.entrySet()) {
            var filePath = entry.getValue(); // for clarity

            var fileNodePath = Paths.get(filePath).getFileName();
            var fileNodeName = (fileNodePath != null) ? fileNodePath.toString() : "missing_file";
            if (rootFiles.hasChild(filePath + fileNodeName)) {
                log.logInfo("[TRACE32] Found duplicate file: \"%s\", source: \"%s\"", entry.getKey(), filePath);
                continue;
            }
            log.logInfo("[TRACE32] Found file: \"%s\", source: \"%s\"", entry.getKey(), filePath);

            var fileNode = rootFiles.createFileNode(fileNodeName, TreeString.valueOf(filePath));
            var moduleName = resolveTreeName(entry.getKey());
            var moduleNode = getNodeTree(root, moduleName);
            if (moduleNode.isPresent()) {
                for (var value : moduleNode.get().getValues()) {
                    fileNode.addValue(value);
                }
            }
        }

        // Hide file nodes
        var fileMetricsBlacklist = Set.of(Metric.FILE, Metric.MODULE, Metric.PACKAGE, Metric.CLASS);
        for (var metric : root.getMetrics()) {
            if (!fileMetricsBlacklist.contains(metric)) {
                rootFiles.addValue(new CoverageBuilder(metric).withTotal(0).withCovered(0).build());
            }
        }

        return root;
    }

    private String resolveTreeName(final String treeName) {
        for (var file : filesToProcess.entrySet()) {
            var moduleName = file.getKey();
            if (treeName.startsWith(moduleName)) {
                var fileName = Paths.get(file.getValue()).getFileName();
                var moduleParent = Paths.get(moduleName).getParent();
                var replaceName = (moduleParent != null) ? moduleParent.resolve(fileName).toString() : fileName.toString();
                return treeName.replace(moduleName, replaceName);
            }
        }
        return treeName;
    }

    @SuppressWarnings("StringSplitter")
    private Optional<Node> getNodeTree(final Node root, final String treeName) {
        var lastNode = root;
        for (var name : treeName.split("[\\/\\\\]")) {
            var node = lastNode.findClass(name);
            if (node.isEmpty()) {
                return Optional.empty();
            }
            lastNode = node.get();
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

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private String readMetric(final XMLEventReader xml, final Node root, final String metric, final QName element) throws XMLStreamException {
        var treeName = "";
        var map = new EnumMap<Fields, Integer>(Fields.class);

        while (true) {
            var event = xml.nextEvent();
            if (event.isEndElement() && event.asEndElement().getName().equals(element)) {
                break;
            }
            else if (event.isStartElement()) {
                var data = xml.nextEvent().asCharacters().getData().trim();
                var tag = event.asStartElement().getName().getLocalPart();
                switch (tag) {
                    case "function" -> readMetric(xml, root, metric, FUNCTION);
                    case "tree" -> treeName = data.replace("\\\\", "").replace('\\', File.separatorChar);
                    default -> {
                        try {
                            map.put(Fields.fromTag(tag), parseInteger(data));
                        }
                        catch (IllegalArgumentException e) {
                            // Not a metric field, just continue
                        }
                    }
                }
            }
        }

        if (!treeName.isEmpty()) {
            var node = makeNodeTree(root, resolveTreeName(treeName));

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
        }

        return treeName;
    }

    private Optional<XMLEventReader> readFile(final String filePath, final Optional<Reader> reader, final FilteredLog log) throws XMLStreamException {
        Reader xmlReader;
        if (reader.isEmpty()) {
            try (var xmlModule = Files.newBufferedReader(Paths.get(filePath), UTF_8)) {
                var xmlContent = IOUtils.toString(xmlModule);
                xmlReader = new StringReader(xmlContent);
                log.logInfo("[TRACE32] Reading file: \"%s\"", filePath);
            }
            catch (FileNotFoundException | NoSuchFileException unused) {
                log.logInfo("[TRACE32] File \"%s\" does not exist.", filePath);
                return Optional.empty();
            }
            catch (IOException e) {
                throw new XMLStreamException("[TRACE32] Error reading file \"" + filePath + "\": " + e.getMessage(), e);
            }
        }
        else {
            xmlReader = reader.get();
        }

        return Optional.of(new SecureXmlParserFactory().createXmlEventReader(xmlReader));
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private void parseFile(final ModuleNode root, final String filePath, final Optional<Reader> reader, final FilteredLog log) throws XMLStreamException {
        final var xml = readFile(filePath, reader, log).orElse(null);
        if (xml == null) {
            return;
        }

        while (xml.hasNext()) {
            var event = xml.nextEvent();
            if (!event.isStartElement() || !event.asStartElement().getName().equals(TRACE32)) {
                continue;
            }
            // <TRACE32>
            while (true) {
                event = xml.nextEvent();
                if (event.isEndElement() && event.asEndElement().getName().equals(TRACE32)) {
                    break;
                }
                if (!event.isStartElement() || !event.asStartElement().getName().equals(COVERAGE)) {
                    continue;
                }
                // <coverage>
                while (true) {
                    event = xml.nextEvent();
                    if (event.isEndElement() && event.asEndElement().getName().equals(COVERAGE)) {
                        break;
                    }
                    if (!event.isStartElement()) {
                        continue;
                    }
                    var name = event.asStartElement().getName();
                    if (name.equals(LIST_MODULE)) {
                        // <COVerage.EXPORT.ListModule>
                        var metric = event.asStartElement().getAttributeByName(METRIC_ATTR).getValue();
                        while (true) {
                            event = xml.nextEvent();
                            if (event.isEndElement() && event.asEndElement().getName().equals(LIST_MODULE)) {
                                break;
                            }
                            if (!event.isStartElement() || !event.asStartElement().getName().equals(MODULE)) {
                                continue;
                            }

                            var treeName = readMetric(xml, root, metric, MODULE);
                            if (treeName.isEmpty() || reader.isEmpty()) {
                                continue;
                            }

                            var filePathParent = Paths.get(filePath).getParent();
                            if (filePathParent == null) {
                                filePathParent = Paths.get(".");
                            }
                            var modulePath = filePathParent
                                    .resolve(treeName)
                                    .resolve("index.xml")
                                    .toString();
                            parseFile(root, modulePath, Optional.empty(), log);
                        }
                        // <COVerage.EXPORT.ListModule>
                    }
                    else if (name.equals(LIST_FUNC)) {
                        // <COVerage.EXPORT.ListFunc>
                        var metric = event.asStartElement().getAttributeByName(METRIC_ATTR).getValue();
                        while (true) {
                            event = xml.nextEvent();
                            if (event.isEndElement() && event.asEndElement().getName().equals(LIST_FUNC)) {
                                break;
                            }
                            if (event.isStartElement() && event.asStartElement().getName().equals(MODULE)) {
                                readMetric(xml, root, metric, MODULE);
                            }
                        }
                        // </COVerage.EXPORT.ListFunc>
                    }
                }
                // </coverage>
            }
            // </TRACE32>
        }
        xml.close();
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private void parseListing(final String filePath, final FilteredLog log) throws XMLStreamException {
        final var xml = readFile(filePath, Optional.empty(), log).orElse(null);
        if (xml == null) {
            return;
        }

        while (xml.hasNext()) {
            var event = xml.nextEvent();
            if (!event.isStartElement() || !event.asStartElement().getName().equals(TRACE32)) {
                continue;
            }
            // <TRACE32>
            while (true) {
                event = xml.nextEvent();
                if (event.isEndElement() && event.asEndElement().getName().equals(TRACE32)) {
                    break;
                }
                if (!event.isStartElement() || !event.asStartElement().getName().equals(LIST_EXPORT)) {
                    continue;
                }
                // <listing><List.EXPORT>
                while (true) {
                    event = xml.nextEvent();
                    if (event.isEndElement() && event.asEndElement().getName().equals(LIST_EXPORT)) {
                        break;
                    }
                    if (event.isStartElement() && event.asStartElement().getName().equals(MIXED)) {
                        var moduleName = event.asStartElement().getAttributeByName(MODULE).getValue()
                                .replace("\\\\", "")
                                .replace('\\', File.separatorChar);
                        filesToProcess.putIfAbsent(moduleName, event.asStartElement()
                                .getAttributeByName(SRCPATH)
                                .getValue()
                                .replace(File.separatorChar == '\\' ? '/' : '\\', File.separatorChar)
                        );
                    }
                }
                // </List.EXPORT></listing>
            }
            // </TRACE32>
        }
        xml.close();
    }
}
