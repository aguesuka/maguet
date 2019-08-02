package cc.aguesuka.downloader;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 从指定地址下载指定种子文件
 *
 * @author :yangmingyuxing
 * 2019/7/14 22:46
 */
public interface IMetaDataDownloader {
    /**
     * 下载元文件
     *
     * @param address  peer地址
     * @param infoHash 种子hash值
     * @return 种子文件的info信息
     * @throws IOException 下载失败
     */
    byte[] downloadMataData(byte[] infoHash, InetSocketAddress address) throws IOException;

}
