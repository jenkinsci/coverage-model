package edu.hm.hafner.coverage;

import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.LineRange;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class LineRangeTest {
    @Test
    void shouldCreateLineRangeNavigableSetFromSortedLinesSortedSet() {
        var fileNode = new FileNode("file.txt", ".");
        var lineRangesA = fileNode.getRangesFromSortedLines(new TreeSet<>(List.of(1, 2, 3, 4)));
        var lineRangesB = fileNode.getRangesFromSortedLines(new TreeSet<>(List.of(1, 2, 4)));

        assertThat(lineRangesA.size()).isEqualTo(1);
        assertThat(lineRangesA).containsExactly(new LineRange(1, 4));
        assertThat(lineRangesB).hasSize(2);
        assertThat(lineRangesB).containsExactly(new LineRange(1, 2), new LineRange(4));
    }

    @Test
    void shouldReturnEmptySetOfLineRangesForNoLines() {
        var fileNode = new FileNode("file.txt", ".");
        var lineRangesA = fileNode.getRangesFromSortedLines(new TreeSet<>());

        assertThat(lineRangesA).isEmpty();
    }
}
