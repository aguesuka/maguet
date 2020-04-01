package cc.aguesuka.btfind.app.config;

import cc.aguesuka.btfind.dht.model.DhtMessageUtil;
import cc.aguesuka.btfind.util.HexUtil;

import java.nio.file.Path;
import java.util.Properties;

/**
 * @author :yangmingyuxing
 * 2020/2/22 10:52
 */
public class ApplicationConfig {
    private static final String METADATA_DIR_PATH = "metadataDirPath";
    private static final String SELF_NODE_ID = "selfNodeId";
    private static final String INFO_HASH_PATH = "infoHashPath";
    private Path metadataDirPath;
    private byte[] selfNodeId = DhtMessageUtil.instance().randomId();
    private Path infoHashPath;

    public static ApplicationConfig of(Properties properties) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.init(properties);
        return applicationConfig;
    }

    private void init(Properties properties) {
        String metadataDirPathStr = properties.getProperty(METADATA_DIR_PATH);
        if (metadataDirPathStr != null && !metadataDirPathStr.isBlank()) {
            metadataDirPath = Path.of(metadataDirPathStr);
        }
        if (properties.containsKey(SELF_NODE_ID)) {
            selfNodeId = HexUtil.decode(properties.getProperty(SELF_NODE_ID));
            if (selfNodeId.length != 20) {
                throw new IllegalArgumentException("selfNodeId must be hex of bytes length is 20");
            }
        }
        String infoHashPathStr = properties.getProperty(INFO_HASH_PATH);
        if (infoHashPathStr != null && !infoHashPathStr.isBlank()) {
            infoHashPath = Path.of(infoHashPathStr);
        }
    }

    public Path getInfoHashPath() {
        return infoHashPath;
    }


    public byte[] getSelfNodeId() {
        return selfNodeId;
    }


    public Path getMetadataDirPath() {
        return metadataDirPath;
    }
}
