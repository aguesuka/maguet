package cc.aguesuka.maguet.util.bencode;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


/**
 * java object to bencode byte array
 *
 *
 * @author aguesuka
 */
class BencodeEncoder {
    private final Output output;

    BencodeEncoder(Output output) {
        this.output = output;
    }

    private void write(byte[] bytes) {
        try {
            writeByteArray(bytes);
        } catch (Exception e) {
            throw new BencodeException(e);
        }
    }

    private void writeByteArray(byte[] bytes) {
        output.writeByteArray(bytes);
    }

    private void writeByte(byte b) {
        output.writeByte(b);
    }



    private void putInteger(Number i) {
        writeByte(BencodeToken.INT);
        write(i.toString().getBytes(StandardCharsets.US_ASCII));
        writeByte(BencodeToken.END);
    }

    private void putString(String s) {
        putByteArray(s.getBytes(StandardCharsets.UTF_8));
    }

    private void putMap(Map<?, ?> map) {
        writeByte(BencodeToken.DICT);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            putString((String) entry.getKey());
            put(entry.getValue());
        }
        writeByte(BencodeToken.END);
    }

    private void putList(List<?> list) {
        writeByte(BencodeToken.LIST);
        for (Object o : list) {
            put(o);
        }
        writeByte(BencodeToken.END);
    }


    void put(Object o) {
        if (o == null) {
            throw new BencodeException("not supports null");
        }
        if (o instanceof Map) {
            putMap((Map<?, ?>) o);
        }  else if (o instanceof Number) {
            putInteger((Number) o);
        } else if (o instanceof List) {
            putList((List<?>) o);
        } else if (o instanceof String) {
            putByteArray(((String) o).getBytes(StandardCharsets.UTF_8));
        } else if (o instanceof byte[]) {
            putByteArray(((byte[]) o));
        } else {
            throw new BencodeException("can not encode:" + o.getClass());
        }
    }

    private void putByteArray(byte[] bytes) {
        write(Integer.toString(bytes.length).getBytes(StandardCharsets.US_ASCII));
        writeByte(BencodeToken.SPLIT);
        write(bytes);
    }

    interface Output{
        void writeByteArray(byte[] bytes);

        void writeByte(byte b);
    }

}