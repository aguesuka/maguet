package cc.aguesuka.downloader;

/**
 * 获得种子文件中的info信息,保存文件
 *
 * @author :yangmingyuxing
 * 2019/7/25 11:00
 */
public interface IInfoHashSaver {
    /**
     * 根据种子信息保存种子
     *
     * @param infoHash 种子info hash
     * @param peerInfo 种子信息
     * @throws Exception 保存失败抛出异常
     */
    void save(byte[] infoHash, byte[] peerInfo) throws Exception;
}
