package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.*;
import edu.hm.hafner.util.FilteredLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Go parser that parses coverage for Go coverage.
 */
public class GoParser extends CoverageParser {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(GoParser.class.getName());

    private static final String GO_TEST_MODE = "mode: ";
    private static final String USAGE_ERROR_MSG = "Invalid Go coverage output format. First line should be coverage mode. "
            + "Options [set, count, atomic]";

    private static final long serialVersionUID = -2355926546294138279L;

    /**
     * Creates a new instance of {@link GoParser}.
     *
     * @param processingMode determines whether to ignore errors
     */
    public GoParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(final Reader reader, final String fileName, final FilteredLog log) {
        try (BufferedReader reader1 = new BufferedReader(reader)) {
            //reading the first line
            ModuleNode moduleNode = readFirstLineAndParse(fileName, reader1, log);
            //should return as the coverage file is empty
            if (moduleNode != null) {
                return moduleNode;
            }

            //Read from second line and parse the coverage
            GoCoverageMeta meta = new GoCoverageMeta();
            ModuleNode root = parseCoverageData(reader1, meta);
            if (root == null) {
                handleEmptyResults(fileName, log);
                return new ModuleNode("empty");
            }
            else {
                meta.resolveAggregate();
                return root;
            }
        }
        catch (IOException e) {
            String ioEMsg = "IOException while reading from Reader: " + e.getMessage();
            log.logError(ioEMsg);
            throw new ParsingException(e, ioEMsg);
        }
    }

    /**
     * Defining the coverage mode.
     */
    public enum CoverMode {
        SET, COUNT, ATOMIC
    }

    private ModuleNode readFirstLineAndParse(final String fileName, final BufferedReader reader, final FilteredLog log) throws InvalidGoCoverageFormatException, IOException {
        String line = reader.readLine();
        //If it is empty file returning with empty module node
        if (line == null || line.isBlank()) {
            handleEmptyResults(fileName, log);
            if (ignoreErrors()) {
                return new ModuleNode("empty");
            }
        }
        else {
            /*
            Setting the coverage mode based on what is defined in the first line, expected coverage mode for go as below
            mode: count
            mode: atomic
            mode: set
             */
            setCoverageMode(line, log);
        }
        return null;
    }

    /**
     * Parse the coverage data.
     *
     * @param reader filename reader
     * @param meta   meta data
     * @return module node
     * @throws IOException when unable to generate the module node
     */
    private ModuleNode parseCoverageData(final BufferedReader reader, final GoCoverageMeta meta) throws IOException {
        //Reading the second line
        ModuleNode root = null;
        String line = reader.readLine();
        if (line == null) {
            return root;
        }
        do {
            // Format is:
            // domain_name/root_dir/repo/dir_path.../file_name:<start-line>.<start-column>,<end-line>.<end-column> <statement-count>  <execution-count>
            // where domain_name/root_dir is optional (i.e. relative imports)
            // Limiting elements to 2 by splitting based on first ":"
            String[] temp = line.split(":", 2);
            String fileAbsolutePath = temp[0];
            String coverageData = temp[1];
            //get the paths in the form like module, package1, package2, filename
            List<String> paths = Arrays.asList(fileAbsolutePath.split("/"));
            //Calculating the relative path based index to find right module
            int relativeImportOffset = fetchRelativePathOffset(paths.get(0));
            //initialize the module node
            root = initializeModuleNode(root, paths, relativeImportOffset);
            //initialize the package node
            PackageNode pNode = initializePackageNode(root, paths, meta, relativeImportOffset);
            //Initialize the file node
            FileNode fNode = initializeFileNode(pNode, paths, meta, relativeImportOffset);

            //Calculate the coverage
            calculateCoverage(fNode, coverageData);
            line = reader.readLine();
        } while (line != null);
        return root;
    }

    private ModuleNode initializeModuleNode(final ModuleNode root, final List<String> paths, final int relativeImportOffset) {
        //Defining the module node as the repo
        var modulePaths = paths.subList(0, relativeImportOffset);
        ModuleNode rootNode = null;
        if (root == null) {
            String module = String.join("/", modulePaths);
            rootNode = new ModuleNode(module);
        }
        return root == null ? rootNode : root;
    }

