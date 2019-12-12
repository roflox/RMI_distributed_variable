import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Client {
    public static void main(String[] args) {
        try {
            Node node1 = (Node) LocateRegistry.getRegistry(1099).lookup("Node1");
            Node node2 = (Node) LocateRegistry.getRegistry(1100).lookup("Node2");
            Node node3 = (Node) LocateRegistry.getRegistry(1101).lookup("Node3");
//            Node node4 = (Node) LocateRegistry.getRegistry(1102).lookup("Node4");
//            Node node5 = (Node) LocateRegistry.getRegistry(1103).lookup("Node5");
            node1.printInfo();
            node1.lookRight();
//            node2.printInfo();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
