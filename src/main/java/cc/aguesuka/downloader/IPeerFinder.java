package cc.aguesuka.downloader;

/**
 * 从dht中找到有种子文件的地址,并从中下载,直到下载一份,并返回种子中的info信息
 *
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
     * @throws Exception dht网络中发生无法继续的异常或者未找到种子或者线程异常
     */
    byte[] findPeer(byte[] infoHash, IMetaDataDownloader metaDataDownloader) throws Exception;
}
