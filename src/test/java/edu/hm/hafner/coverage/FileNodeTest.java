package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.TreeString;

import nl.jqno.equalsverifier.Warning;
import nl.jqno.equalsverifier.api.EqualsVerifierApi;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class FileNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.FILE;
    }

    @Override
    void configureEqualsVerifier(final EqualsVerifierApi<? extends Node> verifier) {
        verifier.withPrefabValues(TreeString.class, TreeString.valueOf("src"), TreeString.valueOf("test"))
                .suppress(Warning.NONFINAL_FIELDS);

    }

    @Override
    FileNode createNode(final String name) {
        var fileNode = new FileNode(name, "path");
        fileNode.addCounters(10, 1, 0);
        fileNode.addCounters(11, 2, 2);
        fileNode.addModifiedLines(10);
        fileNode.addIndirectCoverageChange(15, 123);
        var empty = new FileNode("empty", "path");
        fileNode.computeDelta(empty);
        return fileNode;
    }

    @Test
    void shouldGetFilePath() {
        var module = new ModuleNode("top-level"); // just for testing
        var folder = new PackageNode("folder"); // just for testing
        var fileName = "Coverage.java";
        var relativePath = "relative/path/to/file";
        var file = new FileNode(fileName, relativePath);
        folder.addChild(file);
        module.addChild(folder);

        assertThat(file.getRelativePath()).isEqualTo(relativePath);
        var otherPath = "other";

        assertThat(file.getFiles()).containsExactly(relativePath);
        assertThat(folder.getFiles()).containsExactly(relativePath);
        assertThat(module.getFiles()).containsExactly(relativePath);

        assertThat(module.getAll(Metric.FILE)).containsExactly(file);
        assertThat(folder.getAll(Metric.FILE)).containsExactly(file);
        assertThat(file.getAll(Metric.FILE)).containsExactly(file);

        file.setRelativePath(TreeString.valueOf(otherPath));
        assertThat(file.getRelativePath()).isEqualTo(otherPath);

        assertThat(file.matches(Metric.FILE, fileName)).isTrue();
        assertThat(file.matches(Metric.FILE, otherPath)).isTrue();
        assertThat(file.matches(Metric.FILE, "wrong")).isFalse();

        assertThat(file.matches(Metric.FILE, fileName.hashCode())).isTrue();
        assertThat(file.matches(Metric.FILE, otherPath.hashCode())).isTrue();
        assertThat(file.matches(Metric.FILE, "wrong".hashCode())).isFalse();
    }
}
