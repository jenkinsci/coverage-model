package edu.hm.hafner.coverage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Stores the number of covered and missed items for each code line of a file. Depending on the coverage metric, these
 * items are the instructions or branches of a line, the MC/DC pairs of a line, or the function calls of a line.
 *
 * @author Ullrich Hafner
 */
public final class LineCounters implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("serial")
    private final NavigableMap<Integer, Integer> covered;

    @SuppressWarnings("serial")
    private final NavigableMap<Integer, Integer> missed;

    /** Creates new empty counters. */
    public LineCounters() {
        this(new TreeMap<>(), new TreeMap<>());
    }

    private LineCounters(final NavigableMap<Integer, Integer> covered, final NavigableMap<Integer, Integer> missed) {
        this.covered = covered;
        this.missed = missed;
    }

    /**
     * Creates counters that use the specified maps. Missing maps (i.e., from old serializations) are replaced by empty
     * maps.
     *
     * @param covered the covered items per line
     * @param missed the missed items per line
     * @return the counters
     */
    @SuppressWarnings("PMD.ShortMethodName")
    static LineCounters of(final NavigableMap<Integer, Integer> covered, final NavigableMap<Integer, Integer> missed) {
        return new LineCounters(
                Objects.requireNonNullElseGet(covered, TreeMap::new),
                Objects.requireNonNullElseGet(missed, TreeMap::new));
    }

    /**
     * Sets the counters for the specified line.
     *
     * @param line the line number
     * @param coveredItems the number of covered items
     * @param missedItems the number of missed items
     */
    public void put(final int line, final int coveredItems, final int missedItems) {
        covered.put(line, coveredItems);
        missed.put(line, missedItems);
    }

    /**
     * Returns the number of covered items for the specified line.
     *
     * @param line the line number
     * @return the number of covered items, or 0 if there are no counters for the line
     */
    public int getCovered(final int line) {
        return covered.getOrDefault(line, 0);
    }

    /**
     * Returns the number of missed items for the specified line.
     *
     * @param line the line number
     * @return the number of missed items, or 0 if there are no counters for the line
     */
    public int getMissed(final int line) {
        return missed.getOrDefault(line, 0);
    }

    /**
     * Returns whether there are counters for the specified line.
     *
     * @param line the line number
     * @return {@code true} if there are counters for the line, {@code false} otherwise
     */
    public boolean contains(final int line) {
        return covered.containsKey(line);
    }

    /**
     * Returns the lines that have counters.
     *
     * @return a copy of the lines that have counters
     */
    public NavigableSet<Integer> getLines() {
        return new TreeSet<>(covered.keySet());
    }

    /**
     * Returns the number of covered items for each line that has counters, ordered by line.
     *
     * @return the covered items
     */
    public int[] getCoveredCounters() {
        return toArray(covered);
    }

    /**
     * Returns the number of missed items for each line that has counters, ordered by line.
     *
     * @return the missed items
     */
    public int[] getMissedCounters() {
        return toArray(missed);
    }

    /**
     * Returns the covered items for each line.
     *
     * @return an unmodifiable view of the covered items per line
     */
    public NavigableMap<Integer, Integer> getCoveredPerLine() {
        return Collections.unmodifiableNavigableMap(covered);
    }

    /**
     * Returns a copy of these counters.
     *
     * @return the copy
     */
    public LineCounters copy() {
        return new LineCounters(new TreeMap<>(covered), new TreeMap<>(missed));
    }

    private static int[] toArray(final NavigableMap<Integer, Integer> map) {
        return map.values().stream().mapToInt(i -> i).toArray();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (LineCounters) o;
        return covered.equals(that.covered) && missed.equals(that.missed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(covered, missed);
    }

    @Override
    public String toString() {
        return "covered: %s, missed: %s".formatted(covered, missed);
    }
}
