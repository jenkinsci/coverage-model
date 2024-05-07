package edu.hm.hafner.coverage.parser;

import edu.hm.hafner.coverage.CoverageParser;
import edu.hm.hafner.coverage.FileNode;
import edu.hm.hafner.coverage.assertions.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

class CloverParserTest extends AbstractParserTest {
    @Override
    protected String getFolder() {
        return "clover";
    }

    @Override
    CoverageParser createParser(CoverageParser.ProcessingMode processingMode) {
        return new CloverParser(processingMode);
    }

    @Test
    void testBasic() throws Exception {
        var root = readReport("clover.xml");
        for (FileNode f: root.getAllFileNodes()) {
            switch(f.getFileName()) {
                case "File1.js":
                    Set<Integer> covered = new HashSet<>();
                    addRange(covered, 4,7);
                    addRange(covered, 12,22);
                    addRange(covered, 24,43);
                    addRange(covered, 45,77);
                    addRange(covered, 79,77);
                    Assertions.assertThat(f).hasMissedLines().hasCoveredLines(covered.toArray(new Integer[covered.size()]));

                    break;
                case "File2.js":
                    Assertions.assertThat(f).hasMissedLines(92,127,204,369,492,503,515).hasCoveredLines(21,38,51,65,79,105,117,138,151,164,176,190,215,228,243,257,268,287,303,317,329,339,349,359,380,393,405,416,429,443,456,467,480);
                    break;
                case "File3.jsx":
                    Assertions.assertThat(f).hasMissedLines(45,46,78,104,105,106).hasCoveredLines(13,21,26,29,32,60,61,62,89,93,103);
                    break;
                case "File4.jsx":
                    Assertions.assertThat(f).hasMissedLines(8,50,51,58).hasCoveredLines(4,11,14,30,38,43,46,49,57,61,68,72,77,81,85,89,90);
                    break;
                case "File5.jsx":
                    Assertions.assertThat(f).hasMissedLines(25).hasCoveredLines(18,19,20,24,31,50,59);
                    break;
                case "File6.jsx":
                    Assertions.assertThat(f).hasMissedLines(32,33,35,92).hasCoveredLines(23,24,31,38,72,79,90);
                    break;
                default:
                    throw new Exception("Unexpected file: " + f.getFileName());
            }

        }
    }
    private static void addRange(Set<Integer> collection, int start, int end) {
        // generate a range of integers from start to end (inclusive)
        for (int i = start; i <= end; i++) {
            collection.add(i);
        }
    }
}