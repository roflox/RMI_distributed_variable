package tasks;

import nodes.NodeImpl;

public class Set extends Task {
    public Set(int integer, int starter_id,int logicalTime) {
        super(integer,starter_id,"Set",logicalTime);
    }

    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable = integer;
    }
}