    private FileNode initializeFileNode(final PackageNode pNode, final List<String> paths, final GoCoverageMeta meta, final int relativeImportOffset) {
        //Removing the fist element as it is module
        var filePaths = paths.subList(relativeImportOffset, paths.size());
        //last element in the path is the file name
        var fileName = paths.get(paths.size() - 1);
        var filePath = String.join("/", filePaths);
        //removing file from the paths as it is processed
        FileNode fNode = meta.getOrAddFile(fileName, filePath);
        addChildHelper(pNode, fNode);
        return fNode;
    }

    private PackageNode initializePackageNode(final ModuleNode root, final List<String> paths, final GoCoverageMeta meta,
                                              final int relativeImportOffset) {
        //Removing the fist and last element as those are module and file
        var packagePaths = paths.subList(relativeImportOffset, paths.size() - 1);
        String packageName = String.join("/", packagePaths);
        PackageNode pNode = meta.getOrAddPkg(packageName);
        addChildHelper(root, pNode);
        return pNode;
    }

    /**
     * Initialize module node based on if is relative import of not.
     *
     * @param domainName takes the domain name
     * @return if it relative import or not
     */
    private int fetchRelativePathOffset(final String domainName) {
        //Format is:
        //domain_name/root_dir/repo/dir_path.../file_name:<start-line>.<start-column>,<end-line>.<end-column> <statement-count>  <execution-count>
        //where domain_name/root_dir is optional (i.e. relative imports)
        return domainName.contains(".") ? 3 : 1; // reverse offset for relative imports
    }

    private void calculateCoverage(final FileNode fNode, final String coverageData) {
        String[] split = coverageData.split("[, ]");
        String[] start = split[0].split("\\.");
        String startLine = start[0];
        String[] end = split[1].split("\\.");
        String endLine = end[0];
        int statementCount = Integer.parseInt(split[split.length - 2]);
        int executionCount = Integer.parseInt(split[split.length - 1]);
        int j = Integer.parseInt(endLine);
        for (int i = Integer.parseInt(startLine); i <= j; i++) {
            if (executionCount > 0) {
                fNode.addCounterIncremental(i, statementCount, 0);
            }
            else {
                fNode.addCounterIncremental(i, 0, statementCount);
            }
        }
    }

    private void setCoverageMode(final String line, final FilteredLog log) throws InvalidGoCoverageFormatException {
        String processedLine;
        if (line.startsWith(GO_TEST_MODE)) {
            processedLine = line.substring(GO_TEST_MODE.length());
        }
        else {
            LOGGER.severe(USAGE_ERROR_MSG);
            log.logError(USAGE_ERROR_MSG);
            throw new InvalidGoCoverageFormatException("Expected the coverage mode specification, but got: [" + line + "]");
        }
        //coverage mode
        CoverMode coverMode;
        switch (processedLine) {
            case "set":
                coverMode = CoverMode.SET;
                break;
            case "count":
                coverMode = CoverMode.COUNT;
                break;
            case "atomic":
                coverMode = CoverMode.ATOMIC;
                break;
            default:
                throw new InvalidGoCoverageFormatException("Expected the coverage mode specification, but got: [" + line + "]");
        }
        log.logInfo("Coverage mode set to " + coverMode);
    }

    private void addChildHelper(final Node parent, final Node child) {
        //Using getId to validate if it has child as the comparison done in the method against id of the node
        if (!parent.hasChild(child.getId())) {
            parent.addChild(child);
        }
    }

    static class GoCoverageMeta {
        private final Map<String, PackageNode> pkgs = new HashMap<>();
        private final Map<String, FileNode> files = new HashMap<>();

        public PackageNode getOrAddPkg(final String name) {
            if (pkgs.containsKey(name)) {
                return pkgs.get(name);
            }
            else {
                var pkg = new PackageNode(name);
                pkgs.put(name, pkg);
                return pkg;
            }
        }

        public FileNode getOrAddFile(final String fileName, final String filePath) {
            if (files.containsKey(filePath)) {
                return files.get(filePath);
            }
            else {
                var f = new FileNode(fileName, filePath);
                files.put(filePath, f);
                return f;
            }
        }

        void resolveAggregate() {
            for (FileNode fileNode : getFiles().values()) {
                fileNode.addValue(createValue("LINE", fileNode.getCoveredLines().size(), fileNode.getMissedLines().size()));
            }
        }

        public Map<String, FileNode> getFiles() {
            return files;
        }
    }

    static class InvalidGoCoverageFormatException extends ParsingException {
        private static final long serialVersionUID = -235592654629413823L;

        InvalidGoCoverageFormatException(final String message) {
            super(message);
        }
    }
}
