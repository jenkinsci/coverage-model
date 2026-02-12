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
import edu.umd.cs.findbugs.annotations.CheckForNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A parser for Go coverage reports.
 *
 * @see <a href="https://go.dev/doc/build-cover">Go coverage profiling support</a>
 * @author Ullrich Hafner
 */
public class GoCovParser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = -4511292826873362408L;

    private static final PathUtil PATH_UTIL = new PathUtil();
    private static final Pattern PATH_SEPARATOR = Pattern.compile("/");

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
        super(ProcessingMode.FAIL_FAST);
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
            var projectName = StringUtils.EMPTY;
            var builder = new TreeStringBuilder();

            while (stream.hasNext()) {
                var line = stream.next();
                var matcher = LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    projectName = processLine(matcher, modules, projectName, builder, fileData);
                }
            }

            builder.dedup();
            fileData.buildCoverages();
            handleEmptyResults(reportFile, log, modules.isEmpty());

            var container = new ModuleNode(projectName);
            container.addAllChildren(modules);
            return container;
        }
        catch (IOException exception) {
            throw new ParsingException(exception, "Can't read the coverage report: %s", reportFile);
        }
    }

    private String processLine(final Matcher matcher, final Set<ModuleNode> modules, final String projectName,
            final TreeStringBuilder builder, final FileDataCollector fileData) {
        var fullPath = matcher.group("fullPath");
        var pathParts = parseGoPath(fullPath);

        var updatedProjectName = projectName.isEmpty() && !pathParts.projectName.isEmpty()
                ? pathParts.projectName
                : projectName;

        var moduleName = pathParts.moduleName.isEmpty() ? updatedProjectName : pathParts.moduleName;
        var module = findOrCreateModule(modules, moduleName);
        if (module == null) {
            module = new ModuleNode(moduleName);
            modules.add(module);
        }

        var packageNode = module.findOrCreatePackageNode(pathParts.packagePath);
        var fileNode = packageNode.findOrCreateFileNode(pathParts.fileName,
                builder.intern(PATH_UTIL.getRelativePath(Path.of(pathParts.relativePath))));

        fileData.addFile(fileNode);
        recordCoverage(matcher, fileNode, fileData);

        return updatedProjectName;
    }

    @CheckForNull
    private ModuleNode findOrCreateModule(final Set<ModuleNode> modules, final String moduleName) {
        return modules.stream()
                .filter(m -> m.getName().equals(moduleName))
                .findFirst()
                .orElse(null);
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
     * Parses a Go package path into project, module, package, and file components.
     *
     * @param fullPath the full path from the Go coverage report
     * @return the parsed path components
     */
    private PathParts parseGoPath(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', '/');
        var parts = Arrays.asList(PATH_SEPARATOR.split(normalizedPath));

        if (parts.isEmpty()) {
            return new PathParts(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, 
                    StringUtils.EMPTY, StringUtils.EMPTY);
        }

        var fileName = parts.get(parts.size() - 1);
        var pathInfo = determinePathStructure(parts);

        var packagePath = buildPackagePath(parts, pathInfo.packageStartIndex());
        var relativePath = buildRelativePath(parts, pathInfo.packageStartIndex());

        return new PathParts(pathInfo.projectName(), pathInfo.moduleName(), packagePath, fileName, relativePath);
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private PathInfo determinePathStructure(final List<String> parts) {
        if (parts.size() == 1) {
            return new PathInfo(StringUtils.EMPTY, StringUtils.EMPTY, 0);
        }
        if (parts.size() == 2) {
            return new PathInfo(parts.get(0), parts.get(0), 1);
        }
        if (parts.size() == 3) {
            return parts.get(0).contains(".")
                    ? new PathInfo(parts.get(0), parts.get(0), 1)
                    : new PathInfo(parts.get(0), parts.get(0), 1);
        }
        return parts.get(0).contains(".")
                ? new PathInfo(parts.get(0) + "/" + parts.get(1), parts.get(2), 3)
                : new PathInfo(parts.get(0), parts.get(1), 2);
    }

    private String buildPackagePath(final List<String> parts, final int startIndex) {
        return buildPath(parts, startIndex, parts.size() - 1, ".");
    }

    private String buildRelativePath(final List<String> parts, final int startIndex) {
        return buildPath(parts, startIndex, parts.size(), "/");
    }

    private String buildPath(final List<String> parts, final int startIndex, final int endIndex, 
            final String separator) {
        return String.join(separator, parts.subList(startIndex, endIndex));
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
                var missedLines = getLines(missedRangesPerFile, file);
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
                    .collect(Collectors.toList());
        }

        private void merge(final Map<String, List<LineRange>> map, final String key, final LineRange value) {
            map.merge(key, new ArrayList<>(List.of(value)),
                    (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    });
        }
    }

    private record PathInfo(String projectName, String moduleName, int packageStartIndex) {
    }

    /**
     * Container for parsed Go path components.
     *
     * @param projectName the project name (usually domain/owner or just domain)
     * @param moduleName the module name (usually the repository or project name)
     * @param packagePath the package path (directories between module and file, dot-separated)
     * @param fileName the file name
     * @param relativePath the relative path from module root (slash-separated)
     */
    private record PathParts(String projectName, String moduleName, String packagePath, String fileName,
                             String relativePath) {
    }
}
