package nodes;

import com.sun.tools.javac.Main;
import javafx.util.Pair;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import tasks.Random;
import tasks.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Set;
import java.util.*;

public class NodeImpl implements Node, Runnable {

    private static boolean debug = false;
    private static Logger logger = LogManager.getLogger(NodeImpl.class.getName());
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
    List<Task> taskQueue;


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
    public static void main(String[] args) throws InterruptedException{
        Map<String, Object> arguments = ConsoleArgumentParser.parse(args);
        // getting arguments from map
        String targetRegistryAddress = (String) arguments.get("targetRegistryAddress");
        int targetRegistryPort = (Integer) arguments.get("targetRegistryPort");
        String target = (String) arguments.get("target");
        int port = (Integer) arguments.get("port");
        int registryPort = (Integer) arguments.get("registryPort");
        String nodeName = (String) arguments.get("nodeName");
        debug = (boolean) arguments.get("debug");
        int waitTime = (Integer) arguments.get("waitTime");
        String hostname = (String) arguments.get("hostname");
        if (debug) {
            Configurator.setRootLevel(Level.DEBUG);
        }
        // Toto tu je, aby se dalo vzdáleně připojovat na nody.
        System.setProperty("java.rmi.server.hostname", hostname);

        logger.debug("Running in debug mode.");
        // create own registry
        Registry registry = null;
        try {
            registry = LocateRegistry.createRegistry(registryPort);
        } catch (RemoteException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        NodeImpl nodeImpl = new NodeImpl(nodeName, registry, port);
        logger.info("{}'s registry running on {}:{}. Node is using port {}.",nodeName,hostname,registryPort,port);

        if (target == null) {
            nodeImpl.becomeLeader();
        } else {
            logger.info("Trying to connect to {} with registry " +
                    "on {}:{}.",target,targetRegistryAddress,targetRegistryPort);
            boolean succes = false;
            for(int i = 0; i < 5;i++) {
                succes = nodeImpl.connectToAnotherNode(target, targetRegistryAddress, targetRegistryPort);
                if(succes)
                    break;
                if(i!=4){
                    Main m = new Main();
                    synchronized (m) {
                        logger.warn("Could not establish connection, trying again.");
                        m.wait(waitTime);
                    }
                }
            }
            if(!succes){
                logger.fatal("Could not establish connection with {}:{}. Try it again or change " +
                        "target to which you are trying to connect.", targetRegistryAddress, targetRegistryPort);
                System.exit(1);
            }
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
            logger.error("Port that you are trying to use is probably not available. Try again later.");
            System.exit(1);
        }
    }

    /**
     * @param target                jmeno nodu v registrech
     * @param targetRegistryAddress ip adresa cilovych rmi registru
     * @param targetRegistryPort    port cilovych rmi registru
     *                              připojení nodu k jinému nodu, který je vystavený v RMI registrech
     */
    private boolean connectToAnotherNode(String target, String targetRegistryAddress, int targetRegistryPort) {
        try {
            Registry registry = LocateRegistry.getRegistry(targetRegistryAddress, targetRegistryPort);
            Node node = (Node) registry.lookup(target);
            node.join(this.name, this);
            return true;
        } catch (RemoteException | NotBoundException e) {
            return false;
        }
    }

    /**
     * řekne nodu aby se stal leaderem clusteru
     */
    private void becomeLeader() {
        taskQueue = new ArrayList<>();
        if (allNodes.isEmpty()) {
            if (id == 0) {
                id = 1;
                setLeft(new Pair<>(id, this));
                setRight(new Pair<>(id, this));
                setLeader(new Pair<>(id, this));
                Task rand = new Random(id);
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
                    logger.error((e.getMessage()));
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
        logger.info(String.format("%s is connecting.", name));
        node.setLeader(new Pair<>(leader_id, leader));
        leader.addNode(node);
        node.setRight(new Pair<>(right_id, right));
        right.setLeft(new Pair<>(node.getId(), node));
        node.setLeft(new Pair<>(id, this));
        right = node;
        right_id = node.getId();
        node.executeTask(new tasks.Set(variable, id));
        logger.info(String.format("%s is connected.", name));
//        if (debug) {
//            node.printInfo();
//            node.getRight().getValue().printInfo();
//            if (!node.getRight().equals(node.getLeft())) node.getLeft().getValue().printInfo();
//        }
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
        StringBuilder sb = new StringBuilder();
        boolean broken = false;
        boolean aliveLeader = true;
//        sb.append("node_id: ").append(id);
//        logger.info(sb.toString());
        sb = new StringBuilder();
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
                sb.append("Right: ").append(this.right.getName()).append(" id: ").append(this.right_id);
            } catch (RemoteException e) {
                sb.append("Right node is dead");
                broken = true;
//                sb.append("New Right node is:").append(this.right.getName()).append(", ");
            }
        }
        logger.info(sb.toString());
        sb = new StringBuilder();
        if (this.leader != null) {
            try {
                if (this.isLeader) {
                    logger.info("This node is leader, all nodes:");
                    for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
                        try {
                            sb.append(n.getValue().getName()).append(", id:").append(n.getKey()).append("; ");
                        } catch (RemoteException e) {
                            sb.append(String.format("Node id: %d is dead; ", n.getKey()));
                            broken = true;
                        }
                    }
                } else {
                    sb.append("Leader: ")
                            .append(this.leader.getName())
                            .append(" id: ")
                            .append(this.leader_id);
                }
            } catch (RemoteException e) {
                sb.append("Leader node is dead\n");
                aliveLeader = false;
            }
        }
        logger.info(sb.toString());
        logger.info("Variable: " + variable);
        if(isLeader)
            printQueue();
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
                logger.info("Ring is healthy.");
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
        logger.error("Topology is broken. Repair process is started. Alive leader:" + aliveLeader);
        if (!lookLeft.equals(lookRight) || !isHealthy()) {
            lookLeft.setLeft((new Pair<>(lookRight.getId(), lookRight)));
            lookRight.setRight((new Pair<>(lookLeft.getId(), lookLeft)));

        }
        logger.error("Topology should be repaired. Gonna start election: " + !aliveLeader);
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
        if (isLeader) {
            var newLeader = right.getLeader();
            logger.info("Newly elected leader is: " + newLeader.getKey());
        }
        logger.info(String.format("%s is disconnecting.", name));

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
    public synchronized void executeTask(Task task) throws RemoteException {
        if (task == null) {
            return;
        }
        logger.info("Executing task {}.",task);
//        if (debug)
//            waitSec();
        try {

            if (!isLeader && task.getStarter() != id) { // toto ani nevim proc tu je :D
                working = true;
//                waitSec();
                task.execute(this);
            } else if (leader.isExecutable(task)) { // pro leadera
                working = true;
                if (isLeader) { // toto rika leaderovi ze ma executnout ty tasky na "poddanych" nebo jak je nazvat
                    for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
                        if (task.getStarter() != n.getKey() && !n.getValue().isLeader()) { //toto je tu aby se to nezacyklilo, aby si leader nedal znova za ukol ten task, a nebo aby to nedal za ukol starterovi
                            n.getValue().executeTask(task);
                        }
                    }
                } else { // pro startera
                    this.leader.executeTask(task);
                }
                task.execute(this); // jo jasne, toto je to pro leadera a pro startera
            } else {
                leader.addTaskToQueue(task); // tady to dava do fronty
            }
        } catch (RemoteException e) {
            if (isLeader) {
                repairRing(true);
            } else {
                repairRing(false);
            }
            this.leader.executeTask(new tasks.Set(variable, id));
        }
        working = false;
        executeQueue();
    }

