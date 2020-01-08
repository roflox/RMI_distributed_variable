package nodes;

import javafx.util.Pair;
import tasks.Task;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface Node extends Remote {

    /**
     * @param name jméno objektu uloženého v RMI registrech
     * @param node samotný nodes.Node, který se připojuje do systému
     * @throws RemoteException -
     */
    boolean join(String name, Node node) throws RemoteException;

    /**
     * @param right nový node, který bude označený jako next
     * @throws RemoteException -
     */
    void setRight(Pair<Integer, Node> right) throws RemoteException;

    /**
     * @param left nový node který bude označen jako Previous
     * @throws RemoteException -
     */
    void setLeft(Pair<Integer, Node> left) throws RemoteException;

    /**
     * @return vrací název Nodu v RMI registrech
     * @throws RemoteException -
     */
    String getName() throws RemoteException;

    /**
     * určeno k vypsání všech nodů na které má nodes.Node nějaký ukazatel
     *
     * @throws RemoteException -
     */
    void printInfo() throws RemoteException;

    /**
     * @param leader nový leader
     * @throws RemoteException -
     */
    void setLeader(Pair<Integer, Node> leader) throws RemoteException;

    /**
     * @param node node která bude přidaná do Setu leader nody
     * @throws RemoteException -
     */
    void addNode(Node node) throws RemoteException;

    int getId() throws RemoteException;

    void setId(int id) throws RemoteException;

    void election() throws RemoteException;

    void voteLeader(int smallest) throws RemoteException;

    Node look(String starter, Path where,int logicalTime) throws RemoteException;

    boolean isHealthy() throws RemoteException;

    void repairRing(boolean aliveLeader) throws RemoteException;

    void disconnect() throws RemoteException;

    void ping() throws RemoteException;

    Map<Integer, Node> getNodes(int starter_id) throws RemoteException;

    void executeTask(Task task) throws RemoteException;

    boolean isAvailable() throws RemoteException;

    boolean isExecutable(Task task) throws RemoteException;

    boolean isLeader() throws RemoteException;

    void gatherNodes() throws RemoteException;

    boolean addTaskToQueue(Task t) throws RemoteException;

    Pair<Integer, Node> getLeader() throws RemoteException;

    Pair<Integer, Node> getLeft() throws RemoteException;

    Pair<Integer, Node> getRight() throws RemoteException;
}
