import org.apache.commons.cli.*;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;


public class Node implements NodeInterface {

    NodeInterface nextNode;
    NodeInterface prevNode;
    NodeInterface leaderNode;
    Set<NodeInterface> allNodes = new HashSet<>();
    String name;
    Registry registry;

    public Node(String name, Registry registry, int objectPort) {
        this.name = name;
        this.registry = registry;
        pushNodeToRegistry(objectPort);
    }

    /**
     * jenom pro zjednodušení abych to furt nevypisoval jak blbec
     */
    private static void createOpts(String opt, String longOpt, boolean hasArg, String description, boolean required, Options options) {
        Option option = new Option(opt, longOpt, hasArg, description);
        option.setRequired(required);
        options.addOption(option);
    }

    /**
     * @param args nastaveno jako konzolová aplikace, která přijme options
     *             -t --target cílový node, ke kterému se má nově spuštěný node připojit
     *             -n --name jméno nově vzniklého nodu
     */
    public static void main(String[] args) {
//        System.out.println("Hello World!");
//        Arrays.stream(args).forEach(System.out::println);
        Options options = new Options();
        createOpts("t", "target", true, "Target node name.", false, options);
        createOpts("n", "name", true, "Name of your node, which will be written into RMI registry.", true, options);
        createOpts("rp", "registryPort", true, "RMI registry port. If not set using port 2010.", false, options);
        createOpts("rh", "registryHost", true, "RMI registry host. If not set using localhost.", false, options);
        createOpts("p", "port", true, "Port on which this node will be listening.", true, options);
        createOpts("d", "debug", false, "Option for debug mode. NOT YET WORKING", false, options);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        //parsing opts from args
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        String nodeTarget = cmd.getOptionValue("target");
        String nodeName = cmd.getOptionValue("name");
        int rmiRegPort;
        if (cmd.hasOption("registryPort")) {
            rmiRegPort = Integer.parseInt(cmd.getOptionValue("registryPort"));
        } else {
            rmiRegPort = 0;
        }
        String rmiRegHost = cmd.getOptionValue("registryHost");
        int port = Integer.parseInt(cmd.getOptionValue("port"));
//        System.out.println(nodeName);

        if (rmiRegPort == 0) {
            rmiRegPort = 1099;
        }
        if (rmiRegHost == null) {
            rmiRegHost = "localhost";
        }

        System.out.println(String.format("Using RMI Registry host: %s:%s", rmiRegHost, rmiRegPort));
        // getting RMI registry

        Registry registry = null;
        try {
            if (nodeTarget == null) {
                registry = LocateRegistry.createRegistry(rmiRegPort);
            } else {
                registry = LocateRegistry.getRegistry(rmiRegHost, rmiRegPort);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            System.err.println("Could not create or locate RMI Registry on given host and port.");
            System.exit(1);
        }


        Node node = new Node(nodeName, registry, port);
        System.out.println(String.format("Node %s is now running.", nodeName));
        if (nodeTarget == null) {
            node.becomeLeader();
        } else {
            node.connectToAnotherNode(nodeTarget);
        }
    }

    private void pushNodeToRegistry(int objectPort) {
        try {
            NodeInterface stub = (NodeInterface) UnicastRemoteObject.exportObject(this, objectPort);
            registry.rebind(name, stub);
            System.out.println("Node pushed into registry.");
        } catch (RemoteException e) {
            System.err.println("Something went wrong. Try it again.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void connectToAnotherNode(String nodeTarget) {
        try {
            NodeInterface hostNode = (NodeInterface) registry.lookup(nodeTarget);
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
    }

    @Override
    public void join(String name, NodeInterface node) throws RemoteException {
        System.out.println(String.format("Node %s is connecting.", name));
        node.changeLeader(this.leaderNode);
        node.changeNext(this.nextNode);
        this.nextNode.changePrev(node);
        node.changePrev(this);
        this.nextNode = node;
        node.printNeighbors();
        node.getNext().printNeighbors();
        if(!node.getNext().equals(node.getPrev())) node.getPrev().printNeighbors();
    }

    @Override
    public void changeNext(NodeInterface next) throws RemoteException {
        this.nextNode = next;
//        printNeighbors();
    }

    @Override
    public void changePrev(NodeInterface prev) throws RemoteException {
        this.prevNode = prev;
//        printNeighbors();
    }

    @Override
    public NodeInterface getPrev() throws RemoteException {
        return this.prevNode;
    }

    @Override
    public NodeInterface getNext() throws RemoteException {
        return this.nextNode;
    }

    @Override
    public NodeInterface getLeader() throws RemoteException {
        return this.leaderNode;
    }

    @Override
    public void connected(String name) throws RemoteException {
        System.out.println(String.format("Connected to node %s", name));
    }

    @Override
    public String getName() throws RemoteException {
        return this.name;
    }

    @Override
    public void printNeighbors() throws RemoteException {
        StringBuilder sb = new StringBuilder("\n");
        if(this.nextNode!=null){
            sb.append("Next: ").append(this.nextNode.getName()).append(", ");
        }
        if(this.prevNode!=null){
            sb.append("Prev: ").append(this.prevNode.getName()).append(", ");
        }
        if(this.leaderNode!=null){
            sb.append("Leader: ").append(this.leaderNode.getName());
        }
        System.out.println(sb.toString());
    }

    @Override
    public void changeLeader(NodeInterface leader) throws RemoteException {
        this.leaderNode = leader;
    }

}