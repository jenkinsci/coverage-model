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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for Go coverage reports.
 *
 * <p>
 * Go reports the covered blocks using the package path of a file rather than its file system path. Since there is no
 * fixed relation between both, the package paths are mapped to files using the {@code go.mod} descriptors of the
 * analyzed repository: the module with the longest matching prefix defines the directory of a coverage entry. If no
 * descriptor can be found, then the structure of the package path will be guessed.
 * </p>
 *
 * @see <a href="https://go.dev/doc/build-cover">Go coverage profiling support</a>
 * @see <a href="https://go.dev/ref/mod#go-mod-file">The go.mod file reference</a>
 * @author Ullrich Hafner
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
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

    /** Pattern to match the module directive of a {@code go.mod} descriptor: such files have no block comments. */
    private static final Pattern MODULE_PATTERN = Pattern.compile(
            "^\\s*module\\s+(?<module>\\S+)", Pattern.MULTILINE);

    private static final char PATH_SEPARATOR = '/';
    private static final String GO_MOD = "go.mod";
    private static final String VENDOR_DIRECTORY = "vendor";
    private static final int MAX_MODULE_SEARCH_DEPTH = 10;

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
        var goModules = GoModules.discover(reportFile, log);

        try (var bufferedReader = new BufferedReader(reader);
                var lines = bufferedReader.lines();
                var stream = new LookaheadStream(lines, reportFile)) {
            var fileData = new FileDataCollector();
            var modules = new LinkedHashMap<String, ModuleNode>();
            var builder = new TreeStringBuilder();
            var containerName = goModules.getRootName();

            while (stream.hasNext()) {
                var line = stream.next();
                var matcher = LINE_PATTERN.matcher(line);
                if (matcher.find()) {
                    var fullPath = matcher.group("fullPath");
                    if (containerName.isEmpty()) {
                        containerName = determineContainerName(fullPath);
                    }
                    processLine(matcher, goModules.resolve(fullPath).orElseGet(() -> guessPath(fullPath)),
                            modules, builder, fileData);
                }
            }

            builder.dedup();
            fileData.buildCoverages();
            handleEmptyResults(reportFile, log, modules.isEmpty());

            var container = new ModuleNode(containerName);
            container.addAllChildren(modules.values());
            return container;
        }
        catch (IOException exception) {
            throw new ParsingException(exception, "Can't read the coverage report: %s", reportFile);
        }
    }

    private String determineContainerName(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', PATH_SEPARATOR);
        var parts = StringUtils.split(normalizedPath, PATH_SEPARATOR);

        if (parts.length == 0) {
            return StringUtils.EMPTY;
        }

        if (parts.length >= 3 && parts[0].contains(".")) {
            return parts[0] + PATH_SEPARATOR + parts[1];
        }

        return parts[0];
    }

    private void processLine(final Matcher matcher, final PathParts pathParts,
            final Map<String, ModuleNode> modules, final TreeStringBuilder builder,
            final FileDataCollector fileData) {
        var module = modules.computeIfAbsent(pathParts.moduleName(), ModuleNode::new);

        var packageNode = module.findOrCreatePackageNode(pathParts.packagePath());
        var fileNode = packageNode.findOrCreateFileNode(pathParts.fileName(),
                builder.intern(PATH_UTIL.getRelativePath(Path.of(pathParts.relativePath()))));

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
     * Guesses the structure of a Go package path. Such a guess cannot be correct for all reports, so it is used only
     * if no {@code go.mod} descriptor is available.
     *
     * @param fullPath
     *         the package path of the coverage report
     *
     * @return the guessed path components
     */
    private PathParts guessPath(final String fullPath) {
        var normalizedPath = fullPath.replace('\\', PATH_SEPARATOR);
        var parts = StringUtils.split(normalizedPath, PATH_SEPARATOR);

        if (parts.length == 0) {
            return new PathParts(StringUtils.EMPTY, StringUtils.EMPTY,
                    StringUtils.EMPTY, StringUtils.EMPTY);
        }

        var pathInfo = guessPathStructure(parts);
        return new PathParts(pathInfo.moduleName(),
                joinPath(parts, pathInfo.packageStartIndex(), parts.length - 1),
                parts[parts.length - 1],
                joinPath(parts, pathInfo.packageStartIndex(), parts.length));
    }

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

    private String joinPath(final String[] parts, final int startIndex, final int endIndex) {
        if (startIndex >= endIndex) {
            return StringUtils.EMPTY;
        }
        return String.join(String.valueOf(PATH_SEPARATOR), Arrays.copyOfRange(parts, startIndex, endIndex));
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
     * Maps the package paths of a coverage report to files, using all {@code go.mod} descriptors that are stored
     * below the root of the repository that contains the report.
     */
    private static final class GoModules {
        private static final GoModules NO_MODULES = new GoModules(Path.of(StringUtils.EMPTY), List.of());

        private final Path rootDirectory;
        private final List<GoModule> modules;

        static GoModules discover(final String reportFile, final FilteredLog log) {
            return findRootDirectory(reportFile)
                    .map(root -> new GoModules(root, findModules(root, log)))
                    .orElse(NO_MODULES);
        }

        private static Optional<Path> findRootDirectory(final String reportFile) {
            try {
                var report = Path.of(reportFile).toAbsolutePath().normalize();
                if (Files.isRegularFile(report)) {
                    var directory = report.getParent();
                    while (directory != null) {
                        if (Files.isRegularFile(directory.resolve(GO_MOD))) {
                            return Optional.of(directory);
                        }
                        directory = directory.getParent();
                    }
                }
            }
            catch (InvalidPathException exception) {
                return Optional.empty();
            }
            return Optional.empty();
        }

        private static List<GoModule> findModules(final Path rootDirectory, final FilteredLog log) {
            try (var descriptors = Files.find(rootDirectory, MAX_MODULE_SEARCH_DEPTH,
                    (path, attributes) -> GO_MOD.equals(String.valueOf(path.getFileName())))) {
                return descriptors.filter(descriptor -> isNotVendored(rootDirectory.relativize(descriptor)))
                        .map(descriptor -> readModule(rootDirectory, descriptor, log))
                        .flatMap(Optional::stream)
                        .sorted(Comparator.comparingInt((GoModule module) -> module.prefix().length()).reversed())
                        .toList();
            }
            catch (IOException | UncheckedIOException exception) {
                log.logException(exception, "Cannot search for Go module descriptors in '%s'", rootDirectory);
                return List.of();
            }
        }

        private static boolean isNotVendored(final Path relativeDescriptor) {
            for (Path segment : relativeDescriptor) {
                if (VENDOR_DIRECTORY.equals(segment.toString())) {
                    return false;
                }
            }
            return true;
        }

        private static Optional<GoModule> readModule(final Path rootDirectory, final Path descriptor,
                final FilteredLog log) {
            try {
                var matcher = MODULE_PATTERN.matcher(Files.readString(descriptor));
                if (matcher.find()) {
                    return Optional.of(new GoModule(matcher.group("module"),
                            Objects.requireNonNullElse(descriptor.getParent(), rootDirectory)));
                }
                log.logError("Skipping Go module descriptor without module directive: '%s'", descriptor);
            }
            catch (IOException exception) {
                log.logException(exception, "Cannot read Go module descriptor: '%s'", descriptor);
            }
            return Optional.empty();
        }

        private GoModules(final Path rootDirectory, final List<GoModule> modules) {
            this.rootDirectory = rootDirectory;
            this.modules = modules;
        }

        String getRootName() {
            return modules.stream()
                    .filter(module -> module.directory().equals(rootDirectory))
                    .findFirst()
                    .map(GoModule::name)
                    .orElse(StringUtils.EMPTY);
        }

        Optional<PathParts> resolve(final String fullPath) {
            for (GoModule module : modules) {
                if (fullPath.startsWith(module.prefix())) {
                    return Optional.of(split(module, fullPath.substring(module.prefix().length())));
                }
            }
            return Optional.empty();
        }

        private PathParts split(final GoModule module, final String pathInModule) {
            var separatorIndex = pathInModule.lastIndexOf(PATH_SEPARATOR);
            var packagePath = separatorIndex < 0
                    ? StringUtils.EMPTY
                    : pathInModule.substring(0, separatorIndex);

            return new PathParts(module.name(), packagePath, pathInModule.substring(separatorIndex + 1),
                    rootDirectory.relativize(module.directory().resolve(pathInModule)).toString());
        }
    }

    private record GoModule(String name, String prefix, Path directory) {
        GoModule(final String name, final Path directory) {
            this(name, name + PATH_SEPARATOR, directory);
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
