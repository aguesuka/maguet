package cc.aguesuka.util.bencode;

/**
 * @author :yangmingyuxing
 * 2019/6/30 16:56
 */
public interface IBencode {
    /**
     * 转为bencode byte数组
     * @return bencode byte数组
     */
    default byte[] toBencodeBytes(){
        return Bencode.toBytes(this);
    }

}
