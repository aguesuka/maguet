package cc.aguesuka.dht.token;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author :yangmingyuxing
 * 2019/6/11 18:04
 */
public class KrpcToken {
    public static final String T = "t";
    public static final String T_VALUE = "aa";
    public static final String Y = "y";
    public static final String VERSION = "v";
    public static final String QUERY = "q";
    public static final String A = "a";
    public static final String ID = "id";
    public static final String TARGET = "target";
    public static final String INFO_HASH = "info_hash";
    public static final String TOKEN = "token";
    public static final String NODES = "nodes";
    public static final String PING = "ping";
    public static final String VALUES = "values";
    public static final String FIND_NODE = "find_node";
    public static final String GET_PEERS = "get_peers";
    public static final String ANNOUNCE_PEER = "announce_peer";
    public static final String SAMPLE_INFOHASHES = "sample_infohashes";
    private static final Charset ASCII = StandardCharsets.US_ASCII;
    public static final byte[] QUERY_TYPE = "q".getBytes(ASCII);
    public static final byte[] RETURN = "r".getBytes(ASCII);
    public static final String R = "r";

}
