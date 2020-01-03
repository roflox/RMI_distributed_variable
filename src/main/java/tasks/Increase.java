package tasks;

import nodes.NodeImpl;

public class Increase extends Task {
    public Increase(int integer) {
        super(integer);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable += integer;
    }
}
