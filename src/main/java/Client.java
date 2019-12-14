import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class Client {
    public static void main(String[] args) {
        try {
            Node node1 = (Node) LocateRegistry.getRegistry(1099).lookup("Node1");
            Node node2 = (Node) LocateRegistry.getRegistry(1100).lookup("Node2");
//            Node node3 = (Node) LocateRegistry.getRegistry(1101).lookup("Node3");
            Node node4 = (Node) LocateRegistry.getRegistry(1102).lookup("Node4");
            Node node5 = (Node) LocateRegistry.getRegistry(1103).lookup("Node5");
//            node1.printInfo();
//            System.out.println(node1.isHealthy());
//            System.out.println(node3.lookLeft(null).getName());
//            System.out.println(node1.lookRight(null).getName());
//            if (!node1.isHealthy()) {
//                node1.repairRing();
//            } else {
//                System.out.print(node1.getName() + " is healthy");
//            }
//            node4.printInfo();
//            node4.repairRing();
//            node4.printInfo();
            node1.printInfo();
            node2.printInfo();
            node4.printInfo();
            node5.printInfo();
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }
    }
}
