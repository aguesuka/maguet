package cc.aguesuka.downloader;

import cc.aguesuka.downloader.impl.DoMetaDataDownLoader;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.inject.Injector;
import cc.aguesuka.util.log.LogSetting;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * @author :yangmingyuxing
 * 2019/7/23 23:49
 */
public class DoMetaDataDownLoaderTest {

    @Test
    public void downloadMataData() throws IOException {
        Injector injector = new Injector();
        injector.addClass(LogSetting.class, DoMetaDataDownLoader.class);
        injector.addProperties("config.properties");
        injector.build();
        DoMetaDataDownLoader doMetaDataDownLoad = injector.instanceByClass(DoMetaDataDownLoader.class);
        InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("61.228.10.32"), 18837);
        byte[] target = HexUtil.decode("879377BDC35EC3E4C1CC5C1F0C14476B3E67384B");
        doMetaDataDownLoad.downloadMataData(target, address);
    }
}