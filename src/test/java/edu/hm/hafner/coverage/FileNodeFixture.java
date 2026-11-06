package edu.hm.hafner.coverage;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Creates {@link FileNode} instances with coverage counters and mutations for tests. The methods to add counters and
 * mutations to a {@link FileNode} are not part of the public API, they are available for parsers only (see
 * {@link CoverageParser}). This fixture provides access to these methods in tests of the model and of its clients.
 *
 * @author Ullrich Hafner
 */
public final class FileNodeFixture {
    private final FileNode file;

    /**
     * Creates a fixture for a new file with the specified name and path.
     *
     * @param name the name of the file
     * @param relativePath the relative path of the file
     */
    public FileNodeFixture(final String name, final String relativePath) {
        this(new FileNode(name, relativePath));
    }

    /**
     * Creates a fixture for the specified existing file. All counters and mutations will be added to this file.
     *
     * @param file the file to modify
     */
    public FileNodeFixture(final FileNode file) {
        this.file = file;
    }

    /**
     * Adds the line and branch coverage counters for the specified line.
     *
     * @param line the line number to add the counters for
     * @param covered the number of covered items
     * @param missed the number of missed items
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public FileNodeFixture withCounters(final int line, final int covered, final int missed) {
        file.addCounters(line, covered, missed);

        return this;
    }

    /**
     * Adds the MC/DC pair coverage counters for the specified line.
     *
     * @param line the line number to add the counters for
     * @param covered the number of covered items
     * @param missed the number of missed items
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public FileNodeFixture withMcdcPairCounters(final int line, final int covered, final int missed) {
        file.addMcdcPairCounters(line, covered, missed);

        return this;
    }

    /**
     * Adds the function call coverage counters for the specified line.
     *
     * @param line the line number to add the counters for
     * @param covered the number of covered items
     * @param missed the number of missed items
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public FileNodeFixture withFunctionCallCounters(final int line, final int covered, final int missed) {
        file.addFunctionCallCounters(line, covered, missed);

        return this;
    }

    /**
     * Adds the specified mutation.
     *
     * @param mutation the mutation to add
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public FileNodeFixture withMutation(final Mutation mutation) {
        file.addMutation(mutation);

        return this;
    }

    /**
     * Derives the line coverage from the mutations that have been added so far.
     *
     * @return this fixture
     */
    @CanIgnoreReturnValue
    public FileNodeFixture withLineCoverageOfMutations() {
        file.addLineCoverageOfMutations();

        return this;
    }

    /**
     * Returns the file with all counters and mutations that have been added.
     *
     * @return the file
     */
    public FileNode build() {
        return file;
    }
}
