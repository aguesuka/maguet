package cc.aguesuka.run;

import cc.aguesuka.dht.action.DhtAction;
import cc.aguesuka.dht.connection.Bucket;
import cc.aguesuka.dht.connection.DhtConnectionImpl;
import cc.aguesuka.dht.connection.DhtHandler;
import cc.aguesuka.downloader.impl.DoMetaDataDownLoader;
import cc.aguesuka.downloader.impl.DownloadInfoHash;
import cc.aguesuka.downloader.impl.InfoHashSaver;
import cc.aguesuka.downloader.impl.PeerFinder;
import cc.aguesuka.util.inject.Injector;
import cc.aguesuka.util.log.LogSetting;

/**
 * @author :yangmingyuxing
 * 2019/7/25 18:19
 */
@SuppressWarnings("WeakerAccess")
public class BeansFactory {
    public final static Class<?>[] DHT_CLASSES = new Class<?>[]{
            Bucket.class, DhtConnectionImpl.class,
            DhtHandler.class, DhtAction.class,
            DoMetaDataDownLoader.class
    };

    public final static Class<?>[] DOWNLOADER_CLASSES = new Class<?>[]{
            DoMetaDataDownLoader.class, DownloadInfoHash.class, InfoHashSaver.class, PeerFinder.class
    };

    public final static Class<?>[] UTIL_CLASSES = new Class<?>[]{LogSetting.class};
    public final static String CONFIG_FILE = "config.properties";

    public static Injector createInjector() {
        return new Injector().addProperties(CONFIG_FILE).addClass(UTIL_CLASSES)
                .addClass(DHT_CLASSES).addClass(DOWNLOADER_CLASSES).build();
    }
}
