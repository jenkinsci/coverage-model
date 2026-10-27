package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

import edu.hm.hafner.coverage.Coverage.CoverageBuilder;
import edu.hm.hafner.coverage.FileDeltas.FileDeltasBuilder;
import edu.hm.hafner.util.SerializableTest;
import java.util.List;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

/**
 * Tests the class {@link FileDeltas}.
 *
 * @author Ullrich Hafner
 */
class FileDeltasTest extends SerializableTest<FileDeltas> {
    private static final String FILE = "path/to/File.java";
    private static final String OTHER_FILE = "path/to/Other.java";

    @Override
    protected FileDeltas createSerializable() {
        return createDeltas();
    }

    @Test
    void shouldBeEmpty() {
        var deltas = FileDeltas.empty();

        assertThat(deltas.hasDeltas()).isFalse();
        assertThat(deltas.hasDelta(Metric.LINE)).isFalse();
        assertThat(deltas.hasDelta(FILE, Metric.LINE)).isFalse();
        assertThat(deltas.getDelta(FILE, Metric.LINE)).isEqualTo(Difference.nullObject(Metric.LINE));
        assertThat(new FileDeltasBuilder().build()).isEqualTo(deltas);
    }

    @Test
    void shouldStoreDeltasPerFile() {
        var deltas = createDeltas();

        assertThat(deltas.hasDeltas()).isTrue();
        assertThat(deltas.hasDelta(Metric.LINE)).isTrue();
        assertThat(deltas.hasDelta(Metric.BRANCH)).isTrue();
        assertThat(deltas.hasDelta(Metric.MUTATION)).isFalse();
        assertThat(deltas.hasDelta(FILE, Metric.LINE)).isTrue();
        assertThat(deltas.hasDelta(FILE, Metric.BRANCH)).isFalse();
        assertThat(deltas.hasDelta(OTHER_FILE, Metric.BRANCH)).isTrue();
        assertThat(deltas.getDelta(FILE, Metric.LINE)).isEqualTo(new Difference(Metric.LINE, 50));
        assertThat(deltas.getDelta(FILE, Metric.BRANCH)).isEqualTo(Difference.nullObject(Metric.BRANCH));
    }

    @Test
    void shouldComputeDeltasOfFiles() {
        var builder = new CoverageBuilder();

        var reference = new FileNode("File.java", FILE);
        reference.addValue(
                builder.withMetric(Metric.LINE).withCovered(10).withMissed(10).build());

        var file = new FileNode("File.java", FILE);
        file.addValue(
                builder.withMetric(Metric.LINE).withCovered(20).withMissed(0).build());
        file.addValue(
                builder.withMetric(Metric.BRANCH).withCovered(10).withMissed(5).build());

        var deltas = new FileDeltasBuilder()
                .addDeltas(file.getRelativePath(), file.computeDelta(reference))
                .build();

        assertThat(deltas.hasDelta(FILE, Metric.LINE)).isTrue();
        assertThat(deltas.hasDelta(FILE, Metric.BRANCH)).isFalse();
        assertThat(deltas.getDelta(FILE, Metric.LINE).asDouble()).isEqualTo(50);
    }

    @Test
    void shouldReplaceDeltaOfSameMetric() {
        var deltas = new FileDeltasBuilder()
                .addDelta(FILE, new Difference(Metric.LINE, 10))
                .addDelta(FILE, new Difference(Metric.LINE, 20))
                .build();

        assertThat(deltas.getDelta(FILE, Metric.LINE)).isEqualTo(new Difference(Metric.LINE, 20));
    }

    @Test
    void shouldNotModifyDeltasWhenBuilderChanges() {
        var builder = new FileDeltasBuilder().addDelta(FILE, new Difference(Metric.LINE, 10));
        var deltas = builder.build();

        builder.addDelta(FILE, new Difference(Metric.BRANCH, 5));

        assertThat(deltas.hasDelta(FILE, Metric.BRANCH)).isFalse();
    }

    @Test
    void shouldReadDeltasOfLegacyTree() {
        var module = new ModuleNode("module");
        module.addChild(new FileNode("File.java", FILE, Map.of(Metric.LINE, new Difference(Metric.LINE, 50))));
        module.addChild(new FileNode(
                "Other.java",
                OTHER_FILE,
                Map.of(
                        Metric.BRANCH, new Difference(Metric.BRANCH, 25),
                        Metric.LOC, new Value(Metric.LOC, 10))));
        module.addChild(new FileNode("Unchanged.java", "path/to/Unchanged.java"));

        assertThat(FileDeltas.fromLegacyTree(module)).isEqualTo(createDeltas());
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.forClass(FileDeltas.class).verify();
    }

    private FileDeltas createDeltas() {
        return new FileDeltasBuilder()
                .addDeltas(FILE, List.of(new Difference(Metric.LINE, 50)))
                .addDelta(OTHER_FILE, new Difference(Metric.BRANCH, 25))
                .build();
    }
}
