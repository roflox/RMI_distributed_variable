package tasks;

import nodes.NodeImpl;

import java.io.Serializable;

public abstract class Task implements Serializable {

    int integer;
    protected int starter_id;

    public Task(int starter_id) {
        this.starter_id = starter_id;
    }

    public Task(int integer,int starter_id) {
        this.integer = integer;
        this.starter_id = starter_id;
    }

    public void execute(NodeImpl node) {
    }

    public int getStarter(){
        return starter_id;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj==this)
            return true;

        if(!(obj instanceof Task))
            return false;

        Task t = (Task) obj;

        return this.starter_id==t.starter_id && obj.getClass()==this.getClass() && t.integer==this.integer;
    }

    @Override
    public String toString(){
        return this.getClass() + ", created by "+ starter_id;
    }
}
