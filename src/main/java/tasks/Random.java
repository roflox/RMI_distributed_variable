package tasks;

import nodes.NodeImpl;

public class Random extends Task {
    public Random() {
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = new java.util.Random().nextInt();
    }
}
