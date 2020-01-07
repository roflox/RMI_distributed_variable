package tasks;

import nodes.NodeImpl;

public class Wipe extends Task {
    public Wipe(int starter_id) {
        super(starter_id,"Wipe");
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = 0;
    }
}