    public boolean isAvailable() {
        return !working;
    }

    @Override
    public boolean isExecutable(Task task) throws RemoteException {
        logger.debug("Checking if {} is executable.",task);
        for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
            if (!n.getValue().isAvailable() && task.getStarter() != n.getKey()) {
                logger.warn("Not executable because " + n.getKey() + " is working on something else.");
                return false;
            }
        }
        return true;
    }

    private void printHelp() {
        logger.info("Available commands are:");
        logger.info("\t\t\ti or info - to print info");
        logger.info("\t\t\tq or quit - to gently shutdown node");
        logger.info("\t\t\ta or add - adding 1 to current value of shared variable");
        logger.info("\t\t\ts or subtract - subtracting 1 from current value of shared variable");
        logger.info("\t\t\tw or wipe - for setting current value of variable to 0");
        logger.info("\t\t\tr or random - generate new random variable");
    }

    @Override
    public void run() {
        logger.info("To see all commands, just hit enter.");
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
                        task = new Increase(1, id);
                        break;
                    case "subtract":
                    case "s":
                        task = new Decrease(1, id);
                        break;
                    case "wipe":
                    case "w":
                        task = new Wipe(id);
                        break;
                    case "random":
                    case "r":
                        task = new tasks.Random(id);
                        break;
                    case "debug":
                    case "d":
                        executeQueue();
                        break;
                    default:
                        printHelp();
                }
                this.executeTask(task);
            } catch (RemoteException e) {
                logger.error("Topology is damaged.");
                e.printStackTrace();
                try {
                    this.repairRing(false);
                } catch (RemoteException fe) {
                    logger.error("Fatal error, ring cannot be repaired");
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
    public boolean addTaskToQueue(Task task) throws RemoteException {
        logger.debug("Queued: {}.",task);
        printQueue();
        if (!taskQueue.contains(task)) {
            taskQueue.add(task);
            return true;
        } else {
            logger.error("Same task is already in the queue!");
            return false;
        }
    }

    @Override
    public Pair<Integer, Node> getLeader() throws RemoteException {
        return new Pair<Integer, Node>(leader_id, leader);
    }

    public void executeQueue() throws RemoteException { // tady nastane zacykleni, a
        if (isLeader) {
            if(taskQueue.size()!=0) {
                logger.debug("Gonna execute queue");
                Task task = taskQueue.remove(0);
                while (!isExecutable(task)) {
                    waitCustom(1);
                }
                executeTask(task);
            }
        }
    }

    private void waitSec() {
        try {
            wait(500);
        } catch (Exception e) {
            logger.error("Cannot wait!");
        }
    }

    private void waitCustom(int seconds) {
        try {
            wait(seconds * 1000);
        } catch (Exception e) {
            logger.error("Cannot wait!");
        }
    }

    private void printQueue() {
        if (isLeader) {
            if (debug) {
                StringBuilder sb = new StringBuilder();
                for (Task queued : taskQueue) {
                    sb.append(queued).append(", ");
                }
                logger.debug("Queued tasks:\n" + sb.toString());
            }
        }
    }
}