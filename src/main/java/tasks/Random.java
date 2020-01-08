package tasks;

import nodes.NodeImpl;

public class Random extends Task {
    public Random(int starter_id, int logicalTime) {
        super(starter_id, "Random", logicalTime);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = new java.util.Random().nextInt();
    }
}
