package edu.hm.hafner.metric;

import org.junit.jupiter.api.Test;

import edu.hm.hafner.metric.Coverage.CoverageBuilder;

import static edu.hm.hafner.metric.assertions.Assertions.*;

class FileNodeTest extends AbstractNodeTest {
    @Override
    Metric getMetric() {
        return Metric.FILE;
    }

    @Override
    Node createNode(final String name) {
        var fileNode = new FileNode(name);
        var builder = new CoverageBuilder();
        fileNode.addLineCoverage(10, builder.setMetric(Metric.LINE).setCovered(1).setMissed(2).build());
        fileNode.addBranchCoverage(11, builder.setMetric(Metric.BRANCH).setCovered(2).setMissed(2).build());
        fileNode.addInstructionCoverage(12, builder.setMetric(Metric.INSTRUCTION).setCovered(20).setMissed(0).build());
        fileNode.addChangedCodeLine(10);
        fileNode.addIndirectCoverageChange(15, 123);
        var empty = new FileNode("empty");
        fileNode.computeDelta(empty);
        return fileNode;
    }

    @Test
    void shouldGetFilePath() {
        ModuleNode module = new ModuleNode("top-level"); // just for testing
        PackageNode folder = new PackageNode("folder"); // just for testing
        FileNode file = new FileNode("Coverage.java");

        folder.addChild(file);
        module.addChild(folder);

        var absolutePath = "folder/Coverage.java";

        assertThat(file.getPath()).isEqualTo(absolutePath);
        assertThat(file.getFiles()).containsExactly(absolutePath);
        assertThat(folder.getFiles()).containsExactly(absolutePath);
        assertThat(module.getFiles()).containsExactly(absolutePath);

        assertThat(module.getAll(Metric.FILE)).containsExactly(file);
        assertThat(folder.getAll(Metric.FILE)).containsExactly(file);
        assertThat(file.getAll(Metric.FILE)).containsExactly(file);
    }
}
