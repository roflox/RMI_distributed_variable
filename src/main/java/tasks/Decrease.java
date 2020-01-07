package tasks;

import nodes.NodeImpl;

public class Decrease extends Task {
    public Decrease(int integer, int starter_id) {
        super(integer,starter_id, "Decrease");
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable -= integer;
    }
}
