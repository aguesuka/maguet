package cc.aguesuka.util.bencode;

/**
 * 整数类型,实际取值范围和java int 不同
 *
 * @author :yangmingyuxing
 * 2019/6/30 17:02
 */
public class BencodeInteger implements IBencode {
    /**
     * 数据类型从int改为long,大文件用int类型表示会溢出
     */
    private long data;

    public BencodeInteger(long data) {
        this.data = data;
    }

    public int getInt() {
        return (int) data;
    }

    public long getLong() {
        return data;
    }

    @Override
    public String toString() {
        return Long.toString(data);
    }
}
