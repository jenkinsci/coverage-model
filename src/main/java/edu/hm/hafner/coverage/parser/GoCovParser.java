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
            // Store the coverage information per file in different maps so we can merge them afterward
            var coveredRangesPerFile = new HashMap<String, List<LineRange>>();
            var coveredInstructionsPerFile = new HashMap<String, Integer>();
            var missedRangesPerFile = new HashMap<String, List<LineRange>>();
            var missedInstructionsPerFile = new HashMap<String, Integer>();
            var files = new HashSet<FileNode>();

            var builder = new TreeStringBuilder();
            var modules = new HashSet<ModuleNode>();
            var projectName = StringUtils.EMPTY;
            while (stream.hasNext()) {
                var line = stream.next();
                var matcher = LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    var fullPath = matcher.group("fullPath");
                    var pathParts = parseGoPath(fullPath);
                    
                    if (projectName.isEmpty() && !pathParts.projectName.isEmpty()) {
                        projectName = pathParts.projectName;
                    }

                    var moduleName = pathParts.moduleName.isEmpty() ? projectName : pathParts.moduleName;
                    
                    var possibleModule = modules.stream()
                            .filter(m -> m.getName().equals(moduleName))
                            .findFirst();
                    ModuleNode module;
                    if (possibleModule.isEmpty()) {
                        module = new ModuleNode(moduleName);
                        modules.add(module);
                    }
                    else {
                        module = possibleModule.get();
                    }

                    var packageName = pathParts.packagePath;
                    var packageNode = module.findOrCreatePackageNode(packageName);

                    var fileName = pathParts.fileName;
                    var relativePath = pathParts.relativePath;
                    var fileNode = packageNode.findOrCreateFileNode(fileName,
                            builder.intern(PATH_UTIL.getRelativePath(Path.of(relativePath))));

                    var instructions = asInt(matcher, "statements");
                    var range = new LineRange(asInt(matcher, "lineStart"), asInt(matcher, "lineEnd"));
                    files.add(fileNode);
                    if (asInt(matcher, "executions") > 0) {
                        merge(coveredRangesPerFile, fileNode.getId(), range);
                        coveredInstructionsPerFile.merge(fileNode.getId(), instructions, Integer::sum);
                    }
                    else {
                        merge(missedRangesPerFile, fileNode.getId(), range);
                        missedInstructionsPerFile.merge(fileNode.getId(), instructions, Integer::sum);
                    }
                }
            }

            builder.dedup();

            buildCoverages(files,
                    coveredInstructionsPerFile, missedInstructionsPerFile,
                    coveredRangesPerFile, missedRangesPerFile);

            handleEmptyResults(reportFile, log, modules.isEmpty());

            var container = new ModuleNode(projectName);
            container.addAllChildren(modules);
            return container;
        }
        catch (IOException exception) {
            throw new ParsingException(exception, "Can't read the coverage report: %s", reportFile);
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
        var parts = normalizedPath.split("/");
        
        if (parts.length == 0) {
            return new PathParts(StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY);
        }
        
        var fileName = parts[parts.length - 1];
        
        String projectName;
        String moduleName;
        int packageStartIndex;
        
        if (parts.length == 1) {
            projectName = StringUtils.EMPTY;
            moduleName = StringUtils.EMPTY;
            packageStartIndex = 0;
        } 
        else if (parts.length == 2) {
            projectName = parts[0];
            moduleName = parts[0];
            packageStartIndex = 1;
        } 
        else if (parts.length == 3) {
            if (parts[0].contains(".")) {
                projectName = parts[0];
                moduleName = parts[0];
                packageStartIndex = 1;
            } 
            else {
                projectName = parts[0];
                moduleName = parts[1];
                packageStartIndex = 2;
            }
        } 
        else {
            if (parts[0].contains(".")) {
                projectName = parts[0] + "/" + parts[1];
                moduleName = parts[2];
                packageStartIndex = 3;
            } 
            else {
                projectName = parts[0];
                moduleName = parts[1];
                packageStartIndex = 2;
            }
        }
        
        var packageParts = new ArrayList<String>();
        for (int i = packageStartIndex; i < parts.length - 1; i++) {
            packageParts.add(parts[i]);
        }
        var packagePath = String.join(".", packageParts);
        
        var relativePathParts = new ArrayList<String>();
        for (int i = packageStartIndex; i < parts.length; i++) {
            relativePathParts.add(parts[i]);
        }
        var relativePath = String.join("/", relativePathParts);
        
        return new PathParts(projectName, moduleName, packagePath, fileName, relativePath);
    }
    
    /**
     * Container for parsed Go path components.
     *
     * @param projectName
     *         the project name (usually domain/owner or just domain)
     * @param moduleName
     *         the module name (usually the repository or project name)
     * @param packagePath
     *         the package path (directories between module and file, dot-separated)
     * @param fileName
     *         the file name
     * @param relativePath
     *         the relative path from module root (slash-separated)
     */
    private record PathParts(String projectName, String moduleName, String packagePath, String fileName,
                             String relativePath) {
    }

    private void buildCoverages(final Set<FileNode> files,
            final Map<String, Integer> coveredInstructionsPerFile,
            final Map<String, Integer> missedInstructionsPerFile,
            final Map<String, List<LineRange>> coveredRangesPerFile,
            final Map<String, List<LineRange>> missedRangesPerFile) {
        var lineBuilder = new CoverageBuilder().withMetric(Metric.LINE);
        var instructionBuilder = new CoverageBuilder().withMetric(Metric.INSTRUCTION);
        for (FileNode file : files) {
            var coveredInstructions = coveredInstructionsPerFile.getOrDefault(file.getId(), 0);
            var missedInstructions = missedInstructionsPerFile.getOrDefault(file.getId(), 0);
            file.addValue(instructionBuilder.withCovered(coveredInstructions).withMissed(missedInstructions).build());

            var coveredLines = getLines(coveredRangesPerFile, file);
            var missedLines = getLines(missedRangesPerFile, file);
            missedLines.removeAll(coveredLines);

            file.addValue(lineBuilder.withCovered(coveredLines.size()).withMissed(missedLines.size()).build());

            coveredLines.forEach(line -> file.addCounters(line, 1, 0));
            missedLines.forEach(line -> file.addCounters(line, 0, 1));
        }
    }

    private List<Integer> getLines(final Map<String, List<LineRange>> coveredRangesPerFile, final FileNode file) {
        return coveredRangesPerFile.getOrDefault(file.getId(), List.of()).stream()
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

    private int asInt(final Matcher matcher, final String group) {
        try {
            return Integer.parseInt(matcher.group(group));
        }
        catch (NumberFormatException exception) {
            return 0;
        }
    }
}
