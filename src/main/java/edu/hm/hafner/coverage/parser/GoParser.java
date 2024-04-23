package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.*;
import edu.hm.hafner.util.FilteredLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;

import static edu.hm.hafner.coverage.parser.JacocoParser.createValue;

public class GoParser extends CoverageParser {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(GoParser.class.getName());

    private static final String GO_TEST_MODE = "mode: ";
    private static final String USAGE_ERROR_MSG = "Invalid Go coverage output format. First line should be coverage mode. Options [set, count, atomic]";

    public CoverMode coverMode = CoverMode.OTHER;

    public GoParser(final ProcessingMode processingMode) {
        super(processingMode);
    }

    @Override
    protected ModuleNode parseReport(Reader reader, FilteredLog log) throws InvalidGoCoverageFormatException {
        try {
            BufferedReader reader1 = new BufferedReader(reader);
            String line = reader1.readLine();
            if (line == null || line.isBlank()) {
                handleEmptyResults(log);
                if (ignoreErrors()) {
                    return new ModuleNode("empty");
                }
            } else if (line.startsWith(GO_TEST_MODE)) {
                line = line.substring(GO_TEST_MODE.length());
            } else {
                LOGGER.severe(USAGE_ERROR_MSG);
                log.logError(USAGE_ERROR_MSG);
                throw new InvalidGoCoverageFormatException("Expected the coverage mode specification, but got: [" + line + "]");
            }
            switch(line) {
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

            ModuleNode root = null;
            GoCoverageMeta meta = new GoCoverageMeta();
            line = reader1.readLine();
            do {
                /**
                 * Format is:
                 * domain_name/root_dir/repo/dir_path.../file_name:<start-line>.<start-column>,<end-line>.<end-column> <statement-count>  <execution-count>
                 */
                String[] split = line.split("[\\/ ]");
                String domain = split[0];
                String org = split[1];
                String repo = split[2];
                String module = String.join("/", domain, org, repo);
                if (root == null) {
                    root = new ModuleNode(String.join("/", domain, org, repo));
                }

                int pkgIndxEnd = split.length - 3;
                String pkg = String.join("/", Arrays.copyOfRange(split, 3, pkgIndxEnd));
                if (pkg.endsWith("/")) {
                    pkg = pkg.substring(0, pkg.length()-1);
                }
                PackageNode pNode = meta.getOrAddPkg(pkg);
                addChildHelper(root, pNode);

                String s = split[pkgIndxEnd];
                String[] split2 = s.split("[:,]");
                String fileName = split2[0];
                String filePath;
                if (pkg != null && !pkg.isBlank()) {
                    filePath = String.join("/", pkg, fileName);
                } else {
                    filePath = fileName;
                }
                FileNode fNode = meta.getOrAddFile(fileName, filePath);
                addChildHelper(pNode, fNode);

                String[] start = split2[1].split("\\.");
                String startLine = start[0];
                String startCol = start[1];
                String[] end = split2[2].split("\\.");
                String endLine = end[0];
                String endCol = end[1];
                Integer statementCount = Integer.parseInt(split[split.length-2]);
                Integer executionCount = Integer.parseInt(split[split.length-1]);
                for (Integer i = Integer.parseInt(startLine), j = Integer.parseInt(endLine); i <= j; i++) {
                    if (executionCount > 0) {
                        fNode.addCounterIncremental(i, statementCount, 0);
                    } else {
                        fNode.addCounterIncremental(i, 0, statementCount);
                    }
                }
                line = reader1.readLine();
            } while (line != null);

            if (root == null) {
                handleEmptyResults(log);
            }

            meta.resolveAggregate();

            return root;

        } catch (IOException e) {
            String ioEMsg = "IOException while reading from Reader: " + e.getMessage();
            LOGGER.severe(ioEMsg);
            log.logError(ioEMsg);
        }
        return null;
    }

    public static enum CoverMode {
        SET, COUNT, ATOMIC, OTHER
    }

    private Node addChildHelper(Node parent, Node child) {
        if (!parent.hasChild(child.getName())) {
            parent.addChild(child);
        }
        return child;
    }

    static class GoCoverageMeta {
        HashMap<String, PackageNode> pkgs = new HashMap<>();
        HashMap<String, FileNode> files = new HashMap<>();
        private boolean addHelper(HashMap<String, Node> map, String s, Node c) {
            if (map.containsKey(s)) {
                return false;
            } else {
                map.put(s, c);
                return true;
            }
        }

        boolean hasHelper(HashMap<String, Node> map, String s) {
            return map.containsKey(s);
        }

        public PackageNode getOrAddPkg(String name) {
            if (pkgs.containsKey(name)) {
                return pkgs.get(name);
            } else {
                var pkg = new PackageNode(name);
                pkgs.put(name, pkg);
                return pkg;
            }
        }

        public FileNode getOrAddFile(String fileName, String filePath) {
            if (files.containsKey(filePath)) {
                return files.get(filePath);
            } else {
                var f = new FileNode(fileName, filePath);
                files.put(filePath, f);
                return f;
            }
        }

        void resolveAggregate() {
            for (FileNode fileNode: files.values()) {
                fileNode.addValue(createValue("LINE", fileNode.getCoveredLines().size(), fileNode.getMissedLines().size()));
            }
        }
    }

    static class InvalidGoCoverageFormatException extends RuntimeException {

        public InvalidGoCoverageFormatException(String message) {
            super(message);
        }
    }
}