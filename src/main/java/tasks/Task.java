package tasks;

import nodes.NodeImpl;

import java.io.Serializable;

public abstract class Task implements Serializable {

    int integer;
    protected int starter_id;
    String className;
    int logicalTime;
    boolean executed = false;

    public Task(int starter_id, String className, int logicalTime) {
        this.starter_id = starter_id;
        this.className = className;
        this.logicalTime = logicalTime;
    }

    public boolean wasExecuted(){
        return executed;
    }

    public Task(int integer, int starter_id, String className, int logicalTime) {
        this.integer = integer;
        this.starter_id = starter_id;
        this.className = className;
        this.logicalTime = logicalTime;
    }

    public void setLogicalTime(int i) {
        this.logicalTime = i;
    }

    public void markAsExecuted(){

    }

    public void execute(NodeImpl node) {
        if (node.logicalTime < logicalTime)
            node.logicalTime = logicalTime + 1;
        else {
            node.logicalTime++;
        }
    }

    public int getStarter() {
        return starter_id;
    }

    public int getLogicalTime(){
        return logicalTime;
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
        return className + ", created by " + starter_id + ", logicalTime: "+logicalTime;
    }
}
