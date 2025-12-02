package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.PackageNode;
import edu.hm.hafner.coverage.Value;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.LookaheadStream;
import edu.hm.hafner.util.PathUtil;
import org.apache.commons.lang3.tuple.MutablePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * A parser for LCOV coverage reports.
 */
public class LcovParser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final PathUtil PATH_UTIL = new PathUtil();

    /**
     * Creates a new instance of {@link LcovParser}.
     */
    public LcovParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link LcovParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public LcovParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity", "NullAway"})
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        var root = new ModuleNode(EMPTY);
        Map<String, Map<Integer, MutablePair<Integer, Integer>>> files = new LinkedHashMap<>();
        String currentFile = "-";

        try (var br = new BufferedReader(reader);
                var lines = br.lines();
                var stream = new LookaheadStream(lines, fileName)) {
            while (stream.hasNext()) {
                var raw = stream.next();
                var line = raw.trim();
                if (line.isEmpty()) {
                    continue;
                }

                // Source file (SF:<path>)
                if (line.startsWith("SF:")) {
                    currentFile = line.substring(3);
                    files.putIfAbsent(currentFile, new TreeMap<>());
                }

                // Instruction coverage (DA:<line number>,<execution count>[,<checksum>])
                else if (line.startsWith("DA:")) {
                    String[] parts = line.substring(3).split(",", 3);
                    var ln = Integer.parseInt(parts[0]);
                    var exec = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    var pair = files.getOrDefault(currentFile, new TreeMap<>()).getOrDefault(ln, MutablePair.of(0, 0));
                    pair.setLeft(exec > 0 ? 1 : 0);
                    files.get(currentFile).put(ln, pair);
                }
                // Branch coverage (BRDA:<line>,<block>,<branch>,<taken>)
                else if (line.startsWith("BRDA:")) {
                    String[] parts = line.substring(5).split(",", 4);
                    var ln = Integer.parseInt(parts[0]);
                    var taken = parts.length > 3 ? parts[3] : "-";
                    var pair = files.getOrDefault(currentFile, new TreeMap<>()).getOrDefault(ln, MutablePair.of(0, 0));
                    if (!"-".equals(taken) && !"0".equals(taken)) {
                        pair.setRight(pair.getRight() + 1);
                    }
                    files.get(currentFile).put(ln, pair);
                }
            }
        }
        catch (IOException | NumberFormatException e) {
            throw new ParsingException(e);
        }

        if (files.isEmpty()) {
            handleEmptyResults(fileName, log);
            return new ModuleNode("empty");
        }

        createCoverages(root, files);

        return root;
    }

    private void createCoverages(final ModuleNode root, final Map<String, Map<Integer, MutablePair<Integer, Integer>>> files) {
        // Create nodes in model
        var packageNode = new PackageNode(EMPTY);
        var moduleNode = new ModuleNode(EMPTY);
        moduleNode.addChild(packageNode);
        root.addChild(moduleNode);

        var builder = getTreeStringBuilder();
        var lineBuilder = new CoverageBuilder().withMetric(Metric.LINE);
        var instructionBuilder = new CoverageBuilder().withMetric(Metric.INSTRUCTION);

        for (var entry : files.entrySet()) {
            String path = entry.getKey();
            String normalized = normalizePath(path);
            String filename = baseName(normalized);
            var id = builder.intern(PATH_UTIL.getRelativePath(Path.of(normalized)));
            var fileNode = packageNode.findOrCreateFileNode(filename, id);

            // Compute per-file aggregated metrics from the collected per-line data
            int totalLines = entry.getValue().size();
            int coveredInstructions = 0;
            int coveredLines = 0;
            for (var lineEntry : entry.getValue().entrySet()) {
                int instrCovered = lineEntry.getValue().getLeft();
                int branchesCovered = lineEntry.getValue().getRight();
                if (instrCovered > 0) {
                    coveredInstructions += instrCovered;
                }
                if (instrCovered > 0 || branchesCovered > 0) {
                    coveredLines++;
                }
            }
            int missedInstructions = Math.max(0, totalLines - coveredInstructions);
            int missedLines = Math.max(0, totalLines - coveredLines);

            // Add aggregated values so the aggregator produces PACKAGE/FILE metrics
            fileNode.addValue(instructionBuilder.withCovered(coveredInstructions).withMissed(missedInstructions).build());
            fileNode.addValue(lineBuilder.withCovered(coveredLines).withMissed(missedLines).build());
            fileNode.addValue(new Value(Metric.LOC, totalLines));

            // Add per-line counters
            for (var lineEntry : entry.getValue().entrySet()) {
                int lineNumber = lineEntry.getKey();
                int instrCovered = lineEntry.getValue().getLeft();
                int branchesCovered = lineEntry.getValue().getRight();
                addCounters(fileNode, lineNumber, instrCovered, branchesCovered);
            }
        }
    }

    private void addCounters(final FileNode fileNode, final int lineNumber, final int coveredInstructions, final int coveredBranches) {
        int missed;
        int covered;

        // No branches, only instruction coverage
        if (coveredBranches == 0) {
            covered = coveredInstructions > 0 ? 1 : 0;
            missed = covered > 0 ? 0 : 1;
        }

        // Both
        else {
            covered = coveredBranches;
            missed = coveredBranches - coveredInstructions;
        }
        fileNode.addCounters(lineNumber, covered, missed);
    }

    private static String normalizePath(final String path) {
        return path.trim().replace('\\', '/');
    }

    private static String baseName(final String normalizedPath) {
        if (normalizedPath.isEmpty()) {
            return "";
        }
        int idx = normalizedPath.lastIndexOf('/');
        return idx >= 0 ? normalizedPath.substring(idx + 1) : normalizedPath;
    }
}
