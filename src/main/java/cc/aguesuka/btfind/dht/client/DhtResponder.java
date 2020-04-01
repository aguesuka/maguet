package cc.aguesuka.btfind.dht.client;

import cc.aguesuka.btfind.dht.model.DhtMessage;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 * send response when DhtClient recv query.
 *
 * @author :yangmingyuxing
 * 2020/2/13 11:01
 */
public class DhtResponder implements DhtListener {
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final byte[] selfNodeId;
    private final Random random = new SecureRandom();
    private Consumer<DhtMessage> responseSender;

    public DhtResponder(byte[] selfNodeId) {
        this.selfNodeId = selfNodeId;
    }

    private static DhtMessage baseResponse(DhtMessage src, Map<String, Object> r) {
        HashMap<String, Object> result = new HashMap<>(3);
        result.put("r", r);
        result.put("t", src.getData().get("t"));
        result.put("y", "r");
        return new DhtMessage(src.getAddress(), result);
    }

    private static byte[] nodes() {
        return EMPTY_BYTES;
    }

    private Map<String, Object> singletonIdMap() {
        return Collections.singletonMap("id", selfNodeId);
    }

    private Map<String, Object> idMap(int size) {
        Map<String, Object> idMap = new HashMap<>(size);
        idMap.put("id", selfNodeId);
        return idMap;
    }

    private byte[] nextToken() {
        byte[] token = new byte[2];
        random.nextBytes(token);
        return token;
    }

    @Override
    public void recvPing(DhtMessage msg) {
        responseSender.accept(baseResponse(msg, singletonIdMap()));
    }

    @Override
    public void recvFindNode(DhtMessage msg) {
        Map<String, Object> idMap = idMap(2);
        idMap.put("nodes", nodes());
        responseSender.accept(baseResponse(msg, idMap));
    }


    @Override
    public void recvGetPeers(DhtMessage msg) {
        Map<String, Object> idMap = idMap(3);
        idMap.put("nodes", nodes());
        idMap.put("token", nextToken());
        responseSender.accept(baseResponse(msg, idMap));
    }


    @Override
    public void recvAnnouncePeer(DhtMessage msg) {
        responseSender.accept(baseResponse(msg, singletonIdMap()));
    }

    @Override
    public void setResponseSender(Consumer<DhtMessage> responseSender) {
        this.responseSender = responseSender;
    }
}
