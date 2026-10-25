package edu.hm.hafner.coverage;

import static edu.hm.hafner.coverage.assertions.Assertions.assertThat;

import edu.hm.hafner.util.SerializableTest;
import org.junit.jupiter.api.Test;

/**
 * Tests the class {@link LineCounters}.
 *
 * @author Ullrich Hafner
 */
class LineCountersTest extends SerializableTest<LineCounters> {
    @Override
    protected LineCounters createSerializable() {
        return createCounters();
    }

    @Test
    void shouldBeEmptyInitially() {
        var counters = new LineCounters();

        assertThat(counters).hasNoLines();
        assertThat(counters.contains(1)).isFalse();
        assertThat(counters.getCovered(1)).isZero();
        assertThat(counters.getMissed(1)).isZero();
        assertThat(counters).hasCoveredCounters();
        assertThat(counters.getMissedCounters()).isEmpty();
    }

    @Test
    void shouldStoreCountersPerLine() {
        var counters = createCounters();

        assertThat(counters).hasOnlyLines(3, 10);
        assertThat(counters.contains(3)).isTrue();
        assertThat(counters.contains(4)).isFalse();
        assertThat(counters.getCovered(3)).isEqualTo(2);
        assertThat(counters.getMissed(3)).isEqualTo(1);
        assertThat(counters.getCovered(10)).isEqualTo(5);
        assertThat(counters.getMissed(10)).isZero();
        assertThat(counters.getCoveredCounters()).containsExactly(2, 5);
        assertThat(counters.getMissedCounters()).containsExactly(1, 0);
        assertThat(counters.getCoveredPerLine()).containsOnlyKeys(3, 10).containsEntry(3, 2);
    }

    @Test
    void shouldOverwriteCountersOfLine() {
        var counters = createCounters();

        counters.put(3, 7, 8);

        assertThat(counters.getCovered(3)).isEqualTo(7);
        assertThat(counters.getMissed(3)).isEqualTo(8);
    }

    @Test
    void shouldCopyCountersIndependently() {
        var counters = createCounters();

        var copy = counters.copy();
        assertThat(copy).isEqualTo(counters).isNotSameAs(counters);

        copy.put(20, 1, 1);
        assertThat(copy).isNotEqualTo(counters);
        assertThat(counters.contains(20)).isFalse();
    }

    @Test
    void shouldReturnCopyOfLines() {
        var counters = createCounters();

        counters.getLines().clear();

        assertThat(counters).hasOnlyLines(3, 10);
    }

    private LineCounters createCounters() {
        var counters = new LineCounters();
        counters.put(3, 2, 1);
        counters.put(10, 5, 0);
        return counters;
    }
}
