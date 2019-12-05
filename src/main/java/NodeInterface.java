import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeInterface extends Remote {

    /**
     * @param name jméno objektu uloženého v RMI registrech
     * @param node samotný Node, který se připojuje do systému
     * @throws RemoteException -
     */
    void join(String name, NodeInterface node) throws RemoteException;

    /**
     * @param next nový node, který bude označený jako next
     * @throws RemoteException -
     */
    void changeNext(NodeInterface next) throws RemoteException;

    /**
     * @param prev nový node který bude označen jako Previous
     * @throws RemoteException -
     */
    void changePrev(NodeInterface prev) throws RemoteException;

    NodeInterface getPrev() throws RemoteException;

    NodeInterface getNext() throws RemoteException;

    NodeInterface getLeader() throws RemoteException;

    /**
     * @return vrací název Nodu v RMI registrech
     * @throws RemoteException -
     */
    String getName() throws RemoteException;

    /**
     * určeno k vypsání všech nodů na které má Node nějaký ukazatel
     *
     * @throws RemoteException -
     */
    void printNeighbors() throws RemoteException;

    /**
     * @param leader nový leader
     * @throws RemoteException -
     */
    void changeLeader(NodeInterface leader) throws RemoteException;

    /**
     * @param node node která bude přidaná do Setu leader nody
     * @throws RemoteException -
     */
    void joinSet(NodeInterface node) throws RemoteException;

    int getId() throws RemoteException;

    void setId(int id) throws RemoteException;
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
