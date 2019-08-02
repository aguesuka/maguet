package cc.aguesuka.downloader;

/**
 * 根据info hash下载种子文件并保存
 *
 * @author :yangmingyuxing
 * 2019/7/25 10:56
 */
public interface IDownloadInfoHash {
    /**
     * 根据磁力链接下载种子文件
     *
     * @param infoHash 磁力链接中的info hash
     * @throws Exception 下载失败时抛出异常
     */
    void download(byte[] infoHash) throws Exception;

}
