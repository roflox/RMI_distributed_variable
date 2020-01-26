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
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
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
    boolean working = false;
    boolean isLeader = false;
    List<Task> taskQueue;
    public int logicalTime = 0;
    Thread queueChecker;


    public NodeImpl(String name, Registry registry, int objectPort) {
        this.name = name;
        bindNode(objectPort, registry);
    }

    /**
     * @param args nastaveno jako konzolová aplikace, která přijme options
     *             -n --name                   Name of your node, which will be written into RMI registry. REQUIRED
     *             -h --hostname                Ip address of current node. It is recommended to be used, remote nodes may not connect.
     *             -p --port                    Port on which this node will be listening. REQUIRED
     *             -r --registryPort            Port on which your RMI registry will be. REQUIRED
     *             -t --target                  Target node name.
     *             -A --targetRegistryAddress   Address of target's node RMI registry. If not set using localhost.
     *             -P --targetRegistryPort      Port of target's node RMI registry. REQUIRED when -t --target is present.
     *             -d --debug                   Option for debug mode.
     *             -w --waitTime                For how long should node wait if not connected instantly.
     */
    public static void main(String[] args) throws InterruptedException {
        Map<String, Object> arguments = ConsoleArgumentParser.parseInit(args);
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
        logger.debug("nodeName:{}, hostnane:{}, port:{}, registryPort:{}," +
                        "target:{},targetRegistryAddress:{},targetRegistryPort:{}", nodeName, hostname, port,
                registryPort, target, targetRegistryAddress, targetRegistryPort);
        // create own registry
        Registry registry = null;
        try {
            registry = LocateRegistry.createRegistry(registryPort);
        } catch (RemoteException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }
        NodeImpl nodeImpl = new NodeImpl(nodeName, registry, port);
        logger.info("{}'s registry running on {}:{}. Node is using port {}.", nodeName, hostname, registryPort, port);

        if (target == null) {
            nodeImpl.becomeLeader();
        } else {
            logger.info("Trying to connect to {} with registry " +
                    "on {}:{}.", target, targetRegistryAddress, targetRegistryPort);
            boolean succes = false;
            for (int i = 0; i < 5; i++) {
                succes = nodeImpl.connectToAnotherNode(target, targetRegistryAddress, targetRegistryPort);
                if (succes)
                    break;
                if (i != 4) {
                    Main m = new Main();
                    synchronized (m) {
                        logger.warn("Could not establish connection, trying again.");
                        m.wait(waitTime);
                    }
                }
            }
            if (!succes) {
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
            return node.join(this.name, this);
        } catch (RemoteException | NotBoundException e) {
            logger.debug(e.getMessage());
            return false;
        }
    }

    /**
     * řekne nodu aby se stal leaderem clusteru
     */
    private void becomeLeader() {
        logicalTime++;
        logger.info("This node is now leader.");
        taskQueue = new ArrayList<>();
        if (allNodes.isEmpty()) {
            if (id == 0) {
                id = 1;
                setLeft(new Pair<>(id, this));
                setRight(new Pair<>(id, this));
                setLeader(new Pair<>(id, this));
                Task rand = new Random(id, logicalTime);
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
        queueChecker = new Thread(new QueueChecker(logger, this));
        queueChecker.start();
    }

    /**
     * @param name jméno objektu uloženého v RMI registrech, který se připojuje
     * @param node node, který se připojuje
     * @throws RemoteException připojení nodu, do klusteru
     */
    @Override
    public synchronized boolean join(String name, Node node) throws RemoteException {
        logicalTime++;
        logger.info(String.format("%s is connecting.", name));
        if (leader == null) {
            return false;
        }
        if (!isHealthy()) {
            logger.warn("Node is not healthy.");
            boolean l;
            try {
                leader.ping();
                l = true;
            } catch (RemoteException e) {
                l = false;
            }
            repairRing(l);
        }
        node.setLeader(new Pair<>(leader_id, leader));
        leader.addNode(node);
        node.setRight(new Pair<>(right_id, right));
        right.setLeft(new Pair<>(node.getId(), node));
        node.setLeft(new Pair<>(id, this));
        right = node;
        right_id = node.getId();

        logger.info(String.format("%s is connected.", name));
        return true;
    }

    @Override
    public void setLeft(Pair<Integer, Node> left) {
        logicalTime++;
        this.left = left.getValue();
        this.left_id = left.getKey();
    }

    @Override
    public void setRight(Pair<Integer, Node> right) {
        logicalTime++;
        this.right = right.getValue();
        this.right_id = right.getKey();
    }

    @Override
    public synchronized void setLeader(Pair<Integer, Node> leader) {
        this.leader_id = leader.getKey();
        this.leader = leader.getValue();
        try {
            this.variable = this.leader.getLocalVariable();
        } catch (RemoteException e) {
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    /**
     *
     */
    @Override
    public void printInfo() {
        StringBuilder sb = new StringBuilder();
        boolean broken = false;
        boolean aliveLeader = true;
        if (this.left != null) {
            try {
                sb.append("Left: ").append(this.left.getName()).append(" id: ").append(this.left_id).append(", ");
            } catch (RemoteException e) {
                sb.append("Left node is dead, ");
                broken = true;
            }
        }
        if (this.right != null) {
            try {
                sb.append("Right: ").append(this.right.getName()).append(" id: ").append(this.right_id);
            } catch (RemoteException e) {
                sb.append("Right node is dead");
                broken = true;
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
                broken = true;
            }
        }
        logger.info(sb.toString());
        logger.info("Variable: {}", variable);
        logger.info("Time: {}", logicalTime);
        if (isLeader)
            printQueue();
        logger.debug("Working:{}.", working);
        if (broken) {
            this.repairRing(aliveLeader);
        }
//        LOG.debug(sb.toString());
    }

    @Override
    public synchronized void addNode(Node node) throws RemoteException {
        logicalTime++;
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
        logicalTime++;
        try {
            right.voteLeader(id);
        } catch (RemoteException e) {
            repairRing(false);
        }
    }

    @Override
    public void voteLeader(int smallest) throws RemoteException {
        logicalTime++;
        if (smallest > id) {
            right.voteLeader(id);
        } else if (id == smallest) {
            becomeLeader();
        } else {
            right.voteLeader(smallest);
        }
    }

    @Override
    public Node look(String starter, Path where, int logicalTime) {
        if (logicalTime > this.logicalTime) {
            this.logicalTime = logicalTime + 1;
        } else {
            this.logicalTime++;
        }
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
                    return left.look(starter, Path.left, this.logicalTime);
                } catch (RemoteException e) {
                    return this;
                }
            case right:
                try {
                    return right.look(starter, Path.right, this.logicalTime);
                } catch (RemoteException e) {
                    return this;
                }
        }
        return null;
    }

    public boolean isHealthy() {
        if (left_id == id)
            return true;
        try {
            this.right.ping();
            this.left.ping();
        } catch (RemoteException e) {
            return false;
        }
        return true;
    }

    @Override
    public void repairRing(boolean aliveLeader) {
        for (int i = 0; i < 5; i++) {
            try {
                Node lookRight = this.look(null, Path.right, logicalTime);
                Node lookLeft = this.look(null, Path.left, logicalTime);
                logger.warn("Topology is broken. Repair process is started. Alive leader:" + aliveLeader);
                if (!lookLeft.equals(lookRight) || !isHealthy()) {
                    lookLeft.setLeft((new Pair<>(lookRight.getId(), lookRight)));
                    lookRight.setRight((new Pair<>(lookLeft.getId(), lookLeft)));
                }
                logger.debug(lookLeft);
                logger.debug(lookRight);
                if (left_id == id) {
                    this.left = this;
                    this.right = this;
                    this.left_id = id;
                    this.right_id = id;
                    this.leader = this;
                    this.leader_id = id;
                    this.allNodes = new HashMap<>();
                    this.allNodes.put(id, this);
                    aliveLeader = true;
                }
                logger.warn("Topology should be repaired. Gonna start election: " + !aliveLeader);
                if (aliveLeader)
                    this.leader.gatherNodes();
                else
                    election();
                return;
            } catch (RemoteException e) {
                logger.warn("Ring could not be repaired. Trying again. Remaining attempts {}.", 4 - i);
            }
        }
        logger.fatal("Fatal error. Ring could not be repaired. Shutting down.");
        System.exit(1);
    }

    @Override
    public synchronized void disconnect() throws RemoteException {
        if (left_id != id) {
            left.printInfo();
            right.printInfo();
            left.setRight(new Pair<>(right_id, right));
            right.setLeft(new Pair<>(left_id, left));
            left.printInfo();
            right.printInfo();
        }
        if (isLeader && right_id != id) {
            right.election();
        }
        if (isLeader) {
            logger.warn("Disconnecting. Newly elected leader is {}.", right.getLeader().getValue().getName());
            this.queueChecker.interrupt();
        } else {
            logger.warn("Disconnecting.");
        }
        if (right_id != id) {
            right.getLeader().getValue().gatherNodes();
        }


        System.exit(1);
    }

    @Override
    public void ping() {
    }

    @Override
    public Map<Integer, Node> getNodes(int starter_id) throws RemoteException {
        logicalTime++;
        HashMap<Integer, Node> nodes = new HashMap<>();
        if (this.id != starter_id) {
            nodes.put(this.id, this);
            nodes.putAll(right.getNodes(starter_id));
        }
        return nodes;
    }

    private void startTask(Task task) {
        try {
            if (isLeader) {
                executeTask(task);
                executeQueue();
            } else {
                leader.addTaskToQueue(task);
            }
        } catch (RemoteException e) {
            if (isLeader) {
                repairRing(true);
            } else {
                repairRing(false);
            }
            startTask(task);
        }
    }


    private synchronized void executeTask(Task task) {
        boolean error;
        working = true;
        do {
            error = false;
            try {
                logger.info("Executing task {}", task);
                for (Map.Entry<Integer, Node> n : allNodes.entrySet()) {
                    if (task.getLogicalTime() > logicalTime) {
                        this.logicalTime = task.getLogicalTime();
                    }
                    task.setLogicalTime(logicalTime++);
                    if (n.getKey() != id) {
//                        logger.debug("Executing for {}", n.getKey());
                        n.getValue().doTask(task);
                    }
                }

            } catch (RemoteException e) {
                repairRing(true);
                executeTask(new tasks.Set(variable, id, logicalTime));
                error = true;
            }
        } while (error);
        task.execute(this);
        working = false;
    }

    @Override
    public void doTask(Task t) throws RemoteException {
        if (t.getLogicalTime() > logicalTime) {
            logicalTime = t.getLogicalTime();
        }
        waitCustom(0.2);
        logicalTime++;
        logger.info("Executing task {}", t);
        t.execute(this);
    }

    private void printHelp() {
        logger.info("Available commands are:");
        logger.info("\t\t\ti or info        - to print info");
        logger.info("\t\t\tq or quit        - to gently shutdown node");
        logger.info("\t\t\ta or add         - adding 1 to current value of shared variable");
        logger.info("\t\t\ts or subtract    - subtracting 1 from current value of shared variable");
        logger.info("\t\t\tw or wipe        - for setting current value of variable to 0");
        logger.info("\t\t\tr or random      - generate new random variable");
        logger.info("\t\t\tt or tasks       - print queue on leader, must be in debug mode");
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
                String[] commands = input.split("(?!^)");
                for (String command : commands) {
//                    logger.debug(command);
                    switch (command) {
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
                            task = new Increase(1, id, ++logicalTime);
                            break;
                        case "subtract":
                        case "s":
                            task = new Decrease(1, id, ++logicalTime);
                            break;
                        case "wipe":
                        case "w":
                            task = new Wipe(id, ++logicalTime);
                            break;
                        case "random":
                        case "r":
                            task = new tasks.Random(id, ++logicalTime);
                            break;
                        case "debug":
                        case "d":
                            debug = !debug;
                            if (debug) {
                                Configurator.setRootLevel(Level.DEBUG);
                            } else {
                                Configurator.setRootLevel(Level.INFO);
                            }
                            logger.debug("Now running in debug.");
                            break;
                        case "help":
                        case "h":
                            printHelp();
                            break;
                        case "tasks":
                        case "t":
                            if(isLeader)
                                printQueue();
                            break;
                        default:
                            logger.warn("Command {} is not recognized. Hit type h or help to display help.", command);
                    }
                    if (task != null) {
                        if (isLeader) {
                            addTaskToQueue(task);
                        } else {
                            startTask(task);
                        }
                    }
                }
            } catch (RemoteException e) {
                logger.error("Topology is damaged.");
                e.printStackTrace();
                repairRing(false);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void gatherNodes() throws RemoteException {
        logicalTime++;
        if (isLeader) {
            allNodes = new HashMap<>();
            if (right_id == id) {
                allNodes.put(id, this);
                return;
            }
            allNodes.putAll(right.getNodes(id));
        }
    }

    @Override
    public void addTaskToQueue(Task task) {
//        printQueue();
        taskQueue.add(task);
//        if (!working) {
//            executeQueue();
//        }
    }

    @Override
    public Pair<Integer, Node> getLeader() throws RemoteException {
        return new Pair<>(leader_id, leader);
    }

    @Override
    public int getLocalVariable() throws RemoteException {
        return variable;
    }

    public void executeQueue() { // tady nastane zacykleni, a
        if (taskQueue.size() != 0) {
//            printQueue();
            Task task = taskQueue.remove(0);
            while (working) {
                logger.debug("Working, must wait.");
                waitCustom(1);
            }
            executeTask(task);
        }
    }

    private void waitCustom(double seconds) {
        synchronized (this) {
            try {
                wait((new Double(seconds * 1000)).longValue());
                logger.debug("Waited for {}s.", seconds);
            } catch (Exception e) {
                logger.error("Cannot wait!");
            }
        }
    }

    private void printQueue() {
        if (isLeader) {
            if (debug) {
                StringBuilder sb = new StringBuilder();
                for (Task queued : taskQueue) {
                    sb.append(queued).append(", ");
                }
                logger.debug("Queued tasks:" + sb.toString());
            }
        }
    }
}