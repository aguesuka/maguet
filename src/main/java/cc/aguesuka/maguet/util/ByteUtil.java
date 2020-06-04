package cc.aguesuka.maguet.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * byte util
 *
 * @author aguesuka
 */
public final class ByteUtil {
    private ByteUtil() {
        //no instance
    }

    public static byte[] sha1(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            return messageDigest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] xor(byte[] a, byte[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException();
        }
        byte[] result = new byte[a.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

}

