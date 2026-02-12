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
import java.io.Serializable;
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
    private static final Pattern GO_MOD_MODULE_PATTERN = Pattern.compile("^\\s*module\\s+([^\\s]+)");

    /** 
     * Pattern to match Go coverage lines: path/file.go:line.col,line.col statements executions. 
     */
    private static final Pattern LINE_PATTERN = Pattern.compile(
            "(?<fullPath>[^:]+):"
                    + "(?<lineStart>\\d+)\\.(?<columnStart>\\d+),"
                    + "(?<lineEnd>\\d+)\\.(?<columnEnd>\\d+)\\s+"
                    + "(?<statements>\\d+)\\s+"
                    + "(?<executions>\\d+)");

    private final transient ModuleRegistry moduleRegistry;

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
        this(processingMode, new ModuleRegistry());
    }

    /**
     * Creates a new instance of {@link GoCovParser} with module information.
     *
     * @param processingMode
     *         determines whether to ignore errors
     * @param moduleRegistry
     *         registry containing known Go module names for accurate path resolution
     */
    public GoCovParser(final ProcessingMode processingMode, final ModuleRegistry moduleRegistry) {
        super(processingMode);
        this.moduleRegistry = moduleRegistry;
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

            var container = new ModuleNode(containerName.isEmpty() ? StringUtils.EMPTY : containerName.toString());
            container.addAllChildren(modules);
            return container;
        }
        catch (IOException exception) {
            throw new ParsingException(exception, "Can't read the coverage report: %s", reportFile);
        }
    }

    private String determineContainerName(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', '/');
        var parts = Arrays.stream(PATH_SEPARATOR.split(normalizedPath)).toList();
        
        if (parts.isEmpty()) {
            return StringUtils.EMPTY;
        }
        
        if (parts.size() >= 3 && parts.get(0).contains(".")) {
            return parts.get(0) + "/" + parts.get(1);
        }
        
        return parts.get(0);
    }

    private void processLine(final Matcher matcher, final Set<ModuleNode> modules,
            final TreeStringBuilder builder, final FileDataCollector fileData) {
        var fullPath = matcher.group("fullPath");
        var pathParts = parseGoPath(fullPath);

        var moduleName = pathParts.moduleName;
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
     * Parses a Go package path into module, package, and file components.
     * Uses module registry for accurate module detection via longest-prefix matching when available.
     *
     * @param fullPath the full path from the Go coverage report
     * @return the parsed path components
     */
    private PathParts parseGoPath(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', '/');
        
        var moduleMatch = moduleRegistry.findModuleForPath(normalizedPath);
        if (moduleMatch != null) {
            return createPathPartsFromModule(normalizedPath, moduleMatch);
        }
        
        var parts = Arrays.stream(PATH_SEPARATOR.split(normalizedPath)).toList();

        if (parts.isEmpty()) {
            return new PathParts(StringUtils.EMPTY, StringUtils.EMPTY, 
                    StringUtils.EMPTY, StringUtils.EMPTY);
        }

        var fileName = parts.get(parts.size() - 1);
        var pathInfo = determinePathStructure(parts);

        var packagePath = buildPackagePath(parts, pathInfo.packageStartIndex());
        var relativePath = buildRelativePath(parts, pathInfo.packageStartIndex());

        return new PathParts(pathInfo.moduleName(), packagePath, fileName, relativePath);
    }

    /**
     * Creates path parts from a matched module using Go module semantics.
     * The coverage path format is: module_name/relative_path
     * We extract the relative path after the module name and parse it accordingly.
     *
     * @param fullPath the complete coverage path
     * @param moduleInfo the matched module information
     * @return parsed path components
     */
    private PathParts createPathPartsFromModule(final String fullPath, final ModuleInfo moduleInfo) {
        var moduleName = moduleInfo.name();
        var parts = Arrays.stream(PATH_SEPARATOR.split(fullPath)).toList();
        var fileName = parts.get(parts.size() - 1);
        
        String relativePath;
        String packagePath;
        
        if (fullPath.startsWith(moduleName + "/")) {
            relativePath = fullPath.substring(moduleName.length() + 1);
            var relParts = Arrays.stream(PATH_SEPARATOR.split(relativePath)).toList();
            if (relParts.size() > 1) {
                packagePath = String.join(".", relParts.subList(0, relParts.size() - 1));
            } 
            else {
                packagePath = StringUtils.EMPTY;
            }
        } 
        else if (fullPath.equals(moduleName)) {
            relativePath = StringUtils.EMPTY;
            packagePath = StringUtils.EMPTY;
        } 
        else {
            relativePath = fullPath;
            packagePath = StringUtils.EMPTY;
        }
        
        return new PathParts(moduleName, packagePath, fileName, relativePath);
    }

    /**
     * Determines path structure using heuristics. Fallback when no {@link ModuleRegistry} is available.
     * Note: Cannot handle arbitrary Go module depths; use ModuleRegistry for accurate parsing.
     *
     * @param parts the path segments split by '/'
     * @return path information containing module name and package start index
     */
    private PathInfo determinePathStructure(final List<String> parts) {
        if (parts.size() == 1) {
            return new PathInfo(StringUtils.EMPTY, 0);
        }
        if (parts.size() == 2 || parts.size() == 3) {
            return new PathInfo(parts.get(0), 1);
        }
        return parts.get(0).contains(".")
                ? new PathInfo(parts.get(2), 3)
                : new PathInfo(parts.get(1), 2);
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
     * @param moduleName the module name (usually the repository or project name)
     * @param packagePath the package path (directories between module and file, dot-separated)
     * @param fileName the file name
     * @param relativePath the relative path from module root (slash-separated)
     */
    private record PathParts(String moduleName, String packagePath, String fileName,
                             String relativePath) {
    }

    /**
     * Registry of known Go modules for accurate path-to-module mapping.
     * Implements longest-prefix matching as required by Go module resolution.
     */
    public static class ModuleRegistry implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        
        private final List<ModuleInfo> modules;

        /**
         * Creates an empty module registry.
         */
        public ModuleRegistry() {
            this.modules = new ArrayList<>();
        }

        /**
         * Creates a registry with known modules.
         *
         * @param modules the list of known modules
         */
        public ModuleRegistry(final Collection<ModuleInfo> modules) {
            this.modules = new ArrayList<>(modules);
            this.modules.sort((a, b) -> Integer.compare(b.name().length(), a.name().length()));
        }

        /**
         * Registers a new module.
         *
         * @param name the module name from go.mod
         * @param path the relative path where the module is located
         */
        public void addModule(final String name, final String path) {
            modules.add(new ModuleInfo(name, path));
            modules.sort((a, b) -> Integer.compare(b.name().length(), a.name().length()));
        }

        /**
         * Parses a go.mod file and extracts the module name.
         * According to Go spec, there should be exactly one module declaration per go.mod file.
         * This implementation processes only the first module line found and handles inline comments.
         *
         * @param goModContent the content of the go.mod file
         * @param modulePath the relative path where this module is located
         */
        public void parseAndAddGoMod(final String goModContent, final String modulePath) {
            for (String rawLine : goModContent.lines().toList()) {
                var line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("//")) {
                    continue;
                }
                
                var matcher = GO_MOD_MODULE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String moduleName = matcher.group(1);
                    int commentIndex = moduleName.indexOf("//");
                    if (commentIndex >= 0) {
                        moduleName = moduleName.substring(0, commentIndex).trim();
                    }
                    addModule(moduleName, modulePath);
                    break;
                }
            }
        }

        /**
         * Finds the best matching module for a coverage path using longest-prefix matching.
         * This implements the algorithm described by egonelbre: match the longest module name
         * that is a prefix of the coverage path.
         *
         * @param coveragePath the path from the coverage report (e.g., "ext/sub/sub.go")
         * @return the matching module info, or null if no match found
         */
        @CheckForNull
        public ModuleInfo findModuleForPath(final String coveragePath) {
            for (ModuleInfo module : modules) {
                if (coveragePath.equals(module.name()) || coveragePath.startsWith(module.name() + "/")) {
                    return module;
                }
            }
            return null;
        }

        /**
         * Returns all registered modules.
         *
         * @return the list of modules
         */
        public List<ModuleInfo> getModules() {
            return new ArrayList<>(modules);
        }

        /**
         * Checks if the registry is empty.
         *
         * @return true if no modules are registered
         */
        public boolean isEmpty() {
            return modules.isEmpty();
        }
    }

    /**
     * Information about a Go module extracted from go.mod.
     *
     * @param name the module name (e.g., "github.com/user/project", "ext", "ext/sub")
     * @param path the relative path where this module is located (e.g., "./", "./hack/ext")
     */
    public record ModuleInfo(String name, String path) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
    }
}
