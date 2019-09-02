package cc.aguesuka.util;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Random;

/**
 * @author :yangmingyuxing
 * 2019/7/12 09:35
 */
public class HexUtilTest {

    @Test
    public void litterCaseTest() {
        String s = "4e2405dbde0045f3fa341875913908e84ca0a24c";
        byte[] target = HexUtil.decode(s);
        Assert.assertEquals(s.toUpperCase(), HexUtil.encode(target));
    }

    @Test
    public void encode() {
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 1000; j++) {
                byte[] test = new byte[j];
                random.nextBytes(test);
                assert Arrays.equals(test, HexUtil.decode(HexUtil.encode(test)));
            }
        }
    }
}