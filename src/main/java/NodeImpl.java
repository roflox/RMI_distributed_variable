import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class NodeImpl implements Node {

    private static boolean debug = false;
    Node right;
    Node left;
    Node leader;
    Set<Node> allNodes = new HashSet<>();
    String name;
    int id;
    int variable;

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
            Node node = (Node) registry.lookup(target);
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
        leader = this;
        if (left == null) {
            left = this;
            right = this;
        }
        if (allNodes.isEmpty()) {
            allNodes.add(this);
        }
        id = 1;
        variable = new Random().nextInt();
    }

    @Override
    public synchronized void join(String name, Node node) throws RemoteException {
        System.out.println(String.format("%s is connecting.", name));
        node.setLeader(this.leader);
        node.setRight(this.right);
        this.right.setLeft(node);
        node.setLeft(this);
        this.right = node;
        this.leader.joinSet(node);
        if (debug) {
            node.printInfo();
            node.getNext().printInfo();
            if (!node.getNext().equals(node.getLeft())) node.getLeft().printInfo();
        }
    }

    @Override
    public Node getLeft() throws RemoteException {
        return this.left;
    }

    @Override
    public synchronized void setLeft(Node left) throws RemoteException {
        this.left = left;
    }

    @Override
    public Node getNext() throws RemoteException {
        return this.right;
    }

    @Override
    public synchronized void setRight(Node right) throws RemoteException {
        this.right = right;
    }

    @Override
    public Node getLeader() throws RemoteException {
        return this.leader;
    }

    @Override
    public synchronized void setLeader(Node leader) throws RemoteException {
        this.leader = leader;
    }

    @Override
    public String getName() throws RemoteException {
        return this.name;
    }

    @Override
    public void printInfo() throws RemoteException {
        StringBuilder sb = new StringBuilder("\n");
        if (this.right != null) {
            sb.append("Next: ").append(this.right.getName()).append(" id: ").append(this.right.getId()).append(", ");
        }
        if (this.left != null) {
            sb.append("Prev: ").append(this.left.getName()).append(" id: ").append(this.left.getId()).append(", ");
        }
        if (this.leader != null) {
            sb.append("Leader: ").append(this.leader.getName()).append(" id: ").append(this.leader.getId());
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
    public void lookLeft(Integer id) throws RemoteException {
        if(id==null){
            id = this.id;
        }
        try{
            left.lookLeft(id);
        }catch (RemoteException e){
            e.printStackTrace();
        }
    }

    @Override
    public Node lookRight() throws RemoteException {
        try{
            this.right.ping();
        }catch (RemoteException e){
            System.err.println(this.name+"'s right node is dead.");
            return this;
        }
        Node node = this.right.lookRight();
        if(node!=null){
            synchronized (this){
                this.left = node;
                synchronized (node){
                    node.setLeft(this);
                }
            }
        }
        return null;
    }


    @Override
    public void repairRing() throws RemoteException {

    }

    @Override
    public void disconnect() throws RemoteException {
        synchronized (this) {
            if (!this.left.equals(this)) {
                this.left.setRight(this.right);
                this.right.setLeft(this.left);
            }
        }
        //TODO odevzdat práci počkat na ukončení atd.
        System.out.println(String.format("%s is disconnecting.", name));
        Registry registry = LocateRegistry.getRegistry(1099);
        try {
            registry.unbind(this.name);
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
//        System.exit(0);
//        System.exit(1);
    }

    @Override
    public void ping() throws RemoteException {

    }
}