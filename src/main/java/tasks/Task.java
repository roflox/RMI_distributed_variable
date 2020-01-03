package tasks;

import nodes.NodeImpl;

import java.io.Serializable;

public abstract class Task implements Serializable {

    int integer;

    public Task() {
    }

    public Task(int integer) {
        this.integer = integer;
    }

    public void execute(NodeImpl node) {
    }

}
