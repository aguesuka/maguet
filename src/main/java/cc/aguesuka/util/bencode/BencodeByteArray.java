package cc.aguesuka.util.bencode;


import cc.aguesuka.util.ByteUtil;
import cc.aguesuka.util.HexUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * @author :yangmingyuxing
 * 2019/6/30 16:59
 */
public class BencodeByteArray  implements IBencode, Comparable<BencodeByteArray> {
    private byte[] data;

    public BencodeByteArray(byte[] data) {
        this.data = data;
    }

    public byte[] getBytes() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BencodeByteArray)) {
            return false;
        }
        BencodeByteArray that = (BencodeByteArray) o;
        return Arrays.equals(getBytes(), that.getBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getBytes());
    }

    @Override
    public String toString() {
        if (ByteUtil.isUtf8(ByteBuffer.wrap(getBytes()))) {
            return new String(getBytes(), StandardCharsets.UTF_8);
        }
        return HexUtil.encode(getBytes());
    }

    @Override
    public int compareTo(BencodeByteArray o) {
        return ByteUtil.compareBytes(this.getBytes(), o.getBytes());
    }
}
