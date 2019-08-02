package cc.aguesuka.util.bencode;

import java.util.ArrayList;

/**
 * @author :yangmingyuxing
 * 2019/6/30 16:58
 */
public class BencodeList extends ArrayList<IBencode> implements IBencode, IBencodeContainer<Integer>{
    public void addInt(int i){
        add(new BencodeInteger(i));
    }

    public void addByteArray(byte[] bytes) {
        add(new BencodeByteArray(bytes));
    }
    @Override
    public byte[] getByteArray(Integer key) {
        return ((BencodeByteArray) get(key)).getBytes();
    }

    @Override
    public int getInt(Integer key) {
        return ((BencodeInteger) get(key)).getInt();
    }

    @Override
    public BencodeMap getBencodeMap(Integer key) {
        return ((BencodeMap) get(key));
    }

    @Override
    public BencodeList getBencodeList(Integer key) {
        return ((BencodeList) get(key));
    }
}
