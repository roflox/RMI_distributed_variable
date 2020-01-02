package tasks;

import nodes.NodeImpl;

public abstract class Task {

    int integer;

    public Task(int integer) {
        super();
        this.integer = integer;
    }

    public void execute(NodeImpl node) {
    }

}
