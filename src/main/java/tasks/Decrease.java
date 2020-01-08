package tasks;

import nodes.NodeImpl;

public class Decrease extends Task {
    public Decrease(int integer, int starter_id, int logicalTime) {
        super(integer, starter_id, "Decrease", logicalTime);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable -= integer;
    }
}
