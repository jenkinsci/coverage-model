package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

import edu.hm.hafner.coverage.ChangeSet.ChangeSetBuilder;
import edu.hm.hafner.util.SerializableTest;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Tests the class {@link ChangeSet}.
 *
 * @author Ullrich Hafner
 */
class ChangeSetTest extends SerializableTest<ChangeSet> {
    private static final String FILE = "path/to/File.java";
    private static final String OTHER_FILE = "path/to/Other.java";

    @Override
    protected ChangeSet createSerializable() {
        return createChangeSet();
    }

    @Test
    void shouldBeEmpty() {
        var changes = ChangeSet.empty();

        assertThat(changes).doesNotHaveModifiedLines();
        assertThat(changes.hasModifiedLines(FILE)).isFalse();
        assertThat(changes.getModifiedLines(FILE)).isEmpty();
        assertThat(changes.isModified(FILE, 1)).isFalse();
        assertThat(changes).doesNotHaveIndirectCoverageChanges();
        assertThat(changes.hasIndirectCoverageChanges(FILE)).isFalse();
        assertThat(changes.getIndirectCoverageChanges(FILE)).isEmpty();
        assertThat(new ChangeSetBuilder().build()).isEqualTo(changes);
    }

    @Test
    void shouldStoreModifiedLinesPerFile() {
        var changes = createChangeSet();

        assertThat(changes).hasModifiedLines();
        assertThat(changes.hasModifiedLines(FILE)).isTrue();
        assertThat(changes.hasModifiedLines(OTHER_FILE)).isFalse();
        assertThat(changes.getModifiedLines(FILE)).containsExactly(3, 4, 10);
        assertThat(changes.getModifiedLines(OTHER_FILE)).isEmpty();
        assertThat(changes.isModified(FILE, 4)).isTrue();
        assertThat(changes.isModified(FILE, 5)).isFalse();
        assertThat(changes.isModified(OTHER_FILE, 4)).isFalse();
    }

    @Test
    void shouldStoreIndirectCoverageChangesPerFile() {
        var changes = createChangeSet();

        assertThat(changes).hasIndirectCoverageChanges();
        assertThat(changes.hasIndirectCoverageChanges(OTHER_FILE)).isTrue();
        assertThat(changes.hasIndirectCoverageChanges(FILE)).isFalse();
        assertThat(changes.getIndirectCoverageChanges(OTHER_FILE))
                .containsOnlyKeys(20, 21)
                .containsEntry(21, -1);
        assertThat(changes.getIndirectCoverageChanges(FILE)).isEmpty();
    }

    @Test
    void shouldIgnoreEmptyLines() {
        var changes = new ChangeSetBuilder()
                .addModifiedLines(FILE)
                .addModifiedLines(OTHER_FILE, List.of())
                .build();

        assertThat(changes).doesNotHaveModifiedLines();
        assertThat(changes).isEqualTo(ChangeSet.empty());
    }

    @Test
    void shouldNotBeAffectedByLaterChangesOfBuilder() {
        var builder = new ChangeSetBuilder().addModifiedLines(FILE, 1);
        var changes = builder.build();

        builder.addModifiedLines(FILE, 2);

        assertThat(changes.getModifiedLines(FILE)).containsExactly(1);
    }

    @Test
    void shouldReturnUnmodifiableCollections() {
        var changes = createChangeSet();

        var lines = changes.getModifiedLines(FILE);
        var indirectChanges = changes.getIndirectCoverageChanges(OTHER_FILE);

        assertThatThrownBy(() -> lines.add(1)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> indirectChanges.put(1, 1)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @SuppressWarnings("deprecation")
    void shouldReadChangesOfLegacyTree() {
        var module = new ModuleNode("module");
        var file = new FileNode("File.java", FILE);
        file.addModifiedLines(3, 4, 10);
        var other = new FileNode("Other.java", OTHER_FILE);
        other.addIndirectCoverageChange(20, 1);
        other.addIndirectCoverageChange(21, -1);
        var unchanged = new FileNode("Unchanged.java", "path/to/Unchanged.java");
        module.addChild(file);
        module.addChild(other);
        module.addChild(unchanged);

        assertThat(ChangeSet.fromLegacyTree(module)).isEqualTo(createChangeSet());
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(ChangeSet.class).verify();
    }

    private ChangeSet createChangeSet() {
        return new ChangeSetBuilder()
                .addModifiedLines(FILE, 10, 3)
                .addModifiedLines(FILE, List.of(4))
                .addIndirectCoverageChange(OTHER_FILE, 20, 1)
                .addIndirectCoverageChange(OTHER_FILE, 21, -1)
                .build();
    }
}
