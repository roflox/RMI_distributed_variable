package nodes;

import javafx.util.Pair;
import tasks.*;
import tasks.Random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.Set;


public class NodeImpl implements Node, Runnable {

    private static boolean debug = false;
    //    private static final Logger LOG = LogManager.getLogger(nodes.NodeImpl.class);
    Node right;
    int right_id;
    Node left;
    int left_id;
    Node leader;
    int leader_id;
    Map<Integer, Node> allNodes = new HashMap<>();
    String name;
    int id = 0;
    public int variable;
    private boolean working = false;
    boolean isLeader = false;
    ArrayList<Pair<Integer, Task>> taskQueue;


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
        String hostname = (String) arguments.get("hostname");

        // Toto tu je, aby se dalo vzdáleně připojovat na nody.
        System.setProperty("java.rmi.server.hostname", hostname);

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
        System.out.println(String.format("nodes.Node is using name: %s, port: %d,registry " +
                "port: :%d", nodeName, port, registryPort));

        if (target == null) {
            nodeImpl.becomeLeader();
        } else {
            System.out.println(String.format("Trying to connect to %s with registry " +
                    "on %s:%d", target, targetRegistryAddress, targetRegistryPort));
            nodeImpl.connectToAnotherNode(target, targetRegistryAddress, targetRegistryPort);
        }
        nodeImpl.run();
    }

    /**
     * @param objectPort port na kterem je objekt vystaveny
     * @param registry   registry rmi
     *                   exportuje můj node do registru
     */
    private void bindNode(int objectPort, Registry registry) {
        try {
            Node stub = (Node) UnicastRemoteObject.exportObject(this, objectPort);
            registry.rebind(name, stub);
        } catch (RemoteException e) {
            System.err.println("Port that you are trying to use is probably not available. Try again later.");
            System.exit(1);
        }
    }

    /**
     * @param target                jmeno nodu v registrech
     * @param targetRegistryAddress ip adresa cilovych rmi registru
     * @param targetRegistryPort    port cilovych rmi registru
     *                              připojení nodu k jinému nodu, který je vystavený v RMI registrech
     */
    private void connectToAnotherNode(String target, String targetRegistryAddress, int targetRegistryPort) {
        try {
            Registry registry = LocateRegistry.getRegistry(targetRegistryAddress, targetRegistryPort);
            Node node = (Node) registry.lookup(target);
            node.join(this.name, this);
        } catch (RemoteException | NotBoundException e) {
            System.err.println(String.format("Could not establish connection with %s:%s. Try it again or change " +
                    "target to which you are trying to connect.", targetRegistryAddress, targetRegistryPort));
            System.exit(1);
        }
    }

    /**
     * řekne nodu aby se stal leaderem clusteru
     */
    private void becomeLeader() {
        taskQueue = new ArrayList<>();
        if (left == null) {
            setLeft(new Pair<>(id, this));
            setRight(new Pair<>(id, this));
            setLeader(new Pair<>(id, this));
        }
        if (allNodes.isEmpty()) {
            if (this.id == 0) {
                id = 1;
                Task rand = new Random();
                rand.execute(this);
                allNodes.put(id, this);
            } else {
                try {
                    allNodes.put(id, this);
                    allNodes.putAll(right.getNodes(id));
                    for (Node n : allNodes.values()) {
                        n.setLeader(new Pair<>(id, this));
                    }
                } catch (RemoteException e) {
                    System.err.println((e.getMessage()));
                }
            }
        }
        isLeader = true;
    }

    /**
     * @param name jméno objektu uloženého v RMI registrech, který se připojuje
     * @param node node, který se připojuje
     * @throws RemoteException připojení nodu, do klusteru
     */
    @Override
    public synchronized void join(String name, Node node) throws RemoteException {
        System.out.println(String.format("%s is connecting.", name));
        node.setLeader(new Pair<>(leader_id, leader));
        leader.addNode(node);
        node.setRight(new Pair<>(right_id, right));
        right.setLeft(new Pair<>(node.getId(), node));
        node.setLeft(new Pair<>(id, this));
        right = node;
        right_id = node.getId();
//        node.setVariable(this.variable);
        node.executeTask(new tasks.Set(variable), id);
        if (debug) {
            node.printInfo();
            node.getRight().getValue().printInfo();
            if (!node.getRight().equals(node.getLeft())) node.getLeft().getValue().printInfo();
        }
    }

    @Override
    public Pair<Integer, Node> getLeft() {
        return new Pair<>(left_id, left);
    }

    @Override
    public synchronized void setLeft(Pair<Integer, Node> left) {
        this.left = left.getValue();
        this.left_id = left.getKey();
    }

    @Override
    public Pair<Integer, Node> getRight() {
        return new Pair<>(right_id, right);
    }

    @Override
    public synchronized void setRight(Pair<Integer, Node> right) {
        this.right = right.getValue();
        this.right_id = right.getKey();
    }


    @Override
    public synchronized void setLeader(Pair<Integer, Node> leader) {
        this.leader_id = leader.getKey();
        this.leader = leader.getValue();
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     * @throws RemoteException printuje do konzole informace o nodu
     */
    @Override
    public void printInfo() throws RemoteException {
        StringBuilder sb = new StringBuilder("\n");
        boolean broken = false;
        boolean aliveLeader = true;
        if (this.left != null) {
            try {
                sb.append("Left: ").append(this.left.getName()).append(" id: ").append(this.left_id).append(", ");
            } catch (RemoteException e) {
                sb.append("Left node is dead, ");
                broken = true;
//                sb.append("New Left node is:").append(this.left.getName()).append(", ");
            }
        }
        if (this.right != null) {
            try {
                sb.append("Right: ").append(this.right.getName()).append(" id: ").append(this.right_id).append("\n");
            } catch (RemoteException e) {
                sb.append("Right node is dead\n");
                broken = true;
//                sb.append("New Right node is:").append(this.right.getName()).append(", ");
            }
        }
        if (this.leader != null) {
            try {
                if (this.isLeader) {
                    sb.append("This node is leader, all nodes: ");
                    for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
                        try {
                            sb.append(n.getValue().getName()).append(", id:").append(n.getKey()).append("; ");
                        } catch (RemoteException e) {
                            sb.append(String.format("Node id: %d is dead; ", n.getKey()));
                            broken = true;
                        }
                    }
                    sb.append("\nNumber of tasks in queue:").append(taskQueue.size()).append("\n");
                } else {
                    sb.append("Leader: ")
                            .append(this.leader.getName())
                            .append(" id: ")
                            .append(this.leader_id).append("\n");
                }
            } catch (RemoteException e) {
                sb.append("Leader node is dead\n");
                aliveLeader = false;
            }
        }
        sb.append("Variable: ").append(variable).append("\n");
        System.out.println(sb.toString());
        if (broken) {
            this.repairRing(aliveLeader);
        }
//        LOG.debug(sb.toString());
    }

    @Override
    public synchronized void addNode(Node node) throws RemoteException {
        node.setId(allNodes.keySet().stream().max(Comparator.comparing(Integer::intValue)).get() + 1);
        this.allNodes.put(node.getId(), node);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @throws RemoteException odstartování leader electionu
     */
    @Override
    public void election() throws RemoteException {
        Set<Integer> voters = right.voteLeader(this.id);
        int newLeaderId = voters.stream().min(Integer::compareTo).get();
        newLeader(newLeaderId);
    }

    @Override
    public Set<Integer> voteLeader(int starter) throws RemoteException {
        Set<Integer> ids = new HashSet<>();
        ids.add(id);
        if (right_id == starter) {
            ids.add(starter);
        } else {
            ids.addAll(right.voteLeader(starter));
        }
        return ids;
    }

    @Override
    public Node look(String starter, Path where) {
        if (starter != null) {
            if (starter.equals(this.name)) {
                System.out.println("Ring is healthy.");
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

    public boolean isHealthy() {
        try {
            this.right.ping();
            this.left.ping();
        } catch (RemoteException e) {
            return false;
        }
        return true;
    }

    @Override
    public void repairRing(boolean aliveLeader) throws RemoteException {
        Node lookRight = this.look(null, Path.right);
        Node lookLeft = this.look(null, Path.left);
        System.err.println("Topology is broken. Repair process is started. Alive leader:" + aliveLeader);
        if (!lookLeft.equals(lookRight) || !isHealthy()) {
            lookLeft.setLeft((new Pair<>(lookRight.getId(), lookRight)));
            lookRight.setRight((new Pair<>(lookLeft.getId(), lookLeft)));

        }
        System.err.println("Topology should be repaired. Gonna start election: " + !aliveLeader);
        if (aliveLeader)
            this.leader.gatherNodes();
        else
            election();
    }

    @Override
    public void disconnect() throws RemoteException {
        synchronized (this) {
            if (left_id != id) {
                left.printInfo();
                right.printInfo();
                left.setRight(new Pair<>(right_id, right));
                right.setLeft(new Pair<>(left_id, left));
                left.printInfo();
                right.printInfo();
            }
        }
        if (isLeader && right_id != id) {
            right.election();
        }
        //TODO odevzdat práci počkat na ukončení atd.
        System.out.println(String.format("%s is disconnecting.", name));
        if (isLeader) {
            System.out.println("Newly elected leader is: " + right.getLeader().getKey());
        }
        System.exit(1);
    }

    @Override
    public void ping() {

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
    public Map<Integer, Node> getNodes(int starter_id) throws RemoteException {
        HashMap<Integer, Node> nodes = new HashMap<>();
        if (this.id != starter_id) {
            nodes.put(this.id, this);
            nodes.putAll(right.getNodes(starter_id));
        }
        return nodes;
    }

    @Override
    public synchronized void executeTask(Task task, int starter_id) throws RemoteException {
        if (task == null)
            return;
        System.out.println(String.format("executing task: %s initiated by %s", task.getClass().toString(), starter_id));
        if (debug)
            waitSec();
        try {

//            System.out.println("is executable: " + leader.isExecutable());
            if (!isLeader && starter_id != id) {
                working = true;
                waitSec();
                task.execute(this);
            } else if (leader.isExecutable(starter_id)) {
                working = true;
//                System.out.println("isLeader: " + isLeader);
                if (isLeader) {
                    for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
//                        System.out.println("isStarter: " + (starter_id == n.getKey()));
                        if (starter_id != n.getKey() && !n.getValue().isLeader()) {
                            if (debug)
                                waitSec();
                            n.getValue().executeTask(task, starter_id);
                        }
                    }
                } else {
                    this.leader.executeTask(task, starter_id);
                }
                task.execute(this);
            } else {
                leader.addTaskToQueue(new Pair<>(starter_id, task));
                return;
            }
        } catch (RemoteException e) {
            if (isLeader) {
                repairRing(true);
            } else {
                repairRing(false);
            }
            this.leader.executeTask(new tasks.Set(variable), starter_id);
        }
        working = false;
        executeQueue();
    }


    public boolean isAvailable() {
        return !working;
    }

    @Override
    public boolean isExecutable(int starter_id) throws RemoteException {
        for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
            if (!n.getValue().isAvailable() && starter_id != n.getKey()) {
                return false;
            }
        }
        return true;
    }

    private void printHelp() {
        System.out.println("Available commands are:");
        System.out.println("\t\t\ti or info - to print info");
        System.out.println("\t\t\tq or quit - to gently shutdown node");
        System.out.println("\t\t\ta or add - adding 1 to current value of shared variable");
        System.out.println("\t\t\ts or subtract - subtracting 1 from current value of shared variable");
        System.out.println("\t\t\tw or wipe - for setting current value of variable to 0");
        System.out.println("\t\t\tr or random - generate new random variable");
    }

    @Override
    public void run() {
        System.out.println("To see all commands, just hit enter.");
        BufferedReader bf = new BufferedReader(new InputStreamReader(System.in));
        String input;
        while (true) {
            try {
                input = bf.readLine();
                Task task = null;
                switch (input) {
                    case "quit":
                    case "q":
                        disconnect();
                        break;
                    case "info":
                    case "i":
                        printInfo();
                        break;
                    case "election":
                    case "e":
                        election();
                        break;
                    case "add":
                    case "a":
                        task = new Increase(1);
//                        executeTask(new Increase(1),this.id);
                        break;
                    case "subtract":
                    case "s":
                        task = new Decrease(1);
//                        executeTask(new Decrease(1),this.id);
                        break;
                    case "wipe":
                    case "w":
                        task = new Wipe();
//                        executeTask(new Wipe(),id);
                        break;
                    case "random":
                    case "r":
                        task = new tasks.Random();
//                        executeTask(new tasks.Random(),id);
                        break;
                    case "debug":
                    case "d":
                        executeQueue();
                        break;
                    default:
                        printHelp();
                }
                this.executeTask(task, id);
            } catch (RemoteException e) {
                System.err.println("Topology is damaged.");
                e.printStackTrace();
                try {
                    this.repairRing(false);
                } catch (RemoteException fe) {
                    System.err.println("Fatal error, ring cannot be repaired");
                    if (debug)
                        fe.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public boolean isLeader() {
        return isLeader;
    }

    @Override
    public void gatherNodes() throws RemoteException {
        if (isLeader) {
            allNodes = new HashMap<>();
            allNodes.putAll(right.getNodes(id));
        }
    }

    @Override
    public boolean addTaskToQueue(Pair<Integer, Task> taskPair) throws RemoteException {
        System.err.println("adding task to queue");
        if (!taskQueue.contains(taskPair)) {
            taskQueue.add(taskPair);
            return true;
        } else {
            System.err.println("Same task is already in the queue!");
            return false;
        }
    }

    @Override
    public Pair<Integer, Node> getLeader() throws RemoteException {
        return new Pair<Integer, Node>(leader_id, leader);
    }

    private void executeQueue() throws RemoteException {
        if (isLeader) {
            if (!taskQueue.isEmpty()) {
                System.out.println("executing task from queue (" + taskQueue.size() + ")");
                var toExecute = taskQueue.remove(0);
                executeTask(toExecute.getValue(), toExecute.getKey());
            }
        }
    }


    private void waitSec() {
        try {
            wait(500);
        } catch (Exception e) {
            System.err.println("Cannot wait!");
        }
    }

}