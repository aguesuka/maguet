package cc.aguesuka.btfind.app;

import cc.aguesuka.btfind.app.config.ApplicationConfig;
import cc.aguesuka.btfind.app.config.BootstrapConfig;
import cc.aguesuka.btfind.command.ConsoleListener;
import cc.aguesuka.btfind.command.ForShell;
import cc.aguesuka.btfind.command.ReflectCommandHandler;
import cc.aguesuka.btfind.dht.model.DhtMessage;
import cc.aguesuka.btfind.dht.model.DhtMessageUtil;
import cc.aguesuka.btfind.dht.model.DhtNode;
import cc.aguesuka.btfind.dht.model.DhtNodeInfo;
import cc.aguesuka.btfind.dht.routingtable.RoutingTable;
import cc.aguesuka.btfind.dispatch.EventLoop;
import cc.aguesuka.btfind.metadata.MetadataDownloadTask;
import cc.aguesuka.btfind.util.TimeoutListener;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author :aguesuka
 * 2020/2/19 17:46
 */
public class Bootstrap {
    private Application application;
    private Properties properties;

    private Bootstrap(Properties properties) {
        Objects.requireNonNull(properties);
        this.properties = properties;
    }

    private static void initRoutingTable(Application application, byte[] selfNodeId,
                                         Collection<? extends SocketAddress> addresses) {
        DhtMessageUtil messageUtil = application.getMessageUtil();
        RoutingTable routingTable = application.getRoutingTable();
        for (SocketAddress address : addresses) {
            Map<String, Object> queryData = messageUtil.findNode(selfNodeId, messageUtil.randomId());
            DhtMessage message = new DhtMessage(address, queryData);
            application.getQuerySender().send(message, dhtMessage -> {
                byte[] id = dhtMessage.id();
                if (id == null) {
                    return;
                }
                routingTable.putNodeWithResponse(new DhtNode(dhtMessage.getAddress(), id));
                messageUtil.parserNodes(dhtMessage.nodes()).forEach(routingTable::putNode);
            });
        }
    }

    private static void initRoutingTable(RoutingTable routingTable, Collection<DhtNode> nodes) {
        nodes.forEach(routingTable::putNode);
    }

    private static void initCommand(Application application) {
        ConsoleListener consoleListener = new ConsoleListener();
        application.getEventLoop().interval(0, () -> true, consoleListener::run);
        Consumer<String> quit = p -> System.exit(0);
        // command quit
        consoleListener.addCommand("quit", "quit", "quit", quit);
        consoleListener.addCommand("stop", "stop", "equals quit", quit);

        ReflectCommandHandler reflectCommandHandler = new ReflectCommandHandler(application);
        consoleListener.addCommand("field", "field[objectPath]", "object fields ",
                reflectCommandHandler::field);
        consoleListener.addCommand("select", "select[objectPath[method]]",
                "select object, then call method. if method not empty call toString.", reflectCommandHandler::select);
        consoleListener.addCommand("method", "method[objectPath]",
                "object non param method ", reflectCommandHandler::method);

        Consumer<String> doSaveNodes = p -> {
            Path path = Path.of(p);
            List<DhtNode> goodNodes = application.getRoutingTable().nodes().stream()
                    .filter(dhtNodeInfo -> dhtNodeInfo.getRecord().getRecentSuccessCount() > 0)
                    .map(DhtNodeInfo::getNode).collect(Collectors.toList());
            try {
                application.getNodeDao().saveTo(goodNodes, path);
                System.out.println(String.format("save %d nodes in the file %s ",
                        goodNodes.size(), path.toAbsolutePath()));
            } catch (IOException | RuntimeException e) {
                System.out.println("err " + e.getMessage());
            }
        };

        consoleListener.addCommand("saveNodes", "saveNodes[filePath]",
                "save good nodes to file", doSaveNodes);
        consoleListener.start();
    }

    private static Properties getProperties(List<String> propertiesNames) throws IOException {
        Properties properties = new Properties();
        for (String propertiesName : propertiesNames) {
            Path p = Path.of(propertiesName);
            if (Files.exists(p)) {
                properties.load(Files.newBufferedReader(p));
                return properties;
            }
            URL resource = Bootstrap.class.getClassLoader().getResource(propertiesName);
            if (resource != null) {
                properties.load(resource.openStream());
                return properties;
            }
        }
        throw new IllegalArgumentException("cannot find properties" + String.join(" or ", propertiesNames));
    }

    public static void main(String[] args) throws IOException {
        List<String> propertiesNames = List.of("config.properties", "example-config.properties");
        Properties properties = getProperties(propertiesNames);
        new Bootstrap(properties).start();
    }

    /**
     * get Application for JShell
     *
     * @return ApplicationFactory
     */
    @ForShell
    public Application getApplication() {
        return application;
    }

    public void start() {
        EventLoop.run(eventLoop -> {
            // create config
            ApplicationConfig applicationConfig = ApplicationConfig.of(properties);
            BootstrapConfig bootstrapConfig = BootstrapConfig.of(properties);


            // create applicationFactory
            int dhtClientPort = bootstrapConfig.getDhtClientPort();
            application = new Application(eventLoop, dhtClientPort, applicationConfig);


            // load nodes from nodes.dat file
            List<DhtNode> nodes = new ArrayList<>();
            Path nodeFilePath = bootstrapConfig.getNodeFilePath();
            if (nodeFilePath != null && Files.exists(nodeFilePath)) {
                List<DhtNode> dhtNodes = application.getNodeDao().fromNodeFile(nodeFilePath);
                nodes.addAll(dhtNodes);
            }

            // load nodes from dht.dat file
            Path dhtDataPath = bootstrapConfig.getDhtDataPath();
            if (dhtDataPath != null && Files.exists(dhtDataPath)) {
                nodes.addAll(application.getNodeDao().fromDhtData(dhtDataPath));
            }
            if (nodes.isEmpty()) {
                throw new IllegalArgumentException("there is not bootstrap nodes");
            }

            // add nodes to routingTable
            if (bootstrapConfig.isStartWithFindNode()) {
                /// init routing table by send find node query
                List<SocketAddress> addresses = nodes.stream().map(DhtNode::getAddress).collect(Collectors.toList());
                initRoutingTable(application, applicationConfig.getSelfNodeId(), addresses);
            } else {
                /// init routing table by old id(if node changed,it will add into blacklist)
                initRoutingTable(application.getRoutingTable(), nodes);
            }


            // interval of refresh routingTable TimeoutMap
            eventLoop.interval(3000, () -> true, () -> {
                application.getRoutingTable().refresh();
                application.getQuerySender().refresh();
            });

            eventLoop.timeout(0, new TimeoutListener() {
                int count = 256;

                @Override
                public void timeout() {
                    new MetadataDownloadTask(application);
                    if (--count > 0) {
                        eventLoop.timeout(0, this);
                    }
                }
            });


            // init command
            initCommand(application);
        });
    }

}
