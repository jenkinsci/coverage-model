package edu.hm.hafner.coverage;

import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a range of consecutive lines within a file ({@link FileNode}).
 *
 * @author Jannik Treichel
 */
public class LineRange implements Comparable<LineRange> {

    private final int start;
    private final int end;

    /**
     * Returns a set of LineRanges based on a given sorted set of line numbers.
     *
     * @param lines the set of line numbers to generate ranges from
     * @return a navigable set of LineRanges
     */
    public static NavigableSet<LineRange> getRangesFromSortedLines(final SortedSet<Integer> lines) {
        NavigableSet<LineRange> lineRanges = new TreeSet<>();

        if (lines.isEmpty()) {
            return lineRanges;
        }

        int currentStart = lines.first();
        int currentEnd = lines.first();

        for (int line : lines) {
            if (line - currentEnd > 1) {
                lineRanges.add(new LineRange(currentStart, currentEnd));
                currentStart = line;
            }
            currentEnd = line;
        }
        lineRanges.add(new LineRange(currentStart, currentEnd));

        return lineRanges;
    }

    /**
     * Returns a set of LineRanges based on a given unsorted set of line numbers.
     *
     * @param lines the set of line numbers to generate ranges from
     * @return a navigable set of LineRanges
     */
    public static NavigableSet<LineRange> getRangesFromUnsortedLines(final Set<Integer> lines) {
        return getRangesFromSortedLines(new TreeSet<>(lines));
    }

    /**
     * Constructs a new LineRange instance where the start- and end-
     * values are equal.
     *
     * @param startAndEnd the start- and end-value of the range
     */
    public LineRange(final int startAndEnd) {
        this(startAndEnd, startAndEnd);
    }

    /**
     * Constructs a new LineRange instance with a specified start-
     * and end-values.
     *
     * @param start the first line in the range
     * @param end the last line in the range
     * @throws IllegalArgumentException if start is less than or equal
     *          to zero, or if end is less than start
     */
    public LineRange(final int start, final int end) {
        if (start <= 0) {
            throw new IllegalArgumentException(
                    String.format("A LineRange can only contain positive values! Start-value <%s> is not possible.", start));
        }
        if (end < start) {
            throw new IllegalArgumentException(
                    String.format("The end <%s> of the LineRange cannot be smaller than the start <%s>!", end, start));
        }
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LineRange lineRange = (LineRange) o;
        return start == lineRange.start && end == lineRange.end;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public int compareTo(final LineRange o) {
        return Integer.compare(this.start, o.start);
    }

    @Override
    public String toString() {
        return String.format("<LineRange [%s-%s]>", start, end);
    }
}