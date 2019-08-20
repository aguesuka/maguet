package cc.aguesuka.util.bencode;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/**
 * @author :yangmingyuxing
 * 2019/6/30 16:57
 */
public class BencodeMap extends LinkedHashMap<String, IBencode> implements IBencode ,IBencodeContainer<String>{
    public void putByteArray(String key, byte[] value) {
        put(key, new BencodeByteArray(value));
    }
    public void putString(String key,String value){
        putByteArray(key, value.getBytes(StandardCharsets.UTF_8));
    }
    public void putLong(String key, long value) {
        put(key, new BencodeInteger(value));
    }

    @Override
    public byte[] getByteArray(String key) {
        return ((BencodeByteArray) get(key)).getBytes();
    }

    @Override
    public long getLong(String key) {
        return ((BencodeInteger) get(key)).getLong();
    }

    @Override
    public BencodeMap getBencodeMap(String key) {
        return ((BencodeMap) get(key));
    }

    @Override
    public BencodeList getBencodeList(String key) {
        return ((BencodeList) get(key));
    }
}
