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

    private static BencodeMap getResponse(BencodeMap re, BencodeMap query) {
        BencodeMap clone = (BencodeMap) baseResponse.clone();
        clone.put(T, query.get(T));
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
                    onPing(inetSocketAddress, data);
                    break;
                case ANNOUNCE_PEER:
                    onAnnouncePeer(inetSocketAddress, data);
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

    private void sendTo(InetSocketAddress inetSocketAddress, BencodeMap data) throws IOException {
        publisher.send(inetSocketAddress, data);
    }

    private void onGetPeers(InetSocketAddress inetSocketAddress, BencodeMap data) throws IOException {
        logger.warning("监听到种子GetPeers:" + data);
        BencodeMap re = new BencodeMap() {{
            putByteArray(NODES, nodeWith(data, INFO_HASH));
            putByteArray(TOKEN, makeToken());
            putByteArray(ID, getSelfId());
        }};
        BencodeMap response = getResponse(re, data);
        sendTo(inetSocketAddress, response);
    }

    private void onFindNode(InetSocketAddress inetSocketAddress, BencodeMap data) throws IOException {
        BencodeMap re = new BencodeMap() {{
            putByteArray(NODES, nodeWith(data, TARGET));
            putByteArray(ID, getSelfId());
        }};
        BencodeMap response = getResponse(re, data);
        sendTo(inetSocketAddress, response);
    }

    private void onPing(InetSocketAddress inetSocketAddress, BencodeMap data) throws IOException {
        BencodeMap re = new BencodeMap() {{
            putByteArray(ID, getSelfId());
        }};
        BencodeMap response = getResponse(re, data);
        sendTo(inetSocketAddress, response);
    }

    private void onAnnouncePeer(InetSocketAddress inetSocketAddress, BencodeMap data) throws IOException {
        logger.warning("监听到种子AnnouncePeer:" + data);
        onPing(inetSocketAddress, data);
    }

    private void onException(InetSocketAddress inetSocketAddress, BencodeMap data, Exception e) {
        logger.warning("收到错误回复" + inetSocketAddress.toString() + data);
        logger.log(Level.WARNING, e.getMessage(), e);
    }
}
