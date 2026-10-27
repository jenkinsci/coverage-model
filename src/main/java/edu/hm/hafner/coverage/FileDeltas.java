package edu.hm.hafner.coverage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Describes the differences between the coverage values of the files of a coverage tree and the corresponding values of
 * the files of a reference tree. The files are identified by their {@link FileNode#getRelativePath() relative path}.
 * Only metrics that are available in both files are part of the deltas.
 *
 * <p>Instances are immutable. Use a {@link FileDeltasBuilder} to create the deltas.
 *
 * @author Ullrich Hafner
 */
@SuppressFBWarnings("SING_SINGLETON_IMPLEMENTS_SERIALIZABLE")
public final class FileDeltas implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final FileDeltas EMPTY_DELTAS = new FileDeltas(new TreeMap<>());

    @SuppressWarnings("serial")
    private final NavigableMap<String, NavigableMap<Metric, Difference>> deltas;

    /**
     * Returns deltas without any differences.
     *
     * @return the empty deltas
     */
    public static FileDeltas empty() {
        return EMPTY_DELTAS;
    }

    /**
     * Creates the deltas of a coverage tree that stores the deltas in its {@link FileNode file nodes}. This method is
     * only required to migrate trees that have been created before file deltas have been introduced.
     *
     * @param root the root of the coverage tree
     * @return the deltas that are stored in the tree
     */
    public static FileDeltas fromLegacyTree(final Node root) {
        var builder = new FileDeltasBuilder();
        for (FileNode file : root.getAllFileNodes()) {
            file.getLegacyCoverageDelta().values().stream()
                    .filter(Difference.class::isInstance)
                    .map(Difference.class::cast)
                    .forEach(difference -> builder.addDelta(file.getRelativePath(), difference));
        }
        return builder.build();
    }

    private FileDeltas(final NavigableMap<String, NavigableMap<Metric, Difference>> deltas) {
        this.deltas = deltas;
    }

    /**
     * Returns whether at least one file has a delta.
     *
     * @return {@code true} if there are deltas, {@code false} otherwise
     */
    public boolean hasDeltas() {
        return !deltas.isEmpty();
    }

    /**
     * Returns whether at least one file has a delta for the specified metric.
     *
     * @param metric the metric to check
     * @return {@code true} if at least one file has a delta for the metric, {@code false} otherwise
     */
    public boolean hasDelta(final Metric metric) {
        return deltas.values().stream().anyMatch(delta -> delta.containsKey(metric));
    }

    /**
     * Returns whether the specified file has a delta for the specified metric.
     *
     * @param path the relative path of the file
     * @param metric the metric to check
     * @return {@code true} if the file has a delta for the metric, {@code false} otherwise
     */
    public boolean hasDelta(final String path, final Metric metric) {
        return deltas.getOrDefault(path, Collections.emptyNavigableMap()).containsKey(metric);
    }

    /**
     * Returns the delta of the specified file for the specified metric. If no delta is available, then a
     * {@link Difference#nullObject(Metric) null object} is returned.
     *
     * @param path the relative path of the file
     * @param metric the metric to get the delta for
     * @return the delta of the file for the metric
     */
    public Difference getDelta(final String path, final Metric metric) {
        return deltas.getOrDefault(path, Collections.emptyNavigableMap())
                .getOrDefault(metric, Difference.nullObject(metric));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return deltas.equals(((FileDeltas) o).deltas);
    }

    @Override
    public int hashCode() {
        return deltas.hashCode();
    }

    @Override
    public String toString() {
        return "file deltas: %s".formatted(deltas);
    }

    /** Builds {@link FileDeltas}. */
    public static class FileDeltasBuilder {
        private final NavigableMap<String, NavigableMap<Metric, Difference>> deltas = new TreeMap<>();

        /**
         * Adds the delta of a metric for the specified file. An existing delta of the same metric is replaced.
         *
         * @param path the relative path of the file
         * @param delta the delta of the file
         * @return this builder
         */
        @CanIgnoreReturnValue
        public FileDeltasBuilder addDelta(final String path, final Difference delta) {
            deltas.computeIfAbsent(path, k -> new TreeMap<>()).put(delta.getMetric(), delta);
            return this;
        }

        /**
         * Adds the deltas for the specified file. Existing deltas of the same metrics are replaced.
         *
         * @param path the relative path of the file
         * @param differences the deltas of the file
         * @return this builder
         */
        @CanIgnoreReturnValue
        public FileDeltasBuilder addDeltas(final String path, final Collection<? extends Difference> differences) {
            differences.forEach(difference -> addDelta(path, difference));
            return this;
        }

        /**
         * Creates the deltas.
         *
         * @return the deltas
         */
        public FileDeltas build() {
            var copy = new TreeMap<String, NavigableMap<Metric, Difference>>();
            deltas.forEach((path, delta) -> copy.put(path, new TreeMap<>(delta)));
            return new FileDeltas(copy);
        }
    }
}
