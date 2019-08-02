package cc.aguesuka.run;

import cc.aguesuka.downloader.IDownloadInfoHash;
import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.inject.Injector;

/**
 * @author :yangmingyuxing
 * 2019/8/2 12:46
 */
public class Main {
    public static void main(String[] args) throws Exception {
        String infoHash;
        if (args.length == 0) {
            infoHash = "FF2FC78DD98B75810F57AEF8A856EE9743B1DC55";
            ;
        } else {
            infoHash = args[0];
        }

        try (Injector injector = BeansFactory.createInjector()) {
            IDownloadInfoHash downloadInfoHash = injector.instanceByClass(IDownloadInfoHash.class);
            downloadInfoHash.download(HexUtil.decode(infoHash));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
