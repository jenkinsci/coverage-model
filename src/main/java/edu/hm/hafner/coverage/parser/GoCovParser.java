package edu.hm.hafner.coverage.parser;

import org.apache.commons.lang3.StringUtils;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.LookaheadStream;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.TreeStringBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for Go coverage reports.
 *
 * @see <a href="https://go.dev/doc/build-cover">Go coverage profiling support</a>
 * @author Ullrich Hafner
 */
@SuppressWarnings({"checkstyle:ClassDataAbstractionCoupling"})
public class GoCovParser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = -4511292826873362408L;

    private static final PathUtil PATH_UTIL = new PathUtil();

    /** 
     * Pattern to match Go coverage lines: path/file.go:line.col,line.col statements executions. 
     */
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "(?<fullPath>[^:]+):"
                    + "(?<lineStart>\\d+)\\.(?<columnStart>\\d+),"
                    + "(?<lineEnd>\\d+)\\.(?<columnEnd>\\d+)\\s+"
                    + "(?<statements>\\d+)\\s+"
                    + "(?<executions>\\d+)");

    /**
     * Creates a new instance of {@link GoCovParser}.
     */
    public GoCovParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link GoCovParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public GoCovParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String reportFile, final FilteredLog log) {
        try (var bufferedReader = new BufferedReader(reader);
                var lines = bufferedReader.lines();
                var stream = new LookaheadStream(lines, reportFile)) {
            var fileData = new FileDataCollector();
            var modules = new HashSet<ModuleNode>();
            var containerName = new StringBuilder();
            var builder = new TreeStringBuilder();

            while (stream.hasNext()) {
                var line = stream.next();
                var matcher = LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    var fullPath = matcher.group("fullPath");
                    if (containerName.isEmpty()) {
                        containerName.append(determineContainerName(fullPath));
                    }
                    processLine(matcher, modules, builder, fileData);
                }
            }

            builder.dedup();
            fileData.buildCoverages();
            handleEmptyResults(reportFile, log, modules.isEmpty());

            var container = new ModuleNode(containerName.toString());
            container.addAllChildren(modules);
            return container;
        }
        catch (IOException exception) {
            throw new ParsingException(exception, "Can't read the coverage report: %s", reportFile);
        }
    }

    private String determineContainerName(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', '/');
        var parts = StringUtils.split(normalizedPath, '/');
        
        if (parts.length == 0) {
            return StringUtils.EMPTY;
        }
        
        if (parts.length >= 3 && parts[0].contains(".")) {
            return parts[0] + "/" + parts[1];
        }
        
        return parts[0];
    }

    private void processLine(final Matcher matcher, final Set<ModuleNode> modules,
            final TreeStringBuilder builder, final FileDataCollector fileData) {
        var fullPath = matcher.group("fullPath");
        var pathParts = parseGoPath(fullPath);

        var moduleName = pathParts.moduleName;
        var existingModule = modules.stream()
                .filter(m -> m.getName().equals(moduleName))
                .findFirst();
        
        ModuleNode module;
        if (existingModule.isPresent()) {
            module = existingModule.get();
        }
        else {
            module = new ModuleNode(moduleName);
            modules.add(module);
        }

        var packageNode = module.findOrCreatePackageNode(pathParts.packagePath);
        var fileNode = packageNode.findOrCreateFileNode(pathParts.fileName,
                builder.intern(PATH_UTIL.getRelativePath(Path.of(pathParts.relativePath))));

        fileData.addFile(fileNode);
        recordCoverage(matcher, fileNode, fileData);
    }

    private void recordCoverage(final Matcher matcher, final FileNode fileNode, final FileDataCollector fileData) {
        var instructions = asInt(matcher, "statements");
        var range = new LineRange(asInt(matcher, "lineStart"), asInt(matcher, "lineEnd"));
        var executions = asInt(matcher, "executions");

        if (executions > 0) {
            fileData.addCovered(fileNode.getId(), range, instructions);
        }
        else {
            fileData.addMissed(fileNode.getId(), range, instructions);
        }
    }

    /**
     * Parses a Go package path into module, package, and file components.
     * Uses module registry for accurate module detection via longest-prefix matching when available.
     *
     * @param fullPath the full path from the Go coverage report
     * @return the parsed path components
     */
    private PathParts parseGoPath(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', '/');
        var parts = StringUtils.split(normalizedPath, '/');

        if (parts.length == 0) {
            return new PathParts(StringUtils.EMPTY, StringUtils.EMPTY, 
                    StringUtils.EMPTY, StringUtils.EMPTY);
        }

        var fileName = parts[parts.length - 1];
        var pathInfo = guessPathStructure(parts);

        var packagePath = buildPackagePath(parts, pathInfo.packageStartIndex());
        var relativePath = buildRelativePath(parts, pathInfo.packageStartIndex());

        return new PathParts(pathInfo.moduleName(), packagePath, fileName, relativePath);
    }

    /**
     * Guesses path structure using heuristics. Fallback when no {@link ModuleRegistry} is available.
     * Note: Cannot handle arbitrary Go module depths; use ModuleRegistry for accurate parsing.
     *
     * @param parts the path segments split by '/'
     * @return path information containing module name and package start index
     */
    private PathInfo guessPathStructure(final String... parts) {
        if (parts.length == 1) {
            return new PathInfo(StringUtils.EMPTY, 0);
        }
        if (parts.length == 2 || parts.length == 3) {
            return new PathInfo(parts[0], 1);
        }
        return parts[0].contains(".")
                ? new PathInfo(parts[2], 3)
                : new PathInfo(parts[1], 2);
    }

    private String buildPackagePath(final String[] parts, final int startIndex) {
        return buildPath(parts, startIndex, parts.length - 1);
    }

    private String buildRelativePath(final String[] parts, final int startIndex) {
        return buildPath(parts, startIndex, parts.length);
    }

    private String buildPath(final String[] parts, final int startIndex, final int endIndex) {
        if (startIndex >= endIndex) {
            return StringUtils.EMPTY;
        }
        var result = new StringBuilder(parts[startIndex]);
        for (int i = startIndex + 1; i < endIndex; i++) {
            result.append('/').append(parts[i]);
        }
        return result.toString();
    }

    private int asInt(final Matcher matcher, final String group) {
        try {
            return Integer.parseInt(matcher.group(group));
        }
        catch (NumberFormatException exception) {
            return 0;
        }
    }

    /**
     * Helper class to collect and build file coverage data.
     */
    private static class FileDataCollector {
        private final Map<String, List<LineRange>> coveredRangesPerFile = new HashMap<>();
        private final Map<String, Integer> coveredInstructionsPerFile = new HashMap<>();
        private final Map<String, List<LineRange>> missedRangesPerFile = new HashMap<>();
        private final Map<String, Integer> missedInstructionsPerFile = new HashMap<>();
        private final Set<FileNode> files = new HashSet<>();

        void addFile(final FileNode file) {
            files.add(file);
        }

        void addCovered(final String fileId, final LineRange range, final int instructions) {
            merge(coveredRangesPerFile, fileId, range);
            coveredInstructionsPerFile.merge(fileId, instructions, Integer::sum);
        }

        void addMissed(final String fileId, final LineRange range, final int instructions) {
            merge(missedRangesPerFile, fileId, range);
            missedInstructionsPerFile.merge(fileId, instructions, Integer::sum);
        }

        void buildCoverages() {
            var lineBuilder = new CoverageBuilder().withMetric(Metric.LINE);
            var instructionBuilder = new CoverageBuilder().withMetric(Metric.INSTRUCTION);

            for (FileNode file : files) {
                var coveredInstructions = coveredInstructionsPerFile.getOrDefault(file.getId(), 0);
                var missedInstructions = missedInstructionsPerFile.getOrDefault(file.getId(), 0);
                file.addValue(instructionBuilder.withCovered(coveredInstructions)
                        .withMissed(missedInstructions).build());

                var coveredLines = getLines(coveredRangesPerFile, file);
                var missedLines = new ArrayList<>(getLines(missedRangesPerFile, file));
                missedLines.removeAll(coveredLines);

                file.addValue(lineBuilder.withCovered(coveredLines.size()).withMissed(missedLines.size()).build());

                coveredLines.forEach(line -> file.addCounters(line, 1, 0));
                missedLines.forEach(line -> file.addCounters(line, 0, 1));
            }
        }

        private List<Integer> getLines(final Map<String, List<LineRange>> rangesPerFile, final FileNode file) {
            return rangesPerFile.getOrDefault(file.getId(), List.of()).stream()
                    .map(LineRange::getLines)
                    .flatMap(Collection::stream)
                    .toList();
        }

        private void merge(final Map<String, List<LineRange>> map, final String key, final LineRange value) {
            map.merge(key, new ArrayList<>(List.of(value)),
                    (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    });
        }
    }

    private record PathInfo(String moduleName, int packageStartIndex) {
    }

    /**
     * Container for parsed Go path components.
     *
     * @param moduleName the module name 
     * @param packagePath the package path 
     * @param fileName the file name
     * @param relativePath the relative path from module root
     */
    private record PathParts(String moduleName, String packagePath, String fileName,
                             String relativePath) {
    }
}
