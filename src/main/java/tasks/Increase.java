package tasks;

import nodes.NodeImpl;

public class Increase extends Task {
    public Increase(int integer) {
        super(integer);
    }

    @Override
    public void execute(NodeImpl node) {
        System.out.println("new value will be,"+node.variable+integer);
        node.variable += integer;
        System.out.println(node.variable);
    }
}
