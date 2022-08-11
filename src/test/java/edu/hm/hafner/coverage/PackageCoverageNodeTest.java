package edu.hm.hafner.coverage;

import org.junit.jupiter.api.Test;

import static edu.hm.hafner.coverage.assertions.Assertions.*;

class PackageCoverageNodeTest {

    @Test
    void shouldCreatePackageCoverageNode() {
        assertThat(new PackageCoverageNode("edu.hm.hafner"))
                .hasMetric(CoverageMetric.PACKAGE)
                .hasName("edu.hm.hafner")
                .hasPath("edu/hm/hafner");
    }

    @Test
    void shouldCreateEmptyCopy() {
        PackageCoverageNode node = new PackageCoverageNode("edu");
        PackageCoverageNode subPackage = new PackageCoverageNode("hm");
        node.add(subPackage);

        assertThat(node.getChildren().size()).isNotZero();
        assertThat(node.copyEmpty().getChildren().size()).isZero();
    }

}