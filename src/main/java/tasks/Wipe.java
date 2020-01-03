package tasks;

import nodes.NodeImpl;

public class Wipe extends Task {
    public Wipe() {
        super();
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = 0;
    }
}
