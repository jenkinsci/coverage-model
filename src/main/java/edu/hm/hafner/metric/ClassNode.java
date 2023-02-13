package edu.hm.hafner.metric;

/**
 * A {@link Node} for a specific class.
 */
public final class ClassNode extends Node {
    private static final long serialVersionUID = 1621410859864978552L;

    /**
     * Creates a new {@link ClassNode} with the given name.
     *
     * @param name
     *         the name of the class
     */
    public ClassNode(final String name) {
        super(Metric.CLASS, name);
    }

    @Override
    public ClassNode copy() {
        return new ClassNode(getName());
    }

    /**
     * Create a new method node with the given method name and signature and add it to the list of children.
     *
     * @param methodName
     *         the method name
     * @param signature
     *         the signature of the method
     *
     * @return the created and linked package node
     */
    public MethodNode createMethodNode(final String methodName, final String signature) {
        var fileNode = new MethodNode(methodName, signature);
        addChild(fileNode);
        return fileNode;
    }
}
