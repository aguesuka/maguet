package cc.aguesuka.btfind.util.bencode;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author :yangmingyuxing
 * 2019/9/3 20:09
 */
class BencodeParser {
    private Map<String, Object> rootMap = new LinkedHashMap<>();
    private Input input;
    private boolean isReadMap = true;
    private Deque<Object> stack = new ArrayDeque<>();

    BencodeParser(Input input) {
        this.input = input;
    }


    Map<String, Object> parse() {
        try {
            stack.add(rootMap);
            if (byteFromBuffer() != BencodeToken.DICT) {
                throw ex("not a bencode dict");
            }
            while (!stack.isEmpty()) {
                Object last = stack.getLast();
                if (isReadMap) {
                    String key = readStringOrEnd();
                    if (key != null) {
                        Object bencodeObject = readObject();
                        Objects.requireNonNull(bencodeObject);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) last;
                        map.put(key, bencodeObject);
                    }
                } else {
                    Object bencode = readObject();
                    if (bencode != null) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) last;
                        list.add(bencode);
                    }
                }
            }
            return rootMap;
        } catch (BencodeException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new BencodeException(e);
        }
    }

    private void onEnd() {
        stack.removeLast();
        isReadMap = stack.peekLast() instanceof Map;
    }

    private Object readObject() {
        byte firstByte = byteFromBuffer();
        switch (firstByte) {
            case BencodeToken.END:
                onEnd();
                return null;
            case BencodeToken.DICT:
                Map<String, Object> bencodeMap = new LinkedHashMap<>();
                stack.addLast(bencodeMap);
                isReadMap = true;
                return bencodeMap;
            case BencodeToken.INT:
                return readIntegerAndCheckEnd(byteFromBuffer(), BencodeToken.END);
            case BencodeToken.LIST:
                List<Object> bencodeList = new ArrayList<>();
                stack.addLast(bencodeList);
                isReadMap = false;
                return bencodeList;
            default:
                return readBytesAndCheckEnd(firstByte);
        }
    }

    /**
     * 只有map的key是String
     * 在bencode中是 数字(较短,所以可以用int) + 冒号 + byte[] 类型;
     * 编码格式几乎都是ascii,不过为了保险依然使用utf-8
     *
     * @return String ,如果结束,则返回null
     */
    private String readStringOrEnd() {
        byte b = byteFromBuffer();
        if (b == BencodeToken.END) {
            onEnd();
            return null;
        } else {
            return new String(readBytesAndCheckEnd(b), StandardCharsets.UTF_8);
        }
    }

    private byte[] readBytesAndCheckEnd(byte firstByte) {
        long length = readIntegerAndCheckEnd(firstByte, BencodeToken.SPLIT);
        if (length < 0) {
            throw ex("length of str  < 0");
        }
        return byteArrayFromBuffer((int) length);
    }

    private long readIntegerAndCheckEnd(byte firstByte, byte endChar) {

        if (!Character.isDigit(firstByte) && firstByte != '-') {
            throw ex("there must be digit or char '-' ");
        }
        char[] chars = new char[16];
        int index = 1;
        chars[0] = (char) firstByte;
        while (true) {
            byte codePoint = byteFromBuffer();
            if (!Character.isDigit(codePoint)) {
                if (codePoint != endChar) {
                    throw ex("there must be digit");
                }
                break;
            }
            // 扩容
            if (chars.length <= index) {
                char[] temp = new char[chars.length * 2];
                System.arraycopy(chars, 0, temp, 0, chars.length);
                chars = temp;
            }
            chars[index] = (char) codePoint;
            index++;
        }
        return Long.parseLong(new String(chars, 0, index));
    }

    private BencodeException ex(String msg) {
        return new BencodeException(msg);
    }

    private byte byteFromBuffer() {
        return input.getByte();
    }


    private byte[] byteArrayFromBuffer(int length) {
        return input.getBytes(length);
    }

    public interface Input {
        byte getByte();

        byte[] getBytes(int length);
    }


}
