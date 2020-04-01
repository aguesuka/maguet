package cc.aguesuka.btfind.dht.model;

import cc.aguesuka.btfind.util.bencode.Bencode;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * @author :yangmingyuxing
 * 2020/2/13 10:17
 */
public class DhtMessage {
    private final SocketAddress address;
    private final Map<String, Object> data;

    public DhtMessage(SocketAddress address, Map<String, Object> data) {
        this.address = address;
        this.data = data;
    }

    public SocketAddress getAddress() {
        return address;
    }

    public Map<String, Object> getData() {
        return data;
    }

    private Map<String, Object> r() {
        @SuppressWarnings("unchecked")
        Map<String, Object> r = (Map<String, Object>) data.get("r");
        return r;
    }

    public byte[] id() {
        Map<String, Object> r = r();
        return null == r ? null : (byte[]) r.get("id");
    }

    public byte[] nodes() {
        Map<String, Object> r = r();
        if (null == r) {
            return null;
        }
        return (byte[]) r.get("nodes");
    }

    public List<InetSocketAddress> values() {
        Map<String, Object> r = r();
        if (null == r) {
            return Collections.emptyList();
        }
        Collection<?> values = (Collection<?>) r.get("values");
        if (null == values || values.isEmpty()) {
            return Collections.emptyList();
        }
        List<InetSocketAddress> addressList = new ArrayList<>(values.size());
        for (Object value : values) {
            byte[] addr = (byte[]) value;
            int addrLength = 6;
            if (addr.length != addrLength) {
                return Collections.emptyList();
            }

            try {
                InetAddress ip = InetAddress.getByAddress(Arrays.copyOf(addr, 4));
                int port = Byte.toUnsignedInt(addr[4]) << 8 | Byte.toUnsignedInt(addr[5]);
                addressList.add(new InetSocketAddress(ip, port));
            } catch (UnknownHostException e) {
                return Collections.emptyList();
            }
        }
        return addressList;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DhtMessage.class.getSimpleName() + "[", "]")
                .add("address=" + address)
                .add("msg=" + Bencode.toString(data))
                .toString();
    }
}
