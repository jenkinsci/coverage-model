package edu.hm.hafner.coverage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Describes the changes of a change set (e.g., a commit or a pull request) that are relevant for a coverage tree: the
 * lines of each file that have been modified, and the lines whose coverage has been changed indirectly, i.e., without a
 * modification of the line. The files are identified by their {@link FileNode#getRelativePath() relative path}.
 *
 * <p>Instances are immutable. Use a {@link ChangeSetBuilder} to create a change set.
 *
 * @author Ullrich Hafner
 */
@SuppressFBWarnings("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE")
public final class ChangeSet implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final ChangeSet EMPTY_SET = new ChangeSet(new TreeMap<>(), new TreeMap<>());

    @SuppressWarnings("serial")
    private final NavigableMap<String, NavigableSet<Integer>> modifiedLines;

    @SuppressWarnings("serial")
    private final NavigableMap<String, NavigableMap<Integer, Integer>> indirectCoverageChanges;

    /**
     * Returns a change set without any changes.
     *
     * @return the empty change set
     */
    public static ChangeSet empty() {
        return EMPTY_SET;
    }

    /**
     * Creates the change set of a coverage tree that stores the changes in its {@link FileNode file nodes}. This method
     * is only required to migrate trees that have been created before change sets have been introduced.
     *
     * @param root the root of the coverage tree
     * @return the change set that is stored in the tree
     */
    public static ChangeSet fromLegacyTree(final Node root) {
        var builder = new ChangeSetBuilder();
        for (FileNode file : root.getAllFileNodes()) {
            var path = file.getRelativePath();
            builder.addModifiedLines(path, file.getLegacyModifiedLines());
            file.getLegacyIndirectCoverageChanges()
                    .forEach((line, delta) -> builder.addIndirectCoverageChange(path, line, delta));
        }
        return builder.build();
    }

    private ChangeSet(
            final NavigableMap<String, NavigableSet<Integer>> modifiedLines,
            final NavigableMap<String, NavigableMap<Integer, Integer>> indirectCoverageChanges) {
        this.modifiedLines = modifiedLines;
        this.indirectCoverageChanges = indirectCoverageChanges;
    }

    /**
     * Returns the relative paths of all files that have modified lines or indirect coverage changes.
     *
     * @return the relative paths of the affected files, sorted alphabetically
     */
    public NavigableSet<String> getFiles() {
        var files = new TreeSet<>(modifiedLines.keySet());
        files.addAll(indirectCoverageChanges.keySet());
        return Collections.unmodifiableNavigableSet(files);
    }

    /**
     * Returns whether at least one line of one file has been modified.
     *
     * @return {@code true} if there are modified lines, {@code false} otherwise
     */
    public boolean hasModifiedLines() {
        return !modifiedLines.isEmpty();
    }

    /**
     * Returns whether the specified file has modified lines.
     *
     * @param path the relative path of the file
     * @return {@code true} if the file has modified lines, {@code false} otherwise
     */
    public boolean hasModifiedLines(final String path) {
        return modifiedLines.containsKey(path);
    }

    /**
     * Returns the modified lines of the specified file.
     *
     * @param path the relative path of the file
     * @return the modified lines, or an empty set if the file has not been modified
     */
    public NavigableSet<Integer> getModifiedLines(final String path) {
        return Collections.unmodifiableNavigableSet(modifiedLines.getOrDefault(path, Collections.emptyNavigableSet()));
    }

    /**
     * Returns whether the specified line of the specified file has been modified.
     *
     * @param path the relative path of the file
     * @param line the line to check
     * @return {@code true} if the line has been modified, {@code false} otherwise
     */
    public boolean isModified(final String path, final int line) {
        return getModifiedLines(path).contains(line);
    }

    /**
     * Returns whether the coverage of at least one line of one file has been affected indirectly.
     *
     * @return {@code true} if there are indirect coverage changes, {@code false} otherwise
     */
    public boolean hasIndirectCoverageChanges() {
        return !indirectCoverageChanges.isEmpty();
    }

    /**
     * Returns whether the coverage of the specified file has been affected indirectly.
     *
     * @param path the relative path of the file
     * @return {@code true} if the file has indirect coverage changes, {@code false} otherwise
     */
    public boolean hasIndirectCoverageChanges(final String path) {
        return indirectCoverageChanges.containsKey(path);
    }

    /**
     * Returns the indirect coverage changes of the specified file. The key is the line, the value is the delta of the
     * coverage hits before and after the code changes.
     *
     * @param path the relative path of the file
     * @return the indirect coverage changes, or an empty map if the coverage of the file has not been affected
     */
    public NavigableMap<Integer, Integer> getIndirectCoverageChanges(final String path) {
        return Collections.unmodifiableNavigableMap(
                indirectCoverageChanges.getOrDefault(path, Collections.emptyNavigableMap()));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        var that = (ChangeSet) o;
        return modifiedLines.equals(that.modifiedLines) && indirectCoverageChanges.equals(that.indirectCoverageChanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modifiedLines, indirectCoverageChanges);
    }

    @Override
    public String toString() {
        return "modified lines: %s, indirect coverage changes: %s".formatted(modifiedLines, indirectCoverageChanges);
    }

    /** Builds a {@link ChangeSet}. */
    public static class ChangeSetBuilder {
        private final NavigableMap<String, NavigableSet<Integer>> modifiedLines = new TreeMap<>();
        private final NavigableMap<String, NavigableMap<Integer, Integer>> indirectCoverageChanges = new TreeMap<>();

        /**
         * Marks the specified lines of the specified file as modified.
         *
         * @param path the relative path of the file
         * @param lines the modified lines
         * @return this builder
         */
        @CanIgnoreReturnValue
        public ChangeSetBuilder addModifiedLines(final String path, final int... lines) {
            for (int line : lines) {
                modifiedLines.computeIfAbsent(path, k -> new TreeSet<>()).add(line);
            }
            return this;
        }

        /**
         * Marks the specified lines of the specified file as modified.
         *
         * @param path the relative path of the file
         * @param lines the modified lines
         * @return this builder
         */
        @CanIgnoreReturnValue
        public ChangeSetBuilder addModifiedLines(final String path, final Collection<Integer> lines) {
            for (int line : lines) {
                modifiedLines.computeIfAbsent(path, k -> new TreeSet<>()).add(line);
            }
            return this;
        }

        /**
         * Adds an indirect coverage change for a specific line of the specified file.
         *
         * @param path the relative path of the file
         * @param line the line with the coverage change
         * @param hitsDelta the delta of the coverage hits before and after the code changes
         * @return this builder
         */
        @CanIgnoreReturnValue
        public ChangeSetBuilder addIndirectCoverageChange(final String path, final int line, final int hitsDelta) {
            indirectCoverageChanges.computeIfAbsent(path, k -> new TreeMap<>()).put(line, hitsDelta);
            return this;
        }

        /**
         * Creates the change set.
         *
         * @return the change set
         */
        public ChangeSet build() {
            var lines = new TreeMap<String, NavigableSet<Integer>>();
            modifiedLines.forEach((path, set) -> lines.put(path, new TreeSet<>(set)));
            var changes = new TreeMap<String, NavigableMap<Integer, Integer>>();
            indirectCoverageChanges.forEach((path, map) -> changes.put(path, new TreeMap<>(map)));
            return new ChangeSet(lines, changes);
        }
    }
}
