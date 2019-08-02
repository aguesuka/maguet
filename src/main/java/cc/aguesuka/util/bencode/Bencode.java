package cc.aguesuka.util.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author :yangmingyuxing
 * 2019/6/26 17:47
 */
public class Bencode {
    private static final byte END = 'e';
    private static final byte INT = 'i';
    private static final byte DICT = 'd';
    private static final byte LIST = 'l';
    private static final byte SPLIT = ':';
    private static final String NULL = "";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static BencodeMap parse(ByteBuffer buff) {
        return new BencodeDecode(buff).getNextMap();
    }

    public static byte[] toBytes(IBencode o) {
        BencodeEncode bencodeEncode = new BencodeEncode();
        bencodeEncode.putBencode(o);
        return bencodeEncode.outputStream.toByteArray();
    }

    private static class BencodeDecode {

        private ByteBuffer buff;

        BencodeDecode(ByteBuffer buff) {
            this.buff = buff;
        }

        private byte next() {
            buff.mark();
            byte b = buff.get();
            buff.reset();
            return b;
        }

        private IBencode getNext() {
            byte b = next();
            switch (b) {
                case DICT:
                    return getNextMap();
                case INT:
                    return getNextInt();
                case LIST:
                    return getNextList();
                default:
                    return getNextBytes();
            }
        }

        private void check(byte b) {
            if (b != buff.get()) {
                throw ex();
            }
        }

        private BencodeInteger getNextInt() {
            check(INT);
            int result = nextNums();
            check(END);
            return new BencodeInteger(result);
        }

        private BencodeList getNextList() {
            check(LIST);
            BencodeList result = new BencodeList();
            while (next() != END) {
                result.add(getNext());
            }
            check(END);
            return result;
        }

        private BencodeMap getNextMap() {
            check(DICT);
            BencodeMap result = new BencodeMap();
            while (next() != END) {
                result.put(getNextString(), getNext());
            }
            check(END);
            return result;
        }

        private String getNextString() {
            return new String(getNextBytes().getBytes());
        }

        private RuntimeException ex() {
            return new RuntimeException();
        }

        private int nextNums() {
            buff.mark();
            int i = 1;
            byte b = buff.get();
            char c = '-';
            if (!Character.isDigit(b) && b != c) {
                throw ex();
            }
            while (true) {
                byte codePoint = buff.get();
                if (!Character.isDigit(codePoint)) {
                    break;
                }
                i++;
            }
            buff.reset();
            byte[] bytes = new byte[i];
            buff.get(bytes);
            return Integer.parseInt(new String(bytes, StandardCharsets.US_ASCII));
        }

        private BencodeByteArray getNextBytes() {
            int len = nextNums();
            check(SPLIT);
            byte[] result = new byte[len];
            buff.get(result);
            return new BencodeByteArray(result);
        }
    }

    private static class BencodeEncode {
        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        private void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void putInt(int i) {
            outputStream.write(INT);
            write(Integer.toString(i).getBytes(UTF_8));
            outputStream.write(END);
        }

        private void putString(String s) {
            putByteArray(s.getBytes(UTF_8));
        }

        private void putMap(BencodeMap map) {
            outputStream.write(DICT);
            for (Map.Entry<String, IBencode> entry : map.entrySet()) {
                putString(entry.getKey());
                putBencode(entry.getValue());
            }
            outputStream.write(END);
        }

        private void putList(BencodeList list) {
            outputStream.write(LIST);
            for (IBencode o : list) {
                putBencode(o);
            }
            outputStream.write(END);
        }

        private void putBencode(IBencode o) {
            if (o == null) {
                putString(NULL);
                return;
            }
            if (o instanceof BencodeMap) {
                putMap((BencodeMap) o);
            } else if (o instanceof BencodeList) {
                putList((BencodeList) o);
            } else if (o instanceof BencodeInteger) {
                putInt(((BencodeInteger) o).getInt());
            } else if (o instanceof BencodeByteArray) {
                putByteArray(((BencodeByteArray) o).getBytes());
            } else {
                throw new RuntimeException("无法转换的类型:" + o.getClass());
            }
        }

        private void putByteArray(byte[] bytes) {
            write(Integer.toString(bytes.length).getBytes(UTF_8));
            outputStream.write(SPLIT);
            write(bytes);
        }
    }
}