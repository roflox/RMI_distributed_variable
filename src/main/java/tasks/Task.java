package tasks;

import nodes.NodeImpl;

import java.io.Serializable;

public abstract class Task implements Serializable {

    int integer;
    protected int starter_id;
    String className;

    public Task(int starter_id, String className) {
        this.starter_id = starter_id;
        this.className = className;
    }

    public Task(int integer, int starter_id, String className) {
        this.integer = integer;
        this.starter_id = starter_id;
        this.className = className;
    }

    public void execute(NodeImpl node) {
    }

    public int getStarter() {
        return starter_id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (!(obj instanceof Task))
            return false;

        Task t = (Task) obj;

        return this.starter_id == t.starter_id && obj.getClass() == this.getClass() && t.integer == this.integer;
    }

    @Override
    public String toString() {
        return className + ", created by " + starter_id;
    }
}
