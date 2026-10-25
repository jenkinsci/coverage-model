package edu.hm.hafner.coverage;

import static org.assertj.core.api.BDDAssertions.*;

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

        then(counters.getLines()).isEmpty();
        then(counters.contains(1)).isFalse();
        then(counters.getCovered(1)).isZero();
        then(counters.getMissed(1)).isZero();
        then(counters.getCoveredCounters()).isEmpty();
        then(counters.getMissedCounters()).isEmpty();
    }

    @Test
    void shouldStoreCountersPerLine() {
        var counters = createCounters();

        then(counters.getLines()).containsExactly(3, 10);
        then(counters.contains(3)).isTrue();
        then(counters.contains(4)).isFalse();
        then(counters.getCovered(3)).isEqualTo(2);
        then(counters.getMissed(3)).isEqualTo(1);
        then(counters.getCovered(10)).isEqualTo(5);
        then(counters.getMissed(10)).isZero();
        then(counters.getCoveredCounters()).containsExactly(2, 5);
        then(counters.getMissedCounters()).containsExactly(1, 0);
        then(counters.getCoveredPerLine()).containsOnlyKeys(3, 10).containsEntry(3, 2);
    }

    @Test
    void shouldOverwriteCountersOfLine() {
        var counters = createCounters();

        counters.put(3, 7, 8);

        then(counters.getCovered(3)).isEqualTo(7);
        then(counters.getMissed(3)).isEqualTo(8);
    }

    @Test
    void shouldCopyCountersIndependently() {
        var counters = createCounters();

        var copy = counters.copy();
        then(copy).isEqualTo(counters).isNotSameAs(counters);

        copy.put(20, 1, 1);
        then(copy).isNotEqualTo(counters);
        then(counters.contains(20)).isFalse();
    }

    @Test
    void shouldReturnCopyOfLines() {
        var counters = createCounters();

        counters.getLines().clear();

        then(counters.getLines()).hasSize(2);
    }

    private LineCounters createCounters() {
        var counters = new LineCounters();
        counters.put(3, 2, 1);
        counters.put(10, 5, 0);
        return counters;
    }
}
