import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class NodeImpl implements Node, Runnable {

    private static boolean debug = false;
    //    private static final Logger LOG = LogManager.getLogger(NodeImpl.class);
    Node right;
    Node left;
    Node leader;
    Set<Node> allNodes = new HashSet<>();
    String name;
    int id = 0;
    int variable;
    boolean working = false;

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
//        boolean fun = (boolean) arguments.get("fun");

        // Logger

//        if (fun && !debug)
//            printLogo();

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
        nodeImpl.run();
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
        if (this.id == 0) {
            id = 1;
            variable = new Random().nextInt();
        }else {
            try {
                this.allNodes.addAll(right.getNodes());
            }catch (RemoteException e){
                System.err.println((e.getMessage()));
            }
        }
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
            try {
                sb.append("Right: ").append(this.right.getName()).append(" id: ").append(this.right.getId()).append(", ");
            } catch (RemoteException e) {
                sb.append("Right node is dead");
//                sb.append("New Right node is:").append(this.right.getName()).append(", ");
            }
        }
        if (this.left != null) {
            try {
                sb.append("Left: ").append(this.left.getName()).append(" id: ").append(this.left.getId()).append(", ");
            } catch (RemoteException e) {
                sb.append("Left node is dead, ");
//                sb.append("New Left node is:").append(this.left.getName()).append(", ");
            }
        }
        if (this.leader != null) {
            try {
                sb.append("Leader: ").append(this.leader.getName()).append(" id: ").append(this.leader.getId());
                if(this.leader.getId()==this.id){
                    sb.append("\n This node is leader, all nodes: ");
                    for (Node n: allNodes) {
                        try{
                            sb.append(n.getName()).append(", id:").append(n.getId()).append("; ");
                        }catch (RemoteException e){
                            sb.append("Some nodes are dead.");
                        }
                    }
                }
            } catch (RemoteException e) {
                sb.append("Leader node is dead");
            }

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
        Set<Integer> voters = this.right.elect(this.id);
        int newLeaderId = voters.stream().min(Integer::compareTo).get();
        newLeader(newLeaderId);
    }

    @Override
    public Set<Integer> elect(int starter) throws RemoteException {
        Set<Integer> ids = new HashSet<>();
        ids.add(this.id);
        if (this.id == starter) {
            return ids;
        }
        ids.addAll(right.elect(starter));
        return ids;
    }

    @Override
    public Node look(String starter, Path where) throws RemoteException {
        System.out.println("starter:" + starter);
        if (starter != null) {
            if (starter.equals(this.name)) {
                System.out.println("ring is healthy");
                return this;
            }
        }
        if (starter == null) {
            starter = this.name;
        }
        switch (where) {
            case left:
                try {
                    return left.look(starter, Path.left);
                } catch (RemoteException e) {
                    return this;
                }
            case right:
                try {
                    return right.look(starter, Path.right);
                } catch (RemoteException e) {
                    return this;
                }
        }
        return null;
    }

    public boolean isHealthy() throws RemoteException {
        try {
            this.right.ping();
            this.left.ping();
        } catch (RemoteException e) {
            return false;
        }
        return true;
    }

    @Override
    public void repairRing() throws RemoteException {
        Node lookRight = this.look(null, Path.right);
        Node lookLeft = this.look(null, Path.left);
        System.out.println("lookLeft:" + lookLeft.getName());
        System.out.println("lookRight:" + lookRight.getName());
        if (!lookLeft.equals(lookRight) || !isHealthy()) {
            lookLeft.setLeft(lookRight);
            System.out.println("changed " + lookLeft.getName() + "'s left to " + lookRight.getName());
            lookRight.setRight(lookLeft);
            System.out.println("changed " + lookRight.getName() + "'s right to " + lookLeft.getName());

        }
        System.out.println("ring repaired");
        lookLeft.printInfo();
        lookRight.printInfo();
    }

    @Override
    public void disconnect() throws RemoteException {
        synchronized (this) {
            if (this.left.getId() != this.id) {
                this.left.setRight(this.right);
                this.right.setLeft(this.left);
                this.left.printInfo();
                if (this.left.getId() != this.right.getId())
                    this.right.printInfo();
            }
        }

        if (this.leader.getId() == this.id && this.left.getId() != this.id) {
            this.left.election();
        }
        //TODO odevzdat práci počkat na ukončení atd.
        System.out.println(String.format("%s is disconnecting.", name));
//        Registry registry = LocateRegistry.getRegistry(1099);
//        try {
//            registry.unbind(this.name);
//        } catch (NotBoundException e) {
//            e.printStackTrace();
//        }
//        System.exit(0);
        System.exit(1);
    }

    @Override
    public void ping() throws RemoteException {

    }

    @Override
    public void newLeader(int id) throws RemoteException {
        if (this.id == id) {
            this.becomeLeader();
        } else {
            right.newLeader(id);
        }
    }

    @Override
    public Set<Node> getNodes() throws RemoteException {
        Set<Node> nodes = new HashSet<>();
        if(this.id == leader.getId()){
            return nodes;
        }else {
            nodes.add(this);
            nodes.addAll(right.getNodes());
            return nodes;
        }
    }

    private void printHelp() {
        System.out.println("Available commands are:");
        System.out.println("\t\t\t\\i - to print info");
        System.out.println("\t\t\t\\q - to gently shutdown node");
        System.out.println("\t\t\t\\h - for this message");
    }

    @Override
    public void run() {
        System.out.println("To see all commands, just write \\h and hit enter.");
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        String input;
        while (true) {
            try {
                input = bf.readLine();
                switch (input) {
                    case "\\q":
                        disconnect();
                        break;
                    case "\\i":
                        printInfo();
                        break;
                    case "\\e":
                        election();
                        break;
                    case "\\h":
                        printHelp();
                        break;
                    default:
                        printHelp();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}