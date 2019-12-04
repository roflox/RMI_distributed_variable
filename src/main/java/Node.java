import org.apache.commons.cli.*;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

public class Node implements NodeInterface {

    Node nextNode;
    Node prevNode;
    Node leaderNode;
    Set<NodeInterface> allNodes = new HashSet<>();


    /**
     * @param args nastaveno jako konzolová aplikace, která přijme options
     *             -t --target cílový node, ke kterému se má nově spuštěný node připojit
     *             -n --name jméno nově vzniklého nodu
     */
    public static void main(String[] args) {
//        System.out.println("Hello World!");
//        Arrays.stream(args).forEach(System.out::println);
        Options options = new Options();
        Option targetOption = new Option("t", "target", true, "Target node name.");
        targetOption.setRequired(false);
        options.addOption(targetOption);

        Option nameOption = new Option("n", "name", true, "Name of your node, which will be written into RMI registry.");
        nameOption.setRequired(true);
        options.addOption(nameOption);

        Option registryPortOption = new Option("p", "portRegistry", true, "RMI registry port. If not set using port 2010.");
        registryPortOption.setRequired(false);
        options.addOption(registryPortOption);

        Option registryHostOption = new Option("h", "hostRegistry", true, "RMI registry host. If not set using localhost.");
        registryHostOption.setRequired(false);
        options.addOption(registryHostOption);

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
        String rmiRegPort = cmd.getOptionValue("portRegistry");
        String rmiRegHost = cmd.getOptionValue("hostRegistry");
        System.out.println(nodeName);

        if (rmiRegPort == null) {
            rmiRegPort = "2010";
        }
        if (rmiRegHost == null) {
            rmiRegHost = "localhost";
        }

        System.out.println(String.format("Using RMI Registry host: %s:%s", rmiRegHost, rmiRegPort));

        Node node = new Node();


        //adding node to RMI registry
        try {
            NodeInterface stub =
                    (NodeInterface) UnicastRemoteObject.exportObject(node, 50000);
            Registry registry = LocateRegistry.createRegistry(Integer.parseInt(rmiRegPort));
            registry.rebind(nodeName, stub);
        } catch (NumberFormatException e) {
            System.err.println("Port must be an Integer!");
        } catch (Exception e) {
            // Something is wrong ...
            System.err.println("Server - something is wrong: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(String.format("Node %s is now running.", nodeName));
        if (nodeTarget == null) {
            node.becomeLeader();
        } else {
            node.connectToAnotherNode(nodeTarget);
        }
    }

    private void connectToAnotherNode(String nodeTarget) {

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

    }

    @Override
    public void changeNext(NodeInterface next) throws RemoteException {
    }

    @Override
    public void changePrev(NodeInterface prev) throws RemoteException {

    }

}