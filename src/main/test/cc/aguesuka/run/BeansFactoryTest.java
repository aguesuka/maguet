package cc.aguesuka.run;

import cc.aguesuka.downloader.IDownloadInfoHash;
import cc.aguesuka.downloader.impl.DoMetaDataDownLoader;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.inject.Injector;
import cc.aguesuka.util.inject.annotation.Config;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author :yangmingyuxing
 * 2019/7/25 18:35
 */
public class BeansFactoryTest {

    @Test
    public void createInjector() throws Exception {
        try (Injector injector = BeansFactory.createInjector()) {
            Assert.assertEquals(injector.instanceByClass(DoMetaDataDownLoader.class).getClass(),DoMetaDataDownLoader.class );
        }
    }

    @Test
    public void download() throws Exception {
        try (Injector injector = BeansFactory.createInjector()) {
            IDownloadInfoHash downloadInfoHash = injector.instanceByClass(IDownloadInfoHash.class);
            //magnet:?xt=urn:btih:879377BDC35EC3E4C1CC5C1F0C14476B3E67384B
            //magnet:?xt=urn:btih:FF2FC78DD98B75810F57AEF8A856EE9743B1DC55&dn=CAWD-001.mp4&tr=udp%3a%2f%2ftracker.openbittorrent.com%3a80%2fannounce
            downloadInfoHash.download(HexUtil.decode("418a01fd853229b551e392e449ed4ddab1c53d68"));
        }

    }
}