import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {

    //    /**
//     * @param targetNode jméno nodu ke kterému se má připojit
//     */
//    void connectToNode(String targetNode);
//
    void join(String name, NodeInterface node) throws RemoteException;

    void changeNext(NodeInterface next) throws RemoteException;

    void changePrev(NodeInterface prev) throws RemoteException;


//
//    void registerClient(String name, NodeInterface node);
//
//    void detectNext(NodeInterface dead) throws RemoteException;;
//
//    void detectPrevious(NodeInterface dead) throws RemoteException;;
//
//    void msg(NodeInterface from, NodeInterface to, String message);
//
//    void startElection();
//
//    void electLeader();

}
