package cc.aguesuka.btfind.dht.model;

import cc.aguesuka.btfind.util.ByteUtil;
import cc.aguesuka.btfind.util.HexUtil;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.StringJoiner;

/**
 * Immutable
 *
 * @author :yangmingyuxing
 * 2020/2/15 10:36
 */
public final class DhtNode {
    private final SocketAddress address;
    private final byte[] id;

    public DhtNode(SocketAddress address, byte[] id) {
        InetSocketAddress addr = (InetSocketAddress) address;
        this.address = new InetSocketAddress(addr.getAddress(), addr.getPort());
        this.id = id.clone();
    }

    public SocketAddress getAddress() {
        return address;
    }

    public byte[] getId() {
        return id.clone();
    }

    public boolean equalsId(byte[] id) {
        return Arrays.equals(id, this.id);
    }

    public byte[] xor(byte[] id) {
        return ByteUtil.xor(this.id, id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DhtNode.class.getSimpleName() + "[", "]")
                .add("address=" + address)
                .add("id=" + HexUtil.encode(id))
                .toString();
    }
}
