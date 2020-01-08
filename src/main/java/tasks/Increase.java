package tasks;

import nodes.NodeImpl;

public class Increase extends Task {
    public Increase(int integer, int starter_id, int logicalTime) {
        super(integer, starter_id, "Increase", logicalTime);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable += integer;
    }
}
