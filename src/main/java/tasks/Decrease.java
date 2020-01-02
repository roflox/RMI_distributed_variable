package tasks;

import nodes.NodeImpl;

public class Decrease extends Task {
    public Decrease(int integer) {
        super(integer);
    }

    @Override
    public void execute(NodeImpl node) {
        node.variable -= integer;
    }
}
