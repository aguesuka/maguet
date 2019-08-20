package cc.aguesuka.util.bencode;

/**
 * @author :yangmingyuxing
 * 2019/6/30 17:25
 */
public interface IBencodeContainer<T> {
    /**
     * get 强转为 byte[]
     *
     * @param key 键
     * @return byte[]
     */
    byte[] getByteArray(T key);

    /**
     * get 强转为long
     *
     * @param key 键
     * @return int
     */
    long getLong(T key);

    /**
     * get 强转为BencodeMap
     *
     * @param key 键
     * @return BencodeMap
     */
    BencodeMap getBencodeMap(T key);

    /**
     * get 强转为 BencodeList
     *
     * @param key 键
     * @return BencodeList
     */
    BencodeList getBencodeList(T key);

}
