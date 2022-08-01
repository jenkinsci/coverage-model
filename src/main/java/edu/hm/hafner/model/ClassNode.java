package edu.hm.hafner.model;

public class ClassNode extends Node {
    private static final long serialVersionUID = 1621410859864978552L;

    /**
     * Creates a new class node with the given name.
     *
     * @param name
     *         the name of the class
     */
    public ClassNode(final String name) {
        super(Metric.CLASS, name);
    }

    @Override // TODO
    public String toString() {
        return "[Class]:";
    }
}
