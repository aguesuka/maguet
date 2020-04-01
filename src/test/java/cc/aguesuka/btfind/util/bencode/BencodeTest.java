package cc.aguesuka.btfind.util.bencode;

import cc.aguesuka.btfind.util.HexUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author :yangmingyuxing
 * 2019/8/27 14:19
 */

public class BencodeTest {
    private List<byte[]> bencodeData;
    private List<Map<String, Object>> bencodeMapList;
    private int max;

    @Before
    public void setUp() throws Exception {

        String dataFile = "bencode.txt";
        URL resource = this.getClass().getClassLoader().getResource(dataFile);
        Objects.requireNonNull(resource);
        List<String> lines = Files.readAllLines(Paths.get(resource.toURI()));
        bencodeData = lines.stream().map(HexUtil::decode).collect(Collectors.toList());
        bencodeMapList = bencodeData.stream().map(b -> Bencode.parse(ByteBuffer.wrap(b))).collect(Collectors.toList());
        max = bencodeData.stream().mapToInt(a -> a.length).max().orElse(0);
    }

    @Test
    public void parse() {
        for (byte[] bytes : bencodeData) {
            Map<String, Object> parse = Bencode.parse(ByteBuffer.wrap(bytes));
            Assert.assertArrayEquals(bytes, Bencode.toBytes(parse));
        }
    }

    @Test
    public void parseBytes() {
        for (byte[] bytes : bencodeData) {
            Map<String, Object> parse = Bencode.parse(bytes);
            Assert.assertArrayEquals(bytes, Bencode.toBytes(parse));
        }
    }


    @Test
    public void writeToBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(max);
        for (Map<String, Object> bencodeMap : bencodeMapList) {
            buffer.clear();
            Bencode.writeTo(bencodeMap, buffer);
            buffer.flip();
            Assert.assertArrayEquals(Bencode.toBytes(bencodeMap), Bencode.toBytes(Bencode.parse(buffer)));
        }
    }

}
