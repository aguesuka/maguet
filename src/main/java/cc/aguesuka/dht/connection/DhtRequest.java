package cc.aguesuka.dht.connection;

import cc.aguesuka.util.bencode.BencodeMap;

import static cc.aguesuka.dht.token.KrpcToken.*;

/**
 * @author :yangmingyuxing
 * 2019/7/7 19:59
 */
public class DhtRequest {
    public static BencodeMap ping(byte[] selfId) {
        return new BencodeMap() {{
            put(A, new BencodeMap() {{
                putByteArray(ID, selfId);
            }});
            putString(QUERY, PING);
            putString(T, T_VALUE);
            putString(Y, QUERY);
        }};
    }

    public static BencodeMap findNode(byte[] selfId, byte[] target) {
        return new BencodeMap() {{
            put(A, new BencodeMap() {{
                putByteArray(ID, selfId);
                putByteArray(TARGET, target);
            }});
            putString(QUERY, FIND_NODE);
            putString(T, T_VALUE);
            putString(Y, QUERY);
        }};
    }

    public static BencodeMap getPeer(byte[] selfId, byte[] target) {
        return new BencodeMap() {{
            put(A, new BencodeMap() {{
                putByteArray(ID, selfId);
                putByteArray(INFO_HASH, target);
            }});
            putString(QUERY, GET_PEERS);
            putString(T, T_VALUE);
            putString(Y, QUERY);
        }};
    }

    public static BencodeMap sampleInfoHashes(byte[] selfId, byte[] target) {
        return new BencodeMap() {{
            put(A, new BencodeMap() {{
                putByteArray(ID, selfId);
                putByteArray(INFO_HASH, target);
            }});
            putString(QUERY, SAMPLE_INFOHASHES);
            putString(T, T_VALUE);
            putString(Y, QUERY);
        }};
    }
}
