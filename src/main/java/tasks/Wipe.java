package tasks;

import nodes.NodeImpl;

public class Wipe extends Task {
    public Wipe(int starter_id, int logicalTime) {
        super(starter_id, "Wipe", logicalTime);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = 0;
    }
}
