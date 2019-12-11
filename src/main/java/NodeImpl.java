import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class NodeImpl implements Node {

    private static boolean debug = false;
    Node nextNode;
    Node prevNode;
    Node leaderNode;
    Set<Node> allNodes = new HashSet<>();
    String name;
    int id;

    public NodeImpl(String name, Registry registry, int objectPort) {
        this.name = name;
        bindNode(objectPort, registry);
    }


    /**
     * @param args nastaveno jako konzolová aplikace, která přijme options
     *             -t --target cílový node, ke kterému se má nově spuštěný node připojit
     *             -n --name jméno nově vzniklého nodu
     *             -rp --registryPort port kde běží RMI registry
     *             -rh --registryHost adresa kde běží RMI registry
     *             -p --port port kde bude vystavený Proxy objekt
     *             -d --debug spuštění debug módu
     */
    public static void main(String[] args) {
        Map<String, Object> arguments = ConsoleArgumentParser.parse(args);
        // getting arguments from map
        String targetRegistryAddress = (String) arguments.get("targetRegistryAddress");
        int targetRegistryPort = (Integer) arguments.get("targetRegistryPort");
        String target = (String) arguments.get("target");
        int port = (Integer) arguments.get("port");
        int registryPort = (Integer) arguments.get("registryPort");
        String nodeName = (String) arguments.get("nodeName");
        debug = (boolean) arguments.get("debug");
        boolean development = (boolean) arguments.get("development");

        // Logger

//        LOG.setLevel(Level.INFO);
//        LOG.log(Level.ALL,String.format("Using RMI Registry host: %s:%s", registryHost, registryPort));
//        LOG.debug(String.format("Using RMI Registry host: %s:%s", registryHost, registryPort));
//        logger.info(String.format("Using RMI Registry host: %s:%s", registryHost, registryPort));

        if (development) {
            for (String key : arguments.keySet()) {
                System.out.println(String.format("%s:%s", key, arguments.get(key)));
            }
        }
        // create own registry
        Registry registry = null;
        try {
            registry = LocateRegistry.createRegistry(registryPort);
        } catch (RemoteException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        NodeImpl nodeImpl = new NodeImpl(nodeName, registry, port);
        System.out.println(String.format("Node is using name: %s, port: %d,registry port: :%d", nodeName, port, registryPort));
        if (target == null) {
            nodeImpl.becomeLeader();
        } else {
            System.out.println(String.format("Trying to connect to %s with registry on %s:%d", target, targetRegistryAddress, targetRegistryPort));
            nodeImpl.connectToAnotherNode(target, targetRegistryAddress, targetRegistryPort);
        }
    }

    private void bindNode(int objectPort, Registry registry) {
        try {
            Node stub = (Node) UnicastRemoteObject.exportObject(this, objectPort);
            registry.rebind(name, stub);
//            System.out.println("Node pushed into registry.");

        } catch (RemoteException e) {
            System.err.println("Port that you are trying to use is probably not available. Try again later.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void connectToAnotherNode(String target, String targetRegistryAddress, int targetRegistryPort) {
        try {
            Registry registry = LocateRegistry.getRegistry(targetRegistryAddress, targetRegistryPort);
//            System.out.println(targetRegistryAddress + ":" + targetRegistryPort);
            Node node = (Node) registry.lookup(target);
//            System.out.println(node.getName());
            node.join(this.name, this);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void connectToAnotherNode(String target, Registry targetRegistry) {
        try {
            Node hostNode = (Node) targetRegistry.lookup(target);
            hostNode.join(this.name, this);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void becomeLeader() {
        leaderNode = this;
        if (prevNode == null) {
            prevNode = this;
            nextNode = this;
        }
        if (allNodes.isEmpty()) {
            allNodes.add(this);
        }
        id = 1;
    }

    @Override
    public void join(String name, Node node) throws RemoteException {
        System.out.println(String.format("%s is connecting.", name));
        node.setLeader(this.leaderNode);
        node.changeNext(this.nextNode);
        this.nextNode.changePrev(node);
        node.changePrev(this);
        this.nextNode = node;
        if (debug) {
            node.printInfo();
            node.getNext().printInfo();
            if (!node.getNext().equals(node.getPrev())) node.getPrev().printInfo();
        }
        this.leaderNode.joinSet(node);
    }

    @Override
    public void changeNext(Node next) throws RemoteException {
        this.nextNode = next;
    }

    @Override
    public void changePrev(Node prev) throws RemoteException {
        this.prevNode = prev;
    }

    @Override
    public Node getPrev() throws RemoteException {
        return this.prevNode;
    }

    @Override
    public Node getNext() throws RemoteException {
        return this.nextNode;
    }

    @Override
    public Node getLeader() throws RemoteException {
        return this.leaderNode;
    }

    @Override
    public void setLeader(Node leader) throws RemoteException {
        this.leaderNode = leader;
    }

    @Override
    public String getName() throws RemoteException {
        return this.name;
    }

    @Override
    public void printInfo() throws RemoteException {
        StringBuilder sb = new StringBuilder("\n");
        if (this.nextNode != null) {
            sb.append("Next: ").append(this.nextNode.getName()).append(" id: ").append(this.nextNode.getId()).append(", ");
        }
        if (this.prevNode != null) {
            sb.append("Prev: ").append(this.prevNode.getName()).append(" id: ").append(this.prevNode.getId()).append(", ");
        }
        if (this.leaderNode != null) {
            sb.append("Leader: ").append(this.leaderNode.getName()).append(" id: ").append(this.leaderNode.getId());
        }
        System.out.println(sb.toString());
//        LOG.debug(sb.toString());
    }

    @Override
    public void joinSet(Node node) throws RemoteException {
        this.allNodes.add(node);
        node.setId(allNodes.size());
    }

    @Override
    public int getId() throws RemoteException {
        return this.id;
    }

    @Override
    public void setId(int id) throws RemoteException {
        this.id = id;
    }

    @Override
    public void election() throws RemoteException {

    }

    @Override
    public void elect() throws RemoteException {

    }

    @Override
    public void lookLeft() throws RemoteException {

    }

    @Override
    public void lookRight() throws RemoteException {

    }

    @Override
    public void repairRing() throws RemoteException {

    }

    @Override
    public Registry getRegistry() throws RemoteException {
        return null;
    }
}