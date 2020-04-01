package cc.aguesuka.btfind.dht.dao;

import cc.aguesuka.btfind.dht.model.DhtMessageUtil;
import cc.aguesuka.btfind.dht.model.DhtNode;
import cc.aguesuka.btfind.util.bencode.Bencode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

/**
 * @author :yangmingyuxing
 * 2020/2/15 13:28
 */
public class DhtNodeDao {
    private static DhtNodeDao instance = new DhtNodeDao();

    private DhtNodeDao() {
        //no instance
    }

    public static DhtNodeDao instance() {
        return instance;
    }


    public void saveTo(List<DhtNode> nodes, Path path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(nodes.size() * 26);
        for (DhtNode node : nodes) {
            InetSocketAddress address = (InetSocketAddress) node.getAddress();
            buffer.put(node.getId());
            buffer.put(address.getAddress().getAddress());
            buffer.putShort((short) address.getPort());
        }
        Files.write(path, buffer.array(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
    }

    public List<DhtNode> fromNodeFile(Path path) throws IOException {
        byte[] nodes = Files.readAllBytes(path);
        return DhtMessageUtil.instance().parserNodes(nodes);
    }

    public List<DhtNode> fromDhtData(Path path) throws IOException {

        byte[] bytes = Files.readAllBytes(path);
        Map<String, Object> dhtData = Bencode.parse(bytes);
        byte[] nodes = (byte[]) dhtData.get("nodes");
        return DhtMessageUtil.instance().parserNodes(nodes);
    }

}
