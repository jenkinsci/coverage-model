package edu.hm.hafner.coverage;

import edu.hm.hafner.util.LineRange;
import edu.hm.hafner.util.LineRangeList;
import java.util.List;

/**
 * Groups the missed lines of a {@link FileNode} in {@link LineRange line ranges}.
 *
 * @author Ullrich Hafner
 */
final class MissedLineRanges {
    private static final int UNSET = -1;

    private MissedLineRanges() {
        // prevents instantiation
    }

    /**
     * Returns the lines of the specified file that have no line coverage grouped in LineRanges. E.g., the lines [1, 2,
     * 3] will be grouped in one {@link LineRange} instance.
     *
     * @param file the file to get the missed lines for
     * @return the aggregated LineRanges that have no line coverage
     */
    static LineRangeList of(final FileNode file) {
        var lineRanges = new LineRangeList();

        var missedLines = file.getMissedLines();
        if (missedLines.isEmpty()) {
            return lineRanges;
        }

        if (missedLines.size() == 1) {
            lineRanges.add(new LineRange(missedLines.getFirst()));

            return lineRanges;
        }

        var lines = List.copyOf(file.getLinesWithCoverage());

        int start = UNSET;
        int end = UNSET;

        for (int line : lines) {
            if (file.getCoveredOfLine(line) == 0) {
                if (start == UNSET) {
                    start = line;
                }
                end = line;
            } else {
                if (start != UNSET) {
                    lineRanges.add(new LineRange(start, end));
                    start = UNSET;
                }
            }
        }
        if (start != UNSET) {
            lineRanges.add(new LineRange(start, end));
        }

        return lineRanges;
    }
}
