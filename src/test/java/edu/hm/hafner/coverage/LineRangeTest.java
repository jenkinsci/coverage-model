package edu.hm.hafner.coverage;

import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class LineRangeTest {
    @Test
    void shouldCreateLineRangeWithSameStartAndEnd() {
        var lineRange = new LineRange(10);

        assertThat(lineRange.getStart()).isEqualTo(10);
        assertThat(lineRange.getEnd()).isEqualTo(10);
    }

    @Test
    void shouldCreateLineRangeWithDifferentStartAndEnd() {
        var lineRange = new LineRange(10, 20);

        assertThat(lineRange.getStart()).isEqualTo(10);
        assertThat(lineRange.getEnd()).isEqualTo(20);
    }

    @Test
    void shouldThrowExceptionWhenCreatingSmallerThanZeroLineRange() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineRange(0))
                .withMessageContainingAll("A LineRange can only contain positive values!", "0");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineRange(-1))
                .withMessageContainingAll("A LineRange can only contain positive values!", "-1");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineRange(0, 10))
                .withMessageContainingAll("A LineRange can only contain positive values!", "0");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineRange(-1, 10))
                .withMessageContainingAll("A LineRange can only contain positive values!", "-1");
    }

    @Test
    void shouldThrowExceptionWhenCreatingLineRangeWithSmallerEndThanStartValue() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LineRange(10, 5))
                .withMessageContainingAll("5", "cannot be smaller than", "10");
    }

    @Test
    void shouldCreateLineRangeNavigableSetFromSortedLinesSortedSet() {
        var lineRangesA = LineRange.getRangesFromSortedLines(new TreeSet<>(List.of(1, 2, 3, 4)));
        var lineRangesB = LineRange.getRangesFromSortedLines(new TreeSet<>(List.of(1, 2, 4)));

        assertThat(lineRangesA).hasSize(1);
        assertThat(lineRangesA).containsExactly(new LineRange(1, 4));
        assertThat(lineRangesB).hasSize(2);
        assertThat(lineRangesB).containsExactly(new LineRange(1, 2), new LineRange(4));
    }

    @Test
    void shouldCreateLineRangeNavigableSetFromUnsortedLinesSet() {
        var lineRangesA = LineRange.getRangesFromUnsortedLines(new HashSet<>(List.of(1, 3, 4, 2)));
        var lineRangesB = LineRange.getRangesFromUnsortedLines(new HashSet<>(List.of(1, 4, 2)));

        assertThat(lineRangesA).hasSize(1);
        assertThat(lineRangesA).containsExactly(new LineRange(1, 4));
        assertThat(lineRangesB).hasSize(2);
        assertThat(lineRangesB).containsExactly(new LineRange(1, 2), new LineRange(4));
    }

    @Test
    void shouldReturnEmptySetOfLineRangesForNoLines() {
        var lineRangesA = LineRange.getRangesFromSortedLines(new TreeSet<>());
        var lineRangesB = LineRange.getRangesFromUnsortedLines(new HashSet<>());

        assertThat(lineRangesA).isEmpty();
        assertThat(lineRangesB).isEmpty();
    }

    @Test
    void shouldCompareCorrectly() {
        var lineRangeSmaller = new LineRange(1, 4);
        var lineRangeBigger = new LineRange(5, 6);

        assertThat(lineRangeSmaller.compareTo(lineRangeBigger)).isLessThan(0);
        assertThat(lineRangeBigger.compareTo(lineRangeSmaller)).isGreaterThan(0);
    }

    @Test
    void shouldAdhereToEquals() {
        EqualsVerifier.simple().forClass(LineRange.class).verify();
    }
}