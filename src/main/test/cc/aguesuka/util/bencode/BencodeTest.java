package cc.aguesuka.util.bencode;

import cc.aguesuka.util.HexUtil;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;

/**
 * @author :yangmingyuxing
 * 2019/8/20 00:07
 */
public class BencodeTest {

    @Test
    public void test() {
        String message = "64313A65693065313A6D6431313A75706C6F61645F6F6E6C7969336531323A75745F686F6C6570756E636869346531313A75745F6D65746164617461693265363A75745F70657869316565313A7069313032393365343A726571716932353565313A7631313A372E31302E33352E33363665";
        byte[] old = HexUtil.decode(message);
        BencodeMap parse = Bencode.parse(ByteBuffer.wrap(old));
        Assert.assertArrayEquals(old, parse.toBencodeBytes());
    }
}