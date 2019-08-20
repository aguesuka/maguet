package cc.aguesuka.run;

import cc.aguesuka.downloader.IDownloadInfoHash;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.inject.Injector;
import cc.aguesuka.util.log.LogSetting;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author :yangmingyuxing
 * 2019/8/2 12:46
 */
public class Main {

    public static void main(String[] args) throws Exception {
        String infoHash;
        if (args.length == 0) {
            infoHash = "5A1A7D6EEB329F9833FFD8CF6003547478BF4795";
            ;
        } else {
            infoHash = args[0];
        }
        try (Injector injector = BeansFactory.createInjector()) {
            IDownloadInfoHash downloadInfoHash = injector.instanceByClass(IDownloadInfoHash.class);
            downloadInfoHash.download(HexUtil.decode(infoHash));
        } catch (Exception e) {
            Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }
}
