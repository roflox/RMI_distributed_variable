package tasks;

import nodes.NodeImpl;

public class Set extends Task {
    public Set(int integer) {
        super(integer);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = integer;
    }
}
