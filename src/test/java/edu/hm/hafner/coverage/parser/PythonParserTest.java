package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.assertions.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PythonParserTest extends AbstractParserTest {


    @Override
    protected String getFolder() {
        return "python";
    }

    @Override
    PythonParser createParser(CoverageParser.ProcessingMode processingMode) {
        return new PythonParser(processingMode);
    }

    @Test
    void testBasic() throws Exception {
        var root = readReport("coverage.xml");
        for (FileNode f: root.getAllFileNodes()) {
            switch (f.getRelativePath()) {
                case "src/__init__.py":
                    Assertions.assertThat(f).hasMissedLines(8).hasCoveredLines(1,2,4,5,7,10,12,13,14,16);
                    break;
                case "src/file1.py":
                    Assertions.assertThat(f).hasMissedLines(12,13,17,18,19,20,21,22,23,25,29)
                            .hasCoveredLines(1,2,3,4,5,7,10,11,15);
                    break;
                case "src/dir/__init__.py":
                    Assertions.assertThat(f).hasMissedLines().hasCoveredLines();
                    break;
                case "src/dir/file2.py":
                    Assertions.assertThat(f)
                            .hasMissedLines(26,27,28,29,30,31,36,37,38,39,40,42,43,45,53,54,65,66,68,69,70,72,73,77,80,82,86,87,88,90,91,92,93,94,95,96,97)
                            .hasCoveredLines(1,2,3,4,5,6,7,8,10,13,14,33,84,85);
                    break;
                default:
                    throw new Exception("Unexpected file: " + f.getRelativePath());
            }
        }
    }

    @Test
    void testWorkspace() throws Exception {
        var root = readReport("coverage-2.xml");
        for (FileNode f: root.getAllFileNodes()) {
            switch (f.getRelativePath()) {
                case "./__init__.py":
                    Assertions.assertThat(f).hasMissedLines().hasCoveredLines();
                    break;
                case "./components.py":
                    Assertions.assertThat(f).hasMissedLines()
                            .hasCoveredLines(1,3,6,15,18);
                    break;
                default:
                    throw new Exception("Unexpected file: " + f.getRelativePath());
            }
        }
    }
}