package edu.hm.hafner.mutation;

import edu.hm.hafner.coverage.Coverage;
import edu.hm.hafner.model.Metric;
import edu.hm.hafner.model.Node;

/**
 * Represents a Node with additional methods related to mutations.
 *
 * @author Melissa Bauer
 */
public class MutationNode extends Node {
    private static final long serialVersionUID = -8675077280969902046L;

    /**
     * Creates a new coverage item node with the given name.
     *
     * @param metric
     *         the coverage metric this node belongs to
     * @param name
     *         the name of the node
     */
    public MutationNode(final Metric metric, final String name) {
        super(metric, name);
    }

    // TODO Information in html datei vorhanden, notwendig?
    public int getLineCoverage() {
        return 0;
    }

    // TODO
    public Coverage getMutationCoverage() {
        return Coverage.NO_COVERAGE;
    }

    // TODO Information in html datei vorhanden, notwendig?
    public int getTestStrength() {
        return 0;
    }
}
