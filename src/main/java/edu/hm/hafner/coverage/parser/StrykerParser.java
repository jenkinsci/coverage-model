package edu.hm.hafner.coverage.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.Metric;
import edu.hm.hafner.coverage.ModuleNode;
import edu.hm.hafner.coverage.Mutation;
import edu.hm.hafner.coverage.MutationStatus;
import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.PathUtil;
import edu.hm.hafner.util.TreeString;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Parses Stryker JSON mutation reports into a hierarchical Java object model.
 * 
 * @author Akash Manna
 * @see <a href="https://stryker-mutator.io/">Stryker Mutation</a>
 */
@SuppressWarnings("checkstyle:ClassDataAbstractionCoupling")
public class StrykerParser extends CoverageParser {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final PathUtil PATH_UTIL = new PathUtil();

    private static final String FILES = "files";
    private static final String MUTANTS = "mutants";
    private static final String ID = "id";
    private static final String MUTATOR_NAME = "mutatorName";
    private static final String DESCRIPTION = "description";
    private static final String STATUS = "status";
    private static final String KILLED_BY = "killedBy";
    private static final String LOCATION = "location";
    private static final String START = "start";
    private static final String LINE = "line";

    /**
     * Creates a new instance of {@link StrykerParser}.
     */
    public StrykerParser() {
        this(ProcessingMode.FAIL_FAST);
    }

    /**
     * Creates a new instance of {@link StrykerParser}.
     *
     * @param processingMode
     *         determines whether to ignore errors
     */
    public StrykerParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try {
            var report = MAPPER.readTree(reader);
            var files = report.path(FILES);

            var root = new ModuleNode(EMPTY);
            boolean isEmpty = true;

            if (files.isObject()) {
                var iterator = files.fields();
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    readFile(root, entry.getKey(), entry.getValue());
                    isEmpty = false;
                }
            }

            handleEmptyResults(fileName, log, isEmpty);
            return root;
        }
        catch (IOException exception) {
            throw new ParsingException(exception);
        }
    }

    private void readFile(final ModuleNode root, final String reportFileName, final JsonNode fileNode) {
        var relativePath = normalizePath(PATH_UTIL.getRelativePath(reportFileName));
        var fileName = getFileName(relativePath);
        var packageName = getPackageName(relativePath);
        var packageNode = root.findOrCreatePackageNode(packageName);
        var coverageFile = packageNode.findOrCreateFileNode(fileName, TreeString.valueOf(relativePath));

        int covered = 0;
        int missed = 0;

        var mutants = fileNode.path(MUTANTS);
        if (mutants.isArray()) {
            for (var mutantNode : mutants) {
                var mutation = createMutation(fileName, relativePath, mutantNode);
                coverageFile.addMutation(mutation);

                if (!mutation.isValid()) {
                    continue;
                }
                if (mutation.isDetected()) {
                    covered++;
                }
                else {
                    missed++;
                }
            }
        }

        if (covered + missed > 0) {
            coverageFile.addValue(new CoverageBuilder(Metric.MUTATION).withCovered(covered).withMissed(missed).build());
        }
    }

    private Mutation createMutation(final String fileName, final String relativePath, final JsonNode mutantNode) {
        var status = readStatus(mutantNode.path(STATUS).asText(StringUtils.EMPTY));
        var line = mutantNode.path(LOCATION).path(START).path(LINE).asInt(0);
        var id = mutantNode.path(ID).asText(Integer.toString(line));
        var mutatorName = mutantNode.path(MUTATOR_NAME).asText(StringUtils.EMPTY);
        var description = mutantNode.path(DESCRIPTION).asText(StringUtils.EMPTY);

        return new Mutation.MutationBuilder()
                .withIsDetected(status == MutationStatus.KILLED)
                .withStatus(status)
                .withLine(line)
                .withMutator(mutatorName)
                .withKillingTest(joinStringArray(mutantNode.path(KILLED_BY)))
                .withDescription(description)
                .withSourceFile(fileName)
                .withMutatedClass(toMutatedClass(relativePath))
                .withMutatedMethod("mutation-" + id)
                .withMutatedMethodSignature(StringUtils.EMPTY)
                .build();
    }

    private static String joinStringArray(final JsonNode node) {
        if (!node.isArray()) {
            return StringUtils.EMPTY;
        }

        var builder = new StringBuilder();
        var iterator = node.elements();
        while (iterator.hasNext()) {
            if (!builder.isEmpty()) {
                builder.append(',');
            }
            builder.append(iterator.next().asText(StringUtils.EMPTY));
        }
        return builder.toString();
    }

    private static String getFileName(final String relativePath) {
        var path = Path.of(relativePath).getFileName();
        return path == null ? relativePath : path.toString();
    }

    private static String getPackageName(final String relativePath) {
        var packagePath = StringUtils.substringBeforeLast(relativePath, "/");
        if (StringUtils.isBlank(packagePath)) {
            return EMPTY;
        }
        return packagePath.replace('/', '.');
    }

    private static String normalizePath(final String path) {
        return path.replace('\\', '/');
    }

    private static String toMutatedClass(final String relativePath) {
        return StringUtils.substringBeforeLast(normalizePath(relativePath), ".")
                .replace('/', '.');
    }

    private static MutationStatus readStatus(final String status) {
        return switch (StringUtils.lowerCase(status, Locale.ENGLISH)) {
            case "killed" -> MutationStatus.KILLED;
            case "survived" -> MutationStatus.SURVIVED;
            case "nocoverage" -> MutationStatus.NO_COVERAGE;
            case "timeout" -> MutationStatus.TIMED_OUT;
            case "runtimeerror" -> MutationStatus.RUN_ERROR;
            case "compileerror" -> MutationStatus.MEMORY_ERROR;
            case "ignored" -> MutationStatus.NON_VIABLE;
            default -> MutationStatus.NON_VIABLE;
        };
    }
}