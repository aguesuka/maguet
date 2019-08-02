package cc.aguesuka.downloader;

/**
 * @author :yangmingyuxing
 * 2019/7/25 11:14
 */
public interface IPeerFinder {
    /**
     * 寻找peer地址,并使用种子信息下载者下载
     *
     * @param infoHash           种子 info hash
     * @param metaDataDownloader 种子信息下载者
     * @return 种子信息
     * @throws Exception dht网络中发生无法继续的异常,未找到种子,线程异常
     */
    byte[] findPeer(byte[] infoHash, IMetaDataDownloader metaDataDownloader) throws Exception;
}
