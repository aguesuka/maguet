package cc.aguesuka.btfind.app.config;

import java.nio.file.Path;
import java.util.Properties;


/**
 * @author :aguesuka
 * 2020/2/22 10:52
 */
public class BootstrapConfig {
    private static final String DHT_DATA_PATH = "dhtDataPath";
    private static final String NODE_FILE_PATH = "nodeFilePath";

    private static final String DHT_CLIENT_PORT = "dhtClientPort";
    private static final String START_WITH_FIND_NODE = "startWithFindNode";

    private Path dhtDataPath;
    private Path nodeFilePath;

    private int dhtClientPort;
    private boolean startWithFindNode;

    public static BootstrapConfig of(Properties properties) {
        BootstrapConfig bootstrapConfig = new BootstrapConfig();
        bootstrapConfig.init(properties);
        return bootstrapConfig;
    }

    private void init(Properties properties) {
        String dhtDataPathStr = properties.getProperty(DHT_DATA_PATH);
        if (dhtDataPathStr != null && !dhtDataPathStr.isBlank()) {
            dhtDataPath = Path.of(dhtDataPathStr);
        }


        String nodeFilePathStr = properties.getProperty(NODE_FILE_PATH);
        if (nodeFilePathStr != null && !nodeFilePathStr.isBlank()) {
            nodeFilePath = Path.of(nodeFilePathStr);
        }



        String dhtClientPortStr = properties.getProperty(DHT_CLIENT_PORT);
        if (dhtClientPortStr != null && !dhtClientPortStr.isBlank()) {
            dhtClientPort = Integer.parseInt(dhtClientPortStr);
        }


        String startWithFindNodeStr = properties.getProperty(START_WITH_FIND_NODE);
        startWithFindNode = "true".equalsIgnoreCase(startWithFindNodeStr);
    }


    public Path getDhtDataPath() {
        return dhtDataPath;
    }

    public Path getNodeFilePath() {
        return nodeFilePath;
    }

    public int getDhtClientPort() {
        return dhtClientPort;
    }


    public boolean isStartWithFindNode() {
        return startWithFindNode;
    }


}
