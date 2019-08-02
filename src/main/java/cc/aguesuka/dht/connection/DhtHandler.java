package cc.aguesuka.dht.connection;

import cc.aguesuka.util.HexUtil;
import cc.aguesuka.util.bencode.BencodeByteArray;
import cc.aguesuka.util.bencode.BencodeMap;
import cc.aguesuka.util.inject.annotation.Inject;
import cc.aguesuka.util.log.LogSetting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static cc.aguesuka.dht.token.KrpcToken.*;

/**
 * @author :yangmingyuxing
 * 2019/7/6 17:56
 */
public class DhtHandler {
    private static Logger logger = Logger.getLogger(LogSetting.DEFAULT_NAME);
    private static BencodeMap baseResponse = new BencodeMap() {
        {
            putByteArray(T, "aa".getBytes(StandardCharsets.US_ASCII));
            putByteArray(VERSION, HexUtil.decode("4C540011"));
            putByteArray(Y, RETURN);
        }
    };
    @Inject
    private
    DhtConnection<InetSocketAddress, BencodeMap> publisher;
    @Inject
    private
    Bucket<BencodeByteArray, InetSocketAddress> bucket;

    private static BencodeMap getResponse(BencodeMap re) {
        BencodeMap clone = (BencodeMap) baseResponse.clone();
        clone.put("r", re);
        return clone;
    }

    private byte[] makeToken() {
        return HexUtil.decode("48B6B30148B6B301");
    }

    private byte[] getSelfId() {
        return bucket.selfId;
    }

    void onRequest(InetSocketAddress inetSocketAddress, BencodeMap data) {
        try {
            String q = new String(data.getByteArray(QUERY), StandardCharsets.US_ASCII);
            switch (q) {
                case FIND_NODE:
                    onFindNode(inetSocketAddress, data);
                    break;
                case GET_PEERS:
                    onGetPeers(inetSocketAddress, data);
                    break;
                case PING:
                    onPing(inetSocketAddress);
                    break;
                case ANNOUNCE_PEER:
                    onAnnouncePeer(inetSocketAddress);
                    break;
                default:
                    throw new RuntimeException("token error");
            }
            BencodeByteArray id = ((BencodeByteArray) data.getBencodeMap(A).get(ID));
            bucket.update(id, inetSocketAddress, true);
        } catch (RuntimeException | IOException e) {
            onException(inetSocketAddress, data, e);
        }

    }

    private byte[] nodeWith(BencodeMap data, String key) throws IOException {
        List<Bucket.Node<BencodeByteArray, InetSocketAddress>> nodes = bucket.get((BencodeByteArray)
                data.getBencodeMap(A).get(key), 8);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Bucket.Node<BencodeByteArray, InetSocketAddress> node : nodes) {
            outputStream.write(node.getT().getBytes());
            InetSocketAddress socketAddress = node.getData();
            byte[] address = socketAddress.getAddress().getAddress();
            outputStream.write(address);
            int port = socketAddress.getPort();
            outputStream.write(port >> 4);
            outputStream.write(port);
        }
        return outputStream.toByteArray();
    }

    private void sendTo(InetSocketAddress InetSocketAddress, BencodeMap data) throws IOException {
        publisher.send(InetSocketAddress, data);
    }

    private void onGetPeers(InetSocketAddress InetSocketAddress, BencodeMap data) throws IOException {

        BencodeMap re = new BencodeMap() {{
            putByteArray(NODES, nodeWith(data, INFO_HASH));
            putByteArray(TOKEN, makeToken());
            putByteArray(ID, getSelfId());
        }};
        BencodeMap response = getResponse(re);
        sendTo(InetSocketAddress, response);
    }

    private void onFindNode(InetSocketAddress InetSocketAddress, BencodeMap data) throws IOException {
        BencodeMap re = new BencodeMap() {{
            putByteArray(NODES, nodeWith(data, TARGET));
            putByteArray(ID, getSelfId());
        }};
        BencodeMap response = getResponse(re);
        sendTo(InetSocketAddress, response);
    }

    private void onPing(InetSocketAddress InetSocketAddress) throws IOException {
        BencodeMap re = new BencodeMap() {{
            putByteArray(ID, getSelfId());
        }};
        BencodeMap response = getResponse(re);
        sendTo(InetSocketAddress, response);
    }

    private void onAnnouncePeer(InetSocketAddress InetSocketAddress) throws IOException {
        onPing(InetSocketAddress);
    }

    private void onException(InetSocketAddress InetSocketAddress, BencodeMap data, Exception e) {
        logger.log(Level.WARNING, e.getMessage(), e);
    }
}
