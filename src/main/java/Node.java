import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;

public interface Node extends Remote {

    /**
     * @param name jméno objektu uloženého v RMI registrech
     * @param nodeImpl samotný Node, který se připojuje do systému
     * @throws RemoteException -
     */
    void join(String name, Node nodeImpl) throws RemoteException;

    /**
     * @param next nový node, který bude označený jako next
     * @throws RemoteException -
     */
    void setRight(Node next) throws RemoteException;

    /**
     * @param prev nový node který bude označen jako Previous
     * @throws RemoteException -
     */
    void setLeft(Node prev) throws RemoteException;

    Node getLeft() throws RemoteException;

    Node getNext() throws RemoteException;

    Node getLeader() throws RemoteException;

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
    void printInfo() throws RemoteException;

    /**
     * @param leader nový leader
     * @throws RemoteException -
     */
    void setLeader(Node leader) throws RemoteException;

    /**
     * @param nodeImpl node která bude přidaná do Setu leader nody
     * @throws RemoteException -
     */
    void joinSet(Node nodeImpl) throws RemoteException;

    int getId() throws RemoteException;

    void setId(int id) throws RemoteException;

    void election() throws RemoteException;

    void elect() throws RemoteException;

    Node look(String starter, Path where) throws RemoteException;


    boolean isHealthy() throws RemoteException;

    void repairRing() throws RemoteException;

    void disconnect() throws RemoteException;

    void ping() throws RemoteException;
}
