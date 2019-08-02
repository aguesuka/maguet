package cc.aguesuka.util.bencode;

/**
 * @author :yangmingyuxing
 * 2019/6/30 17:02
 */
public class BencodeInteger implements IBencode {
    private int data;
    public int getInt(){
        return data;
    }

    public BencodeInteger(int data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return Integer.toString(data);
    }
}
