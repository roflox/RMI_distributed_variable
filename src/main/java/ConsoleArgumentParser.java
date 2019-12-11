import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

/**
 * tato třída je zde pouze abych neměl "bordel" v NodeImpl :)
 */
public class ConsoleArgumentParser {

    static Map<String, Object> parse(String[] args) {
        Options options = new Options();
        createOpts("n", "name", true, "Name of your node, which will be written into RMI registry. REQUIRED", true, options);
        createOpts("p", "port", true, "Port on which this node will be listening. REQUIRED", true, options);
        createOpts("r", "registryPort", true, "Port on which this node will be listening.", true, options);
        createOpts("t", "target", true, "Target node name.", false, options);
        createOpts("a", "targetRegistryAddress", true, "Address of target's node RMI registry. If not set using localhost.", false, options);
        createOpts("P", "targetRegistryPort", true, "Port of target's node RMI registry. If not set using port 2010.", false, options);
        createOpts("d", "debug", false, "Option for debug mode.", false, options);
        createOpts("D", "development", false, "Option for development debugging.", false, options);

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

        map.put("nodeName", cmd.getOptionValue("name"));
        map.put("targetRegistryAddress", cmd.getOptionValue("targetRegistryAddress") == null ? "localhost" : cmd.getOptionValue("targetRegistryAddress"));
        map.put("debug", cmd.hasOption("debug"));
        map.put("development", cmd.hasOption("development"));
        try {
            map.put("port", Integer.parseInt(cmd.getOptionValue("port")));
            map.put("registryPort",Integer.parseInt(cmd.getOptionValue("registryPort")));
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
    private static void createOpts(String opt, String longOpt, boolean hasArg, String description,
                                   boolean required, Options options) {
        Option option = new Option(opt, longOpt, hasArg, description);
        option.setRequired(required);
        options.addOption(option);
    }
}
