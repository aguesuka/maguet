package cc.aguesuka.btfind.dht.model;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.*;

/**
 * @author :aguesuka
 * 2020/2/14 15:23
 */
public class DhtMessageUtil {
    private static DhtMessageUtil instance = new DhtMessageUtil();
    private SecureRandom random = new SecureRandom();

    private DhtMessageUtil() {
    }

    public static DhtMessageUtil instance() {
        return instance;
    }

    public byte[] randomId() {
        byte[] result = new byte[20];
        random.nextBytes(result);
        return result;
    }

    public Map<String, Object> findNode(byte[] id, byte[] target) {
        return baseMap("find_node", a(id, "target", target));
    }

    public Map<String, Object> getPeers(byte[] id, byte[] infoHash) {
        return baseMap("get_peers", a(id, "info_hash", infoHash));
    }

    private Map<String, Object> baseMap(String type, Map<String, Object> a) {
        byte[] t = new byte[2];
        random.nextBytes(t);
        // keep the order!
        Map<String, Object> baseMap = new LinkedHashMap<>(4);
        baseMap.put("a", a);
        baseMap.put("q", type);
        baseMap.put("t", t);
        baseMap.put("y", "q");
        return baseMap;
    }

    public Map<String, Object> ping(byte[] selfNodeId) {
        return baseMap("ping", a(selfNodeId));
    }

    private Map<String, Object> a(byte[] id) {
        return Collections.singletonMap("id", id);
    }

    private Map<String, Object> a(byte[] id, String key, byte[] value) {
        // keep the order!
        Map<String, Object> a = new LinkedHashMap<>(2);
        a.put("id", id);
        a.put(key, value);
        return a;
    }

    public List<DhtNode> parserNodes(byte[] nodes) {
        if (null == nodes || nodes.length == 0 || nodes.length % 26 != 0) {
            return Collections.emptyList();
        }
        ByteBuffer buffer = ByteBuffer.wrap(nodes);
        // new DhtNode will clone id
        byte[] id = new byte[20];
        // ip holder is int
        byte[] ip = new byte[4];
        List<DhtNode> result = new ArrayList<>(nodes.length / 26);
        while (buffer.hasRemaining()) {
            buffer.get(id);
            buffer.get(ip);
            int port = Short.toUnsignedInt(buffer.getShort());
            try {
                InetAddress ipAddress = Inet4Address.getByAddress(ip);
                result.add(new DhtNode(new InetSocketAddress(ipAddress, port), id));
            } catch (UnknownHostException e) {
                return Collections.emptyList();
            }
        }
        return result;
    }
}
