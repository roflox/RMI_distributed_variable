package nodes;

import org.apache.commons.cli.*;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * tato třída je zde pouze abych neměl "bordel" v nodes.NodeImpl :)
 */
public class ConsoleArgumentParser {

    static Map<String, Object> parse(String[] args) {
        Options options = new Options();
        createOptions("n", "name", true, "Name of your node, which will be written into RMI registry. REQUIRED", true, options);
        createOptions("h", "hostname", true, "Ip address of current node. It is recommended to be used, remote nodes may not connect.", false, options);
        createOptions("p", "port", true, "Port on which this node will be listening. REQUIRED", true, options);
        createOptions("r", "registryPort", true, "Port on which this node will be listening.", true, options);
        createOptions("t", "target", true, "Target node name.", false, options);
        createOptions("A", "targetRegistryAddress", true, "Address of target's node RMI registry. If not set using localhost.", false, options);
        createOptions("P", "targetRegistryPort", true, "Port of target's node RMI registry. REQUIRED when -t --target is present.", false, options);
        createOptions("d", "debug", false, "Option for debug mode.", false, options);
        createOptions("D", "development", false, "Option for development debugging.", false, options);
        createOptions("f", "fun", false, "For fun behaviour, only works without debug.", false, options);


        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        //parsing opts from args
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            formatter.printHelp("DSV_Node [OPTION]...", options);
            System.exit(1);
        }

        Map<String, Object> map = new HashMap<>();
        map.put("target", cmd.getOptionValue("target"));
        if (cmd.hasOption("target") && !cmd.hasOption("targetRegistryPort")) {
            System.err.println("When target option is used you must enter targetRegistryPort");
            formatter.printHelp("DSV_Node [OPTION]...", options);
            System.exit(1);
        }
        if (!cmd.hasOption("hostname")) {

            try (final DatagramSocket socket = new DatagramSocket()) {
                //otevře socket a získá lokální ip adresu, přes kterou se připojil na dns googlu
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                String ip = socket.getLocalAddress().getHostAddress();
                socket.disconnect();
//                System.out.println(ip);
                map.put("hostname", ip);
            } catch (SocketException | UnknownHostException e) {
                System.err.println("Local address was not provided, and 8.8.8.8 is not reachable. Using default 127.0.0.1");
                map.put("hostname", "127.0.0.1");
            }
        }else {
            map.put("hostname", cmd.getOptionValue("registryAddress"));
        }
        map.put("nodeName", cmd.getOptionValue("name"));
        map.put("targetRegistryAddress", cmd.getOptionValue("targetRegistryAddress") == null ? "localhost" : cmd.getOptionValue("targetRegistryAddress"));
        map.put("debug", cmd.hasOption("debug"));
        map.put("development", cmd.hasOption("development"));
        map.put("fun", cmd.hasOption("fun"));

        try {
            map.put("port", Integer.parseInt(cmd.getOptionValue("port")));
            map.put("registryPort", Integer.parseInt(cmd.getOptionValue("registryPort")));
            if (cmd.hasOption("targetRegistryPort")) {
                map.put("targetRegistryPort", Integer.parseInt(cmd.getOptionValue("targetRegistryPort")));
            } else {
                map.put("targetRegistryPort", 1099);
            }
        } catch (NumberFormatException e) {
//            LOG.error("Port and rmiRegPort must be an integer!");
            System.exit(1);
        }
        return map;
    }

    /**
     * jenom pro zjednodušení abych to furt nevypisoval jak blbec
     */
    private static void createOptions(String opt, String longOpt, boolean hasArg, String description,
                                      boolean required, Options options) {
        Option option = new Option(opt, longOpt, hasArg, description);
        option.setRequired(required);
        options.addOption(option);
    }
}
