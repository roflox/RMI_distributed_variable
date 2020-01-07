package tasks;

import nodes.NodeImpl;

public class Increase extends Task {
    public Increase(int integer, int starter_id) {
        super(integer,starter_id,"Increase");
    }



    @Override
    public void execute(NodeImpl node) {
        super.execute(node);
        node.variable += integer;
    }
    @Override
    public String toString(){
        return this.getClass() + ", created by "+ starter_id;
    }
}
